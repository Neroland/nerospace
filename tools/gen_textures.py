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

try:
    from PIL import Image
except ModuleNotFoundError:
    # Pillow is optional: textures are committed art, so on a machine without it we just skip
    # regeneration (e.g. so `gradlew genAssets` can still run gen_bbmodels.py). `pip install pillow`
    # to enable procedural texture (re)generation.
    print("gen_textures: Pillow not installed; skipping texture generation (pip install pillow).")
    sys.exit(0)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOCK_DIR = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/block")
ITEM_DIR = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/item")
ENTITY_DIR = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/entity")
PARTICLE_DIR = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/particle")
GUI_DIR = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/gui")
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)
os.makedirs(ENTITY_DIR, exist_ok=True)
os.makedirs(PARTICLE_DIR, exist_ok=True)
os.makedirs(GUI_DIR, exist_ok=True)

S = 16  # texture size
ES = 64  # entity texture size (matches the GreenxertzCreatureModel LayerDefinition 64x64)

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

# Cindara / Phase 7 palette — charred volcanic ash + ember.
C_DARK   = (28, 12, 10, 255)
C_ASH    = (60, 50, 48, 255)
C_RED    = (200, 50, 30, 255)
C_ORANGE = (240, 120, 40, 255)
C_EMBER  = (255, 180, 70, 255)
C_GLOW   = (255, 230, 150, 255)
EMBER_RAMP = [C_DARK, C_RED, C_ORANGE, C_EMBER, C_GLOW]

# Glacira palette (NEW_DESTINATION_DESIGN.md) — frozen ice moon: deep glacial blue -> frost white.
I_DEEP  = (10, 30, 55, 255)
I_BLUE  = (60, 130, 200, 255)
I_CYAN  = (120, 210, 240, 255)
I_FROST = (200, 240, 255, 255)
I_WHITE = (240, 252, 255, 255)
FROST_RAMP = [I_DEEP, I_BLUE, I_CYAN, I_FROST, I_WHITE]


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
    # Vanilla-style orientation: head in the TOP-LEFT (two up-tips), handle diagonal to the
    # BOTTOM-RIGHT. The handheld display transform treats the top-left as the working tip, so this
    # renders head-up in hand (the old top-centre/vertical-handle layout looked upside down).
    img = new_img()
    px = img.load()
    for (x, y) in [(1, 1), (2, 1), (6, 1), (7, 1)]:
        px[x, y] = N_GLOW
    for (x, y) in [(1, 2), (2, 2), (3, 2), (5, 2), (6, 2), (7, 2)]:
        px[x, y] = N_REDHI if x in (1, 7) else N_RED
    for (x, y) in [(2, 3), (3, 3), (4, 3), (5, 3)]:
        px[x, y] = N_MAG
    for (x, y) in [(3, 4), (4, 4)]:
        px[x, y] = N_PURPLE
    for (x, y) in [(4, 5), (5, 5)]:
        px[x, y] = N_DARK
    for (x, y) in [(6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11), (12, 12)]:
        px[x, y] = WOOD[0]
        if x + 1 < S:
            px[x + 1, y] = WOOD_D
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


def gen_destination_compass(name, needle):
    img = new_img()
    px = img.load()
    cx = cy = 8
    for y in range(S):
        for x in range(S):
            d = ((x - cx + 0.5) ** 2 + (y - cy + 0.5) ** 2) ** 0.5
            if d <= 7:
                if d > 6:
                    px[x, y] = METAL_D
                elif d > 5:
                    px[x, y] = METAL_L
                else:
                    px[x, y] = (40, 42, 52, 255)
    # needle: coloured north half + white south half
    for (x, y) in [(8, 4), (8, 5), (8, 6), (7, 5), (9, 5)]:
        px[x, y] = needle
    for (x, y) in [(8, 9), (8, 10), (8, 11)]:
        px[x, y] = R_WHITE
    px[8, 8] = (220, 220, 230, 255)
    save(img, os.path.join(ITEM_DIR, name + ".png"))


def gen_rocket_fuel_bucket():
    img = new_img()
    px = img.load()
    # handle arc
    for (x, y) in [(4, 3), (5, 2), (6, 2), (9, 2), (10, 2), (11, 3), (4, 4), (11, 4)]:
        px[x, y] = R_GRAY
    # rim
    for x in range(3, 13):
        px[x, 5] = R_WHITE
    # body (narrowing trapezoid)
    rows = {6: (3, 13), 7: (3, 13), 8: (4, 12), 9: (4, 12),
            10: (4, 12), 11: (5, 11), 12: (5, 11), 13: (6, 10)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1):
            if x == x0:
                px[x, y] = R_WHITE
            elif x == x1 - 1:
                px[x, y] = R_DARK
            else:
                px[x, y] = R_GRAY
    # fuel surface (orange) just below the rim
    for x in range(4, 12):
        px[x, 6] = FUEL
    for x in range(5, 11):
        px[x, 7] = FUEL_D
    px[6, 6] = FUEL_HI
    px[9, 6] = FUEL_HI
    for x in range(6, 10):
        px[x, 13] = R_DARK
    save(img, os.path.join(ITEM_DIR, "rocket_fuel_bucket.png"))


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


def gen_fuel_tank():
    """Metal tank with a central glass window showing an orange fuel column."""
    rng = random.Random(810)
    img = new_img()
    noise_fill(img, METAL, rng)
    px = img.load()
    bevel(img, METAL_L, METAL_D)
    # Glass window: a recessed column down the middle showing fuel.
    win_x0, win_x1 = 5, 10  # inclusive
    win_y0, win_y1 = 3, 12
    fuel_top = 6  # at/below this y inside the window is "fuel"
    for y in range(win_y0, win_y1 + 1):
        for x in range(win_x0, win_x1 + 1):
            if x == win_x0 or x == win_x1:
                px[x, y] = METAL_D  # window frame edges
            elif y < fuel_top:
                px[x, y] = (40, 36, 48, 255)  # empty headspace (dark glass)
            else:
                t = (y - fuel_top) / max(1, (win_y1 - fuel_top))
                ramp = [C_ORANGE, C_EMBER, (255, 150, 60, 255), C_RED]
                px[x, y] = ramp[min(len(ramp) - 1, int(t * len(ramp)))]
    # fuel surface highlight
    for x in range(win_x0 + 1, win_x1):
        px[x, fuel_top] = C_GLOW
    # corner rivets
    for (rx, ry) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[rx, ry] = METAL_L
    save(img, os.path.join(BLOCK_DIR, "fuel_tank.png"))


