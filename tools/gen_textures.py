#!/usr/bin/env python3
"""
Generate starter 16x16 pixel-art textures for the Nerospace mod.
Theme: nerosium = red/purple cosmic crystal.
Outputs PNGs into src/main/resources/assets/nerospace/textures/{block,item}.
Deterministic (seeded) so re-runs are stable.
"""
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOCK_DIR = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/block")
ITEM_DIR = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/item")
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)

S = 16  # texture size

# ---- Palette (RGBA) ----
CLEAR = (0, 0, 0, 0)
# nerosium ramp: dark core -> purple -> magenta -> red -> bright glow
N_DARK   = (43, 13, 58, 255)    # #2b0d3a
N_PURPLE = (106, 31, 140, 255)  # #6a1f8c
N_MAG    = (179, 39, 158, 255)  # #b3279e
N_RED    = (224, 58, 58, 255)   # #e03a3a
N_REDHI  = (255, 90, 96, 255)   # #ff5a60
N_GLOW   = (255, 138, 216, 255) # #ff8ad8
N_BRIGHT = (255, 208, 240, 255) # #ffd0f0
NEROS = [N_DARK, N_PURPLE, N_MAG, N_RED, N_REDHI, N_GLOW]

# stone / deepslate / metal bases
STONE   = [(122,122,122,255),(132,132,132,255),(112,112,112,255),(140,140,140,255)]
DEEP    = [(71,71,74,255),(80,80,84,255),(63,63,66,255),(88,88,92,255)]
METAL   = [(58,52,66,255),(70,63,80,255),(48,43,56,255),(82,75,92,255)]
METAL_L = (120, 110, 134, 255)
METAL_D = (34, 30, 42, 255)
WOOD    = [(92,62,38,255),(78,52,32,255),(104,72,46,255)]
WOOD_D  = (54, 36, 22, 255)


def new_img():
    return Image.new("RGBA", (S, S), CLEAR)


def noise_fill(img, palette, rng):
    px = img.load()
    for y in range(S):
        for x in range(S):
            px[x, y] = rng.choice(palette)
    return img


def blob(img, cx, cy, r, colors, rng, density=0.85):
    """Scatter a rough circular cluster of ore specks."""
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r and rng.random() < density * (1 - d / (r + 0.001)):
                # brighter toward center
                t = 1 - d / (r + 0.001)
                idx = min(len(colors) - 1, int(t * len(colors)))
                px[x, y] = colors[idx]


def bevel(img, light, dark):
    """Add a 1px highlight on top/left and shadow on bottom/right."""
    px = img.load()
    for i in range(S):
        px[i, 0] = light
        px[0, i] = light
        px[i, S - 1] = dark
        px[S - 1, i] = dark


def save(img, path):
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


# ---------------- BLOCKS ----------------

def gen_ore(base_palette, name):
    rng = random.Random(hash(name) & 0xffffffff)
    img = new_img()
    noise_fill(img, base_palette, rng)
    # a few nerosium clusters
    for (cx, cy, r) in [(4, 5, 3.2), (11, 6, 2.6), (7, 12, 3.0), (13, 12, 1.8)]:
        blob(img, cx, cy, r, NEROS, rng, density=0.95)
    # a couple of bright glints
    px = img.load()
    for (gx, gy) in [(4, 4), (11, 12)]:
        px[gx, gy] = N_BRIGHT
    save(img, os.path.join(BLOCK_DIR, name + ".png"))


def gen_storage_block():
    rng = random.Random(101)
    img = new_img()
    noise_fill(img, [N_PURPLE, N_MAG, N_DARK, (90, 26, 120, 255)], rng)
    px = img.load()
    # crystalline diamond facets
    for y in range(S):
        for x in range(S):
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                px[x, y] = N_GLOW
            elif (x + y) % 8 == 4:
                px[x, y] = N_RED
    # central bright gem
    blob(img, 8, 8, 3.4, [N_RED, N_REDHI, N_GLOW, N_BRIGHT], rng, density=1.0)
    bevel(img, N_GLOW, N_DARK)
    save(img, os.path.join(BLOCK_DIR, "nerosium_block.png"))


def gen_raw_block():
    rng = random.Random(202)
    img = new_img()
    noise_fill(img, [N_DARK, (60, 20, 40, 255), (52, 16, 60, 255)], rng)
    # rough embedded chunks
    for (cx, cy, r) in [(4, 4, 2.6), (11, 5, 2.4), (5, 11, 2.4), (12, 12, 2.8), (8, 8, 1.8)]:
        blob(img, cx, cy, r, [N_PURPLE, N_MAG, N_RED, N_REDHI], rng, density=0.95)
    bevel(img, (120, 50, 90, 255), N_DARK)
    save(img, os.path.join(BLOCK_DIR, "raw_nerosium_block.png"))


