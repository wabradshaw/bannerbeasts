"""Generate BannerBeasts v2 per-faction reference sheets.

A6 portrait to start (swap PAGE_W/PAGE_H to A5 - 148x210 - later), two
text columns per page, flowing onto more pages if a faction's content
doesn't fit one. Each entry is an icon + bold name + wrapped description,
pulled from the Powers tab's Weapon/Skill Description columns.

Per faction, three sections, each only shown if non-empty:
  1. Weapons whose Powers-tab Weapon Type is this faction's own type
     label (e.g. Bomb for Gobbo) - faction-exclusive weapons only, not
     the general-purpose ones (Sword, Axe, ...) every faction can use.
  2. Abilities whose Powers-tab Skill Type is this faction's own type
     label (e.g. Burst for Gobbo) - i.e. faction-exclusive abilities.
  3. Abilities whose Skill Type is "Rare" (e.g. Slow, Beserk, Poison) -
     unusual, not faction-locked, but shown only if this faction's
     roster actually uses them.
Abilities with Skill Type "General" (Veteran, Shield, Move, ...) are
assumed common knowledge and left off every faction's sheet entirely.
"Debug" rows (Blank, the bare numbers) are always excluded.

Within each section: sort by how many roster entries use it (most first),
then by Skill Class in the order attack/block("defense")/shoot/move/
leadership/other, then alphabetically. Weapons have no Skill Class, so
they sort by count then name only.

Usage:
    python generate_reference.py            # every faction
    python generate_reference.py -f g       # Gobbo only (same faction
                                              codes as the other generators)

Also writes output/reference.pdf combining every faction's pages.
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / "chits"))
sys.path.insert(0, str(Path(__file__).parent.parent / "cards"))
import generate_chits as gc  # noqa: E402
import generate_cards as cc  # noqa: E402

OUTPUT_DIR = Path(__file__).parent / "output"

CLASS_PRIORITY = {"Attack": 0, "Block": 1, "Shoot": 2, "Move": 3, "Leadership": 4}
CLASS_OTHER = 5

# ---- page geometry (mm) - A6 portrait; swap to (148, 210) for A5 later ----
PAGE_W, PAGE_H = 105.0, 148.0
MARGIN = 4.0
TITLE_FONT_SIZE = 5.0
TITLE_BASELINE_Y = MARGIN + 4
HEADER_LINE_Y = TITLE_BASELINE_Y + 3  # clear of the title's descenders
HEADER_H = HEADER_LINE_Y + 1
FACTION_ICON_SIZE = 8.0

COL_GAP = 3.0
COL_W = (PAGE_W - 2 * MARGIN - COL_GAP) / 2
COL_X = [MARGIN, MARGIN + COL_W + COL_GAP]
CONTENT_TOP = HEADER_H
CONTENT_BOTTOM = PAGE_H - MARGIN

ICON_SIZE = 7.0
TEXT_GAP = 1.5
TEXT_WIDTH = COL_W - ICON_SIZE - TEXT_GAP

SECTION_FONT_SIZE = 3.3
SECTION_HEIGHT = 5.0
NAME_FONT_SIZE = 2.7
NAME_LINE_H = 3.1
DESC_FONT_SIZE = 2.3
DESC_LINE_H = 2.7
ENTRY_GAP = 1.8
CHAR_WIDTH_RATIO = 0.52


def load_powers() -> tuple[dict, dict]:
    wb_ = gc.openpyxl.load_workbook(gc.WORKBOOK_PATH, data_only=True)
    ws = wb_["Powers"]
    headers = [c.value for c in ws[1]]
    idx = {h: i for i, h in enumerate(headers)}
    weapons, skills = {}, {}
    for row in ws.iter_rows(min_row=2):
        v = [c.value for c in row]
        wname = v[idx["Weapons"]]
        if wname:
            weapons[wname] = {"type": v[idx["Weapon Type"]], "desc": v[idx["Weapon Description"]]}
        sname = v[idx["Skills"]]
        if sname and isinstance(sname, str):
            skills[sname] = {"type": v[idx["Skill Type"]], "cls": v[idx["Skill Class"]], "desc": v[idx["Skill Description"]]}
    return weapons, skills


def load_faction_usage() -> tuple[dict, dict, list[str]]:
    """Per faction: Counter of weapon usage, Counter of ability usage
    (each counted once per roster entry, not per stacked repeat), and
    the list of factions in first-seen sheet order."""
    wb_ = gc.openpyxl.load_workbook(gc.WORKBOOK_PATH, data_only=True)
    ws = wb_["U2"]
    headers = [c.value for c in ws[1]]
    idx = {h: i for i, h in enumerate(headers)}
    weapon_count: dict[str, dict[str, int]] = {}
    ability_count: dict[str, dict[str, int]] = {}
    order: list[str] = []
    for row in ws.iter_rows(min_row=2):
        v = [c.value for c in row]
        faction = v[idx["Faction"]]
        if not faction:
            continue
        if faction not in order:
            order.append(faction)
        w = v[idx["Weapon"]]
        if w:
            weapon_count.setdefault(faction, {})[w] = weapon_count.setdefault(faction, {}).get(w, 0) + 1
        seen = {v[idx[c]] for c in ("S1", "S2", "S3", "S4") if isinstance(v[idx[c]], str)}
        for a in seen:
            ability_count.setdefault(faction, {})[a] = ability_count.setdefault(faction, {}).get(a, 0) + 1
    return weapon_count, ability_count, order


def wrap_text(text: str, max_width_mm: float, font_size: float) -> list[str]:
    max_chars = max(1, int(max_width_mm / (font_size * CHAR_WIDTH_RATIO)))
    words = (text or "").split()
    lines, cur = [], ""
    for word in words:
        trial = f"{cur} {word}".strip()
        if len(trial) <= max_chars or not cur:
            cur = trial
        else:
            lines.append(cur)
            cur = word
    if cur:
        lines.append(cur)
    return lines or [""]


def infer_type_label(faction: str, weapons: dict, skills: dict, weapon_count: dict, ability_count: dict) -> str:
    """The Powers tab's Weapon/Skill Type column uses each faction's
    thematic name (e.g. "Undead" for Boneborn), not necessarily its
    display name in Factions/U2 - infer it from whichever non-General
    type label this faction's own roster actually uses, rather than
    hardcoding a lookup that would need updating per new faction."""
    candidates = {
        weapons.get(n, {}).get("type")
        for n in weapon_count.get(faction, {})
        if weapons.get(n, {}).get("type") not in (None, "General")
    } | {
        skills.get(n, {}).get("type")
        for n in ability_count.get(faction, {})
        if skills.get(n, {}).get("type") not in (None, "General", "Rare", "Debug")
    }
    return candidates.pop() if len(candidates) == 1 else faction


def build_sections(faction: str, weapons: dict, skills: dict, weapon_count: dict, ability_count: dict) -> list[tuple[str, list[dict]]]:
    type_label = infer_type_label(faction, weapons, skills, weapon_count, ability_count)

    def class_key(name: str) -> tuple:
        cls = skills.get(name, {}).get("cls")
        return (CLASS_PRIORITY.get(cls, CLASS_OTHER), name)

    w_used = weapon_count.get(faction, {})
    weapon_entries = sorted(
        ({"name": n, "count": c, "desc": weapons.get(n, {}).get("desc") or "", "icon_file": cc.WEAPON_ICON_MAP.get(n)}
         for n, c in w_used.items() if weapons.get(n, {}).get("type") == type_label),
        key=lambda e: (-e["count"], e["name"]),
    )

    a_used = ability_count.get(faction, {})
    faction_abilities = sorted(
        ({"name": n, "count": c, "desc": skills.get(n, {}).get("desc") or "", "icon_file": gc.ICON_MAP.get(n)}
         for n, c in a_used.items() if skills.get(n, {}).get("type") == type_label),
        key=lambda e: (-e["count"], *class_key(e["name"])),
    )
    rare_abilities = sorted(
        ({"name": n, "count": c, "desc": skills.get(n, {}).get("desc") or "", "icon_file": gc.ICON_MAP.get(n)}
         for n, c in a_used.items() if skills.get(n, {}).get("type") == "Rare"),
        key=lambda e: (-e["count"], *class_key(e["name"])),
    )

    sections = [
        ("Weapons", weapon_entries),
        ("Faction Abilities", faction_abilities),
        ("Unusual Abilities", rare_abilities),
    ]
    return [(title, entries) for title, entries in sections if entries]


def render_icon_or_dot(icon_file: str | None, x: float, y: float, size: float, uid: str) -> str:
    if icon_file and (gc.ROOT_DIR / icon_file).exists():
        return gc.load_icon_fragment(icon_file, x, y, size, uid)
    return (
        f'<rect x="{x}" y="{y}" width="{size}" height="{size}" rx="1.5" '
        f'fill="none" stroke="red" stroke-dasharray="0.4,0.4"/>'
    )


def render_pages(faction: str, sections: list[tuple[str, list[dict]]], faction_icon_b64: str | None) -> list[str]:
    # build the flat block list: (kind, height, render_fn)
    blocks = []
    for title, entries in sections:
        blocks.append(("section", SECTION_HEIGHT, title))
        for e in entries:
            name_lines = wrap_text(e["name"], TEXT_WIDTH, NAME_FONT_SIZE)
            desc_lines = wrap_text(e["desc"], TEXT_WIDTH, DESC_FONT_SIZE)
            text_h = len(name_lines) * NAME_LINE_H + len(desc_lines) * DESC_LINE_H
            h = max(ICON_SIZE, text_h) + ENTRY_GAP
            blocks.append(("entry", h, (e, name_lines, desc_lines)))

    pages: list[list[tuple]] = [[]]
    col_idx, y = 0, CONTENT_TOP
    for kind, h, payload in blocks:
        if y + h > CONTENT_BOTTOM:
            col_idx += 1
            y = CONTENT_TOP
            if col_idx >= len(COL_X):
                pages.append([])
                col_idx = 0
        # don't start a page/column with a dangling section header
        if kind == "section" and y + h + ICON_SIZE > CONTENT_BOTTOM and y > CONTENT_TOP:
            col_idx += 1
            y = CONTENT_TOP
            if col_idx >= len(COL_X):
                pages.append([])
                col_idx = 0
        pages[-1].append((kind, COL_X[col_idx], y, payload))
        y += h

    svgs = []
    for page_blocks in pages:
        parts = [
            f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
            f'width="{PAGE_W}mm" height="{PAGE_H}mm" viewBox="0 0 {PAGE_W} {PAGE_H}">',
            f'<rect x="0" y="0" width="{PAGE_W}" height="{PAGE_H}" fill="white"/>',
            f'<rect x="{gc.BORDER_W/2}" y="{gc.BORDER_W/2}" width="{PAGE_W-gc.BORDER_W}" height="{PAGE_H-gc.BORDER_W}" '
            f'fill="none" stroke="black" stroke-width="{gc.BORDER_W}"/>',
            f'<text x="{MARGIN}" y="{TITLE_BASELINE_Y}" font-size="{TITLE_FONT_SIZE}" font-family="sans-serif" '
            f'font-weight="bold" fill="black">{faction}</text>',
            f'<line x1="{MARGIN}" y1="{HEADER_LINE_Y}" x2="{PAGE_W-MARGIN}" y2="{HEADER_LINE_Y}" stroke="black" stroke-width="0.3"/>',
        ]
        if faction_icon_b64:
            fx, fy = PAGE_W - MARGIN - FACTION_ICON_SIZE, MARGIN - 1.0
            parts.append(
                f'<image x="{fx}" y="{fy}" width="{FACTION_ICON_SIZE}" height="{FACTION_ICON_SIZE}" '
                f'xlink:href="data:image/png;base64,{faction_icon_b64}"/>'
            )

        uid_n = 0
        for kind, x, y, payload in page_blocks:
            if kind == "section":
                parts.append(
                    f'<text x="{x}" y="{y+3.5}" font-size="{SECTION_FONT_SIZE}" font-family="sans-serif" '
                    f'font-weight="bold" fill="black">{payload}</text>'
                )
                parts.append(f'<line x1="{x}" y1="{y+4.3}" x2="{x+COL_W}" y2="{y+4.3}" stroke="#999999" stroke-width="0.2"/>')
                continue
            entry, name_lines, desc_lines = payload
            uid_n += 1
            parts.append(render_icon_or_dot(entry["icon_file"], x, y, ICON_SIZE, f"{faction}_{uid_n}"))
            tx = x + ICON_SIZE + TEXT_GAP
            ty = y + NAME_FONT_SIZE
            for line in name_lines:
                parts.append(
                    f'<text x="{tx}" y="{ty}" font-size="{NAME_FONT_SIZE}" font-family="sans-serif" '
                    f'font-weight="bold" fill="black">{line}</text>'
                )
                ty += NAME_LINE_H
            for line in desc_lines:
                parts.append(
                    f'<text x="{tx}" y="{ty}" font-size="{DESC_FONT_SIZE}" font-family="sans-serif" '
                    f'fill="#222222">{line}</text>'
                )
                ty += DESC_LINE_H

        parts.append("</svg>")
        svgs.append("".join(parts))
    return svgs


# ---- A4 print sheet: tile multiple copies of the A6 pages, 2x2 (mm) ----
A4_W, A4_H = gc.A4_W, gc.A4_H
PRINT_COLS = int(A4_W // PAGE_W)
PRINT_ROWS = int(A4_H // PAGE_H)
PRINT_MARGIN_X = (A4_W - PRINT_COLS * PAGE_W) / 2
PRINT_MARGIN_Y = (A4_H - PRINT_ROWS * PAGE_H) / 2
PAGES_PER_PRINT_SHEET = PRINT_COLS * PRINT_ROWS


def build_print_sheets(paths: list[Path], copies: int) -> list[Path]:
    """Tile `copies` copies of each reference page onto A4 sheets, packed
    tight (each page's own border doubles as a cut line)."""
    expanded = [p for p in paths for _ in range(copies)]
    pages = []
    for i in range(0, len(expanded), PAGES_PER_PRINT_SHEET):
        chunk = expanded[i:i + PAGES_PER_PRINT_SHEET]
        parts = [
            f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
            f'width="{A4_W}mm" height="{A4_H}mm" viewBox="0 0 {A4_W} {A4_H}">',
            f'<rect width="{A4_W}" height="{A4_H}" fill="white"/>',
        ]
        for j, path in enumerate(chunk):
            col, row = j % PRINT_COLS, j // PRINT_COLS
            x = PRINT_MARGIN_X + col * PAGE_W
            y = PRINT_MARGIN_Y + row * PAGE_H
            parts.append(gc.embed_svg(path.read_text(encoding="utf-8"), x, y, PAGE_W, PAGE_H))
        parts.append("</svg>")
        out_path = OUTPUT_DIR / f"print-sheet{i // PAGES_PER_PRINT_SHEET + 1}.svg"
        out_path.write_text("".join(parts), encoding="utf-8")
        pages.append(out_path)
    return pages


def main():
    parser = gc.argparse.ArgumentParser(description="Generate BannerBeasts faction reference sheets")
    parser.add_argument("-f", "--faction", default=None, help="faction code letters, e.g. ug for Boneborn+Gobbo")
    parser.add_argument("-c", "--copies", type=int, default=2, help="copies of each page to print per faction (default 2)")
    args = parser.parse_args()

    faction_names = None
    if args.faction:
        unknown = [ch for ch in args.faction if ch not in gc.FACTION_CODES]
        if unknown:
            parser.error(f"unknown faction code(s): {''.join(unknown)}")
        faction_names = {gc.FACTION_CODES[ch] for ch in args.faction}

    weapons, skills = load_powers()
    weapon_count, ability_count, faction_order = load_faction_usage()
    icon_names = gc.load_faction_icon_names()

    OUTPUT_DIR.mkdir(exist_ok=True)
    written: list[Path] = []
    for faction in faction_order:
        if faction_names is not None and faction not in faction_names:
            continue
        sections = build_sections(faction, weapons, skills, weapon_count, ability_count)
        if not sections:
            print(f"SKIP {faction}: nothing to show")
            continue
        faction_icon_b64 = gc.get_faction_icon_b64(faction, icon_names)
        pages = render_pages(faction, sections, faction_icon_b64)
        for i, svg in enumerate(pages, start=1):
            suffix = "" if len(pages) == 1 else f"-{i}"
            out_path = OUTPUT_DIR / f"{gc.slugify(faction)}{suffix}.svg"
            out_path.write_text(svg, encoding="utf-8")
            written.append(out_path)
            print(f"wrote {out_path}")

    if written:
        pdf_path = OUTPUT_DIR / "reference.pdf"
        gc.build_combined_pdf(written, pdf_path)
        print(f"wrote {pdf_path}")

        print_sheets = build_print_sheets(written, args.copies)
        for s in print_sheets:
            print(f"wrote {s}")
        print_pdf_path = OUTPUT_DIR / "reference-print.pdf"
        gc.build_combined_pdf(print_sheets, print_pdf_path)
        print(f"wrote {print_pdf_path}")
        print(f"{len(written)} page(s) x{args.copies} copies -> {len(print_sheets)} A4 sheet(s), "
              f"{PAGES_PER_PRINT_SHEET} per sheet ({PRINT_COLS}x{PRINT_ROWS})")


if __name__ == "__main__":
    main()