def gen_oxygen_generator():
    """Metal machine face with a glowing cyan vent core (oxygen) and bubble dots."""
    rng = random.Random(811)
    img = new_img()
    noise_fill(img, METAL, rng)
    px = img.load()
    bevel(img, METAL_L, METAL_D)
    O2_DARK = (18, 54, 64, 255)
    O2_MID = (60, 170, 200, 255)
    O2_HI = (150, 230, 250, 255)
    O2_GLOW = (210, 250, 255, 255)
    cx = cy = 8
    # central circular vent
    for y in range(S):
        for x in range(S):
            d = ((x - cx + 0.5) ** 2 + (y - cy + 0.5) ** 2) ** 0.5
            if d <= 2.0:
                px[x, y] = O2_GLOW
            elif d <= 3.2:
                px[x, y] = O2_HI
            elif d <= 4.2:
                px[x, y] = O2_MID
            elif d <= 4.9:
                px[x, y] = O2_DARK
    # vent grille lines across the core
    for x in range(4, 12):
        px[x, 8] = O2_DARK
    # rising "bubble" dots in the corners
    for (bx, by) in [(3, 12), (12, 11), (4, 4), (12, 4)]:
        px[bx, by] = O2_HI
        if by - 1 >= 0:
            px[bx, by - 1] = O2_MID
    # corner rivets
    for (rx, ry) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[rx, ry] = METAL_L
    save(img, os.path.join(BLOCK_DIR, "oxygen_generator.png"))


# ---------------- PHASE 7: CINDARA ----------------

def gen_cindrite_ore():
    rng = random.Random(901)
    img = new_img()
    noise_fill(img, STONE, rng)
    for (cx, cy, r) in [(5, 5, 3.0), (11, 7, 2.6), (8, 12, 2.8)]:
        blob(img, cx, cy, r, EMBER_RAMP, rng, density=0.95)
    px = img.load()
    px[5, 4] = C_GLOW
    px[11, 7] = C_GLOW
    save(img, os.path.join(BLOCK_DIR, "cindrite_ore.png"))


def gen_cindrite_block():
    rng = random.Random(902)
    img = new_img()
    noise_fill(img, [C_DARK, C_ASH, (40, 20, 16, 255)], rng)
    px = img.load()
    for i in range(S):
        px[i, 8] = C_ORANGE
        px[8, i] = C_ORANGE
    blob(img, 8, 8, 3.2, [C_RED, C_ORANGE, C_EMBER, C_GLOW], rng, density=1.0)
    bevel(img, C_EMBER, C_DARK)
    save(img, os.path.join(BLOCK_DIR, "cindrite_block.png"))


def gen_cindrite():
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
                px[x, y] = C_GLOW
            elif x < mid:
                px[x, y] = C_ORANGE
            else:
                px[x, y] = C_RED
    for x in range(7, 9):
        px[x, 3] = C_EMBER
    save(img, os.path.join(ITEM_DIR, "cindrite.png"))


# ---------------- GLACIRA (NEW_DESTINATION_DESIGN.md) ----------------

def gen_glacite_ore():
    rng = random.Random(905)
    img = new_img()
    noise_fill(img, STONE, rng)
    for (cx, cy, r) in [(5, 5, 3.0), (11, 7, 2.6), (8, 12, 2.8)]:
        blob(img, cx, cy, r, FROST_RAMP, rng, density=0.95)
    px = img.load()
    px[5, 4] = I_WHITE
    px[11, 7] = I_WHITE
    save(img, os.path.join(BLOCK_DIR, "glacite_ore.png"))


def gen_glacite_block():
    rng = random.Random(906)
    img = new_img()
    noise_fill(img, [I_DEEP, (24, 56, 92, 255), (40, 84, 130, 255)], rng)
    px = img.load()
    for i in range(S):
        px[i, 8] = I_CYAN
        px[8, i] = I_CYAN
    blob(img, 8, 8, 3.2, [I_BLUE, I_CYAN, I_FROST, I_WHITE], rng, density=1.0)
    bevel(img, I_FROST, I_DEEP)
    save(img, os.path.join(BLOCK_DIR, "glacite_block.png"))


def gen_glacite():
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
                px[x, y] = I_WHITE
            elif x < mid:
                px[x, y] = I_CYAN
            else:
                px[x, y] = I_BLUE
    for x in range(7, 9):
        px[x, 3] = I_FROST
    save(img, os.path.join(ITEM_DIR, "glacite.png"))


def gen_station_floor():
    rng = random.Random(710)
    img = new_img()
    noise_fill(img, [R_GRAY, (138, 138, 156, 255), (162, 162, 180, 255)], rng)
    px = img.load()
    # panel seams + central cross
    for i in range(S):
        px[i, 0] = R_DARK
        px[0, i] = R_DARK
        px[i, 8] = R_DARK
        px[8, i] = R_DARK
    bevel(img, R_WHITE, R_DARK)
    for (bx, by) in [(2, 2), (13, 2), (2, 13), (13, 13), (7, 7), (8, 8)]:
        px[bx, by] = R_WHITE  # rivets
    save(img, os.path.join(BLOCK_DIR, "station_floor.png"))


def gen_station_wall():
    rng = random.Random(711)
    img = new_img()
    noise_fill(img, [(176, 176, 192, 255), R_WHITE, (158, 158, 176, 255)], rng)
    px = img.load()
    for y in range(S):
        if y % 4 == 0:
            for x in range(S):
                px[x, y] = R_GRAY
    bevel(img, R_WHITE, R_DARK)
    for (bx, by) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[bx, by] = R_DARK
    save(img, os.path.join(BLOCK_DIR, "station_wall.png"))


