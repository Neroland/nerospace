#!/usr/bin/env python3
"""
Offline previewer for the Nerospace entity models (with eyes + animation poses).

Minecraft `EntityModel` geometry is just data — boxes with a position, size and a per-part pose
(offset + rotation). This rebuilds each mob's cubes (mirroring the matching `*Model.java`), projects
a 3/4 view with face shading + drawn eyes, and writes `art/preview/<name>.png`. Animated limbs are
hip-pivoted and tagged with a group; passing a pose (extra xRot per group) renders a walk frame, so
animation can be checked offline too.

IMPORTANT: this is a hand-kept MIRROR of the Java models + their setupAnim phases — keep them in sync.

Run:  python3 tools/render_models.py
Deps: Pillow.
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art", "preview")
os.makedirs(OUT, exist_ok=True)
PI = math.pi

# A cube = (kind, (ox,oy,oz, rx,ry,rz), (bx,by,bz, bw,bh,bd), group)
#   kind  -> "body" | "head" | "face" (head cube that shows eyes) | "limb"
#   pose  -> part offset + rotation (radians); box coords are local to the pivot
#   group -> animation group name (None = static)


# ---------------- CINDER STALKER (magma quadruped) ----------------
CINDER = [
    ("body", (0, 0, 0, 0, 0, 0), (-6, 8, -6, 12, 9, 11), None),
    ("body", (0, 0, 0, 0, 0, 0), (-5, 5, -5, 10, 4, 8), None),
    ("body", (0, 0, 0, 0, 0, 0), (-5, 16, -5, 10, 3, 9), None),
    ("face", (0, 0, 0, 0, 0, 0), (-4, 9, -13, 8, 8, 8), None),
    ("head", (0, 0, 0, 0, 0, 0), (-4.5, 8, -11, 9, 2, 6), None),
    ("head", (0, 0, 0, 0, 0, 0), (-3.5, 15, -13, 7, 2, 8), None),
    ("limb", (-3, 9, -9, -0.5, 0, 0.25), (-1, -4, -1, 2, 5, 2), None),
    ("limb", (3, 9, -9, -0.5, 0, -0.25), (-1, -4, -1, 2, 5, 2), None),
] + [("limb", (0, 6, pz, -0.35, 0, 0), (-3, -4, -1, 6, 5, 2), None) for pz in (-3, 1, 5)] + [
    ("limb", (-4, 16, -3, 0, 0, 0), (-2, 0, -2, 4, 8, 4), "leg_fl"),
    ("limb", (4, 16, -3, 0, 0, 0), (-2, 0, -2, 4, 8, 4), "leg_fr"),
    ("limb", (-4, 16, 3, 0, 0, 0), (-2, 0, -2, 4, 8, 4), "leg_bl"),
    ("limb", (4, 16, 3, 0, 0, 0), (-2, 0, -2, 4, 8, 4), "leg_br"),
]
CINDER_ANIM = {"leg_fl": (0, 0.6), "leg_br": (0, 0.6), "leg_fr": (PI, 0.6), "leg_bl": (PI, 0.6)}

# ---------------- GREENLING (cute biped) ----------------
GREENLING = [
    ("body", (0, 0, 0, 0, 0, 0), (-3.5, 15, -3, 7, 6, 6), None),
    ("body", (0, 0, 0, 0, 0, 0), (-3, 19, -2.5, 6, 3, 5), None),
    ("face", (0, 0, 0, 0, 0, 0), (-4, 7, -4, 8, 8, 8), None),
    ("head", (0, 0, 0, 0, 0, 0), (-4.5, 10, -3.5, 9, 3, 7), None),
    ("limb", (0, 7, 0, 0, 0, 0), (-0.5, -6, -0.5, 1, 6, 1), None),
    ("limb", (-1.5, 7, 0, 0, 0, 0.5), (-0.5, -5, -0.5, 1, 5, 1), None),
    ("limb", (1.5, 7, 0, 0, 0, -0.5), (-0.5, -5, -0.5, 1, 5, 1), None),
    ("limb", (-3.5, 15.5, 0, 0, 0, 0.15), (-1.5, 0, -1, 2, 5, 2), "arm_l"),
    ("limb", (3.5, 15.5, 0, 0, 0, -0.15), (-0.5, 0, -1, 2, 5, 2), "arm_r"),
    ("limb", (-1.25, 21, 0, 0, 0, 0), (-1.25, 0, -1.5, 2.5, 3, 3), "leg_l"),
    ("limb", (1.25, 21, 0, 0, 0, 0), (-1.25, 0, -1.5, 2.5, 3, 3), "leg_r"),
]
GREENLING_ANIM = {"leg_l": (0, 0.5), "leg_r": (PI, 0.5), "arm_l": (PI, 0.3), "arm_r": (0, 0.3)}

# ---------------- QUARTZ CRAWLER (six-legged geode) ----------------
QUARTZ = [
    ("body", (0, 0, 0, 0, 0, 0), (-4, 12, -4, 8, 3, 8), None),
    ("body", (0, 0, 0, 0, 0, 0), (-5, 15, -5, 10, 4, 10), None),
    ("body", (0, 0, 0, 0, 0, 0), (-5.5, 17, -5.5, 11, 2, 11), None),
    ("face", (0, 0, 0, 0, 0, 0), (-3, 15, -9, 6, 4, 4), None),
    ("limb", (-1.5, 12, 0, -0.2, 0, 0.3), (-1, -4, -1, 2, 5, 2), None),
    ("limb", (1, 12, -1, 0, 0, 0), (-1, -5, -1, 2, 6, 2), None),
    ("limb", (0, 12, 2.5, -0.3, 0, -0.2), (-1, -3, -1, 2, 4, 2), None),
]
QUARTZ_ANIM = {}
for _i, _z in enumerate((-3.5, 0, 3.5)):
    QUARTZ.append(("limb", (-5, 16, _z, 0, 0, 0.55), (-1, 0, -1, 2, 10, 2), "leg_l%d" % _i))
    QUARTZ.append(("limb", (5, 16, _z, 0, 0, -0.55), (-1, 0, -1, 2, 10, 2), "leg_r%d" % _i))
    QUARTZ_ANIM["leg_l%d" % _i] = (_i * 2.1, 0.3)
    QUARTZ_ANIM["leg_r%d" % _i] = (PI + _i * 2.1, 0.3)

# ---------------- XERTZ STALKER (crystal hunter, hero biped) ----------------
XERTZ = [
    ("body", (0, 0, 0, 0, 0, 0), (-3, 12, -2.5, 6, 4, 5), None),
    ("body", (0, 0, 0, 0, 0, 0), (-3.5, 4, -3, 7, 9, 6), None),
    ("body", (0, 0, 0, 0, 0, 0), (-2.5, 5, -4, 5, 5, 2), None),
    ("body", (0, 0, 0, 0, 0, 0), (-6, 4, -2.5, 3, 4, 5), None),
    ("body", (0, 0, 0, 0, 0, 0), (3, 4, -2.5, 3, 4, 5), None),
    ("body", (0, 0, 0, 0, 0, 0), (-2, 1, -2, 4, 4, 4), None),
    ("face", (0, 0, 0, 0, 0, 0), (-3, -3, -6, 6, 5, 7), None),
    ("head", (0, 0, 0, 0, 0, 0), (-2.5, 2, -6, 5, 2, 6), None),
    ("limb", (0, -2, 1, -0.4, 0, 0), (-1, -7, -1, 2, 6, 3), None),
] + [("limb", (0, 5, fz, -0.25, 0, 0), (-0.5, -fh, -1, 1, fh, 3), None)
     for fz, fh in ((1.5, 7), (4, 6), (7, 4))]
# Arms (single hip-pivoted parts, chained cubes share pivot+group).
for _side, _x, _roll in (("arm_l", -5, 0.12), ("arm_r", 5, -0.12)):
    for _box in ((-1.5, 0, -1.5, 3, 7, 3), (-1.5, 7, -1.5, 3, 6, 3), (-0.5, 11, -2, 1, 10, 5)):
        XERTZ.append(("limb", (_x, 5, 0, 0, 0, _roll), _box, _side))
# Legs (single hip-pivoted parts: thigh + shin + foot).
for _side, _x in (("leg_l", -2.5), ("leg_r", 2.5)):
    for _box in ((-2, 0, -2, 4, 6, 4), (-1.5, 6, -1.5, 3, 3, 3), (-1.5, 7, -5, 3, 2, 6)):
        XERTZ.append(("limb", (_x, 15, 0, 0, 0, 0), _box, _side))
XERTZ_ANIM = {"leg_l": (0, 0.5), "leg_r": (PI, 0.5), "arm_l": (PI, 0.22), "arm_r": (0, 0.22)}

MODELS = {
    "xertz_stalker": (XERTZ, XERTZ_ANIM),
    "quartz_crawler": (QUARTZ, QUARTZ_ANIM),
    "greenling": (GREENLING, GREENLING_ANIM),
    "cinder_stalker": (CINDER, CINDER_ANIM),
}
COLORS = {
    "xertz_stalker": {"body": (44, 124, 122), "head": (30, 78, 82), "face": (30, 78, 82),
                      "limb": (120, 214, 200), "eye": (255, 70, 200)},
    "quartz_crawler": {"body": (150, 200, 165), "head": (74, 112, 88), "face": (74, 112, 88),
                       "limb": (210, 250, 210), "eye": (200, 255, 214)},
    "greenling": {"body": (84, 192, 118), "head": (52, 140, 88), "face": (52, 140, 88),
                  "limb": (150, 240, 150), "eye": (235, 255, 150)},
    "cinder_stalker": {"body": (52, 40, 36), "head": (34, 24, 20), "face": (34, 24, 20),
                       "limb": (224, 124, 52), "eye": (255, 210, 120)},
}

CORNERS = [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0), (0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]
FACES = [((0, 1, 2, 3), (0, 0, -1)), ((5, 4, 7, 6), (0, 0, 1)), ((4, 0, 3, 7), (-1, 0, 0)),
         ((1, 5, 6, 2), (1, 0, 0)), ((4, 5, 1, 0), (0, -1, 0)), ((3, 2, 6, 7), (0, 1, 0))]


def rot(v, rx, ry, rz):
    x, y, z = v
    c, s = math.cos(rx), math.sin(rx); y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ry), math.sin(ry); x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(rz), math.sin(rz); x, y = x * c - y * s, x * s + y * c
    return (x, y, z)


def walk_pose(anim):
    return {g: math.cos(ph) * amp for g, (ph, amp) in anim.items()}


def render(name, cubes, pose, yaw=-0.55, pitch=0.12, size=520):
    base = name[:-5] if name.endswith("_walk") else name
    colors = COLORS[base]
    light = (-0.45, -0.7, 0.55)
    ln = math.sqrt(sum(c * c for c in light)); light = tuple(c / ln for c in light)
    pose = pose or {}

    polys, allpts, face_quad = [], [], None
    for kind, (ox, oy, oz, rx, ry, rz), (bx, by, bz, bw, bh, bd), group in cubes:
        rxa = rx + pose.get(group, 0.0)
        verts = []
        for cx, cy, cz in CORNERS:
            wv = rot((bx + cx * bw, by + cy * bh, bz + cz * bd), rxa, ry, rz)
            wv = (wv[0] + ox, wv[1] + oy, wv[2] + oz)
            verts.append(wv); allpts.append(wv)
        for fi, (idx, normal) in enumerate(FACES):
            n = rot(normal, rxa, ry, rz)
            polys.append(([verts[i] for i in idx], n, colors[kind], kind == "face" and fi == 0))

    cx = sum(p[0] for p in allpts) / len(allpts)
    cyc = sum(p[1] for p in allpts) / len(allpts)
    cz = sum(p[2] for p in allpts) / len(allpts)
    scale = 13.0

    def cam(p):
        return rot((p[0] - cx, p[1] - cyc, p[2] - cz), pitch, yaw, 0)

    drawn, miny = [], 1e9
    eye_quad = None
    maxy = -1e9
    for verts, normal, col, is_face in polys:
        cv = [cam(v) for v in verts]
        nc = rot(normal, pitch, yaw, 0)
        bright = 0.32 + 0.68 * max(0.0, sum(nc[i] * light[i] for i in range(3)))
        shaded = tuple(max(0, min(255, int(c * bright))) for c in col)
        pts2 = [(v[0] * scale, v[1] * scale) for v in cv]
        depth = sum(v[2] for v in cv) / 4.0
        drawn.append((depth, pts2, shaded))
        if is_face and nc[2] > 0.05:
            eye_quad = (depth, pts2)
        for _px, _py in pts2:
            miny = min(miny, _py); maxy = max(maxy, _py)
    drawn.sort(key=lambda d: d[0])

    minx = min(px for _d, pts, _c in drawn for px, _py in pts)
    maxx = max(px for _d, pts, _c in drawn for px, _py in pts)
    img = Image.new("RGBA", (size, size), (32, 34, 40, 255))
    dr = ImageDraw.Draw(img)
    offx = size / 2 - (minx + maxx) / 2
    offy = size * 0.93 - maxy
    dr.line([(0, maxy + offy + 1), (size, maxy + offy + 1)], fill=(58, 60, 68), width=2)
    for _depth, pts2, shaded in drawn:
        poly = [(px + offx, py + offy) for px, py in pts2]
        outline = tuple(max(0, int(c * 0.5)) for c in shaded)
        dr.polygon(poly, fill=shaded + (255,), outline=outline + (255,))

    # Eyes on the face cube's front quad (TL,TR,BR,BL).
    if eye_quad:
        _d, q = eye_quad
        q = [(px + offx, py + offy) for px, py in q]

        def bilerp(u, v):
            top = (q[0][0] + (q[1][0] - q[0][0]) * u, q[0][1] + (q[1][1] - q[0][1]) * u)
            bot = (q[3][0] + (q[2][0] - q[3][0]) * u, q[3][1] + (q[2][1] - q[3][1]) * u)
            return (top[0] + (bot[0] - top[0]) * v, top[1] + (bot[1] - top[1]) * v)

        eye = colors["eye"]
        r = 4
        for u in (0.30, 0.70):
            ex, ey = bilerp(u, 0.42)
            dr.ellipse([ex - r, ey - r, ex + r, ey + r], fill=(10, 10, 12, 255))
            dr.ellipse([ex - r + 1, ey - r + 1, ex + r - 1, ey + r - 1], fill=eye + (255,))
    img.save(os.path.join(OUT, name + ".png"))


if __name__ == "__main__":
    order = ["xertz_stalker", "quartz_crawler", "greenling", "cinder_stalker"]
    for n in order:
        cubes, anim = MODELS[n]
        render(n, cubes, None)
        render(n + "_walk", cubes, walk_pose(anim))
        print("wrote", n)
    sheet = Image.new("RGBA", (520 * 2, 520 * 2), (20, 22, 26, 255))
    for i, n in enumerate(order):
        sheet.paste(Image.open(os.path.join(OUT, n + ".png")), ((i % 2) * 520, (i // 2) * 520))
    sheet.save(os.path.join(OUT, "_contact_sheet.png"))
    walk = Image.new("RGBA", (520 * 2, 520 * 2), (20, 22, 26, 255))
    for i, n in enumerate(order):
        walk.paste(Image.open(os.path.join(OUT, n + "_walk.png")), ((i % 2) * 520, (i // 2) * 520))
    walk.save(os.path.join(OUT, "_walk_sheet.png"))
    print("wrote contact + walk sheets")
# end
