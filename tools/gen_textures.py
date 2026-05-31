#!/usr/bin/env python3
"""
Generate starter 16x16 pixel-art textures for the Nerospace mod.

Phase 1/2 (nerosium) -> red/purple cosmic crystal.
Phase 3 (Greenxertz) -> green/steel, to set the planet apart.

Outputs PNGs into src/main/resources/assets/nerospace/textures/{block,item}.
Deterministic (seeded) so re-runs are stable.
"""
import os
import random
import sys
import hashlib
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
N_DARK   = (43, 13, 58, 255)
N_PURPLE = (106, 31, 140, 255)
N_MAG    = (179, 39, 158, 255)
N_RED    = (224, 58, 58, 255)
N_REDHI  = (255, 90, 96, 255)
N_GLOW   = (255, 138, 216, 255)
N_BRIGHT = (255, 208, 240, 255)
NEROS = [N_DARK, N_PURPLE, N_MAG, N_RED, N_REDHI, N_GLOW]

# stone / deepslate / metal bases
STONE   = [(122,122,122,255),(132,132,132,255),(112,112,112,255),(140,140,140,255)]
DEEP    = [(71,71,74,255),(80,80,84,255),(63,63,66,255),(88,88,92,255)]
METAL   = [(58,52,66,255),(70,63,80,255),(48,43,56,255),(82,75,92,255)]
METAL_L = (120, 110, 134, 255)
METAL_D = (34, 30, 42, 255)
WOOD    = [(92,62,38,255),(78,52,32,255),(104,72,46,255)]
WOOD_D  = (54, 36, 22, 255)

# Greenxertz (Phase 3) palette — green / steel.
G_DARK    = (16, 40, 28, 255)
G_STEEL_D = (54, 64, 58, 255)
G_STEEL   = (96, 112, 100, 255)
G_STEEL_L = (150, 170, 150, 255)
G_GREEN   = (60, 170, 90, 255)
G_GREEN_L = (120, 230, 140, 255)
G_GLOW    = (180, 255, 190, 255)
STEEL_RAMP = [G_STEEL_D, G_STEEL, G_GREEN, G_GREEN_L, G_GLOW]
# Xertz quartz — pale green/white crystal
Q_WHITE  = (228, 242, 228, 255)
Q_PALE   = (186, 222, 192, 255)
Q_GREEN  = (138, 200, 150, 255)
Q_SHADOW = (104, 150, 116, 255)
QUARTZ_RAMP = [Q_SHADOW, Q_GREEN, Q_PALE, Q_WHITE]

# Rocket / Phase 4 palette
FUEL     = (255, 150, 40, 255)
FUEL_D   = (200, 92, 20, 255)
FUEL_HI  = (255, 214, 120, 255)
R_WHITE  = (232, 232, 244, 255)
R_GRAY   = (150, 150, 168, 255)
R_DARK   = (70, 70, 86, 255)
R_WINDOW = (150, 220, 255, 255)
GOLD     = (240, 200, 80, 255)
HAZ_Y    = (250, 200, 50, 255)
HAZ_K    = (28, 28, 34, 255)


def new_img():
    return Image.new("RGBA", (S, S), CLEAR)


def noise_fill(img, palette, rng):
    px = img.load()
    for y in range(S):
        for x in range(S):
            px[x, y] = rng.choice(palette)
    return img


def blob(img, cx, cy, r, colors, rng, density=0.85):
    """Scatter a rough circular cluster of specks."""
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r and rng.random() < density * (1 - d / (r + 0.001)):
                t = 1 - d / (r + 0.001)
                idx = min(len(colors) - 1, int(t * len(colors)))
                px[x, y] = colors[idx]


def bevel(img, light, dark):
    """1px highlight on top/left, shadow on bottom/right."""
    px = img.load()
    for i in range(S):
        px[i, 0] = light
        px[0, i] = light
        px[i, S - 1] = dark
        px[S - 1, i] = dark


def save(img, path):
    # ADDITIVE-ONLY: never clobber an existing asset (pass --force to override).
    if os.path.exists(path) and "--force" not in sys.argv:
        print("skip (exists)", os.path.relpath(path, ROOT))
        return
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


# ---------------- PHASE 1/2: NEROSIUM ----------------

def gen_ore(base_palette, name):
    rng = random.Random(int(hashlib.md5(name.encode()).hexdigest(), 16) & 0xffffffff)
    img = new_img()
    noise_fill(img, base_palette, rng)
    for (cx, cy, r) in [(4, 5, 3.2), (11, 6, 2.6), (7, 12, 3.0), (13, 12, 1.8)]:
        blob(img, cx, cy, r, NEROS, rng, density=0.95)
    px = img.load()
    for (gx, gy) in [(4, 4), (11, 12)]:
        px[gx, gy] = N_BRIGHT
    save(img, os.path.join(BLOCK_DIR, name + ".png"))


