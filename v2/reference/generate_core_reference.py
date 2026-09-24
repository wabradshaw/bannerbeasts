"""Generate the BannerBeasts v2 "default things" core reference sheet:
the General-type weapons/armour/abilities every faction shares, which
the per-faction sheets (generate_reference.py) deliberately leave out.

A5 portrait, one page, two parts:
  - Top: weapons & armour, 3 columns - armour tiers, then General
    weapons chunked 4-per-column (so 6 weapons split 4+2).
  - Bottom: General-type abilities, a grid of 3 columns as many rows as
    needed (15 abilities today -> 5 rows), in Powers-tab sheet order.

All content is pulled straight from the Powers tab's General-type rows,
so it updates automatically if that list changes - no hardcoded names.

Usage: python generate_core_reference.py
Writes output/core.svg and output/core.pdf.
"""
import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / "chits"))
sys.path.insert(0, str(Path(__file__).parent.parent / "cards"))
import generate_chits as gc  # noqa: E402
import generate_cards as cc  # noqa: E402
from generate_reference import wrap_text  # noqa: E402

OUTPUT_DIR = Path(__file__).parent / "output"

# ---- page geometry (mm) - A5 portrait ----
PAGE_W, PAGE_H = 148.0, 210.0
MARGIN = 6.0
TITLE_FONT_SIZE = 5.5
TITLE_BASELINE_Y = MARGIN + 5
HEADER_LINE_Y = TITLE_BASELINE_Y + 3  # clear of the title's descenders
HEADER_H = HEADER_LINE_Y + 1

SECTION_FONT_SIZE = 3.6
SECTION_GAP = 5.0

COLS = 3
COL_GAP = 3.0
COL_W = (PAGE_W - 2 * MARGIN - (COLS - 1) * COL_GAP) / COLS
COL_X = [MARGIN + i * (COL_W + COL_GAP) for i in range(COLS)]

ICON_SIZE = 8.0
TEXT_GAP = 1.5
TEXT_WIDTH = COL_W - ICON_SIZE - TEXT_GAP
NAME_FONT_SIZE = 2.9
NAME_LINE_H = 3.3
DESC_FONT_SIZE = 2.5
DESC_LINE_H = 2.9
ROW_GAP = 1.8

# fixed row height = 1 name line + 2 description lines - a floor, not a
# cap; see compute_uniform_row_height(). Every row on the page (both
# sections) ends up this same height, or taller if content demands it.
FIXED_ENTRY_H = NAME_LINE_H + 2 * DESC_LINE_H + ROW_GAP


def load_general_items() -> tuple[list[dict], list[dict], list[dict]]:
    wb_ = gc.openpyxl.load_workbook(gc.WORKBOOK_PATH, data_only=True)
    ws = wb_["Powers"]
    headers = [c.value for c in ws[1]]
    idx = {h: i for i, h in enumerate(headers)}
    weapons, armours, skills = [], [], []
    for row in ws.iter_rows(min_row=2):
        v = [c.value for c in row]
        if v[idx["Weapons"]] and v[idx["Weapon Type"]] == "General":
            weapons.append({"name": v[idx["Weapons"]], "desc": v[idx["Weapon Description"]] or "",
                             "icon_file": cc.WEAPON_ICON_MAP.get(v[idx["Weapons"]])})
        if v[idx["Armour"]] and v[idx["Armour Type"]] == "General":
            armours.append({"name": v[idx["Armour"]], "desc": v[idx["Armour Description"]] or "",
                             "icon_file": cc.ARMOUR_ICON_MAP.get(v[idx["Armour"]])})
        sname = v[idx["Skills"]]
        if sname and isinstance(sname, str) and v[idx["Skill Type"]] == "General":
            skills.append({"name": sname, "desc": v[idx["Skill Description"]] or "",
                            "icon_file": gc.ICON_MAP.get(sname)})
    return weapons, armours, skills


def chunk(items: list, size: int) -> list[list]:
    return [items[i:i + size] for i in range(0, len(items), size)]


def find_target_armour(weapon: dict, armours: list[dict]) -> dict | None:
    """Which armour tier a weapon's own description says it's good
    against - matched from the text itself (e.g. "Good against Heavy"),
    not by row position, so it stays correct if the two lists are ever
    reordered independently. Claw's "Automatically hits Unarmoured"
    counts too, since that's the same relationship in different words."""
    for armour in armours:
        if re.search(rf'\b{re.escape(armour["name"])}\b', weapon["desc"]):
            return armour
    return None


ARROW_COLOR = "#444444"


