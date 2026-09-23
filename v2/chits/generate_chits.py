"""Generate BannerBeasts v2 ability chits (60mm x 15mm landscape tokens).

Reads the U2 tab of "Bannerbeasts Roller.xlsx" and renders one print-ready
SVG chit per unit. Known "stacking" abilities collapse into a single icon
with a computed number underneath, using either an explicit number the
designer wrote in the next S-column (e.g. Shoot, 3.0 -> "3"), or a repeat
count run through that ability's stacking formula (e.g. Extra strike,
Extra strike -> "3"). Anything else that repeats renders as one icon per
occurrence, since we don't have a stacking rule for it yet.

Usage:
    python generate_chits.py                  # every unit
    python generate_chits.py -f ug             # Boneborn (u) + Gobbo (g) only
    python generate_chits.py -t 1 2            # tiers 1 and 2 only
    python generate_chits.py -f g -t 3         # Gobbo tier 3 only

    Compact spec (digits + faction letters in one token, any order):
    a repeated tier digit means that many copies of that tier's chits.
    python generate_chits.py 112ug             # Boneborn+Gobbo, tier 1 x2 copies, tier 2 x1, no tier 3
    python generate_chits.py -112ug            # same, leading '-' is optional
    python generate_chits.py -0g               # one blank Gobbo chit (tier 0 = blank, not a sheet row)

Also writes output/sheetN.svg - every generated chit tiled onto as many
A4 pages as needed, packed as tight as the 60x15mm chit size allows.
"""
import argparse
import base64
import io
import re
import sys
import warnings
from pathlib import Path

import fitz  # PyMuPDF
import numpy as np
import openpyxl
from PIL import Image, ImageChops, ImageDraw
from svgelements import Path as SvgPath

ROOT_DIR = Path(__file__).parent.parent
ICONS_DIR = ROOT_DIR
FACTIONS_DIR = ROOT_DIR / "factions"
OUTPUT_DIR = Path(__file__).parent / "output"
WORKBOOK_PATH = ROOT_DIR / "Bannerbeasts Roller.xlsx"

ICON_MAP = {
    "Veteran": "veteran.svg",
    "Shield": "shield.svg",
    "Charge": "charge.svg",
    "Dodge": "dodge.svg",
    "Accuracy": "accuracy.svg",
    "Shoot": "ranged.svg",
    "Range": "range.svg",
    "Move": "move.svg",
    "Turn": "turn.svg",
    "Slow": "slow.svg",
    "Brave": "brave.svg",
    "Cowardly": "cowardly.svg",
    "Unbreakable": "anchored.svg",
    "Extra wound": "damaging.svg",
    "Extra strike": "multistrike.svg",
    "Poison": "poison.svg",
    "Armour": "armour.svg",
    "Beserk": "beserk.svg",
    "-G-Burst": "g-burst.svg",
    "-G-Rabid": "g-rabid.svg",
    "-G-Jetpack": "jetpack.svg",
    "-U-Ethereal": "ethereal.svg",
    "-U-Scary": "scary.svg",
    "-U-Vampiric": "u-vampiric.svg",
}

# Known stacking rules: ability -> f(stack_count) -> label string.
# stack_count comes from either an explicit number the designer wrote in
# the sheet, or a count of repeated occurrences.
def table_rule(mapping: dict[int, str]):
    """Lookup by stack count, clamped to the highest defined count for
    anything beyond it (for scales that aren't a simple linear formula)."""
    return lambda n: mapping.get(int(n), mapping[max(mapping)])


STACK_RULES = {
    "Shoot": lambda n: str(int(n)),
    "Extra strike": lambda n: str(int(n) + 1),
    "Veteran": lambda n: f"{5 - int(n)}+",
    "Move": lambda n: f"+{int(n)}",
    "Range": lambda n: str(int(n) + 2),
    "Accuracy": lambda n: f"{5 - int(n)}+",
    "Armour": lambda n: f"+{int(n)}",
    "-U-Vampiric": table_rule({1: "5+", 2: "3+", 3: "2+"}),
    "Poison": table_rule({1: "6s", 2: "5+", 3: "4+"}),
}