def gen_storage_block():
    rng = random.Random(101)
    img = new_img()
    noise_fill(img, [N_PURPLE, N_MAG, N_DARK, (90, 26, 120, 255)], rng)
    px = img.load()
    for y in range(S):
        for x in range(S):
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                px[x, y] = N_GLOW
            elif (x + y) % 8 == 4:
                px[x, y] = N_RED
    blob(img, 8, 8, 3.4, [N_RED, N_REDHI, N_GLOW, N_BRIGHT], rng, density=1.0)
    bevel(img, N_GLOW, N_DARK)
    save(img, os.path.join(BLOCK_DIR, "nerosium_block.png"))


def gen_raw_block():
    rng = random.Random(202)
    img = new_img()
    noise_fill(img, [N_DARK, (60, 20, 40, 255), (52, 16, 60, 255)], rng)
    for (cx, cy, r) in [(4, 4, 2.6), (11, 5, 2.4), (5, 11, 2.4), (12, 12, 2.8), (8, 8, 1.8)]:
        blob(img, cx, cy, r, [N_PURPLE, N_MAG, N_RED, N_REDHI], rng, density=0.95)
    bevel(img, (120, 50, 90, 255), N_DARK)
    save(img, os.path.join(BLOCK_DIR, "raw_nerosium_block.png"))


def gen_grinder():
    rng = random.Random(303)
    img = new_img()
    noise_fill(img, METAL, rng)
    px = img.load()
    bevel(img, METAL_L, METAL_D)
    for i in range(S):
        px[1, i] = METAL_L if i < S - 1 else METAL_D
        px[i, 1] = METAL_L if i < S - 1 else METAL_D
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = METAL_D
    for y in range(5, 11):
        for x in range(5, 11):
            px[x, y] = N_RED if (x + y) % 2 == 0 else N_PURPLE
    for y in range(7, 9):
        for x in range(7, 9):
            px[x, y] = N_GLOW
    for (bx, by) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[bx, by] = METAL_L
    save(img, os.path.join(BLOCK_DIR, "nerosium_grinder.png"))


def gen_ingot():
    img = new_img()
    px = img.load()
    rows = {5: (4, 12), 6: (3, 13), 7: (3, 13), 8: (3, 13), 9: (3, 13), 10: (4, 12)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1):
            if y == 5 or x == x0:
                px[x, y] = N_GLOW
            elif y == 10 or x == x1 - 1:
                px[x, y] = N_DARK
            elif (x + y) % 3 == 0:
                px[x, y] = N_RED
            else:
                px[x, y] = N_MAG
    for x in range(4, 12):
        px[x, 5] = N_BRIGHT
    save(img, os.path.join(ITEM_DIR, "nerosium_ingot.png"))


def gen_dust():
    rng = random.Random(404)
    img = new_img()
    px = img.load()
    for _ in range(70):
        x = rng.randint(3, 12)
        y = rng.randint(6, 13)
        if rng.random() < (y - 4) / 12:
            px[x, y] = rng.choice([N_PURPLE, N_MAG, N_RED, N_REDHI, N_GLOW])
    for (sx, sy) in [(5, 4), (10, 5), (8, 3)]:
        px[sx, sy] = N_GLOW
    save(img, os.path.join(ITEM_DIR, "nerosium_dust.png"))


def gen_raw():
    rng = random.Random(505)
    img = new_img()
    px = img.load()
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
        px[x, y] = rng.choice([N_PURPLE, N_MAG, N_RED, N_DARK])
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
    handle = [(8, 6), (9, 7), (10, 8), (11, 9), (12, 10), (12, 11)]
    for (x, y) in handle:
        px[x, y] = WOOD[0]
        if y + 1 < S:
            px[x, y + 1] = WOOD_D
        if x + 1 < S:
            px[x + 1, y] = WOOD[1]
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
    for (x, y) in [(3,3),(12,3),(5,4),(10,4)]:
        px[x, y] = N_DARK
    save(img, os.path.join(ITEM_DIR, "nerosium_pickaxe.png"))


# ---------------- PHASE 3: GREENXERTZ ----------------

def gen_nerosteel_ore():
    rng = random.Random(601)
    img = new_img()
    noise_fill(img, STONE, rng)
    for (cx, cy, r) in [(5, 5, 3.0), (11, 7, 2.6), (8, 12, 2.8)]:
        blob(img, cx, cy, r, STEEL_RAMP, rng, density=0.95)
    px = img.load()
    px[5, 4] = G_GLOW
    px[11, 7] = G_GLOW
    save(img, os.path.join(BLOCK_DIR, "nerosteel_ore.png"))