def gen_grinder():
    rng = random.Random(303)
    img = new_img()
    noise_fill(img, METAL, rng)
    px = img.load()
    # metal frame
    bevel(img, METAL_L, METAL_D)
    for i in range(S):
        px[1, i] = METAL_L if i < S - 1 else METAL_D
        px[i, 1] = METAL_L if i < S - 1 else METAL_D
    # inset dark grinder window (rows 4..11, cols 4..11)
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = METAL_D
    # glowing grinder teeth / hopper
    for y in range(5, 11):
        for x in range(5, 11):
            if (x + y) % 2 == 0:
                px[x, y] = N_RED
            else:
                px[x, y] = N_PURPLE
    # bright core
    for y in range(7, 9):
        for x in range(7, 9):
            px[x, y] = N_GLOW
    # corner bolts
    for (bx, by) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[bx, by] = METAL_L
    save(img, os.path.join(BLOCK_DIR, "nerosium_grinder.png"))


# ---------------- ITEMS ----------------

def gen_ingot():
    img = new_img()
    px = img.load()
    # parallelogram ingot
    rows = {
        5:  (4, 12), 6: (3, 13), 7: (3, 13),
        8:  (3, 13), 9: (3, 13), 10: (4, 12),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1):
            if y in (5,) or x == x0:
                px[x, y] = N_GLOW
            elif y in (10,) or x == x1 - 1:
                px[x, y] = N_DARK
            elif (x + y) % 3 == 0:
                px[x, y] = N_RED
            else:
                px[x, y] = N_MAG
    # top sheen line
    for x in range(4, 12):
        px[x, 5] = N_BRIGHT
    save(img, os.path.join(ITEM_DIR, "nerosium_ingot.png"))


def gen_dust():
    rng = random.Random(404)
    img = new_img()
    px = img.load()
    # loose pile of granules in lower-center
    for _ in range(70):
        x = rng.randint(3, 12)
        y = rng.randint(6, 13)
        # pile shape: more toward bottom-center
        if rng.random() < (y - 4) / 12:
            px[x, y] = rng.choice([N_PURPLE, N_MAG, N_RED, N_REDHI, N_GLOW])
    # a few stray sparkles up top
    for (sx, sy) in [(5, 4), (10, 5), (8, 3)]:
        px[sx, sy] = N_GLOW
    save(img, os.path.join(ITEM_DIR, "nerosium_dust.png"))


def gen_raw():
    rng = random.Random(505)
    img = new_img()
    px = img.load()
    # irregular nugget cluster
    shape = [
        (6,4),(7,4),(8,4),
        (5,5),(6,5),(7,5),(8,5),(9,5),(10,5),
        (4,6),(5,6),(6,6),(7,6),(8,6),(9,6),(10,6),(11,6),
        (4,7),(5,7),(6,7),(7,7),(8,7),(9,7),(10,7),(11,7),
        (4,8),(5,8),(6,8),(7,8),(8,8),(9,8),(10,8),(11,8),
        (5,9),(6,9),(7,9),(8,9),(9,9),(10,9),(11,9),
        (5,10),(6,10),(7,10),(8,10),(9,10),(10,10),
        (6,11),(7,11),(8,11),(9,11),
    ]
    for (x, y) in shape:
        c = rng.choice([N_PURPLE, N_MAG, N_RED, N_DARK])
        px[x, y] = c
    # highlights and shadow for volume
    for (x, y) in shape:
        if x <= 5 or y <= 5:
            if rng.random() < 0.5:
                px[x, y] = N_GLOW if rng.random() < 0.5 else N_REDHI
        if x >= 10 or y >= 10:
            if rng.random() < 0.5:
                px[x, y] = N_DARK
    save(img, os.path.join(ITEM_DIR, "raw_nerosium.png"))


def gen_pickaxe():
    img = new_img()
    px = img.load()
    # wooden handle: clean diagonal from head socket to bottom-right
    handle = [(8, 6), (9, 7), (10, 8), (11, 9), (12, 10), (12, 11)]
    for (x, y) in handle:
        px[x, y] = WOOD[0]
        if y + 1 < S:
            px[x, y + 1] = WOOD_D       # under-shadow
        if x + 1 < S:
            px[x + 1, y] = WOOD[1]      # side
    # pick head: symmetric arc across the top (two pointed wings + middle bar)
    head_top = [(4,2),(5,2),(10,2),(11,2)]
    head_mid = [(3,3),(4,3),(5,3),(6,3),(9,3),(10,3),(11,3),(12,3)]
    head_bar = [(5,4),(6,4),(7,4),(8,4),(9,4),(10,4)]
    socket   = [(7,5),(8,5)]
    for (x, y) in head_top:
        px[x, y] = N_GLOW
    for (x, y) in head_mid:
        px[x, y] = N_REDHI if x in (3, 12) else N_RED
    for (x, y) in head_bar:
        px[x, y] = N_MAG
    for (x, y) in socket:
        px[x, y] = N_PURPLE
    # shading: darken the outer/lower edge of the head
    for (x, y) in [(3,3),(12,3),(5,4),(10,4)]:
        px[x, y] = N_DARK
    save(img, os.path.join(ITEM_DIR, "nerosium_pickaxe.png"))


if __name__ == "__main__":
    gen_ore(STONE, "nerosium_ore")
    gen_ore(DEEP, "deepslate_nerosium_ore")
    gen_storage_block()
    gen_raw_block()
    gen_grinder()
    gen_ingot()
    gen_dust()
    gen_raw()
    gen_pickaxe()
    print("done")