# Single-letter faction codes for -f/--faction. "u"/"g" confirmed by user;
# the rest are first-guess and collision-avoided (r/k, s/l clash on first
# letter) - flag any you want changed and it's a one-line edit here.
FACTION_CODES = {
    "g": "Gobbo",
    "r": "Ratkin",
    "u": "Boneborn",
    "s": "Silk Court",
    "k": "Redspines",       # guess - "r" taken by Ratkin
    "c": "Coppersand",
    "e": "Scaled Empire",
    "d": "Disciples",
    "l": "Slimes",           # guess - "s" taken by Silk Court
    "i": "Imps",
}

TIER_COLORS = {
    1: "#CD7F32",  # bronze
    2: "#C0C0C0",  # silver
    3: "#FFD700",  # gold
}

# ---- chit geometry (all in mm; viewBox 1 unit = 1mm) ----
CHIT_W = 60
CHIT_H = 15
BORDER_W = 0.5
SIDE_BAND_W = 4.0
ICON_SIZE = 9
ICON_TOP = 1.0
NUMBER_FONT_SIZE = 3.2
NUMBER_BASELINE_Y = ICON_TOP + ICON_SIZE + 3.0
ICON_GAP = 1.5
MAX_ICONS = 5
FACTION_ICON_SIZE = 8.0
FACTION_ICON_MARGIN = 0.6
BACK_ICON_SIZE = 11.0