def gen_xertz_quartz_ore():
    rng = random.Random(602)
    img = new_img()
    noise_fill(img, STONE, rng)
    for (cx, cy, r) in [(4, 5, 2.4), (11, 5, 2.2), (7, 11, 2.6), (12, 12, 1.8)]:
        blob(img, cx, cy, r, QUARTZ_RAMP, rng, density=0.95)
    px = img.load()
    for (gx, gy) in [(4, 4), (11, 4), (7, 10)]:
        px[gx, gy] = Q_WHITE
    save(img, os.path.join(BLOCK_DIR, "xertz_quartz_ore.png"))


def gen_nerosteel_block():
    rng = random.Random(603)
    img = new_img()
    noise_fill(img, [G_STEEL, G_STEEL_D, (88, 104, 92, 255)], rng)
    px = img.load()
    for y in range(S):
        if y % 3 == 0:
            for x in range(S):
                if rng.random() < 0.5:
                    px[x, y] = G_STEEL_L
    for i in range(S):
        px[i, 8] = G_GREEN
        px[8, i] = G_GREEN
    px[8, 8] = G_GLOW
    bevel(img, G_STEEL_L, G_DARK)
    for (bx, by) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[bx, by] = G_GLOW
    save(img, os.path.join(BLOCK_DIR, "nerosteel_block.png"))


def gen_raw_nerosteel():
    rng = random.Random(604)
    img = new_img()
    px = img.load()
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
        px[x, y] = rng.choice([G_STEEL, G_STEEL_D, G_GREEN, G_DARK])
    for (x, y) in shape:
        if x <= 5 or y <= 5:
            if rng.random() < 0.5:
                px[x, y] = G_GLOW if rng.random() < 0.4 else G_STEEL_L
        if x >= 10 or y >= 10:
            if rng.random() < 0.5:
                px[x, y] = G_DARK
    save(img, os.path.join(ITEM_DIR, "raw_nerosteel.png"))


def gen_nerosteel_ingot():
    img = new_img()
    px = img.load()
    rows = {5: (4, 12), 6: (3, 13), 7: (3, 13), 8: (3, 13), 9: (3, 13), 10: (4, 12)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1):
            if x == x0:
                px[x, y] = G_STEEL_L
            elif y == 10 or x == x1 - 1:
                px[x, y] = G_DARK
            elif (x + y) % 4 == 0:
                px[x, y] = G_GREEN
            else:
                px[x, y] = G_STEEL
    for x in range(4, 12):
        px[x, 5] = G_GLOW
    save(img, os.path.join(ITEM_DIR, "nerosteel_ingot.png"))


def gen_xertz_quartz():
    img = new_img()
    px = img.load()
    shape = {
        3: (7, 9), 4: (6, 10), 5: (5, 11), 6: (4, 12),
        7: (4, 12), 8: (4, 12), 9: (5, 11), 10: (6, 10),
        11: (7, 9), 12: (8, 8),
    }
    for y, (x0, x1) in shape.items():
        mid = (x0 + x1) / 2
        for x in range(x0, x1):
            if abs(x - mid) < 1:
                px[x, y] = Q_WHITE
            elif x < mid:
                px[x, y] = Q_PALE
            else:
                px[x, y] = Q_GREEN
    for x in range(7, 9):
        px[x, 3] = Q_WHITE
    for y, (x0, x1) in shape.items():
        if y >= 10:
            px[x1 - 1, y] = Q_SHADOW
    save(img, os.path.join(ITEM_DIR, "xertz_quartz.png"))


def gen_greenxertz_navigator():
    img = new_img()
    px = img.load()
    cx = cy = 8
    for y in range(S):
        for x in range(S):
            d = ((x - cx + 0.5) ** 2 + (y - cy + 0.5) ** 2) ** 0.5
            if d <= 6.5:
                if d > 5.3:
                    px[x, y] = METAL_D
                elif d > 4.6:
                    px[x, y] = METAL_L
                else:
                    px[x, y] = (24, 36, 30, 255)
    for (x, y) in [(8,8),(8,7),(9,6),(9,5),(10,5)]:
        px[x, y] = G_GLOW
    px[10, 4] = G_GREEN_L
    for (x, y) in [(8,9),(7,10),(7,11)]:
        px[x, y] = N_RED
    for (x, y) in [(8, 3), (8, 12), (3, 8), (12, 8)]:
        px[x, y] = G_GREEN
    px[8, 8] = Q_WHITE
    save(img, os.path.join(ITEM_DIR, "greenxertz_navigator.png"))



# ---------------- PHASE 4: ROCKETS ----------------

