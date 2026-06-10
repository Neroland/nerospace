#!/usr/bin/env python3
"""
Entity contact sheet (ART_OVERHAUL_DESIGN.md §6/§8) — replaces the old hand-kept render_models.py
mirrors: cube geometry is parsed straight from each model's `model_sync` marker block (the same
parser the sync uses), so the preview can never drift from the Java. Each entity renders as a 3/4
isometric of its marker cubes, shaded per face and coloured by sampling its painted texture's UV
footprint — a true palette + silhouette read for review before runClient.

Marker-external parts (rotated shards, multi-cube limbs) are not drawn — this is a review sheet,
not a renderer; runClient remains the final look check.

Run:  python3 tools/render_entity_previews.py
Deps: Pillow.
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import model_sync  # noqa: E402  (shares REGISTRY + the Java cube parser)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_ENTITY = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/entity")
OUT = os.path.join(ROOT, "art", "preview")
os.makedirs(OUT, exist_ok=True)

SCALE = 4
CELL_W, CELL_H = 220, 230
BG = (24, 26, 32, 255)
LABEL = (200, 210, 224, 255)


def _cubes(entry):
    path = os.path.join(ROOT, entry["java"])
    with open(path, encoding="utf-8") as fh:
        src = fh.read()
    if model_sync.BEGIN not in src:
        return []
    block = src.split(model_sync.BEGIN)[1].split(model_sync.END)[0]
    cubes = []
    for m in model_sync._CUBE_RE.finditer(block):
        name, u, v = m.group(1), int(m.group(2)), int(m.group(3))
        x, y, z, w, h, d = (float(m.group(i)) for i in range(4, 10))
        ox, oy, oz = (float(m.group(i)) for i in range(10, 13))
        cubes.append((name, u, v, x + ox, y + oy, z + oz, w, h, d))
    return cubes


def _avg_color(tex, u, v, w, h, d):
    """Average the cube's box-UV footprint (skipping transparency) for the preview tint."""
    if tex is None:
        return (150, 150, 160)
    px = tex.load()
    total = [0, 0, 0]
    count = 0
    x1 = min(tex.width, int(u + 2 * (w + d)))
    y1 = min(tex.height, int(v + d + h))
    for yy in range(int(v), y1):
        for xx in range(int(u), x1):
            r, g, b, a = px[xx, yy]
            if a > 0:
                total[0] += r
                total[1] += g
                total[2] += b
                count += 1
    if count == 0:
        return (150, 150, 160)
    return tuple(c // count for c in total)


def _shade(color, f):
    return tuple(max(0, min(255, int(c * f))) for c in color)


def _iso(x, y, z, cx, cy):
    """3/4 projection: java space (y down, ground 24) onto the cell canvas."""
    sx = cx + (x - z * 0.6) * SCALE
    sy = cy + (y + z * 0.35) * SCALE
    return sx, sy


def _draw_cube(draw, tex, cube, cx, cy):
    _name, u, v, x, y, z, w, h, d = cube
    color = _avg_color(tex, u, v, w, h, d)
    # three visible faces of the box: top, front (north/-z), east side
    corners = {
        "tnw": _iso(x, y, z, cx, cy), "tne": _iso(x + w, y, z, cx, cy),
        "tsw": _iso(x, y, z + d, cx, cy), "tse": _iso(x + w, y, z + d, cx, cy),
        "bnw": _iso(x, y + h, z, cx, cy), "bne": _iso(x + w, y + h, z, cx, cy),
        "bse": _iso(x + w, y + h, z + d, cx, cy),
    }
    draw.polygon([corners["tnw"], corners["tne"], corners["tse"], corners["tsw"]],
                 fill=_shade(color, 1.25))                                    # top
    draw.polygon([corners["tnw"], corners["tne"], corners["bne"], corners["bnw"]],
                 fill=_shade(color, 1.0))                                     # front
    draw.polygon([corners["tne"], corners["tse"], corners["bse"], corners["bne"]],
                 fill=_shade(color, 0.7))                                     # side
    draw.line([corners["tnw"], corners["tne"], corners["bne"], corners["bnw"], corners["tnw"]],
              fill=_shade(color, 0.45))


def main():
    entries = model_sync.REGISTRY
    cols = 4
    rows = (len(entries) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * CELL_W, rows * CELL_H), BG)
    draw = ImageDraw.Draw(sheet)
    for i, entry in enumerate(entries):
        name = os.path.basename(entry["bbmodel"])[: -len(".bbmodel")]
        col, row = i % cols, i // cols
        cx = col * CELL_W + CELL_W // 2
        cy = row * CELL_H + 40
        tex_path = os.path.join(TEX_ENTITY, entry["texture"] + ".png")
        tex = Image.open(tex_path).convert("RGBA") if os.path.exists(tex_path) else None
        cubes = _cubes(entry)
        # painter's order: back-to-front by z, then top-down
        cubes.sort(key=lambda c: (c[5], c[4]))
        for cube in cubes:
            _draw_cube(draw, tex, cube, cx, cy + 60)
        draw.text((col * CELL_W + 10, row * CELL_H + CELL_H - 18), name, fill=LABEL)
    path = os.path.join(OUT, "entities_sheet.png")
    sheet.save(path)
    print("wrote", os.path.relpath(path, ROOT), f"({len(entries)} entities)")


if __name__ == "__main__":
    main()