# ---- A4 print sheet layout (mm) ----
A4_W = 210
A4_H = 297
SHEET_MIN_MARGIN = 5
SHEET_COLS = int((A4_W - 2 * SHEET_MIN_MARGIN) // CHIT_W)
SHEET_ROWS = int((A4_H - 2 * SHEET_MIN_MARGIN) // CHIT_H)
SHEET_MARGIN_X = (A4_W - SHEET_COLS * CHIT_W) / 2
SHEET_MARGIN_Y = (A4_H - SHEET_ROWS * CHIT_H) / 2
CHITS_PER_SHEET = SHEET_COLS * SHEET_ROWS


def apply_color_matrix(img: Image.Image, values: list[float]) -> Image.Image:
    """Apply an SVG feColorMatrix (type="matrix", 20 values) to an RGBA
    image, per spec: operates on non-premultiplied [0,1] channel values,
    output = M * [R,G,B,A,1]."""
    arr = np.asarray(img.convert("RGBA"), dtype=np.float64) / 255.0
    h, w, _ = arr.shape
    m = np.array(values, dtype=np.float64).reshape(4, 5)
    src = np.hstack([arr.reshape(-1, 4), np.ones((h * w, 1))])
    out = np.clip(src @ m.T, 0, 1).reshape(h, w, 4)
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA")


def bake_recolor_filters(inner: str) -> str:
    """Icons can recolour their shared grayscale glyph two different ways,
    both via Inkscape filters browsers render fine but our PDF export
    path (MuPDF's SVG converter) doesn't support at all:

    - Two-tone icons (e.g. Slow, -G-Rabid) overlay two copies of the
      glyph: a colour-themed copy on the bottom, and a purple copy on
      top clipped to part of the shape via clip-path. Each copy's colour
      itself comes from a "flood + composite-in" filter chain. A filtered
      <image> that MuPDF can't handle drops entirely (leaving just its
      background rect visible), and an unsupported clip-path is ignored
      so the top layer paints over the whole icon instead of its wedge.
    - Some single-colour icons (e.g. Spear: "a coloured icon, then
      recoloured in Inkscape") instead apply one direct feColorMatrix to
      shift the glyph's original colour to the intended one - no flood,
      no clip.

    Since all of this is genuinely part of the icon's design, not
    decoration, bake it into the PNG pixels here instead: flood-recolour
    via the source alpha as a mask, clip via a rasterised polygon from
    the clipPath's own path data, and apply direct matrices with numpy -
    so nothing needs filter or clip-path support at render time at all."""
    filter_colors: dict[str, tuple[int, int, int]] = {}
    filter_matrices: dict[str, list[float]] = {}
    for fid, body in re.findall(r'<filter[^>]*\bid="([^"]+)"[^>]*>(.*?)</filter>', inner, re.S):
        flood_m = re.search(r'<feFlood[^>]*flood-color="rgb\(([^)]+)\)"', body)
        if flood_m:
            filter_colors[fid] = tuple(int(v.strip()) for v in flood_m.group(1).split(","))
            continue
        matrices = re.findall(r'<feColorMatrix\b[^>]*\bvalues="([^"]+)"', body)
        values = [float(v) for v in matrices[0].split()] if matrices else []
        if len(matrices) == 1 and len(values) == 20 and not re.search(r"<fe(Flood|Composite|Blend)\b", body):
            filter_matrices[fid] = values

    clip_paths = dict(re.findall(r'<clipPath[^>]*\bid="([^"]+)"[^>]*>.*?<path[^>]*\bd="([^"]+)"', inner, re.S))
    if not filter_colors and not filter_matrices and not clip_paths:
        return inner

    def attr(tag: str, name: str) -> str | None:
        m = re.search(rf'\b{name}="([^"]*)"', tag)
        return m.group(1) if m else None

    def process(m: re.Match) -> str:
        tag = m.group(0)
        href = attr(tag, "xlink:href")
        if not href or not href.startswith("data:image/png;base64,"):
            return tag
        b64 = href[len("data:image/png;base64,"):]
        img = Image.open(io.BytesIO(base64.b64decode(re.sub(r"\s+", "", b64)))).convert("RGBA")
        changed = False

        style = attr(tag, "style") or ""
        filt_id = re.search(r"filter:url\(#([^)]+)\)", style)
        if filt_id and filt_id.group(1) in filter_colors:
            r, g, b = filter_colors[filt_id.group(1)]
            solid = Image.new("RGBA", img.size, (r, g, b, 255))
            solid.putalpha(img.getchannel("A"))
            img = solid
            new_style = re.sub(r"filter:url\([^)]*\);?", "", style)
            tag = tag.replace(f'style="{style}"', f'style="{new_style}"', 1)
            changed = True
        elif filt_id and filt_id.group(1) in filter_matrices:
            img = apply_color_matrix(img, filter_matrices[filt_id.group(1)])
            new_style = re.sub(r"filter:url\([^)]*\);?", "", style)
            tag = tag.replace(f'style="{style}"', f'style="{new_style}"', 1)
            changed = True

        clip_id_m = re.search(r'clip-path="url\(#([^)]+)\)"', tag)
        if clip_id_m and clip_id_m.group(1) in clip_paths:
            # clip-path is evaluated in the image's own local coordinate
            # system (same one its x/y/width/height are in) - the image's
            # own transform, if any, only places the already-clipped
            # result into the parent, so it plays no part in this mapping
            x, y = float(attr(tag, "x")), float(attr(tag, "y"))
            w, h = float(attr(tag, "width")), float(attr(tag, "height"))
            pts, last = [], None
            for pt in SvgPath(clip_paths[clip_id_m.group(1)]).as_points():
                if pt is None:
                    continue
                local = ((pt.x - x) / w * img.width, (pt.y - y) / h * img.height)
                if local != last:
                    pts.append(local)
                    last = local
            if len(pts) >= 3:
                mask = Image.new("L", img.size, 0)
                ImageDraw.Draw(mask).polygon(pts, fill=255)
                img = img.copy()
                img.putalpha(ImageChops.multiply(img.getchannel("A"), mask))
                changed = True
            tag = re.sub(r'\s*clip-path="url\([^)]*\)"', "", tag)

        if not changed:
            return m.group(0)
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        return tag.replace(b64, base64.b64encode(buf.getvalue()).decode("ascii"))

    return re.sub(r"<image\b[^>]*/>", process, inner)


def strip_editor_metadata(inner: str) -> str:
    """Drop Inkscape/Sodipodi editor metadata (namedview blocks and
    inkscape:*/sodipodi:* attributes) - it's invisible but uses namespace
    prefixes we don't declare on the wrapper <svg>, which XML-strict
    readers reject as unbound prefixes."""
    inner = re.sub(r"<(sodipodi|inkscape):[\w-]+\b[^>]*?(?:/>|>.*?</\1:[\w-]+>)", "", inner, flags=re.S)
    inner = re.sub(r'\s+(?:sodipodi|inkscape):[\w-]+="[^"]*"', "", inner)
    inner = bake_recolor_filters(inner)
    # any remaining filter reference (not the recolor pattern above) is
    # assumed decorative (e.g. a shadow) and dropped rather than risk
    # MuPDF rendering the element solid black.
    inner = re.sub(r"filter:url\([^)]*\);?", "", inner)
    return inner


def embed_as_group(text: str, x: float, y: float, target_w: float, target_h: float, uid: str | None = None) -> str:
    """Embed another SVG's content at (x,y) scaled to (target_w,target_h),
    using a <g transform="translate(...) scale(...)"> wrapper rather than
    a nested <svg x y width height viewBox>. Browsers render both the
    same, but MuPDF's SVG-to-PDF converter (used for combined PDF export)
    mishandles nested <svg> positioning/scaling - it silently drops or
    mis-places that content. <g transform> is honoured correctly, so this
    is the only technique used anywhere content gets embedded, even
    though nested <svg> would look identical on screen."""
    open_tag_end = text.index(">", text.index("<svg")) + 1
    close_tag_start = text.rindex("</svg>")
    inner = text[open_tag_end:close_tag_start]
    inner = strip_editor_metadata(inner)
    if uid is not None:
        inner = re.sub(r'id="([^"]+)"', lambda m: f'id="{m.group(1)}_{uid}"', inner)
        inner = re.sub(r"url\(#([^)]+)\)", lambda m: f"url(#{m.group(1)}_{uid})", inner)
    vb = re.search(r'viewBox="([^"]+)"', text).group(1)
    _, _, vb_w, vb_h = (float(v) for v in vb.split())
    sx, sy = target_w / vb_w, target_h / vb_h
    return f'<g transform="translate({x},{y}) scale({sx},{sy})">{inner}</g>'


def load_icon_fragment(icon_filename: str, x: float, y: float, size: float, uid: str) -> str:
    path = ICONS_DIR / icon_filename
    text = path.read_text(encoding="utf-8")
    return embed_as_group(text, x, y, size, size, uid)


def parse_slots(raw_values: list) -> list[tuple[str, float | None]]:
    """Turn a raw S1-S4 row into (ability, explicit_number_or_None) pairs,
    treating a numeric cell as an explicit stack number for the ability
    immediately before it."""
    tokens = [v for v in raw_values if v is not None]
    parsed: list[tuple[str, float | None]] = []
    i = 0
    while i < len(tokens):
        t = tokens[i]
        if isinstance(t, (int, float)):
            parsed.append((f"#{t:g}", None))  # orphan number, no preceding ability
            i += 1
            continue
        ability = str(t)
        if i + 1 < len(tokens) and isinstance(tokens[i + 1], (int, float)):
            parsed.append((ability, tokens[i + 1]))
            i += 2
        else:
            parsed.append((ability, None))
            i += 1
    return parsed


def build_slots(raw_values: list) -> list[tuple[str, str | None]]:
    """Collapse parsed (ability, explicit_number) pairs into display slots:
    explicit numbers win outright; otherwise a known stacker uses its
    repeat count through STACK_RULES; otherwise each occurrence stays its
    own icon (no number) so unconfirmed stackers stay visible, not guessed."""
    parsed = parse_slots(raw_values)
    order: list[str] = []
    info: dict[str, dict] = {}
    for ability, explicit in parsed:
        if ability not in info:
            order.append(ability)
            info[ability] = {"count": 0, "explicit": None}
        info[ability]["count"] += 1
        if explicit is not None:
            info[ability]["explicit"] = explicit

    slots: list[tuple[str, str | None]] = []
    for ability in order:
        d = info[ability]
        if d["explicit"] is not None:
            slots.append((ability, str(int(d["explicit"]))))
        elif ability in STACK_RULES:
            slots.append((ability, STACK_RULES[ability](d["count"])))
        else:
            slots.extend((ability, None) for _ in range(d["count"]))
    return slots


def render_chit(name: str, raw_values: list, tier: int | None = None, faction_icon_b64: str | None = None) -> str:
    slots = build_slots(raw_values)
    n = len(slots)
    if n > MAX_ICONS:
        raise ValueError(f"{name}: {n} icon slots exceeds MAX_ICONS={MAX_ICONS}")

    group_w = n * ICON_SIZE + (n - 1) * ICON_GAP if n else 0
    start_x = (CHIT_W - group_w) / 2
    tier_color = TIER_COLORS.get(tier)  # tier 0 (blank) and any unmapped tier get no band at all

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{CHIT_W}mm" height="{CHIT_H}mm" viewBox="0 0 {CHIT_W} {CHIT_H}">',
        f'<rect x="0" y="0" width="{CHIT_W}" height="{CHIT_H}" fill="white"/>',
    ]
    if tier_color:
        parts.append(f'<rect x="0" y="0" width="{SIDE_BAND_W}" height="{CHIT_H}" fill="{tier_color}"/>')
        parts.append(f'<rect x="{CHIT_W - SIDE_BAND_W}" y="0" width="{SIDE_BAND_W}" height="{CHIT_H}" fill="{tier_color}"/>')
    parts.append(
        f'<rect x="{BORDER_W/2}" y="{BORDER_W/2}" width="{CHIT_W-BORDER_W}" height="{CHIT_H-BORDER_W}" '
        f'fill="none" stroke="black" stroke-width="{BORDER_W}"/>'
    )

    for i, (ability, label) in enumerate(slots):
        icon_file = ICON_MAP.get(ability)
        x = start_x + i * (ICON_SIZE + ICON_GAP)
        if icon_file:
            parts.append(load_icon_fragment(icon_file, x, ICON_TOP, ICON_SIZE, uid=f"{name}_{i}"))
        else:
            parts.append(
                f'<rect x="{x}" y="{ICON_TOP}" width="{ICON_SIZE}" height="{ICON_SIZE}" '
                f'fill="none" stroke="red" stroke-dasharray="0.5,0.5"/>'
                f'<text x="{x+ICON_SIZE/2}" y="{ICON_TOP+ICON_SIZE/2}" font-size="1.6" '
                f'text-anchor="middle" fill="red">{ability}?</text>'
            )
        if label:
            cx = x + ICON_SIZE / 2
            parts.append(
                f'<text x="{cx}" y="{NUMBER_BASELINE_Y}" font-size="{NUMBER_FONT_SIZE}" '
                f'font-family="sans-serif" font-weight="bold" text-anchor="middle" fill="black">{label}</text>'
            )

    if faction_icon_b64:
        # fixed position for every chit, band or no band - drawn on top of it
        fx = CHIT_W - FACTION_ICON_MARGIN - FACTION_ICON_SIZE
        fy = CHIT_H - FACTION_ICON_MARGIN - FACTION_ICON_SIZE
        parts.append(
            f'<image x="{fx}" y="{fy}" width="{FACTION_ICON_SIZE}" height="{FACTION_ICON_SIZE}" '
            f'xlink:href="data:image/png;base64,{faction_icon_b64}"/>'
        )

    parts.append("</svg>")
    return "".join(parts)


def render_back(faction_icon_b64: str | None) -> str:
    """Chit back: no border, no bands, just a centered faction icon."""
    size = BACK_ICON_SIZE
    x = (CHIT_W - size) / 2
    y = (CHIT_H - size) / 2
    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{CHIT_W}mm" height="{CHIT_H}mm" viewBox="0 0 {CHIT_W} {CHIT_H}">',
        f'<rect x="0" y="0" width="{CHIT_W}" height="{CHIT_H}" fill="white"/>',
    ]
    if faction_icon_b64:
        parts.append(
            f'<image x="{x}" y="{y}" width="{size}" height="{size}" '
            f'xlink:href="data:image/png;base64,{faction_icon_b64}"/>'
        )
    parts.append("</svg>")
    return "".join(parts)


