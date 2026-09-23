"""Generate BannerBeasts v2 unit cards (poker size, 63.5x88.9mm portrait).

Cards are for tier 2+ units (tier 1 is chit-only and currently has no
Weapon/Armour data in the sheet anyway). Reuses the chit generator's data
loading, icon mapping, ability-stacking and faction-icon logic so the two
stay in sync - see ../chits/generate_chits.py.

Layout top to bottom: unit name, a reserved gap for artwork, weapon +
armour icons, then a trait row identical in spirit to a chit (icons with
stack numbers underneath). Faction icon fixed bottom-right, same as chits.

Usage: identical CLI to generate_chits.py (-f/-t, or the compact
digit+letter spec with repeat-for-copies and tier 0 = blank card):
    python generate_cards.py                # every tier 2+ unit
    python generate_cards.py -f g -t 2       # Gobbo tier 2 only
    python generate_cards.py -12ug           # tier1 x1 + tier2 x2, Boneborn+Gobbo

Unlike chits (one page per faction), card sheets are COMBINED - all
matched cards pack continuously across shared A4 sheets, 9-up (3x3).
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / "chits"))
import generate_chits as gc  # noqa: E402

ROOT_DIR = Path(__file__).parent.parent
OUTPUT_DIR = Path(__file__).parent / "output"

WEAPON_ICON_MAP = {
    "Blunt": "blunt.svg",
    "Sword": "sword.svg",
    "Axe": "axe.svg",
    "Claw": "claw.svg",
    "Spear": "spear.svg",
    "Blank": "blank-weapon.svg",
    "-Gobbo - Bomb": "bombs.svg",
    "-Undead-Scythe": "scythe.svg",
}
ARMOUR_ICON_MAP = {
    "Heavy": "heavy-armour.svg",
    "Medium": "medium-armour.svg",
    "Light": "light-armour.svg",
    "Unarmoured": "blank-armour.svg",
}

# ---- card geometry (mm; viewBox 1 unit = 1mm) - poker size, portrait ----
CARD_W = 63.5
CARD_H = 88.9
BORDER_W = gc.BORDER_W

TITLE_BAND_H = 11.0
NAME_Y = 7.5
NAME_FONT_SIZE = 5.5

ART_BOTTOM = 50.0  # art area runs from the title divider down to here, full width

LOADOUT_ICON_SIZE = 16.0
LOADOUT_TOP = ART_BOTTOM - LOADOUT_ICON_SIZE / 2  # straddles the art border, half overlapping
LOADOUT_GAP = 7.0

TRAIT_ICON_SIZE = 11.0
TRAIT_TOP = LOADOUT_TOP + LOADOUT_ICON_SIZE + 3.0
TRAIT_GAP = 1.5
TRAIT_NUMBER_FONT_SIZE = 3.6
TRAIT_NUMBER_Y = TRAIT_TOP + TRAIT_ICON_SIZE + 3.0
MAX_TRAIT_ICONS = gc.MAX_ICONS

FACTION_ICON_SIZE = 12.0
FACTION_ICON_MARGIN = 0.6

# ---- A4 sheet, 9-up (mm) ----
A4_W = gc.A4_W
A4_H = gc.A4_H
SHEET_MIN_MARGIN = 5
SHEET_COLS = int((A4_W - 2 * SHEET_MIN_MARGIN) // CARD_W)
SHEET_ROWS = int((A4_H - 2 * SHEET_MIN_MARGIN) // CARD_H)
SHEET_MARGIN_X = (A4_W - SHEET_COLS * CARD_W) / 2
SHEET_MARGIN_Y = (A4_H - SHEET_ROWS * CARD_H) / 2
CARDS_PER_SHEET = SHEET_COLS * SHEET_ROWS


ICON_CORNER_RATIO = 3.4285712 / 16  # matches the icon art's own rounded-square background


def render_icon_or_placeholder(
    icon_file: str | None, label: str, x: float, y: float, size: float, uid: str, bordered: bool = False,
) -> str:
    if icon_file and (ROOT_DIR / icon_file).exists():
        svg = gc.load_icon_fragment(icon_file, x, y, size, uid)
        if bordered:
            rx = size * ICON_CORNER_RATIO
            svg += (
                f'<rect x="{x}" y="{y}" width="{size}" height="{size}" rx="{rx}" '
                f'fill="none" stroke="black" stroke-width="0.4"/>'
            )
        return svg
    return (
        f'<rect x="{x}" y="{y}" width="{size}" height="{size}" '
        f'fill="none" stroke="red" stroke-dasharray="0.5,0.5"/>'
        f'<text x="{x+size/2}" y="{y+size/2}" font-size="1.6" '
        f'text-anchor="middle" fill="red">{label}?</text>'
    )


def render_card(
    name: str, unit_name: str | None, weapon, armour, raw_slots: list,
    tier: int | None = None, faction_icon_b64: str | None = None,
) -> str:
    tier_color = gc.TIER_COLORS.get(tier)

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{CARD_W}mm" height="{CARD_H}mm" viewBox="0 0 {CARD_W} {CARD_H}">',
        f'<rect x="0" y="0" width="{CARD_W}" height="{CARD_H}" fill="white"/>',
    ]
    if tier_color:
        parts.append(f'<rect x="0" y="0" width="{CARD_W}" height="{TITLE_BAND_H}" fill="{tier_color}"/>')
        parts.append(f'<line x1="0" y1="{TITLE_BAND_H}" x2="{CARD_W}" y2="{TITLE_BAND_H}" stroke="black" stroke-width="0.3"/>')
    parts.append(
        f'<rect x="{BORDER_W/2}" y="{BORDER_W/2}" width="{CARD_W-BORDER_W}" height="{CARD_H-BORDER_W}" '
        f'fill="none" stroke="black" stroke-width="{BORDER_W}"/>'
    )

    # name
    display_name = unit_name or "-"
    parts.append(
        f'<text x="{CARD_W/2}" y="{NAME_Y}" font-size="{NAME_FONT_SIZE}" font-family="sans-serif" '
        f'font-weight="bold" text-anchor="middle" fill="black">{display_name}</text>'
    )

    # art area: uncoloured, full width, only the bottom edge drawn
    # (top/left/right are already implied by the title divider and card border)
    parts.append(
        f'<line x1="0" y1="{ART_BOTTOM}" x2="{CARD_W}" y2="{ART_BOTTOM}" stroke="#cccccc" stroke-width="0.3"/>'
    )

    # weapon + armour
    loadout_w = 2 * LOADOUT_ICON_SIZE + LOADOUT_GAP
    lx = (CARD_W - loadout_w) / 2
    parts.append(render_icon_or_placeholder(
        WEAPON_ICON_MAP.get(weapon), str(weapon), lx, LOADOUT_TOP, LOADOUT_ICON_SIZE, f"{name}_weapon", bordered=True))
    parts.append(render_icon_or_placeholder(
        ARMOUR_ICON_MAP.get(armour), str(armour), lx + LOADOUT_ICON_SIZE + LOADOUT_GAP, LOADOUT_TOP,
        LOADOUT_ICON_SIZE, f"{name}_armour", bordered=True))

    # traits, same rendering rules as a chit
    slots = gc.build_slots(raw_slots)
    n = len(slots)
    if n > MAX_TRAIT_ICONS:
        raise ValueError(f"{name}: {n} trait slots exceeds MAX_TRAIT_ICONS={MAX_TRAIT_ICONS}")
    group_w = n * TRAIT_ICON_SIZE + (n - 1) * TRAIT_GAP if n else 0
    start_x = (CARD_W - group_w) / 2
    for i, (ability, label) in enumerate(slots):
        x = start_x + i * (TRAIT_ICON_SIZE + TRAIT_GAP)
        parts.append(render_icon_or_placeholder(
            gc.ICON_MAP.get(ability), ability, x, TRAIT_TOP, TRAIT_ICON_SIZE, f"{name}_trait_{i}"))
        if label:
            cx = x + TRAIT_ICON_SIZE / 2
            parts.append(
                f'<text x="{cx}" y="{TRAIT_NUMBER_Y}" font-size="{TRAIT_NUMBER_FONT_SIZE}" '
                f'font-family="sans-serif" font-weight="bold" text-anchor="middle" fill="black">{label}</text>'
            )

    # faction icon, fixed bottom-right
    if faction_icon_b64:
        fx = CARD_W - FACTION_ICON_MARGIN - FACTION_ICON_SIZE
        fy = CARD_H - FACTION_ICON_MARGIN - FACTION_ICON_SIZE
        parts.append(
            f'<image x="{fx}" y="{fy}" width="{FACTION_ICON_SIZE}" height="{FACTION_ICON_SIZE}" '
            f'xlink:href="data:image/png;base64,{faction_icon_b64}"/>'
        )

    parts.append("</svg>")
    return "".join(parts)


def render_sheet_page(chunk: list[Path]) -> str:
    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{A4_W}mm" height="{A4_H}mm" viewBox="0 0 {A4_W} {A4_H}">',
        f'<rect width="{A4_W}" height="{A4_H}" fill="white"/>',
    ]
    for i, path in enumerate(chunk):
        col, row = i % SHEET_COLS, i // SHEET_COLS
        x = SHEET_MARGIN_X + col * CARD_W
        y = SHEET_MARGIN_Y + row * CARD_H
        parts.append(gc.embed_svg(path.read_text(encoding="utf-8"), x, y, CARD_W, CARD_H))
    parts.append("</svg>")
    return "".join(parts)


def build_combined_sheets(paths: list[Path]) -> list[Path]:
    """Cards, unlike chits, pack continuously across shared sheets
    regardless of faction - 9 per A4 page, no forced page breaks."""
    pages = []
    for page_num in range(0, len(paths), CARDS_PER_SHEET):
        chunk = paths[page_num:page_num + CARDS_PER_SHEET]
        out_path = OUTPUT_DIR / f"sheet{page_num // CARDS_PER_SHEET + 1}.svg"
        out_path.write_text(render_sheet_page(chunk), encoding="utf-8")
        pages.append(out_path)
    return pages


def build_preview_html(svg_paths: list[Path], out_path: Path) -> None:
    parts = ['<html><body style="background:#888;padding:20px;font-family:sans-serif;">']
    for p in svg_paths:
        svg = p.read_text(encoding="utf-8")
        svg = svg.replace(f'width="{CARD_W}mm" height="{CARD_H}mm"', 'width="317" height="444"')
        parts.append(f'<div style="display:inline-block;vertical-align:top;margin:10px;">'
                      f'<div style="color:white;">{p.stem}</div>{svg}</div>')
    parts.append("</body></html>")
    out_path.write_text("".join(parts), encoding="utf-8")


def main():
    parser = gc.argparse.ArgumentParser(description="Generate BannerBeasts unit cards")
    parser.add_argument("spec", nargs="?", default=None)
    parser.add_argument("-f", "--faction", default=None)
    parser.add_argument("-t", "--tier", nargs="+", type=int, default=None)

    raw_args = sys.argv[1:]
    if len(raw_args) == 1 and raw_args[0] not in ("-h", "--help") and re.fullmatch(r"-[0-9a-zA-Z]+", raw_args[0]):
        raw_args = [raw_args[0][1:]]
    args = parser.parse_args(raw_args)

    if args.spec:
        faction_names, tier_copies = gc.parse_spec(args.spec, parser.error)
    else:
        faction_names = None
        if args.faction:
            unknown = [ch for ch in args.faction if ch not in gc.FACTION_CODES]
            if unknown:
                parser.error(f"unknown faction code(s): {''.join(unknown)}")
            faction_names = {gc.FACTION_CODES[ch] for ch in args.faction}
        tier_copies = {t: 1 for t in args.tier} if args.tier else None

    units = gc.load_units(faction_names, tier_copies)
    if not units:
        print("no units matched")
        return

    icon_names = gc.load_faction_icon_names()
    OUTPUT_DIR.mkdir(exist_ok=True)
    written = []
    for u in units:
        if u["unit"]:
            unit_slug = gc.slugify(u["unit"])
        elif u["row"] is not None:
            unit_slug = f'row{u["row"]}'
        else:
            unit_slug = "blank"
        base_name = f'{gc.slugify(u["faction"])}_{unit_slug}'
        faction_icon_b64 = gc.get_faction_icon_b64(u["faction"], icon_names)
        for copy_idx in range(1, u["copies"] + 1):
            card_name = base_name if u["copies"] == 1 else f"{base_name}-copy{copy_idx}"
            try:
                svg = render_card(
                    card_name, u["unit"], u["weapon"], u["armour"], u["raw_slots"],
                    tier=u["tier"], faction_icon_b64=faction_icon_b64,
                )
            except ValueError as e:
                print(f"SKIP {card_name}: {e}")
                continue
            out_path = OUTPUT_DIR / f"{card_name}.svg"
            out_path.write_text(svg, encoding="utf-8")
            written.append(out_path)
            print(f"wrote {out_path}")

    if written:
        preview_path = OUTPUT_DIR / "preview.html"
        build_preview_html(written, preview_path)
        print(f"wrote {preview_path}")

        sheets = build_combined_sheets(written)
        for s in sheets:
            print(f"wrote {s}")
        print(f"{len(written)} cards -> {len(sheets)} A4 sheet(s), {CARDS_PER_SHEET} per sheet ({SHEET_COLS}x{SHEET_ROWS})")

        pdf_path = OUTPUT_DIR / "cards.pdf"
        gc.build_combined_pdf(sheets, pdf_path)
        print(f"wrote {pdf_path}")


if __name__ == "__main__":
    main()