# ---------------- PHASE 5/7/10: GREENXERTZ & CINDARA CREATURES (entities) ----------------
#
# 64x64 entity textures for the four bespoke creature models. Each model is box-UV with three
# texOffs regions shared across its parts:
#   - body parts   -> texOffs(0, 0)   => upper-left block,  UV area x[0..44]  y[0..28]
#   - head parts   -> texOffs(0, 28)  => lower-left block,  UV area x[0..44]  y[28..64]
#   - detail parts -> texOffs(44, 0)  => right strip,       UV area x[44..64] y[0..64]
#     (crests/fins/arms/legs for the stalker; crystals for the crawler; fronds/limbs for the
#      greenling; horns/back-plates/legs for the cinder stalker)
# We paint each region with a palette + pattern matched to the creature's identity, then drop
# eye glints onto the head's front-face UV cell. Brightest pixels (facets / quartz / embers /
# eyes) are picked up by gen_entity_glow() into the emissive overlay, so they read in the dark.
#
# These four files are committed art. The generators are additive (skip if the PNG exists) and
# only (re)render when invoked with the deliberate, creature-scoped --creatures flag, so they
# never trample the rest of the committed textures the way a global --force would.

FORCE_CREATURES = "--creatures" in sys.argv or "--force" in sys.argv


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def _eyes(px, head_w, head_d, eye, socket, glint=None, big=False):
    """Paint a pair of eyes on the head FRONT face. For box-UV the front face starts at
    (u+d, v+d) with size (w, h); heads use texOffs(0, 28) so u=0, v=28."""
    fx = head_d            # front-face left edge  (u + d)
    fy = 28 + head_d       # front-face top edge    (v + d)
    cx = fx + head_w / 2.0
    # eye sockets sit a little inset from centre, a third of the way down the face
    ey = int(fy + (3 if big else 2))
    off = max(1, int(head_w * (0.28 if big else 0.22)))
    ew = 2 if big else 1
    for sgn in (-1, 1):
        ex = int(round(cx + sgn * off)) - (ew // 2)
        for dx in range(ew):
            for dy in range(ew):
                px[ex + dx, ey + dy] = socket
        px[ex, ey] = eye
        if ew > 1:
            px[ex + 1, ey] = eye
        if glint is not None:
            px[ex, ey] = glint


def _gen_creature(name, paint_body, paint_head, paint_detail, head_w, head_d,
                  eye, socket, seed, glint=None, big_eyes=False):
    path = os.path.join(ENTITY_DIR, name + ".png")
    if os.path.exists(path) and not FORCE_CREATURES:
        print("skip (exists)", os.path.relpath(path, ROOT))
        return
    rng = random.Random(seed)
    img = Image.new("RGBA", (ES, ES), CLEAR)
    px = img.load()
    # Region 1: body (x 0..44, y 0..28)
    for y in range(0, 28):
        for x in range(0, 44):
            px[x, y] = paint_body(x, y, rng)
    # Region 2: head (x 0..44, y 28..64)
    for y in range(28, 64):
        for x in range(0, 44):
            px[x, y] = paint_head(x, y, rng)
    # Region 3: detail strip (x 44..64, y 0..64)
    for y in range(0, 64):
        for x in range(44, 64):
            px[x, y] = paint_detail(x, y, rng)
    _eyes(px, head_w, head_d, eye, socket, glint, big_eyes)
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


# --- Per-creature palettes ------------------------------------------------

# Xertz Stalker — crystalline apex predator: deep emerald/teal crystal hide with angular cyan
# facets and a bright cyan-white glow on its edges & eyes.
XS_DARK  = (12, 38, 40, 255)
XS_BODY  = (22, 70, 66, 255)
XS_MID   = (34, 120, 104, 255)
XS_FACET = (90, 220, 200, 255)
XS_GLOW  = (190, 255, 240, 255)

# Quartz Crawler — milky quartz carapace: pale grey-white plates with rose-quartz veins and
# soft green crystal sparkle on its back cluster.
# NB: only QC_CRY (the back crystal cluster) and the eye glint exceed the glow threshold (205);
# the carapace stays below it so the plates read as solid, non-emissive quartz.
QC_SEAM  = (118, 120, 134, 255)
QC_PLATE = (176, 178, 190, 255)
QC_HI    = (200, 202, 204, 255)
QC_ROSE  = (196, 150, 168, 255)
QC_CRY   = (150, 245, 180, 255)

# Greenling — soft friendly sprout: warm leafy greens, a lighter belly, gentle spots; small
# dark eyes with a tiny white glint (kept dim so it barely glows).
# Greenling stays soft: every body colour is below the glow threshold so only its tiny eye glint
# is emissive (a friendly creature, not a glowing one).
GL_DARK  = (40, 120, 58, 255)
GL_BODY  = (86, 190, 96, 255)
GL_LITE  = (150, 200, 140, 255)
GL_SPOT  = (64, 158, 74, 255)
GL_LEAF  = (120, 200, 120, 255)

# Cinder Stalker — ember-cracked volcanic hide: charcoal/obsidian rock veined with glowing
# orange-red lava cracks and bright ember eyes.
CS_ROCK  = (34, 26, 28, 255)
CS_ROCK2 = (52, 42, 44, 255)
CS_OBS   = (20, 14, 20, 255)
CS_RED   = (200, 56, 28, 255)
CS_ORANGE= (244, 128, 40, 255)
CS_EMBER = (255, 196, 96, 255)
CS_GLOW_EYE = (255, 232, 150, 255)


def _xs_body(x, y, rng):
    # angular crystalline facets: diagonal banding + sparse bright facet edges
    base = _mix(XS_DARK, XS_BODY, ((x + y) % 12) / 12.0)
    if (x * 2 + y) % 9 == 0:
        base = XS_MID
    if rng.random() < 0.06:
        base = XS_FACET
    return base


def _xs_head(x, y, rng):
    base = _mix(XS_BODY, XS_MID, (y % 10) / 10.0)
    if rng.random() < 0.05:
        base = XS_FACET
    return base


def _xs_detail(x, y, rng):
    # blade-arms / crest / fins: brighter crystal with glowing edges
    t = ((y) % 8) / 8.0
    base = _mix(XS_MID, XS_FACET, t)
    if (x + y) % 6 == 0:
        base = XS_GLOW
    return base


def _qc_body(x, y, rng):
    # domed carapace plates: light quartz with darker seams on a grid + rose veins
    seam = (x % 11 == 0) or (y % 9 == 0)
    base = QC_SEAM if seam else _mix(QC_PLATE, QC_HI, rng.random() * 0.5)
    if rng.random() < 0.04:
        base = QC_ROSE
    return base


def _qc_head(x, y, rng):
    base = _mix(QC_PLATE, QC_HI, (y % 6) / 6.0)
    if y % 7 == 0:
        base = QC_SEAM
    return base


def _qc_detail(x, y, rng):
    # back crystal cluster + legs: green-quartz sparkle
    base = _mix(QC_HI, QC_CRY, ((x + y) % 7) / 7.0)
    if rng.random() < 0.10:
        base = QC_CRY
    return base


def _gl_body(x, y, rng):
    # soft mottled green, lighter toward the belly band (lower rows)
    t = y / 28.0
    base = _mix(GL_BODY, GL_LITE, t * 0.7)
    if rng.random() < 0.05:
        base = GL_SPOT
    return base


def _gl_head(x, y, rng):
    base = _mix(GL_BODY, GL_LITE, ((y - 28) % 8) / 12.0)
    if rng.random() < 0.04:
        base = GL_SPOT
    return base


def _gl_detail(x, y, rng):
    # leaf crest + little limbs
    base = _mix(GL_LEAF, GL_LITE, (y % 6) / 6.0)
    if x % 4 == 0:
        base = GL_DARK
    return base


def _cs_body(x, y, rng):
    # dark rock with branching glowing lava cracks
    base = CS_ROCK if rng.random() < 0.55 else CS_ROCK2
    crack = ((x * 3 + y * 2) % 17 < 2) or ((x - y) % 13 == 0)
    if crack:
        r = rng.random()
        base = CS_RED if r < 0.4 else (CS_ORANGE if r < 0.8 else CS_EMBER)
    return base


def _cs_head(x, y, rng):
    base = CS_ROCK if rng.random() < 0.6 else CS_OBS
    if ((x + y) % 11) < 2:
        base = CS_RED if rng.random() < 0.6 else CS_ORANGE
    return base


def _cs_detail(x, y, rng):
    # horns/back-plates = obsidian; back-plate ridges glow hot at the seams
    base = CS_OBS if rng.random() < 0.7 else CS_ROCK
    if y % 6 == 0:
        base = CS_ORANGE if rng.random() < 0.5 else CS_EMBER
    return base


# Frost Strider — Glacira's stilt-legged ice predator: deep glacial blue hide sheeted with pale
# ice plates; bright frost-white shard edges and cold cyan eyes glow in the dark.
FS_DEEP  = (14, 36, 62, 255)
FS_BODY  = (32, 72, 112, 255)
FS_ICE   = (84, 150, 196, 255)
FS_PLATE = (150, 212, 240, 255)
FS_GLOW  = (220, 248, 255, 255)


def _fs_body(x, y, rng):
    # sheeted ice plates: horizontal banding with frozen seams + sparse bright plate glints
    base = _mix(FS_DEEP, FS_BODY, ((y * 2 + x) % 10) / 10.0)
    if y % 7 == 0:
        base = FS_ICE
    if rng.random() < 0.05:
        base = FS_PLATE
    return base


def _fs_head(x, y, rng):
    base = _mix(FS_BODY, FS_ICE, ((y - 28) % 8) / 8.0)
    if rng.random() < 0.05:
        base = FS_PLATE
    return base


def _fs_detail(x, y, rng):
    # shards + stilt legs: pale ice with glowing frost edges
    t = (y % 8) / 8.0
    base = _mix(FS_ICE, FS_PLATE, t)
    if (x + y) % 6 == 0:
        base = FS_GLOW
    return base


def gen_creatures():
    # head_w / head_d are the model head's width & depth (for front-face eye placement).
    _gen_creature("xertz_stalker", _xs_body, _xs_head, _xs_detail,
                  head_w=6, head_d=7, eye=XS_GLOW, socket=XS_DARK, seed=501, glint=XS_GLOW)
    _gen_creature("quartz_crawler", _qc_body, _qc_head, _qc_detail,
                  head_w=6, head_d=4, eye=QC_CRY, socket=QC_SEAM, seed=502, glint=(220, 255, 235, 255))
    _gen_creature("greenling", _gl_body, _gl_head, _gl_detail,
                  head_w=8, head_d=8, eye=(20, 40, 24, 255), socket=GL_DARK, seed=503,
                  glint=(235, 255, 235, 255), big_eyes=True)
    _gen_creature("cinder_stalker", _cs_body, _cs_head, _cs_detail,
                  head_w=8, head_d=8, eye=CS_EMBER, socket=CS_OBS, seed=504, glint=CS_GLOW_EYE)
    _gen_creature("frost_strider", _fs_body, _fs_head, _fs_detail,
                  head_w=5, head_d=7, eye=FS_GLOW, socket=FS_DEEP, seed=505, glint=FS_GLOW)


# Egg silhouette mask (per-row x range) for a 16x16 vanilla-style spawn egg: pointed top, round
# bottom. Outer cells are the outline; inner cells take the base colour with accent spots.
_EGG_ROWS = {
    2: (7, 9), 3: (6, 10), 4: (6, 10), 5: (5, 11), 6: (5, 11), 7: (4, 12), 8: (4, 12),
    9: (4, 12), 10: (4, 12), 11: (4, 12), 12: (5, 11), 13: (5, 11), 14: (6, 10),
}


def gen_spawn_egg(name, base, spot, outline, seed):
    """A classic spawn-egg icon: base-coloured egg with accent spots and a dark outline, matched to
    the creature's palette. Additive: only (re)rendered under the --creatures flag."""
    path = os.path.join(ITEM_DIR, name + "_spawn_egg.png")
    if os.path.exists(path) and not FORCE_CREATURES:
        print("skip (exists)", os.path.relpath(path, ROOT))
        return
    rng = random.Random(seed)
    img = Image.new("RGBA", (S, S), CLEAR)
    px = img.load()
    for y, (x0, x1) in _EGG_ROWS.items():
        for x in range(x0, x1 + 1):
            edge = (x == x0 or x == x1 or y == min(_EGG_ROWS) or y == max(_EGG_ROWS)
                    or (y - 1 not in _EGG_ROWS) or (y + 1 not in _EGG_ROWS))
            if edge:
                px[x, y] = outline
            else:
                px[x, y] = spot if rng.random() < 0.32 else base
    # subtle top-left highlight for a rounded read
    for (hx, hy) in [(7, 4), (8, 4), (6, 5)]:
        r, g, b, a = px[hx, hy]
        if a:
            px[hx, hy] = (min(255, r + 40), min(255, g + 40), min(255, b + 40), 255)
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


def gen_spawn_eggs():
    gen_spawn_egg("xertz_stalker", XS_BODY, XS_FACET, XS_DARK, 611)
    gen_spawn_egg("quartz_crawler", (188, 190, 202, 255), QC_CRY, QC_SEAM, 612)
    gen_spawn_egg("greenling", GL_BODY, GL_LITE, GL_DARK, 613)
    gen_spawn_egg("cinder_stalker", CS_ROCK2, CS_ORANGE, CS_OBS, 614)
    gen_spawn_egg("frost_strider", FS_BODY, FS_PLATE, FS_DEEP, 615)


def gen_entity_rocket():
    """64x64 entity texture for the rocket model (metallic body + red accent band)."""
    path = os.path.join(ENTITY_DIR, "rocket.png")
    if os.path.exists(path) and "--force" not in sys.argv:
        print("skip (exists)", os.path.relpath(path, ROOT))
        return
    rng = random.Random(901)
    img = Image.new("RGBA", (ES, ES), CLEAR)
    px = img.load()
    for y in range(ES):
        for x in range(ES):
            c = rng.choice([R_WHITE, R_WHITE, R_GRAY])
            if 18 <= (y % 28) <= 21:
                c = N_RED if (x % 2 == 0) else N_REDHI
            if (y % 28) >= 26:
                c = R_DARK
            px[x, y] = c
    for (wx, wy) in [(10, 8), (11, 8), (10, 9), (11, 9)]:
        px[wx, wy] = R_WINDOW
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


# ---------------- OXYGEN / TERRAFORM (particles + machine) ----------------

def gen_oxygen_particle():
    """8x8 soft white radial dot (tinted in code: cyan for O2, green for terraform)."""
    path = os.path.join(PARTICLE_DIR, "oxygen.png")
    if os.path.exists(path) and "--force" not in sys.argv:
        print("skip (exists)", os.path.relpath(path, ROOT))
        return
    n = 8
    img = Image.new("RGBA", (n, n), CLEAR)
    px = img.load()
    cx = cy = (n - 1) / 2.0
    for y in range(n):
        for x in range(n):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            a = max(0.0, 1.0 - d / (n / 2.0))
            px[x, y] = (255, 255, 255, int(255 * (a ** 1.6)))
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


def gen_terraformer():
    """Metal machine face with a green terraforming core + soil band (terraform design §2)."""
    rng = random.Random(820)
    img = new_img()
    noise_fill(img, METAL, rng)
    px = img.load()
    bevel(img, METAL_L, METAL_D)
    # earthy soil band across the lower third
    DIRT = (98, 70, 46, 255)
    DIRT_D = (70, 49, 32, 255)
    for y in range(11, 14):
        for x in range(2, 14):
            px[x, y] = DIRT if (x + y) % 2 == 0 else DIRT_D
    # green grass crown on the band
    for x in range(2, 14):
        px[x, 10] = G_GREEN if x % 2 == 0 else G_GREEN_L
    # central glowing green orb (the terraform core)
    cx, cy = 8, 6
    for y in range(S):
        for x in range(S):
            d = ((x - cx + 0.5) ** 2 + (y - cy + 0.5) ** 2) ** 0.5
            if d <= 1.6:
                px[x, y] = G_GLOW
            elif d <= 2.6:
                px[x, y] = G_GREEN_L
            elif d <= 3.4:
                px[x, y] = G_GREEN
    # corner rivets
    for (rx, ry) in [(2, 2), (13, 2)]:
        px[rx, ry] = METAL_L
    save(img, os.path.join(BLOCK_DIR, "terraformer.png"))


def gen_entity_glow(name, threshold=205):
    """Derive an emissive glow overlay from a creature texture: keep only its brightest pixels
    (painted eyes / crystals / embers) on a transparent sheet so they glow via the eyes render layer."""
    src = os.path.join(ENTITY_DIR, name + ".png")
    out = os.path.join(ENTITY_DIR, name + "_glow.png")
    if not os.path.exists(src):
        print("skip glow (no base)", name)
        return
    if os.path.exists(out) and not FORCE_CREATURES:
        print("skip (exists)", os.path.relpath(out, ROOT))
        return
    base = Image.open(src).convert("RGBA")
    w, h = base.size
    glow = Image.new("RGBA", (w, h), CLEAR)
    bp = base.load()
    gp = glow.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = bp[x, y]
            if a > 0 and max(r, g, b) >= threshold:
                gp[x, y] = (r, g, b, 255)
    glow.save(out)
    print("wrote", os.path.relpath(out, ROOT))


def gen_panel_block(name, accent, symbol):
    """A steel panel cube with an accent border + simple centre symbol; creative variants get pink."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    rng = random.Random(int(hashlib.md5(name.encode()).hexdigest(), 16) & 0xffffffff)
    steel = [(86, 92, 100, 255), (98, 104, 112, 255), (110, 116, 124, 255)]
    for y in range(16):
        for x in range(16):
            px[x, y] = rng.choice(steel)
    # Accent border.
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = accent
    # Centre symbol (5x5 stamp of '1' cells).
    for dy, row in enumerate(symbol):
        for dx, cell in enumerate(row):
            if cell:
                px[5 + dx, 5 + dy] = accent
    save(img, os.path.join(BLOCK_DIR, name + ".png"))


SYM_BOLT = [(0, 1, 1, 0, 0), (0, 1, 1, 0, 0), (0, 1, 1, 1, 0), (0, 0, 1, 1, 0), (0, 0, 1, 1, 0)]
SYM_DROP = [(0, 0, 1, 0, 0), (0, 1, 1, 1, 0), (1, 1, 1, 1, 1), (1, 1, 1, 1, 1), (0, 1, 1, 1, 0)]
SYM_GAS = [(0, 1, 0, 1, 0), (1, 0, 1, 0, 1), (0, 1, 0, 1, 0), (1, 0, 1, 0, 1), (0, 1, 0, 1, 0)]
SYM_BOX = [(1, 1, 1, 1, 1), (1, 0, 0, 0, 1), (1, 0, 1, 0, 1), (1, 0, 0, 0, 1), (1, 1, 1, 1, 1)]


def gen_universal_pipe_glass():
    """Translucent tube texture for the Universal Pipe (alpha drives the chunk layer in 26.1)."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    rng = random.Random(0x9192)
    for y in range(16):
        for x in range(16):
            a = 110 + rng.randint(-10, 10)
            c = 190 + rng.randint(-12, 12)
            px[x, y] = (c, min(255, c + 8), 255, a)
    rim = (96, 102, 112, 255)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = rim
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[x, y] = (130, 136, 146, 255)
    for i in range(3, 13):
        r, g, b, a = px[i, 16 - i - 1]
        px[i, 16 - i - 1] = (min(255, r + 35), min(255, g + 35), 255, min(255, a + 25))
    save(img, os.path.join(BLOCK_DIR, "universal_pipe_glass.png"))


def gen_storage_endpoints():
    pink = (236, 100, 196, 255)
    gen_panel_block("battery", (224, 80, 106, 255), SYM_BOLT)
    gen_panel_block("creative_battery", pink, SYM_BOLT)
    gen_panel_block("fluid_tank", (60, 120, 240, 255), SYM_DROP)
    gen_panel_block("creative_fluid_tank", pink, SYM_DROP)
    gen_panel_block("gas_tank", (84, 212, 106, 255), SYM_GAS)
    gen_panel_block("creative_gas_tank", pink, SYM_GAS)
    gen_panel_block("item_store", (232, 232, 244, 255), SYM_BOX)
    gen_panel_block("creative_item_store", pink, SYM_BOX)


# ---- Tier 2 (cindrite-upgraded) Oxygen Suit -------------------------------
#
# Derived art: the committed Tier 1 suit textures re-trimmed with cindrite embers (Cindara palette —
# hot orange/red over dark volcanic rock) so the upgrade reads at a glance. Additive like the rest:
# skipped when the output PNG already exists.

CINDRITE_EMBER = (236, 108, 32)
CINDRITE_DEEP = (118, 38, 18)


def _emberize(src_path, dst_path):
    """Recolour a Tier 1 suit texture toward the cindrite palette: saturated (suit-coloured) pixels
    blend toward ember orange, dark trim deepens toward volcanic rock. Grey/metal pixels keep their
    read so the suit silhouette stays recognisably the same family."""
    if os.path.exists(dst_path) and "--force" not in sys.argv:
        print("skip (exists)", os.path.relpath(dst_path, ROOT))
        return
    if not os.path.exists(src_path):
        print("MISSING source", os.path.relpath(src_path, ROOT))
        return
    img = Image.open(src_path).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            mx, mn = max(r, g, b), min(r, g, b)
            sat = 0 if mx == 0 else (mx - mn) / mx
            lum = (r + g + b) / 3.0
            if sat > 0.18:  # coloured trim -> ember
                t = min(1.0, sat * 1.4)
                er, eg, eb = CINDRITE_EMBER if lum > 90 else CINDRITE_DEEP
                # keep the source's shading by scaling the ember by relative luminance
                shade = max(0.45, min(1.25, lum / 140.0))
                r = int(r * (1 - t) + er * shade * t)
                g = int(g * (1 - t) + eg * shade * t)
                b = int(b * (1 - t) + eb * shade * t)
                px[x, y] = (min(255, r), min(255, g), min(255, b), a)
    img.save(dst_path)
    print("wrote", os.path.relpath(dst_path, ROOT))


def gen_oxygen_suit_t2():
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        _emberize(os.path.join(ITEM_DIR, "oxygen_suit_%s.png" % piece),
                  os.path.join(ITEM_DIR, "oxygen_suit_t2_%s.png" % piece))
    equip = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/entity/equipment")
    for layer in ("humanoid", "humanoid_leggings"):
        _emberize(os.path.join(equip, layer, "oxygen_suit.png"),
                  os.path.join(equip, layer, "oxygen_suit_t2.png"))


# ---- Hazard suit variants (SUIT_HAZARD_DESIGN.md) ---------------------------
#
# Derived art like the T2 suit, same algorithm with different palettes:
#  - Thermal (heat):  recolour the T2 art FURTHER — bright ember seams over dark obsidian
#    plating, so it reads "furnace-grade" next to T2's warm trim.
#  - Cryo (cold):     recolour the T1 art toward the Glacira frost palette — the clean
#    white-cyan opposite of the ember track.

HEAT_BRIGHT = (255, 150, 60)
HEAT_DEEP = (44, 22, 26)
COLD_BRIGHT = (160, 220, 245)
COLD_DEEP = (40, 80, 130)


def _retrim(src_path, dst_path, bright, deep, lum_split=90):
    """The _emberize algorithm with a parameterised palette: saturated (suit-coloured) pixels
    blend toward `bright` (above `lum_split` luminance) or `deep`; greys keep their read."""
    if os.path.exists(dst_path) and "--force" not in sys.argv:
        print("skip (exists)", os.path.relpath(dst_path, ROOT))
        return
    if not os.path.exists(src_path):
        print("MISSING source", os.path.relpath(src_path, ROOT))
        return
    img = Image.open(src_path).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            mx, mn = max(r, g, b), min(r, g, b)
            sat = 0 if mx == 0 else (mx - mn) / mx
            lum = (r + g + b) / 3.0
            if sat > 0.18:
                t = min(1.0, sat * 1.4)
                er, eg, eb = bright if lum > lum_split else deep
                shade = max(0.45, min(1.25, lum / 140.0))
                r = int(r * (1 - t) + er * shade * t)
                g = int(g * (1 - t) + eg * shade * t)
                b = int(b * (1 - t) + eb * shade * t)
                px[x, y] = (min(255, r), min(255, g), min(255, b), a)
    img.save(dst_path)
    print("wrote", os.path.relpath(dst_path, ROOT))


def gen_oxygen_suit_heat():
    equip = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/entity/equipment")
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        _retrim(os.path.join(ITEM_DIR, "oxygen_suit_t2_%s.png" % piece),
                os.path.join(ITEM_DIR, "oxygen_suit_heat_%s.png" % piece),
                HEAT_BRIGHT, HEAT_DEEP, lum_split=120)
    for layer in ("humanoid", "humanoid_leggings"):
        _retrim(os.path.join(equip, layer, "oxygen_suit_t2.png"),
                os.path.join(equip, layer, "oxygen_suit_heat.png"),
                HEAT_BRIGHT, HEAT_DEEP, lum_split=120)


def gen_oxygen_suit_cold():
    equip = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/entity/equipment")
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        _retrim(os.path.join(ITEM_DIR, "oxygen_suit_%s.png" % piece),
                os.path.join(ITEM_DIR, "oxygen_suit_cold_%s.png" % piece),
                COLD_BRIGHT, COLD_DEEP)
    for layer in ("humanoid", "humanoid_leggings"):
        _retrim(os.path.join(equip, layer, "oxygen_suit.png"),
                os.path.join(equip, layer, "oxygen_suit_cold.png"),
                COLD_BRIGHT, COLD_DEEP)


def gen_oxygen_hud_icon():
    """16x16 HUD icon for the bespoke oxygen gauge (OxygenHudLayer): a chunky air bubble with two
    trailing bubblets, inked outline, cyan body matching the gauge fill (0xFF3CC8E6). GUI texture
    only — no model/datagen entry needed."""
    INK = (5, 8, 13, 255)          # matches OxygenHudLayer.INK
    O2 = (60, 200, 230, 255)       # matches the gauge fill
    O2_D = (32, 132, 168, 255)     # shaded lower-right
    O2_HI = (210, 245, 255, 255)   # specular highlight
    img = new_img()
    px = img.load()
    cx, cy, r = 6.4, 7.4, 4.6
    for y in range(S):
        for x in range(S):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                # body, shaded toward the lower-right
                px[x, y] = O2_D if (x - cx) + (y - cy) > 2.2 else O2
            elif d <= r + 1.0:
                px[x, y] = INK
    # specular highlight (upper-left)
    for (hx, hy) in ((5, 5), (6, 5), (5, 6)):
        px[hx, hy] = O2_HI
    # two trailing bubblets, rising to the upper-right
    for (bx, by, br) in ((12, 4, 1), (14, 2, 0)):
        for y in range(max(0, by - br - 1), min(S, by + br + 2)):
            for x in range(max(0, bx - br - 1), min(S, bx + br + 2)):
                d = ((x - bx) ** 2 + (y - by) ** 2) ** 0.5
                if d <= br:
                    px[x, y] = O2
                elif d <= br + 1.0:
                    px[x, y] = INK
    save(img, os.path.join(GUI_DIR, "oxygen_hud_icon.png"))


# ---- Per-tier rocket entity textures (cockpit rework) -----------------------
#
# Layout matches RocketModel (64x64): body (12,36,12)@(0,0) — sides v12..48 in four 12-px
# columns; nose (8,8,8)@(0,48); fins @(48,0); console (8,5,1)@(32,48). Each side face gets a
# DARK-FRAMED, fully TRANSPARENT window (entityCutout discards alpha = a real hole the standing
# rider sees out of), an accent stripe per tier, and the console carries the interior gauge art.

ROCKET_TIER_SPECS = {
    "rocket_t1": {"accent": (224, 58, 58, 255), "trim": (160, 36, 36, 255)},
    "rocket_t2": {"accent": (179, 39, 158, 255), "trim": (106, 31, 140, 255)},
    "rocket_t3": {"accent": (240, 200, 80, 255), "trim": (60, 170, 90, 255)},
    # Tier 4 — Glacira run: ice-cyan accent + glacial blue trim (palette rule: steel + per-tier accent).
    "rocket_t4": {"accent": (120, 210, 240, 255), "trim": (60, 130, 200, 255)},
}


def gen_rocket_tier_entity(name, spec):
    rng = random.Random(hash(name) & 0xffffffff)
    img = Image.new("RGBA", (64, 64), CLEAR)
    px = img.load()
    whites = [(232, 232, 244, 255), (214, 214, 228, 255), (196, 196, 212, 255)]
    dark = (70, 70, 86, 255)
    ink = (28, 28, 36, 255)
    accent, trim = spec["accent"], spec["trim"]

    def plate(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[x, y] = rng.choice(whites)

    plate(12, 0, 36, 12)
    plate(0, 12, 48, 48)
    for u0 in (0, 12, 24, 36):
        for x in range(u0, u0 + 12):
            for y in range(28, 33):
                px[x, y] = accent
            px[x, 12] = dark
            px[x, 47] = dark
            px[x, 44] = trim
        # Window band at the standing rider's eye line (model y ~ -5; v = 12 + (y + 16)).
        for y in range(20, 27):
            for x in range(u0 + 3, u0 + 9):
                px[x, y] = ink
        for y in range(21, 26):
            for x in range(u0 + 4, u0 + 8):
                px[x, y] = CLEAR
    plate(0, 48, 32, 64)
    for x in range(0, 32):
        px[x, 56] = accent
        px[x, 63] = dark
    plate(48, 0, 60, 14)
    for x in range(48, 60):
        px[x, 4] = accent
        px[x, 13] = dark
    for y in range(48, 54):
        for x in range(32, 50):
            px[x, y] = (16, 22, 30, 255)
    g = (88, 224, 128, 255)
    a = (255, 196, 64, 255)
    r = (236, 80, 80, 255)
    b = (96, 196, 255, 255)
    for i, c in enumerate((g, g, a, g, r, b, g, a)):
        px[34 + i, 50] = c
    for i in range(6):
        px[42 + i, 52] = b if i % 2 == 0 else (40, 90, 130, 255)
    for i in range(4):
        px[34 + i * 2, 52] = a
    save(img, os.path.join(ENTITY_DIR, name + ".png"))


def gen_rocket_tier_entities():
    for name, spec in ROCKET_TIER_SPECS.items():
        gen_rocket_tier_entity(name, spec)


# ---- Launch Gantry (Heavy Launch Complex module) ----------------------------

def gen_launch_gantry():
    """Steel service-tower lattice: rocket-family steel with red accent cross-bracing."""
    rng = random.Random(0x6A47)
    img = new_img()
    px = img.load()
    steel = [(150, 150, 168, 255), (132, 132, 150, 255), (118, 118, 136, 255)]
    dark = (70, 70, 86, 255)
    accent = (224, 80, 106, 255)  # rocket red
    for y in range(S):
        for x in range(S):
            px[x, y] = rng.choice(steel)
    # Lattice frame: edges + cross-braces.
    for i in range(S):
        px[i, 0] = dark
        px[i, S - 1] = dark
        px[0, i] = dark
        px[S - 1, i] = dark
        px[i, i] = accent
        px[i, S - 1 - i] = accent
    # Horizontal service platforms.
    for x in range(1, 15):
        px[x, 5] = dark
        px[x, 10] = dark
    save(img, os.path.join(BLOCK_DIR, "launch_gantry.png"))


# ---- Star Guide (progression block, 1.0) -----------------------------------

def _star(px, cx, cy, color, arm=2):
    """A 4-point pixel star stamped around (cx, cy)."""
    for d in range(-arm, arm + 1):
        if 0 <= cx + d < S:
            px[cx + d, cy] = color
        if 0 <= cy + d < S:
            px[cx, cy + d] = color


def gen_star_guide():
    """Pedestal cube: dark steel with a nerosium-purple accent border and a glowing star face."""
    rng = random.Random(0x57A6)
    img = noise_fill(new_img(), METAL, rng)
    px = img.load()
    for i in range(S):  # accent border
        for (x, y) in ((i, 0), (i, S - 1), (0, i), (S - 1, i)):
            px[x, y] = N_PURPLE
    bevel_inner = N_MAG
    for (x, y) in ((1, 1), (S - 2, 1), (1, S - 2), (S - 2, S - 2)):
        px[x, y] = bevel_inner
    _star(px, 8, 7, N_GLOW, arm=3)
    _star(px, 8, 7, N_BRIGHT, arm=1)
    for (sx, sy) in ((3, 11), (12, 4), (12, 12), (3, 3)):  # tiny satellite stars
        px[sx, sy] = N_GLOW
    save(img, os.path.join(BLOCK_DIR, "star_guide.png"))


def gen_star_guide_book():
    """Item: a purple-bound guidebook with a star on the cover and a steel clasp."""
    img = new_img()
    px = img.load()
    COVER = N_PURPLE
    COVER_D = N_DARK
    PAGE = (236, 230, 214, 255)
    for y in range(2, 14):
        for x in range(3, 13):
            px[x, y] = COVER
    for y in range(2, 14):  # spine shading + page block on the right
        px[3, y] = COVER_D
        px[12, y] = PAGE
    for y in range(3, 13):
        px[11, y] = PAGE
    for x in range(3, 13):  # cover outline
        px[x, 2] = COVER_D
        px[x, 13] = COVER_D
    _star(px, 7, 7, N_GLOW, arm=2)
    px[7, 7] = N_BRIGHT
    px[12, 7] = METAL_L  # clasp
    px[13, 7] = METAL_D
    save(img, os.path.join(ITEM_DIR, "star_guide_book.png"))


def gen_gui_star_guide():
    """The Star Guide screen panel: 240x200 sci-fi hull in a 256x256 sheet (see StarGuideScreen).
    Layout zones: title strip, chapter rail (x 6..78), step canvas (x 80..234, y 20..96) and the
    guide-text panel (y 96..194)."""
    W, H = 240, 200
    img = Image.new("RGBA", (256, 256), CLEAR)
    px = img.load()
    rng = random.Random(0x57A66)
    INK = (5, 8, 13, 255)
    HULL = [(13, 17, 25, 255), (15, 20, 29, 255), (11, 15, 22, 255)]
    PANEL = (8, 11, 17, 255)
    ACCENT = (176, 90, 224, 255)
    ACCENT_D = (88, 45, 112, 255)
    for y in range(H):  # hull body with light noise
        for x in range(W):
            px[x, y] = rng.choice(HULL)
    for i in range(W):  # outer frame
        px[i, 0] = ACCENT
        px[i, H - 1] = ACCENT_D
    for i in range(H):
        px[0, i] = ACCENT
        px[W - 1, i] = ACCENT_D
    for y in range(1, H - 1):  # inset shadow line
        px[1, y] = INK
        px[W - 2, y] = INK
    for x in range(1, W - 1):
        px[x, 1] = INK
        px[x, H - 2] = INK
    # Recessed zones: chapter rail, step canvas, text panel.
    def recess(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[x, y] = PANEL
        for x in range(x0, x1):
            px[x, y0] = INK
            px[x, y1 - 1] = (30, 40, 56, 255)
        for y in range(y0, y1):
            px[x0, y] = INK
            px[x1 - 1, y] = (30, 40, 56, 255)
    recess(6, 19, 78, 194)     # chapter rail
    recess(80, 19, 234, 95)    # step canvas
    recess(80, 97, 234, 194)   # guide text
    for x in range(6, 234, 2):  # title underline dots
        px[x, 16] = ACCENT_D
    save(img, os.path.join(GUI_DIR, "star_guide.png"))


if __name__ == "__main__":
    # Heavy Launch Complex + cockpit rework.
    gen_launch_gantry()
    gen_rocket_tier_entities()
    # Star Guide (progression block, 1.0).
    gen_star_guide()
    gen_star_guide_book()
    gen_gui_star_guide()
    gen_oxygen_particle()
    gen_oxygen_hud_icon()
    gen_terraformer()
    # Creature base textures (additive; (re)render only under the creature-scoped --creatures flag).
    gen_creatures()
    gen_spawn_eggs()
    for _name in ("xertz_stalker", "quartz_crawler", "greenling", "cinder_stalker", "frost_strider"):
        gen_entity_glow(_name)
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
    gen_fuel_tank()
    gen_oxygen_generator()
    # Storage endpoints + creative sources.
    gen_storage_endpoints()
    # Universal pipe translucent tube.
    gen_universal_pipe_glass()
    # Suit-and-station integration — Tier 2 (cindrite-upgraded) oxygen suit, derived from Tier 1 art.
    gen_oxygen_suit_t2()
    # Glacira (NEW_DESTINATION_DESIGN.md): glacite chain, Tier 4 rocket icon, compass.
    gen_glacite_ore()
    gen_glacite_block()
    gen_glacite()
    gen_rocket_tier("rocket_tier_4", I_FROST, I_CYAN, I_BLUE, boosters=True, glow=I_WHITE)
    gen_destination_compass("glacira_compass", I_CYAN)
    # Hazard suit variants (SUIT_HAZARD_DESIGN.md), derived from the committed T2/T1 art.
    gen_oxygen_suit_heat()
    gen_oxygen_suit_cold()