def load_units(faction_names: set[str] | None, tier_copies: dict[int, int] | None) -> list[dict]:
    warnings.filterwarnings("ignore")
    wb = openpyxl.load_workbook(WORKBOOK_PATH, data_only=True)
    ws = wb["U2"]
    headers = [c.value for c in ws[1]]
    idx = {h: i for i, h in enumerate(headers)}
    units = []
    all_factions: list[str] = []
    for row in ws.iter_rows(min_row=2):
        values = [c.value for c in row]
        faction = values[idx["Faction"]]
        unit = values[idx["Unit"]]
        if faction is None:
            continue
        if faction not in all_factions:
            all_factions.append(faction)
        if faction_names is not None and faction not in faction_names:
            continue
        tier_raw = values[idx["Tier"]]
        tier = int(tier_raw) if isinstance(tier_raw, (int, float)) else None
        if tier_copies is not None and tier not in tier_copies:
            continue
        copies = tier_copies[tier] if tier_copies is not None else 1
        raw_slots = [values[idx[c]] for c in ("S1", "S2", "S3", "S4") if c in idx]
        weapon = values[idx["Weapon"]] if "Weapon" in idx else None
        armour = values[idx["Armour"]] if "Armour" in idx else None
        units.append({
            "faction": faction, "unit": unit, "tier": tier, "raw_slots": raw_slots,
            "row": row[0].row, "copies": copies, "weapon": weapon, "armour": armour,
        })

    # tier 0 isn't a real row in the sheet - it's a blank chit, one per
    # matched faction, for things like faction dividers in a deck.
    if tier_copies is not None and 0 in tier_copies:
        for faction in all_factions:
            if faction_names is not None and faction not in faction_names:
                continue
            units.append({
                "faction": faction, "unit": None, "tier": 0, "raw_slots": [None] * 4,
                "row": None, "copies": tier_copies[0], "weapon": None, "armour": None,
            })

    return units