def render_arrow(x1: float, y1: float, x2: float, y2: float, size: float = ICON_SIZE) -> str:
    """A single bold arrow silhouette (rectangular shaft + triangular
    head), as tall as an icon at its head, from (x1,y1) to (x2,y2) - one
    filled polygon, no <marker> def, kept as a plain shape for PDF-export
    safety (see generate_chits.py for why: MuPDF's SVG support is
    narrower than a browser's)."""
    angle = math.atan2(y2 - y1, x2 - x1)
    length = math.hypot(x2 - x1, y2 - y1)
    shaft_w, head_w, head_len = size * 0.35, size, size * 0.7
    shaft_len = max(0.0, length - head_len)
    local = [
        (0, -shaft_w / 2), (shaft_len, -shaft_w / 2), (shaft_len, -head_w / 2),
        (length, 0),
        (shaft_len, head_w / 2), (shaft_len, shaft_w / 2), (0, shaft_w / 2),
    ]
    cos_a, sin_a = math.cos(angle), math.sin(angle)
    pts = [(x1 + px * cos_a - py * sin_a, y1 + px * sin_a + py * cos_a) for px, py in local]
    points = " ".join(f"{x:.2f},{y:.2f}" for x, y in pts)
    return f'<polygon points="{points}" fill="{ARROW_COLOR}"/>'


def render_icon_or_dot(icon_file: str | None, x: float, y: float, size: float, uid: str) -> str:
    if icon_file and (gc.ROOT_DIR / icon_file).exists():
        return gc.load_icon_fragment(icon_file, x, y, size, uid)
    return (f'<rect x="{x}" y="{y}" width="{size}" height="{size}" rx="1.5" '
            f'fill="none" stroke="red" stroke-dasharray="0.4,0.4"/>')


def entry_lines(entry: dict) -> tuple[list[str], list[str]]:
    return wrap_text(entry["name"], TEXT_WIDTH, NAME_FONT_SIZE), wrap_text(entry["desc"], TEXT_WIDTH, DESC_FONT_SIZE)


def natural_height(entry: dict) -> float:
    name_lines, desc_lines = entry_lines(entry)
    return len(name_lines) * NAME_LINE_H + len(desc_lines) * DESC_LINE_H + ROW_GAP


def compute_uniform_row_height(entries: list[dict]) -> float:
    """One row height for the whole page: the fixed 3-line height, or
    taller if any entry's actual content needs more (a floor, not a cap).
    There's room on the page for it, so this now also covers Spear's full
    4-line entry in a single row - it no longer needs a double-height
    slot, and neither does anything else, since Spear is the tallest."""
    return max([FIXED_ENTRY_H] + [natural_height(e) for e in entries])


def render_entry(entry: dict, x: float, y: float, uid: str) -> str:
    name_lines, desc_lines = entry_lines(entry)
    parts = [render_icon_or_dot(entry["icon_file"], x, y, ICON_SIZE, uid)]
    tx, ty = x + ICON_SIZE + TEXT_GAP, y + NAME_FONT_SIZE
    for line in name_lines:
        parts.append(f'<text x="{tx}" y="{ty}" font-size="{NAME_FONT_SIZE}" font-family="sans-serif" '
                      f'font-weight="bold" fill="black">{line}</text>')
        ty += NAME_LINE_H
    for line in desc_lines:
        parts.append(f'<text x="{tx}" y="{ty}" font-size="{DESC_FONT_SIZE}" font-family="sans-serif" '
                      f'fill="#222222">{line}</text>')
        ty += DESC_LINE_H
    return "".join(parts)


def render_weapons_armour(armours: list[dict], weapon_col1: list[dict], weapon_col2: list[dict], row_h: float, top_y: float) -> tuple[str, float]:
    """All three columns are one entry per row, all rows the same fixed
    height `row_h` (shared with the abilities grid below, so every row on
    the page is the same size) - row_h is generous enough to fit even
    Spear's 4-line entry in a single row, so no column needs a taller
    double-height slot to keep pace with the others."""
    n_rows = max(len(armours), len(weapon_col1), len(weapon_col2))
    row_y = [top_y + i * row_h for i in range(n_rows + 1)]

    parts = []
    for col_i, (label, items) in enumerate((("armour", armours), ("weapon1", weapon_col1), ("weapon2", weapon_col2))):
        for i, entry in enumerate(items):
            parts.append(render_entry(entry, COL_X[col_i], row_y[i], f"core_{label}_{i}"))

    # weapon -> armour "good against" arrows, drawn last so they sit on
    # top of the text they cross (only weapon_col1 entries have a
    # matching armour in their description; Spear/Blank in weapon_col2
    # don't, so find_target_armour naturally skips them)
    for i, weapon in enumerate(weapon_col1):
        target = find_target_armour(weapon, armours)
        if target is None:
            continue
        # a half-icon gap before the weapon icon, then one icon-width of
        # arrow pointing left - a compact, detached marker rather than a
        # line spanning (and covering) the text in between
        y = row_y[i] + ICON_SIZE / 2
        x1 = COL_X[1] - ICON_SIZE / 2
        parts.append(render_arrow(x1, y, x1 - ICON_SIZE, y))

    return "".join(parts), row_y[-1]


