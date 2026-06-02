#!/usr/bin/env python3
"""
Offline previewer for the Nerospace entity models.

Minecraft `EntityModel` geometry is just data — boxes with a position, size and a per-part
pose (offset + rotation). This tool rebuilds each mob's cubes (mirroring the numbers in the
matching `*Model.java`), projects a 3/4 view with simple face shading, and writes a PNG to
`art/preview/<name>.png`. That lets the model be eyeballed without launching the game.

IMPORTANT: the GEOMETRY dicts below must be kept in sync with the Java models by hand — this is a
preview mirror, not a parser. When you tweak a model's cubes, update both.

Run:  python3 tools/render_models.py
Deps: Pillow.
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art", "preview")
os.makedirs(OUT, exist_ok=True)

# A cube = (kind, (ox,oy,oz, rx,ry,rz), (bx,by,bz, bw,bh,bd))
#   kind   -> colour group: "body" | "head" | "limb"
#   pose   -> part offset + rotation (radians), matching PartPose.offset/offsetAndRotation
#   box    -> addBox args; coordinates are local to the pose pivot


def leg(x, z, w, h, d):
    return ("limb", (0, 0, 0, 0, 0, 0), (x, 17, z, w, h, d))


CINDER = (
    [("body", (0, 0, 0, 0, 0, 0), (-6, 8, -6, 12, 9, 11)),
     ("body", (0, 0, 0, 0, 0, 0), (-5, 5, -5, 10, 4, 8)),
     ("body", (0, 0, 0, 0, 0, 0), (-5, 16, -5, 10, 3, 9)),
     ("head", (0, 0, 0, 0, 0, 0), (-4, 9, -13, 8, 8, 8)),
     ("head", (0, 0, 0, 0, 0, 0), (-4.5, 8, -11, 9, 2, 6)),
     ("head", (0, 0, 0, 0, 0, 0), (-3.5, 15, -13, 7, 2, 8)),
     ("limb", (-3, 9, -9, -0.5, 0, 0.25), (-1, -4, -1, 2, 5, 2)),
     ("limb", (3, 9, -9, -0.5, 0, -0.25), (-1, -4, -1, 2, 5, 2))]
    + [("limb", (0, 6, pz, -0.35, 0, 0), (-3, -4, -1, 6, 5, 2)) for pz in (-3, 1, 5)]
    + [leg(-6, -5, 4, 7, 4), leg(2, -5, 4, 7, 4), leg(-6, 1, 4, 7, 4), leg(2, 1, 4, 7, 4)]
)

GREENLING = [
    ("body", (0, 0, 0, 0, 0, 0), (-3.5, 15, -3, 7, 6, 6)),
    ("body", (0, 0, 0, 0, 0, 0), (-3, 19, -2.5, 6, 3, 5)),
    ("head", (0, 0, 0, 0, 0, 0), (-4, 7, -4, 8, 8, 8)),
    ("head", (0, 0, 0, 0, 0, 0), (-4.5, 10, -3.5, 9, 3, 7)),
    ("limb", (0, 7, 0, 0, 0, 0), (-0.5, -6, -0.5, 1, 6, 1)),
    ("limb", (-1.5, 7, 0, 0, 0, 0.5), (-0.5, -5, -0.5, 1, 5, 1)),
    ("limb", (1.5, 7, 0, 0, 0, -0.5), (-0.5, -5, -0.5, 1, 5, 1)),
    ("limb", (-3.5, 15.5, 0, 0, 0, 0.15), (-1.5, 0, -1, 2, 5, 2)),
    ("limb", (3.5, 15.5, 0, 0, 0, -0.15), (-0.5, 0, -1, 2, 5, 2)),
    ("limb", (0, 0, 0, 0, 0, 0), (-2.5, 21, -1.5, 2.5, 3, 3)),
    ("limb", (0, 0, 0, 0, 0, 0), (0, 21, -1.5, 2.5, 3, 3)),
]

QUARTZ = (
    [("body", (0, 0, 0, 0, 0, 0), (-4, 12, -4, 8, 3, 8)),
     ("body", (0, 0, 0, 0, 0, 0), (-5, 15, -5, 10, 4, 10)),
     ("body", (0, 0, 0, 0, 0, 0), (-5.5, 17, -5.5, 11, 2, 11)),
     ("head", (0, 0, 0, 0, 0, 0), (-3, 15, -9, 6, 4, 4)),
     ("limb", (-1.5, 12, 0, -0.2, 0, 0.3), (-1, -4, -1, 2, 5, 2)),
     ("limb", (1, 12, -1, 0, 0, 0), (-1, -5, -1, 2, 6, 2)),
     ("limb", (0, 12, 2.5, -0.3, 0, -0.2), (-1, -3, -1, 2, 4, 2))]
    + [("limb", (-5, 16, z, 0, 0, 0.55), (-1, 0, -1, 2, 10, 2)) for z in (-3.5, 0, 3.5)]
    + [("limb", (5, 16, z, 0, 0, -0.55), (-1, 0, -1, 2, 10, 2)) for z in (-3.5, 0, 3.5)]
)


def arm(x):
    roll = 0.12 if x < 0 else -0.12
    return [
        ("limb", (x, 5, 0, 0, 0, roll), (-1.5, 0, -1.5, 3, 7, 3)),
        ("limb", (x, 12, 0, 0, 0, 0), (-1.5, 0, -1.5, 3, 6, 3)),
        ("limb", (x, 16, 0, 0, 0, 0), (-0.5, 0, -2, 1, 10, 5)),
    ]


def xleg(x):
    return [
        ("limb", (x, 15, 0, 0, 0, 0), (-2, 0, -2, 4, 6, 4)),
        ("limb", (x, 20, 0, 0, 0, 0), (-1.5, 0, -1.5, 3, 4, 3)),
        ("limb", (x, 0, 0, 0, 0, 0), (-1.5, 22, -5, 3, 2, 6)),
    ]


XERTZ = (
    [("body", (0, 0, 0, 0, 0, 0), (-3, 12, -2.5, 6, 4, 5)),
     ("body", (0, 0, 0, 0, 0, 0), (-3.5, 4, -3, 7, 9, 6)),
     ("body", (0, 0, 0, 0, 0, 0), (-2.5, 5, -4, 5, 5, 2)),
     ("body", (0, 0, 0, 0, 0, 0), (-6, 4, -2.5, 3, 4, 5)),
     ("body", (0, 0, 0, 0, 0, 0), (3, 4, -2.5, 3, 4, 5)),
     ("body", (0, 0, 0, 0, 0, 0), (-2, 1, -2, 4, 4, 4)),
     ("head", (0, 0, 0, 0, 0, 0), (-3, -3, -6, 6, 5, 7)),
     ("head", (0, 0, 0, 0, 0, 0), (-2.5, 2, -6, 5, 2, 6)),
     ("limb", (0, -2, 1, -0.4, 0, 0), (-1, -7, -1, 2, 6, 3))]
    + [("limb", (0, 5, fz, -0.25, 0, 0), (-0.5, -fh, -1, 1, fh, 3))
       for fz, fh in ((1.5, 7), (4, 6), (7, 4))]
    + arm(-5) + arm(5) + xleg(-2.5) + xleg(2.5)
)

MODELS = {
    "xertz_stalker": XERTZ,
    "quartz_crawler": QUARTZ,
    "greenling": GREENLING,
    "cinder_stalker": CINDER,
}

COLORS = {
    "xertz_stalker": {"body": (44, 124, 122), "head": (30, 78, 82), "limb": (120, 214, 200)},
    "quartz_crawler": {"body": (150, 200, 165), "head": (74, 112, 88), "limb": (210, 250, 210)},
    "greenling": {"body": (84, 192, 118), "head": (52, 140, 88), "limb": (150, 240, 150)},
    "cinder_stalker": {"body": (52, 40, 36), "head": (34, 24, 20), "limb": (224, 124, 52)},
}

# Cube face definitions: 4 corner indices (of the 8 box corners) + outward normal.
CORNERS = [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0), (0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]
FACES = [
    ((0, 1, 2, 3), (0, 0, -1)),
    ((5, 4, 7, 6), (0, 0, 1)),
    ((4, 0, 3, 7), (-1, 0, 0)),
    ((1, 5, 6, 2), (1, 0, 0)),
    ((4, 5, 1, 0), (0, -1, 0)),
    ((3, 2, 6, 7), (0, 1, 0)),
]


def rot(v, rx, ry, rz):
    x, y, z = v
    # Apply X, then Y, then Z (matches ModelPart.translateAndRotate order Z*Y*X).
    cy, sy = math.cos(rx), math.sin(rx)
    y, z = y * cy - z * sy, y * sy + z * cy
    cy, sy = math.cos(ry), math.sin(ry)
    x, z = x * cy + z * sy, -x * sy + z * cy
    cy, sy = math.cos(rz), math.sin(rz)
    x, y = x * cy - y * sy, x * sy + y * cy
    return (x, y, z)


def render(name, cubes, yaw=-0.55, pitch=0.14, size=520):
    colors = COLORS[name]
    light = (-0.45, -0.7, 0.55)
    ln = math.sqrt(sum(c * c for c in light))
    light = tuple(c / ln for c in light)

    # Build world geometry: (face_world_verts, world_normal, base_colour).
    polys = []
    allpts = []
    for kind, pose, box in cubes:
        ox, oy, oz, rx, ry, rz = pose
        bx, by, bz, bw, bh, bd = box
        verts = []
        for cx, cy, cz in CORNERS:
            lv = (bx + cx * bw, by + cy * bh, bz + cz * bd)
            wv = rot(lv, rx, ry, rz)
            wv = (wv[0] + ox, wv[1] + oy, wv[2] + oz)
            verts.append(wv)
            allpts.append(wv)
        for idx, normal in FACES:
            n = rot(normal, rx, ry, rz)
            polys.append(([verts[i] for i in idx], n, colors[kind]))

    cx = sum(p[0] for p in allpts) / len(allpts)
    cyc = sum(p[1] for p in allpts) / len(allpts)
    cz = sum(p[2] for p in allpts) / len(allpts)

    def cam(p):
        x, y, z = p[0] - cx, p[1] - cyc, p[2] - cz
        return rot((x, y, z), pitch, yaw, 0)

    # Project + depth-sort (painter's: far first).
    drawn = []
    minx = miny = 1e9
    maxx = maxy = -1e9
    scale = 13.0
    for verts, normal, col in polys:
        cv = [cam(v) for v in verts]
        nc = rot(normal, pitch, yaw, 0)
        bright = 0.32 + 0.68 * max(0.0, sum(nc[i] * light[i] for i in range(3)))
        shaded = tuple(max(0, min(255, int(c * bright))) for c in col)
        pts2 = [(v[0] * scale, v[1] * scale) for v in cv]
        depth = sum(v[2] for v in cv) / 4.0
        drawn.append((depth, pts2, shaded))
        for px, py in pts2:
            minx = min(minx, px); maxx = max(maxx, px)
            miny = min(miny, py); maxy = max(maxy, py)
    drawn.sort(key=lambda d: d[0])

    img = Image.new("RGBA", (size, size), (32, 34, 40, 255))
    dr = ImageDraw.Draw(img)
    # floor line
    offx = size / 2 - (minx + maxx) / 2
    offy = size * 0.94 - maxy
    dr.line([(0, maxy + offy), (size, maxy + offy)], fill=(60, 62, 70), width=2)
    for _depth, pts2, shaded in drawn:
        poly = [(px + offx, py + offy) for px, py in pts2]
        outline = tuple(max(0, int(c * 0.55)) for c in shaded)
        dr.polygon(poly, fill=shaded + (255,), outline=outline + (255,))
    img.save(os.path.join(OUT, name + ".png"))
    print("wrote", os.path.relpath(os.path.join(OUT, name + ".png"), ROOT))


if __name__ == "__main__":
    for n, cubes in MODELS.items():
        render(n, cubes)
    # Contact sheet.
    sheet = Image.new("RGBA", (520 * 2, 520 * 2), (20, 22, 26, 255))
    order = ["xertz_stalker", "quartz_crawler", "greenling", "cinder_stalker"]
    for i, n in enumerate(order):
        im = Image.open(os.path.join(OUT, n + ".png"))
        sheet.paste(im, ((i % 2) * 520, (i // 2) * 520))
    sheet.save(os.path.join(OUT, "_contact_sheet.png"))
    print("wrote", os.path.relpath(os.path.join(OUT, "_contact_sheet.png"), ROOT))