def slugify(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def load_faction_icon_names() -> dict[str, str]:
    """Faction display name -> the Factions tab's 'Icon' column value
    (a second-choice filename stem, since e.g. Gobbo's icon file is
    "gobbos.png" - the Icon column value - not "gobbo.png")."""
    wb = openpyxl.load_workbook(WORKBOOK_PATH, data_only=True)
    ws = wb["Factions"]
    headers = [c.value for c in ws[1]]
    idx = {h: i for i, h in enumerate(headers)}
    result = {}
    for row in ws.iter_rows(min_row=2):
        values = [c.value for c in row]
        name, icon = values[idx["Name"]], values[idx.get("Icon", -1)]
        if name and icon:
            result[name] = icon
    return result


FACTION_ICON_CACHE: dict[str, str | None] = {}


def get_faction_icon_b64(faction: str, icon_names: dict[str, str]) -> str | None:
    if faction in FACTION_ICON_CACHE:
        return FACTION_ICON_CACHE[faction]
    candidates = [slugify(faction)]
    if icon_names.get(faction):
        candidates.append(slugify(icon_names[faction]))
    b64 = None
    for cand in candidates:
        path = FACTIONS_DIR / f"{cand}.png"
        if path.exists():
            b64 = base64.b64encode(path.read_bytes()).decode("ascii")
            break
    if b64 is None:
        print(f"NOTE: no faction icon found for {faction} (tried {', '.join(candidates)}.png in factions/)")
    FACTION_ICON_CACHE[faction] = b64
    return b64


def embed_svg(text: str, x: float, y: float, w: float, h: float) -> str:
    return embed_as_group(text, x, y, w, h)


def render_sheet_page(chunk: list[Path]) -> str:
    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{A4_W}mm" height="{A4_H}mm" viewBox="0 0 {A4_W} {A4_H}">',
        f'<rect width="{A4_W}" height="{A4_H}" fill="white"/>',
    ]
    for i, path in enumerate(chunk):
        col, row = i % SHEET_COLS, i // SHEET_COLS
        x = SHEET_MARGIN_X + col * CHIT_W
        y = SHEET_MARGIN_Y + row * CHIT_H
        parts.append(embed_svg(path.read_text(encoding="utf-8"), x, y, CHIT_W, CHIT_H))
    parts.append("</svg>")
    return "".join(parts)