def _draw_rocket(px, nose, fin, stripe, boosters=False, glow=None):
    """Draw a 16x16 vertical rocket sprite onto an already-loaded px buffer."""
    body_cols = (6, 7, 8, 9)
    # nose cone
    px[7, 1] = nose; px[8, 1] = nose
    px[7, 2] = nose; px[8, 2] = nose
    for c in (6, 7, 8, 9):
        px[c, 3] = nose
    # body (white) rows 4..10
    for y in range(4, 11):
        for c in body_cols:
            px[c, y] = R_WHITE
    # shading: left col light, right col dark
    for y in range(4, 11):
        px[6, y] = R_WHITE
        px[9, y] = R_GRAY
    # window
    px[7, 5] = R_WINDOW; px[8, 5] = R_WINDOW
    px[7, 6] = R_WINDOW; px[8, 6] = (90, 160, 210, 255)
    # accent stripe
    for c in body_cols:
        px[c, 8] = stripe
    # fins
    for y in (10, 11, 12):
        px[5, y] = fin
        px[10, y] = fin
    px[4, 12] = fin; px[11, 12] = fin
    # nozzle
    for c in (6, 7, 8, 9):
        px[c, 11] = R_DARK
    # boosters (higher tiers)
    if boosters:
        for y in range(6, 12):
            px[4, y] = R_GRAY
            px[11, y] = R_GRAY
        px[4, 12] = fin; px[11, 12] = fin
    # flame
    px[7, 13] = FUEL_HI; px[8, 13] = FUEL_HI
    px[6, 13] = FUEL; px[9, 13] = FUEL
    px[7, 14] = FUEL; px[8, 14] = FUEL
    px[7, 15] = FUEL_D; px[8, 15] = FUEL_D
    if glow:
        for c in body_cols:
            px[c, 4] = glow


def gen_rocket_tier(name, nose, fin, stripe, boosters=False, glow=None):
    img = new_img()
    _draw_rocket(img.load(), nose, fin, stripe, boosters, glow)
    save(img, os.path.join(ITEM_DIR, name + ".png"))


def gen_rocket_fuel_canister():
    img = new_img()
    px = img.load()
    # cap + spout
    for c in (6, 7, 8):
        px[c, 2] = R_DARK
    px[9, 3] = R_GRAY
    # body
    for y in range(4, 14):
        for x in range(4, 12):
            px[x, y] = R_GRAY
    # bevel-ish shading
    for y in range(4, 14):
        px[4, y] = R_WHITE
        px[11, y] = R_DARK
    for x in range(4, 12):
        px[x, 4] = R_WHITE if x < 11 else R_DARK
        px[x, 13] = R_DARK
    # fuel window glowing
    for y in range(6, 12):
        for x in range(6, 10):
            px[x, y] = FUEL if (x + y) % 2 == 0 else FUEL_D
    px[6, 6] = FUEL_HI; px[7, 6] = FUEL_HI
    px[8, 10] = FUEL_HI
    # hazard tick label
    px[5, 5] = HAZ_Y; px[10, 5] = HAZ_Y
    save(img, os.path.join(ITEM_DIR, "rocket_fuel_canister.png"))


def gen_rocket_launch_pad():
    rng = random.Random(701)
    img = new_img()
    noise_fill(img, METAL, rng)
    px = img.load()
    # hazard border (top + bottom rows)
    for x in range(S):
        c = HAZ_Y if (x // 2) % 2 == 0 else HAZ_K
        px[x, 0] = c
        px[x, 1] = HAZ_K if c == HAZ_Y else HAZ_Y
        px[x, S - 1] = c
        px[x, S - 2] = HAZ_K if c == HAZ_Y else HAZ_Y
    # central landing ring
    cx = cy = 8
    for y in range(S):
        for x in range(S):
            d = ((x - cx + 0.5) ** 2 + (y - cy + 0.5) ** 2) ** 0.5
            if 3.6 <= d <= 4.6:
                px[x, y] = N_RED
            elif d < 3.6:
                px[x, y] = (24, 18, 30, 255)
    # crosshair
    for i in range(5, 11):
        px[i, 8] = N_GLOW
        px[8, i] = N_GLOW
    px[8, 8] = N_BRIGHT
    save(img, os.path.join(BLOCK_DIR, "rocket_launch_pad.png"))


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
    gen_nerosteel_ore()
    gen_xertz_quartz_ore()
    gen_nerosteel_block()
    gen_raw_nerosteel()
    gen_nerosteel_ingot()
    gen_xertz_quartz()
    gen_greenxertz_navigator()
    # Phase 4 — rockets
    gen_rocket_launch_pad()
    gen_rocket_fuel_canister()
    gen_rocket_tier("rocket_tier_1", N_RED, N_REDHI, N_MAG, boosters=False, glow=None)
    gen_rocket_tier("rocket_tier_2", N_PURPLE, N_MAG, N_GLOW, boosters=True, glow=None)
    gen_rocket_tier("rocket_tier_3", GOLD, G_GREEN_L, GOLD, boosters=True, glow=G_GLOW)
    print("done")