def render_skill_grid(skills: list[dict], row_h: float, top_y: float) -> str:
    rows = chunk(skills, COLS)
    parts = []
    y = top_y
    for row in rows:
        for col_i, entry in enumerate(row):
            parts.append(render_entry(entry, COL_X[col_i], y, f"core_skill_{y:.1f}_{col_i}"))
        y += row_h
    return "".join(parts)


def main():
    weapons, armours, skills = load_general_items()
    weapon_cols = chunk(weapons, 4)  # user spec: 4 weapons in col B, rest spill to col C
    weapon_col1 = weapon_cols[0] if weapon_cols else []
    weapon_col2 = weapon_cols[1] if len(weapon_cols) > 1 else []

    # one row height for the entire page - every row, top section and
    # bottom, is exactly this tall
    row_h = compute_uniform_row_height(armours + weapon_col1 + weapon_col2 + skills)

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{PAGE_W}mm" height="{PAGE_H}mm" viewBox="0 0 {PAGE_W} {PAGE_H}">',
        f'<rect x="0" y="0" width="{PAGE_W}" height="{PAGE_H}" fill="white"/>',
        f'<rect x="{gc.BORDER_W/2}" y="{gc.BORDER_W/2}" width="{PAGE_W-gc.BORDER_W}" height="{PAGE_H-gc.BORDER_W}" '
        f'fill="none" stroke="black" stroke-width="{gc.BORDER_W}"/>',
        f'<text x="{MARGIN}" y="{TITLE_BASELINE_Y}" font-size="{TITLE_FONT_SIZE}" font-family="sans-serif" '
        f'font-weight="bold" fill="black">Core Reference</text>',
        f'<line x1="{MARGIN}" y1="{HEADER_LINE_Y}" x2="{PAGE_W-MARGIN}" y2="{HEADER_LINE_Y}" stroke="black" stroke-width="0.3"/>',
    ]

    y = HEADER_H + 2
    parts.append(f'<text x="{MARGIN}" y="{y+3}" font-size="{SECTION_FONT_SIZE}" font-family="sans-serif" '
                 f'font-weight="bold" fill="black">Weapons &amp; Armour</text>')
    parts.append(f'<line x1="{MARGIN}" y1="{y+3.8}" x2="{PAGE_W-MARGIN}" y2="{y+3.8}" stroke="#999999" stroke-width="0.2"/>')
    y += SECTION_GAP

    top_svg, y = render_weapons_armour(armours, weapon_col1, weapon_col2, row_h, y)
    parts.append(top_svg)

    y += SECTION_GAP - 1
    parts.append(f'<text x="{MARGIN}" y="{y+3}" font-size="{SECTION_FONT_SIZE}" font-family="sans-serif" '
                 f'font-weight="bold" fill="black">Abilities</text>')
    parts.append(f'<line x1="{MARGIN}" y1="{y+3.8}" x2="{PAGE_W-MARGIN}" y2="{y+3.8}" stroke="#999999" stroke-width="0.2"/>')
    y += SECTION_GAP

    parts.append(render_skill_grid(skills, row_h, y))
    parts.append("</svg>")
    svg = "".join(parts)

    OUTPUT_DIR.mkdir(exist_ok=True)
    out_path = OUTPUT_DIR / "core.svg"
    out_path.write_text(svg, encoding="utf-8")
    print(f"wrote {out_path}")

    pdf_path = OUTPUT_DIR / "core.pdf"
    gc.build_combined_pdf([out_path], pdf_path)
    print(f"wrote {pdf_path}")

    # print sheet: 2 copies side by side on landscape A4 (2x148=296mm
    # fits within 297mm width, height matches exactly at 210mm) - cut
    # or fold down the middle for two A5 copies per sheet
    print_w, print_h = gc.A4_H, gc.A4_W  # landscape: 297x210
    margin_x = (print_w - 2 * PAGE_W) / 2
    print_svg = "".join([
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{print_w}mm" height="{print_h}mm" viewBox="0 0 {print_w} {print_h}">',
        f'<rect width="{print_w}" height="{print_h}" fill="white"/>',
        gc.embed_svg(svg, margin_x, 0, PAGE_W, PAGE_H),
        gc.embed_svg(svg, margin_x + PAGE_W, 0, PAGE_W, PAGE_H),
        "</svg>",
    ])
    print_svg_path = OUTPUT_DIR / "core-print-sheet.svg"
    print_svg_path.write_text(print_svg, encoding="utf-8")
    print(f"wrote {print_svg_path}")

    print_pdf_path = OUTPUT_DIR / "core-print.pdf"
    gc.build_combined_pdf([print_svg_path], print_pdf_path)
    print(f"wrote {print_pdf_path}")


if __name__ == "__main__":
    main()