def build_sheets(chits: list[tuple[Path, str]], label: str = "sheet") -> list[Path]:
    """Tile chits onto A4 pages, packing tight (chits' own borders double
    as cut lines) with just enough margin to stay printer-safe. Each
    faction always starts on a fresh page - even if the previous
    faction's last page had room left - and overflows to more pages of
    its own if it has more chits than fit on one."""
    groups: dict[str, list[Path]] = {}
    order: list[str] = []
    for path, faction in chits:
        if faction not in groups:
            groups[faction] = []
            order.append(faction)
        groups[faction].append(path)

    pages = []
    for faction in order:
        paths = groups[faction]
        n_pages = -(-len(paths) // CHITS_PER_SHEET)  # ceil
        for page_idx in range(n_pages):
            chunk = paths[page_idx * CHITS_PER_SHEET:(page_idx + 1) * CHITS_PER_SHEET]
            suffix = "" if n_pages == 1 else f"-{page_idx + 1}"
            out_path = OUTPUT_DIR / f"{label}-{slugify(faction)}{suffix}.svg"
            out_path.write_text(render_sheet_page(chunk), encoding="utf-8")
            pages.append(out_path)
    return pages


def build_combined_pdf(sheet_paths: list[Path], out_path: Path) -> None:
    """Concatenate full-page sheet SVGs (already A4-sized) into a single
    print-ready PDF, one sheet per page, in the given order."""
    combined = fitz.open()
    for path in sheet_paths:
        page_doc = fitz.open(str(path))
        pdf_bytes = page_doc.convert_to_pdf()
        page_pdf = fitz.open("pdf", pdf_bytes)
        combined.insert_pdf(page_pdf)
    combined.save(str(out_path))


def build_preview_html(svg_paths: list[Path], out_path: Path) -> None:
    parts = ['<html><body style="background:#888;padding:20px;font-family:sans-serif;">']
    for p in svg_paths:
        svg = p.read_text(encoding="utf-8")
        svg = svg.replace(f'width="{CHIT_W}mm" height="{CHIT_H}mm"', 'width="600" height="150"')
        parts.append(f'<div style="color:white;margin-bottom:4px;">{p.stem}</div>{svg}<div style="height:20px;"></div>')
    parts.append("</body></html>")
    out_path.write_text("".join(parts), encoding="utf-8")


def parse_spec(spec: str, error) -> tuple[set[str] | None, dict[int, int] | None]:
    """Compact spec: tier digits (repeat a digit for extra copies of that
    tier) and faction letters, in any order, e.g. "112ug" -> tier 1 x2,
    tier 2 x1, no tier 3; factions Boneborn+Gobbo."""
    digits = [ch for ch in spec if ch.isdigit()]
    letters = [ch for ch in spec if ch.isalpha()]
    leftover = [ch for ch in spec if not ch.isdigit() and not ch.isalpha()]
    if leftover:
        error(f"unrecognised character(s) in spec: {''.join(leftover)}")

    tier_copies = None
    if digits:
        tier_copies = {}
        for ch in digits:
            t = int(ch)
            tier_copies[t] = tier_copies.get(t, 0) + 1

    faction_names = None
    if letters:
        unknown = [ch for ch in letters if ch not in FACTION_CODES]
        if unknown:
            error(f"unknown faction code(s): {''.join(unknown)} (known: {''.join(FACTION_CODES)})")
        faction_names = {FACTION_CODES[ch] for ch in letters}

    return faction_names, tier_copies


def main():
    parser = argparse.ArgumentParser(description="Generate BannerBeasts ability chits")
    parser.add_argument(
        "spec", nargs="?", default=None,
        help="compact spec, e.g. 112ug = tier1 x2 + tier2 x1 copies, Boneborn+Gobbo",
    )
    parser.add_argument("-f", "--faction", help="faction code letters, e.g. ug for Boneborn+Gobbo", default=None)
    parser.add_argument("-t", "--tier", nargs="+", type=int, help="tier numbers, e.g. -t 1 2", default=None)

    # allow "-112ug" as well as "112ug": argparse would otherwise treat a
    # leading '-' as an unknown option, so strip it here first (but leave
    # real flags like -f/-t alone).
    raw_args = sys.argv[1:]
    if len(raw_args) == 1 and raw_args[0] not in ("-h", "--help") and re.fullmatch(r"-[0-9a-zA-Z]+", raw_args[0]):
        raw_args = [raw_args[0][1:]]

    args = parser.parse_args(raw_args)

    if args.spec:
        faction_names, tier_copies = parse_spec(args.spec, parser.error)
    else:
        faction_names = None
        if args.faction:
            unknown = [ch for ch in args.faction if ch not in FACTION_CODES]
            if unknown:
                parser.error(f"unknown faction code(s): {''.join(unknown)} (known: {''.join(FACTION_CODES)})")
            faction_names = {FACTION_CODES[ch] for ch in args.faction}
        tier_copies = {t: 1 for t in args.tier} if args.tier else None

    units = load_units(faction_names, tier_copies)
    if not units:
        print("no units matched")
        return

    icon_names = load_faction_icon_names()
    OUTPUT_DIR.mkdir(exist_ok=True)
    written = []
    written_factions = []
    for u in units:
        if u["unit"]:
            unit_slug = slugify(u["unit"])
        elif u["row"] is not None:
            unit_slug = f'row{u["row"]}'
        else:
            unit_slug = "blank"
        base_name = f'{slugify(u["faction"])}_{unit_slug}'
        faction_icon_b64 = get_faction_icon_b64(u["faction"], icon_names)
        for copy_idx in range(1, u["copies"] + 1):
            chit_name = base_name if u["copies"] == 1 else f"{base_name}-copy{copy_idx}"
            try:
                svg = render_chit(chit_name, u["raw_slots"], tier=u["tier"], faction_icon_b64=faction_icon_b64)
            except ValueError as e:
                print(f"SKIP {chit_name}: {e}")
                continue
            out_path = OUTPUT_DIR / f"{chit_name}.svg"
            out_path.write_text(svg, encoding="utf-8")
            written.append(out_path)
            written_factions.append(u["faction"])
            print(f"wrote {out_path}")

    if written:
        preview_path = OUTPUT_DIR / "preview.html"
        build_preview_html(written, preview_path)
        print(f"wrote {preview_path}")

        sheets = build_sheets(list(zip(written, written_factions)))
        for s in sheets:
            print(f"wrote {s}")
        print(f"{len(written)} chits -> {len(sheets)} A4 sheet(s), {CHITS_PER_SHEET} per sheet ({SHEET_COLS}x{SHEET_ROWS})")

        # one back file per faction, plus back sheets matching each
        # faction's front chit count so double-sided printing lines up
        back_chits = []
        for faction in dict.fromkeys(written_factions):  # first-seen order, deduped
            back_svg = render_back(get_faction_icon_b64(faction, icon_names))
            back_path = OUTPUT_DIR / f"{slugify(faction)}_back.svg"
            back_path.write_text(back_svg, encoding="utf-8")
            print(f"wrote {back_path}")
            count = written_factions.count(faction)
            back_chits.extend([(back_path, faction)] * count)

        back_sheets = build_sheets(back_chits, label="sheet-back")
        for s in back_sheets:
            print(f"wrote {s}")

        pdf_path = OUTPUT_DIR / "chits.pdf"
        build_combined_pdf(sheets + back_sheets, pdf_path)
        print(f"wrote {pdf_path}")


if __name__ == "__main__":
    main()
