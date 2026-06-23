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
# ART_OVERHAUL_DESIGN.md §2 (A1): every family ramp retuned in place — deeper shadows, hotter
# highlights, more chroma in the mids. These constants are the single source of truth: every
# painter below samples them, so the whole mod re-skins coherently on a --force regen.
CLEAR = (0, 0, 0, 0)
# nerosium ramp v2: deeper dark core -> electric purple -> hot magenta -> signal red -> pink glow
N_DARK   = (30, 8, 46, 255)
N_PURPLE = (118, 26, 164, 255)
N_MAG    = (208, 36, 176, 255)
N_RED    = (248, 56, 64, 255)
N_REDHI  = (255, 102, 108, 255)
N_GLOW   = (255, 150, 224, 255)
N_BRIGHT = (255, 214, 244, 255)
NEROS = [N_DARK, N_PURPLE, N_MAG, N_RED, N_REDHI, N_GLOW]

# stone / deepslate / metal bases. METAL v2: the old grey-purple mud becomes a cooler blued
# steel with a real highlight range — the single biggest dullness fix (§2 table).
STONE   = [(122,122,122,255),(132,132,132,255),(112,112,112,255),(140,140,140,255)]
DEEP    = [(71,71,74,255),(80,80,84,255),(63,63,66,255),(88,88,92,255)]
METAL   = [(58,64,82,255),(68,76,98,255),(46,52,68,255),(82,92,118,255)]
METAL_L = (160, 176, 210, 255)
METAL_D = (28, 32, 44, 255)
WOOD    = [(92,62,38,255),(78,52,32,255),(104,72,46,255)]
WOOD_D  = (54, 36, 22, 255)

# Greenxertz palette v2 — greens up (more chroma), steel keeps its olive but +1 contrast step.
G_DARK    = (14, 44, 28, 255)
G_STEEL_D = (52, 66, 56, 255)
G_STEEL   = (98, 118, 100, 255)
G_STEEL_L = (158, 182, 154, 255)
G_GREEN   = (52, 190, 92, 255)
G_GREEN_L = (120, 244, 140, 255)
G_GLOW    = (186, 255, 196, 255)
STEEL_RAMP = [G_STEEL_D, G_STEEL, G_GREEN, G_GREEN_L, G_GLOW]
# Xertz quartz v2 — stays pale but gains a mint mid so the facets read.
Q_WHITE  = (236, 250, 238, 255)
Q_PALE   = (192, 232, 198, 255)
Q_GREEN  = (150, 225, 170, 255)
Q_SHADOW = (96, 152, 112, 255)
QUARTZ_RAMP = [Q_SHADOW, Q_GREEN, Q_PALE, Q_WHITE]

# Rocket palette v2 — warmer hull white, visible panel-line grey, hotter fuel amber.
FUEL     = (255, 158, 36, 255)
FUEL_D   = (210, 96, 16, 255)
FUEL_HI  = (255, 222, 128, 255)
R_WHITE  = (240, 240, 250, 255)
R_GRAY   = (158, 160, 180, 255)
R_DARK   = (62, 64, 84, 255)
R_WINDOW = (140, 224, 255, 255)
GOLD     = (252, 208, 72, 255)
HAZ_Y    = (255, 206, 44, 255)
HAZ_K    = (24, 24, 30, 255)

# Cindara ember v2 — darker char, hotter orange.
C_DARK   = (20, 8, 6, 255)
C_ASH    = (62, 50, 46, 255)
C_RED    = (212, 52, 26, 255)
C_ORANGE = (255, 132, 36, 255)
C_EMBER  = (255, 190, 76, 255)
C_GLOW   = (255, 238, 160, 255)
EMBER_RAMP = [C_DARK, C_RED, C_ORANGE, C_EMBER, C_GLOW]

# Glacira frost v2 — small chroma push on the blue/cyan mids.
I_DEEP  = (8, 30, 60, 255)
I_BLUE  = (48, 138, 224, 255)
I_CYAN  = (110, 220, 252, 255)
I_FROST = (196, 242, 255, 255)
I_WHITE = (242, 252, 255, 255)
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
    _ingot_polish(px, rows)
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
    _ingot_polish(px, rows)
    save(img, os.path.join(ITEM_DIR, "nerosteel_ingot.png"))


def _ingot_polish(px, rows):
    """Item quality pass (A1): a diagonal reflection streak + a grounded underside shadow, so
    ingots read as polished bars instead of flat lozenges."""
    for (rx, ry) in [(5, 6), (6, 7), (7, 8)]:   # reflection streak across the face
        r, g, b, a = px[rx, ry]
        px[rx, ry] = (min(255, r + 70), min(255, g + 70), min(255, b + 70), 255)
    bottom = max(rows)
    x0, x1 = rows[bottom]
    for x in range(x0 + 1, x1 + 1):             # contact shadow under the bar
        px[min(x, S - 1), min(bottom + 1, S - 1)] = (20, 20, 26, 140)


def _gem_facets(px, shape, light, dark):
    """Item quality pass (A1): cut facet lines into the shared gem diamond — a left-leaning
    facet seam, a sparkle at the crown, and a settled shadow on the lower-right girdle."""
    for y, (x0, x1) in shape.items():           # facet seam, one px left of centre
        mid = (x0 + x1) // 2
        if y % 2 == 0 and x0 < mid - 1:
            px[mid - 1, y] = light
    for y, (x0, x1) in shape.items():           # lower-right girdle shadow
        if y >= 9:
            px[x1 - 1, y] = dark
    top = min(shape)
    cx = sum(shape[top]) // 2
    px[cx, top] = (255, 255, 255, 255)          # crown sparkle
    if top + 1 in shape:
        px[cx - 1, top + 1] = light


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
    _gem_facets(px, shape, Q_WHITE, Q_SHADOW)
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


def gen_fuel_refinery():
    """Steel still: a coiled refining pipe over a glowing burner, with a fuel-orange droplet."""
    rng = random.Random(814)
    img = new_img()
    noise_fill(img, METAL, rng)
    px = img.load()
    bevel(img, METAL_L, METAL_D)
    # Refining coil: three stacked horizontal pipe runs.
    for cy in (4, 7, 10):
        for x in range(3, 13):
            px[x, cy] = METAL_D
            px[x, cy - 1] = (150, 120, 70, 255)  # brass coil highlight
    # Burner glow beneath the coil.
    for x in range(4, 12):
        px[x, 12] = C_ORANGE if (x % 2 == 0) else C_EMBER
    px[8, 13] = C_GLOW
    # A fuel droplet forming at the spout.
    px[8, 2] = C_ORANGE
    px[8, 3] = C_GLOW
    # corner rivets
    for (rx, ry) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[rx, ry] = METAL_L
    save(img, os.path.join(BLOCK_DIR, "fuel_refinery.png"))


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
    _gem_facets(px, shape, C_EMBER, C_DARK)
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
    _gem_facets(px, shape, I_FROST, I_DEEP)
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

# Creature sheets are deliberately DECOUPLED from --force (art overhaul A1): the global palette
# regen must not wipe entity skins with the old painters — they re-render in the entity phase
# (A3) under their own scoped flag.
FORCE_CREATURES = "--creatures" in sys.argv


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


# --- Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5) -------------------

# Meadow Loper — placid tan-green grazer of mature terraformed Greenxertz: warm hide with darker
# patches, a pale muzzle and bone-white horn nubs. Friendly: nothing crosses the glow threshold.
ML_DARK  = (62, 74, 46, 255)
ML_BODY  = (126, 148, 90, 255)
ML_LITE  = (168, 192, 126, 255)
ML_PATCH = (98, 116, 70, 255)
ML_HORN  = (200, 202, 186, 255)

# Ember Strutter — ember-feathered ground bird of terraformed Cindara: rust body, hot orange
# feather tips and a few bright ember flecks (the flecks alone glow, like banked coals).
ES_DARK  = (46, 22, 16, 255)
ES_BODY  = (152, 62, 30, 255)
ES_FEATH = (200, 100, 38, 255)
ES_EMBER = (248, 168, 70, 255)
ES_BEAK  = (200, 170, 80, 255)

# Woolly Drift — pale shaggy fleece of terraformed Glacira with frost glints (only the sparse ice
# glints exceed the glow threshold) and a darker bare face.
WD_WOOL   = (228, 240, 248, 255)
WD_WOOL_D = (188, 206, 222, 255)
WD_FACE   = (96, 114, 134, 255)
WD_FACE_L = (128, 148, 168, 255)
WD_ICE    = (170, 230, 248, 255)


def _ml_body(x, y, rng):
    base = _mix(ML_BODY, ML_LITE, ((x + y) % 10) / 10.0)
    if (x // 6 + y // 5) % 3 == 0 and rng.random() < 0.5:
        base = ML_PATCH
    return base


def _ml_head(x, y, rng):
    return _mix(ML_BODY, ML_DARK, (y % 8) / 8.0)


def _ml_detail(x, y, rng):
    # horns/tail/legs strip: horn bone at the top rows, hide below
    if y < 6:
        return ML_HORN
    return _mix(ML_DARK, ML_BODY, (y % 6) / 6.0)


def _es_body(x, y, rng):
    base = _mix(ES_BODY, ES_FEATH, ((x + 2 * y) % 9) / 9.0)
    if rng.random() < 0.04:
        base = ES_EMBER
    return base


def _es_head(x, y, rng):
    base = _mix(ES_FEATH, ES_BODY, (y % 6) / 6.0)
    if rng.random() < 0.03:
        base = ES_EMBER
    return base


def _es_detail(x, y, rng):
    # beak/comb/wings/legs strip: beak gold at the top, feather shafts below
    if y < 4:
        return ES_BEAK
    base = _mix(ES_DARK, ES_FEATH, (y % 7) / 7.0)
    if (x + y) % 8 == 0:
        base = ES_EMBER
    return base


def _wd_body(x, y, rng):
    base = _mix(WD_WOOL_D, WD_WOOL, rng.random())
    if rng.random() < 0.03:
        base = WD_ICE
    return base


def _wd_head(x, y, rng):
    return _mix(WD_FACE, WD_FACE_L, (y % 6) / 6.0)


def _wd_detail(x, y, rng):
    # tufts/ears/legs strip: wool above, bare skin below
    if y < 32:
        return _mix(WD_WOOL_D, WD_WOOL, rng.random())
    return _mix(WD_FACE, WD_FACE_L, (y % 5) / 5.0)


# ---- Anatomy-aware creature painting (ART_OVERHAUL_DESIGN.md §4.1, art overhaul A3) -----------
# Each creature's skin is painted onto its REAL box-UV layout: the cube list is parsed straight out
# of the model's `model_sync` marker block, so every part gets directional shading (lit top, dark
# belly) plus a per-creature pattern. Parts outside the marker block (rotated shards, multi-cube
# limbs) sample the base noise fill, which uses the same ramp — they still match.

import model_sync as _ms  # same tools/ dir; shares the Java cube parser


def _java_cubes(java_class):
    path = os.path.join(ROOT, "src/main/java/za/co/neroland/nerospace/client", java_class + ".java")
    with open(path, encoding="utf-8") as fh:
        src = fh.read()
    if _ms.BEGIN in src:
        src = src.split(_ms.BEGIN)[1].split(_ms.END)[0]
    cubes = []
    for m in _ms._CUBE_RE.finditer(src):
        name, u, v = m.group(1), int(m.group(2)), int(m.group(3))
        w, h, d = (max(1, int(round(float(m.group(i))))) for i in (7, 8, 9))
        cubes.append((name, u, v, w, h, d))
    return cubes


def _paint_face(px, x0, y0, fw, fh, ramp, level, rng, pattern, part, face, bounds=(ES, ES)):
    hi = len(ramp) - 1
    for yy in range(fh):
        for xx in range(fw):
            x, y = x0 + xx, y0 + yy
            if not (0 <= x < bounds[0] and 0 <= y < bounds[1]):
                continue
            idx = max(0, min(hi, level + (1 if rng.random() < 0.18 else 0)
                             - (1 if rng.random() < 0.18 else 0)))
            color = ramp[idx]
            if pattern is not None:
                pat = pattern(part, face, xx / max(1, fw - 1) if fw > 1 else 0.5,
                              yy / max(1, fh - 1) if fh > 1 else 0.5, rng)
                if pat is not None:
                    color = pat
            px[x, y] = color


def _paint_box_uv(px, u, v, w, h, d, ramp, rng, pattern, part, bounds=(ES, ES)):
    """Box-UV faces with directional light: lit top, dark belly, mid sides, dimmer back."""
    mid = (len(ramp) - 1) // 2 + 1
    _paint_face(px, u + d, v, w, d, ramp, len(ramp) - 2, rng, pattern, part, "top", bounds)
    _paint_face(px, u + d + w, v, w, d, ramp, 0, rng, pattern, part, "bottom", bounds)
    _paint_face(px, u, v + d, d, h, ramp, mid - 1, rng, pattern, part, "west", bounds)
    _paint_face(px, u + d, v + d, w, h, ramp, mid, rng, pattern, part, "north", bounds)
    _paint_face(px, u + d + w, v + d, d, h, ramp, mid - 1, rng, pattern, part, "east", bounds)
    _paint_face(px, u + d + w + d, v + d, w, h, ramp, max(0, mid - 2), rng, pattern, part, "south", bounds)


def _eyes_on_head(px, head, eye, socket, glint=None, big=False):
    """Eyes on the parsed head cube's FRONT face (replaces the old hardcoded texOffs(0,28) math)."""
    _name, u, v, w, _h, d = head
    fx, fy = u + d, v + d
    cx = fx + w / 2.0
    ey = int(fy + (3 if big else 2))
    off = max(1, int(w * (0.28 if big else 0.22)))
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


def _gen_creature_v2(name, java_class, ramp, seed, pattern=None, part_ramps=None,
                     eye=(20, 20, 24, 255), socket=(0, 0, 0, 255), glint=None, big_eyes=False):
    path = os.path.join(ENTITY_DIR, name + ".png")
    if os.path.exists(path) and not FORCE_CREATURES:
        print("skip (exists)", os.path.relpath(path, ROOT))
        return
    rng = random.Random(seed)
    img = Image.new("RGBA", (ES, ES), CLEAR)
    px = img.load()
    # Base fill: dim ramp noise so non-marker parts (rotated shards, multi-cube limbs) match.
    for y in range(ES):
        for x in range(ES):
            px[x, y] = ramp[0] if rng.random() < 0.55 else ramp[1]
    head = None
    for cube in _java_cubes(java_class):
        part, u, v, w, h, d = cube
        part_ramp = ramp
        if part_ramps:
            for prefix, override in part_ramps.items():
                if part.startswith(prefix):
                    part_ramp = override
                    break
        _paint_box_uv(px, u, v, w, h, d, part_ramp, rng, pattern, part)
        if part == "head":
            head = cube
    if head is not None:
        _eyes_on_head(px, head, eye, socket, glint, big_eyes)
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


# Per-creature patterns: return a colour to stamp, or None to keep the shaded ramp pixel.
def _pat_facets(part, face, nx, ny, rng):
    if part in ("torso", "pelvis", "chest") and (nx + ny) % 0.34 < 0.08:
        return XS_FACET
    return XS_MID if rng.random() < 0.04 else None


def _pat_plates(part, face, nx, ny, rng):
    if part in ("dome", "shell", "rim") and (nx in (0.0, 1.0) or ny in (0.0, 1.0)):
        return QC_SEAM
    return QC_ROSE if rng.random() < 0.03 else None


def _pat_spots(part, face, nx, ny, rng):
    return GL_SPOT if rng.random() < 0.06 else None


def _pat_cracks(part, face, nx, ny, rng):
    if part in ("body", "shoulders", "belly") and rng.random() < 0.07:
        return C_ORANGE if rng.random() < 0.7 else C_GLOW
    return None


def _pat_frost(part, face, nx, ny, rng):
    if face == "top" and ny < 0.34:
        return FS_ICE
    return FS_GLOW if rng.random() < 0.02 else None


def _pat_hide(part, face, nx, ny, rng):
    if part == "body" and rng.random() < 0.10:
        return ML_PATCH
    return None


def _pat_feathers(part, face, nx, ny, rng):
    if part in ("body", "wing_l", "wing_r", "tail_fan") and int(ny * 6) % 2 == 1:
        return ES_DARK if rng.random() < 0.5 else None
    return ES_EMBER if rng.random() < 0.03 else None


def _pat_wool(part, face, nx, ny, rng):
    if part.startswith(("body", "tuft")) and rng.random() < 0.12:
        return WD_ICE if rng.random() < 0.12 else WD_WOOL_D
    return None


def gen_creatures():
    _gen_creature_v2("xertz_stalker", "XertzStalkerModel",
                     [XS_DARK, XS_BODY, XS_MID, XS_FACET, XS_GLOW], 501,
                     pattern=_pat_facets, eye=XS_GLOW, socket=XS_DARK, glint=XS_GLOW)
    _gen_creature_v2("quartz_crawler", "QuartzCrawlerModel",
                     [QC_SEAM, QC_PLATE, QC_HI, (224, 226, 234, 255), QC_CRY], 502,
                     pattern=_pat_plates, eye=QC_CRY, socket=QC_SEAM, glint=(220, 255, 235, 255))
    _gen_creature_v2("greenling", "GreenlingModel",
                     [GL_DARK, GL_SPOT, GL_BODY, GL_LEAF, GL_LITE], 503,
                     pattern=_pat_spots, part_ramps={"belly": [GL_BODY, GL_LITE, GL_LITE,
                                                              (200, 235, 190, 255), (220, 245, 210, 255)]},
                     eye=(20, 40, 24, 255), socket=GL_DARK, glint=(235, 255, 235, 255), big_eyes=True)
    _gen_creature_v2("cinder_stalker", "CinderStalkerModel",
                     [CS_OBS, CS_ROCK, CS_ROCK2, CS_RED, CS_ORANGE], 504,
                     pattern=_pat_cracks, eye=CS_EMBER, socket=CS_OBS, glint=CS_GLOW_EYE)
    _gen_creature_v2("frost_strider", "FrostStriderModel",
                     [FS_DEEP, FS_BODY, FS_PLATE, FS_ICE, FS_GLOW], 505,
                     pattern=_pat_frost, eye=FS_GLOW, socket=FS_DEEP, glint=FS_GLOW)
    # Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5): friendly, mostly non-emissive.
    _gen_creature_v2("meadow_loper", "MeadowLoperModel",
                     [ML_DARK, ML_PATCH, ML_BODY, ML_LITE, (196, 214, 156, 255)], 506,
                     pattern=_pat_hide, part_ramps={"muzzle": [ML_BODY, ML_LITE, ML_LITE,
                                                               (200, 216, 170, 255), (214, 226, 188, 255)],
                                                    "horn": [ML_HORN, ML_HORN, ML_HORN, ML_HORN, ML_HORN]},
                     eye=(38, 34, 26, 255), socket=ML_DARK, big_eyes=True)
    _gen_creature_v2("ember_strutter", "EmberStrutterModel",
                     [ES_DARK, ES_BODY, ES_FEATH, ES_EMBER, (255, 214, 120, 255)], 507,
                     pattern=_pat_feathers, part_ramps={"beak": [ES_BEAK] * 5,
                                                        "comb": [(200, 60, 40, 255)] * 5},
                     eye=(30, 16, 12, 255), socket=ES_DARK)
    _gen_creature_v2("woolly_drift", "WoollyDriftModel",
                     [WD_WOOL_D, WD_WOOL_D, WD_WOOL, WD_WOOL, (240, 248, 252, 255)], 508,
                     pattern=_pat_wool, part_ramps={"head": [WD_FACE, WD_FACE, WD_FACE_L,
                                                             WD_FACE_L, (150, 170, 190, 255)],
                                                    "leg": [WD_FACE, WD_FACE, WD_FACE_L,
                                                            WD_FACE_L, WD_FACE_L]},
                     eye=(30, 36, 48, 255), socket=WD_FACE, big_eyes=True)


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
    gen_spawn_egg("meadow_loper", ML_BODY, ML_LITE, ML_DARK, 616)
    gen_spawn_egg("ember_strutter", ES_BODY, ES_EMBER, ES_DARK, 617)
    gen_spawn_egg("woolly_drift", WD_WOOL, WD_ICE, WD_FACE, 618)


def gen_loper_haunch():
    """Item: a hearty haunch — red-brown meat on a bone end (Meadow Loper drop)."""
    MEAT = (172, 80, 56, 255)
    MEAT_D = (128, 54, 38, 255)
    MEAT_HI = (210, 120, 88, 255)
    BONE = (232, 226, 206, 255)
    BONE_D = (190, 184, 162, 255)
    img = new_img()
    px = img.load()
    rng = random.Random(701)
    # meat mass (diagonal oval, lower-left)
    for y in range(5, 14):
        for x in range(2, 11):
            d = ((x - 6) ** 2 / 18.0 + (y - 9.5) ** 2 / 14.0)
            if d <= 1.0:
                px[x, y] = MEAT_D if d > 0.7 else (MEAT_HI if rng.random() < 0.15 else MEAT)
    # bone sticking out top-right
    for i in range(4):
        px[10 + i, 5 - i // 2] = BONE if i < 3 else BONE_D
        px[10 + i, 6 - i // 2] = BONE_D
    px[14, 3] = BONE
    px[13, 2] = BONE
    save(img, os.path.join(ITEM_DIR, "loper_haunch.png"))


def gen_strutter_drumstick():
    """Item: a lean drumstick — ember-bird meat on a thin bone (Ember Strutter drop)."""
    MEAT = (196, 110, 60, 255)
    MEAT_D = (150, 78, 42, 255)
    BONE = (232, 226, 206, 255)
    img = new_img()
    px = img.load()
    rng = random.Random(702)
    for y in range(3, 10):
        for x in range(3, 10):
            d = ((x - 6) ** 2 + (y - 6) ** 2) ** 0.5
            if d <= 3.4:
                px[x, y] = MEAT_D if d > 2.4 else (MEAT if rng.random() > 0.1 else MEAT_D)
    for i in range(5):  # the bone shaft to the lower-right
        px[9 + i // 2, 9 + i] = BONE
    px[11, 14] = BONE
    px[12, 14] = BONE
    save(img, os.path.join(ITEM_DIR, "strutter_drumstick.png"))


def gen_drift_fleece():
    """Item: a tuft of insulating fleece — pale wool with frost glints (Woolly Drift drop)."""
    img = new_img()
    px = img.load()
    rng = random.Random(703)
    for y in range(3, 13):
        for x in range(2, 14):
            d = ((x - 8) ** 2 / 30.0 + (y - 8) ** 2 / 20.0)
            if d <= 1.0:
                base = _mix(WD_WOOL_D, WD_WOOL, rng.random())
                if rng.random() < 0.06:
                    base = WD_ICE
                px[x, y] = base
    save(img, os.path.join(ITEM_DIR, "drift_fleece.png"))


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

def gen_rocket_fuel_block():
    """The rocket_fuel fluid's block/particle tile: a viscous amber liquid with slick highlights.
    (Found missing by check_assets — the fluid block model always referenced it. The A4 fluid pass
    layers animated still/flow strips on top; this tile stays as the particle texture.)"""
    rng = random.Random(940)
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            t = ((x * 3 + y * 5) % 13) / 13.0
            px[x, y] = _mix(FUEL_D, FUEL, t * 0.8)
    for (cx, cy) in [(4, 3), (11, 7), (6, 12), (13, 13)]:   # slick highlights
        px[cx, cy] = FUEL_HI
        px[min(S - 1, cx + 1), cy] = FUEL_HI
    for i in range(0, S, 4):                                  # lazy surface swirls
        px[(i + 5) % S, (i * 2 + 3) % S] = _mix(FUEL, FUEL_HI, 0.5)
    save(img, os.path.join(BLOCK_DIR, "rocket_fuel.png"))


def gen_rocket_fuel_fluid():
    """Animated still/flow strips for the rocket_fuel FluidType (art overhaul A4): four 16x16
    frames of drifting amber swirl + the .png.mcmeta animation metadata (texture metadata, not
    model JSON — datagen still owns all models/blockstates)."""
    import json as _json
    for (name, frametime, drift) in (("rocket_fuel_still", 12, 1), ("rocket_fuel_flow", 6, 3)):
        path = os.path.join(BLOCK_DIR, name + ".png")
        if os.path.exists(path) and "--force" not in sys.argv:
            print("skip (exists)", os.path.relpath(path, ROOT))
        else:
            img = Image.new("RGBA", (S, S * 4), CLEAR)
            px = img.load()
            for frame in range(4):
                oy = frame * S
                shift = frame * drift
                for y in range(S):
                    for x in range(S):
                        t = (((x + shift) * 3 + (y + shift) * 5) % 13) / 13.0
                        px[x, oy + y] = _mix(FUEL_D, FUEL, t * 0.85)
                for (cx, cy) in [(4, 3), (11, 7), (6, 12), (13, 13)]:
                    px[(cx + shift) % S, oy + cy] = FUEL_HI
            img.save(path)
            print("wrote", os.path.relpath(path, ROOT))
        meta = path + ".mcmeta"
        if not os.path.exists(meta) or "--force" in sys.argv:
            with open(meta, "w") as fh:
                _json.dump({"animation": {"frametime": frametime}}, fh, indent=2)
            print("wrote", os.path.relpath(meta, ROOT))


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


def gen_hydration_module():
    """Metal machine face with a glacite-cyan melt core and a water band (deeper terraform §3.1)."""
    rng = random.Random(821)
    img = new_img()
    noise_fill(img, METAL, rng)
    px = img.load()
    bevel(img, METAL_L, METAL_D)
    # water band across the lower third (the melt output)
    WATER = (40, 110, 200, 255)
    WATER_D = (24, 70, 150, 255)
    for y in range(11, 14):
        for x in range(2, 14):
            px[x, y] = WATER if (x + y) % 2 == 0 else WATER_D
    # frost crown on the band (glacite going in)
    for x in range(2, 14):
        px[x, 10] = I_CYAN if x % 2 == 0 else I_FROST
    # central glowing glacite core
    cx, cy = 8, 6
    for y in range(S):
        for x in range(S):
            d = ((x - cx + 0.5) ** 2 + (y - cy + 0.5) ** 2) ** 0.5
            if d <= 1.6:
                px[x, y] = I_WHITE
            elif d <= 2.6:
                px[x, y] = I_FROST
            elif d <= 3.4:
                px[x, y] = I_BLUE
    # corner rivets
    for (rx, ry) in [(2, 2), (13, 2)]:
        px[rx, ry] = METAL_L
    save(img, os.path.join(BLOCK_DIR, "hydration_module.png"))


def gen_terraform_monitor():
    """Terraform Monitor (art overhaul A2 split): the SIDE texture is plain trimmed steel (foot/
    pillar/screen edges); the screen face moved to terraform_monitor_front (gen_machine_faces)."""
    img = _steel_face_img(822, accent=G_GREEN)
    save(img, os.path.join(BLOCK_DIR, "terraform_monitor.png"))


# ---- Art overhaul A2: per-face machine textures (ART_OVERHAUL_DESIGN.md §3) -------------------
# Every shaped machine keeps its block texture as the SIDE and gains _front/_top faces. The
# painters share one trimmed-steel base so the family identity comes from the accents.

def _steel_face_img(seed, accent=None):
    """The shared machine face: blued-steel noise, bevel, optional accent trim lines."""
    rng = random.Random(seed)
    img = new_img()
    noise_fill(img, METAL, rng)
    bevel(img, METAL_L, METAL_D)
    px = img.load()
    if accent:
        for x in range(2, 14):
            px[x, 1] = accent
            px[x, 14] = accent
    return img


def _save_block(img, name):
    save(img, os.path.join(BLOCK_DIR, name + ".png"))


def gen_machine_faces():
    # Nerosium Grinder — front: toothed intake; top: the hopper pit between the rim walls.
    img = _steel_face_img(830, accent=N_RED)
    px = img.load()
    for y in range(4, 12):
        for x in range(3, 13):
            px[x, y] = METAL_D
    for x in range(3, 13):                       # interlocking crusher teeth
        px[x, 6 if x % 2 == 0 else 7] = N_RED
        px[x, 9 if x % 2 == 0 else 8] = N_GLOW if x % 4 == 0 else N_MAG
    _save_block(img, "nerosium_grinder_front")
    img = _steel_face_img(831)
    px = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            px[x, y] = METAL_D
    for y in range(3, 13, 3):                    # crusher slats seen from above
        for x in range(3, 13):
            px[x, y] = N_RED if x % 2 == 0 else N_PURPLE
    _save_block(img, "nerosium_grinder_top")

    # Combustion Generator — front: ember grill; top: vent ring around the chimney base.
    img = _steel_face_img(832, accent=FUEL)
    px = img.load()
    for y in range(5, 13):
        for x in range(3, 13):
            px[x, y] = HAZ_K
    for y in range(6, 12, 2):                    # glowing grill slits
        for x in range(4, 12):
            px[x, y] = C_GLOW if (x + y) % 5 == 0 else C_ORANGE
    _save_block(img, "combustion_generator_front")
    img = _steel_face_img(833)
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = ((x - 8) ** 2 + (y - 8) ** 2) ** 0.5
            if 3.0 <= d <= 4.5:
                px[x, y] = C_ORANGE if (x + y) % 2 == 0 else C_RED
            elif d < 3.0:
                px[x, y] = HAZ_K
    _save_block(img, "combustion_generator_top")

    # Passive Generator — top: the ambient collector panel (green cell grid).
    img = _steel_face_img(834)
    px = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            if x % 4 == 1 or y % 4 == 1:
                px[x, y] = METAL_D
            else:
                px[x, y] = G_GREEN if (x // 4 + y // 4) % 2 == 0 else G_GREEN_L
    _save_block(img, "passive_generator_top")

    # Oxygen Generator — top: the electrolysis dome plate (concentric cyan rings).
    img = _steel_face_img(835)
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = ((x - 8) ** 2 + (y - 8) ** 2) ** 0.5
            if d <= 2.0:
                px[x, y] = I_WHITE
            elif d <= 4.0:
                px[x, y] = I_CYAN
            elif d <= 6.0:
                px[x, y] = I_BLUE
    _save_block(img, "oxygen_generator_top")

    # Terraformer — front: the green core lens; top: the soil tray seen from above.
    img = _steel_face_img(836, accent=G_GREEN)
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = ((x - 8) ** 2 + (y - 8) ** 2) ** 0.5
            if d <= 2.0:
                px[x, y] = G_GLOW
            elif d <= 3.4:
                px[x, y] = G_GREEN_L
            elif d <= 4.6:
                px[x, y] = G_GREEN
            elif d <= 5.4:
                px[x, y] = METAL_D
    _save_block(img, "terraformer_front")
    rng = random.Random(837)
    img = new_img()
    DIRT = (98, 70, 46, 255)
    DIRT_D = (70, 49, 32, 255)
    noise_fill(img, [DIRT, DIRT_D, (84, 60, 40, 255)], rng)
    px = img.load()
    for _ in range(14):                          # fresh green shoots in the tray
        x, y = rng.randint(1, 14), rng.randint(1, 14)
        px[x, y] = G_GREEN if rng.random() < 0.6 else G_GREEN_L
    bevel(img, (120, 90, 60, 255), DIRT_D)
    _save_block(img, "terraformer_top")

    # Hydration Module — front: the melt window (meltwater behind glass); top: cyan ridge slats.
    img = _steel_face_img(838, accent=I_CYAN)
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            t = (12 - y) / 9.0
            px[x, y] = _mix(I_DEEP, I_BLUE, max(0.0, 1.0 - t))
    for x in range(3, 13):                       # meniscus line
        px[x, 6] = I_FROST
    _save_block(img, "hydration_module_front")
    img = _steel_face_img(839)
    px = img.load()
    for y in range(4, 12, 3):
        for x in range(2, 14):
            px[x, y] = I_CYAN if x % 2 == 0 else I_BLUE
    _save_block(img, "hydration_module_top")

    # Terraform Monitor — front: the dark stage-bar display (the old block face).
    img = _steel_face_img(840, accent=G_GREEN)
    px = img.load()
    SCREEN = (8, 14, 12, 255)
    for y in range(3, 13):
        for x in range(2, 14):
            px[x, y] = SCREEN
    for (bx, h, col) in ((4, 3, G_GREEN), (7, 5, I_CYAN), (10, 7, G_GLOW)):
        for y in range(12 - h, 12):
            for x in range(bx, bx + 2):
                px[x, y] = col
    _save_block(img, "terraform_monitor_front")

    # Battery — top: twin terminal pads (energy gold + signal red). The creative variant gets the
    # gold-trim treatment (ART_OVERHAUL_DESIGN.md sign-off Q3 extends gold trim to all creatives).
    for (name, accent) in (("battery_top", None), ("creative_battery_top", GOLD)):
        img = _steel_face_img(841, accent=accent)
        px = img.load()
        for y in range(3, 7):
            for x in range(3, 7):
                px[x, y] = GOLD
            for x in range(9, 13):
                px[x, y + 6] = N_RED
        _save_block(img, name)

    # Item Store — front: the drawer plate with a steel handle; top: cross slats.
    img = _steel_face_img(842, accent=METAL_L)
    px = img.load()
    for y in range(3, 13):
        px[3, y] = METAL_D
        px[12, y] = METAL_D
    for x in range(3, 13):
        px[x, 3] = METAL_D
        px[x, 12] = METAL_D
    for x in range(6, 10):                       # handle bar
        px[x, 7] = METAL_L
        px[x, 8] = METAL_D
    _save_block(img, "item_store_front")
    img = _steel_face_img(843)
    px = img.load()
    for i in range(2, 14):
        px[i, 8] = METAL_D
        px[8, i] = METAL_D
    _save_block(img, "item_store_top")

    # Tank content cores: what shows between the frame beams (per block name — the datagen maps
    # `<block>_core`; the creative variants share their parent's look).
    for (name, dark, mid, hi, seed) in (
            ("fluid_tank_core", (16, 52, 120, 255), (44, 110, 210, 255), (130, 190, 250, 255), 844),
            ("creative_fluid_tank_core", (16, 52, 120, 255), (44, 110, 210, 255), (130, 190, 250, 255), 844),
            ("gas_tank_core", I_DEEP, I_BLUE, I_CYAN, 845),
            ("creative_gas_tank_core", I_DEEP, I_BLUE, I_CYAN, 845),
            ("fuel_tank_core", FUEL_D, FUEL, FUEL_HI, 846)):
        rng = random.Random(seed)
        img = new_img()
        px = img.load()
        for y in range(S):
            for x in range(S):
                t = ((x * 3 + y * 5) % 13) / 13.0
                px[x, y] = _mix(dark, mid, t * 0.85)
        for (cx, cy) in [(4, 3), (11, 7), (6, 12), (13, 13)]:
            px[cx, cy] = hi
        _save_block(img, name)

    # Launch Gantry — top: the boarding platform with a hazard-striped rim.
    img = _steel_face_img(847)
    px = img.load()
    for i in range(S):                           # hazard rim
        for (x, y) in ((i, 0), (i, S - 1), (0, i), (S - 1, i)):
            px[x, y] = HAZ_Y if (x + y) % 4 < 2 else HAZ_K
    for y in range(3, 13, 3):                    # grating
        for x in range(2, 14):
            px[x, y] = METAL_D
    _save_block(img, "launch_gantry_top")


def gen_gui_machine_panel(name, accent, accent_d, slots=((80, 46),)):
    """A standard 176x166 machine-screen hull in a 256x256 sheet (TexturedContainerScreen):
    sci-fi hull noise, accent frame, recessed readout zone and 18x18 slot sockets for the
    machine slots + the standard player inventory at (8,84)/(8,142)."""
    W, H = 176, 166
    img = Image.new("RGBA", (256, 256), CLEAR)
    px = img.load()
    rng = random.Random(hash(name) & 0xFFFF)
    INK = (5, 8, 13, 255)
    HULL = [(13, 17, 25, 255), (15, 20, 29, 255), (11, 15, 22, 255)]
    PANEL = (8, 11, 17, 255)
    SOCKET = (20, 26, 36, 255)
    SOCKET_HI = (44, 56, 74, 255)
    for y in range(H):
        for x in range(W):
            px[x, y] = rng.choice(HULL)
    for i in range(W):  # outer frame
        px[i, 0] = accent
        px[i, H - 1] = accent_d
    for i in range(H):
        px[0, i] = accent
        px[W - 1, i] = accent_d
    for y in range(1, H - 1):  # inset shadow line
        px[1, y] = INK
        px[W - 2, y] = INK
    for x in range(1, W - 1):
        px[x, 1] = INK
        px[x, H - 2] = INK

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

    def socket(sx, sy):  # an 18x18 vanilla-style slot at slot coords (item drawn at sx+1, sy+1)
        for y in range(sy - 1, sy + 17):
            for x in range(sx - 1, sx + 17):
                px[x, y] = SOCKET
        for x in range(sx - 1, sx + 17):
            px[x, sy - 1] = INK
            px[x, sy + 16] = SOCKET_HI
        for y in range(sy - 1, sy + 17):
            px[sx - 1, y] = INK
            px[sx + 16, y] = SOCKET_HI

    recess(6, 16, 170, 80)  # readout zone above the inventory
    for (sx, sy) in slots:
        socket(sx, sy)
    for row in range(3):  # player inventory
        for col in range(9):
            socket(8 + col * 18, 84 + row * 18)
    for col in range(9):  # hotbar
        socket(8 + col * 18, 142)
    save(img, os.path.join(GUI_DIR, name + ".png"))


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
# Exclamation mark — the warning glyph for the hidden Sentry test block.
SYM_BANG = [(0, 0, 1, 0, 0), (0, 0, 1, 0, 0), (0, 0, 1, 0, 0), (0, 0, 0, 0, 0), (0, 0, 1, 0, 0)]


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


# ---- Tier 1 Oxygen Suit — full generator redo (ART_OVERHAUL_DESIGN.md §4.3) -------------------
# The worn layers are painted onto the REAL vanilla humanoid armor layout, so the suit reads as a
# suit on the player: cyan visor glass, chest console, twin backpack tank, accent cuff seals. The
# T2/Thermal/Cryo variants keep deriving from this art via the recolor chain below, so repainting
# T1 re-skins the whole family.

SUIT_SHELL = [(70, 76, 92, 255), (110, 118, 138, 255), (150, 158, 180, 255),
              (190, 198, 218, 255), (224, 230, 244, 255)]
SUIT_ACCENT = (224, 70, 70, 255)        # T1 signal red (family rule: T1 = steel + red)
SUIT_VISOR = (16, 22, 30, 255)
SUIT_GLASS = (120, 214, 248, 255)


def gen_oxygen_suit_t1():
    rng = random.Random(990)
    equip = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/entity/equipment")

    def out(layer):
        folder = os.path.join(equip, layer)
        os.makedirs(folder, exist_ok=True)
        return os.path.join(folder, "oxygen_suit.png")

    bounds = (64, 32)
    # humanoid layer: helmet + chest/arms + boots.
    path = out("humanoid")
    if not (os.path.exists(path) and "--force" not in sys.argv):
        img = Image.new("RGBA", (64, 32), CLEAR)
        px = img.load()
        _paint_box_uv(px, 0, 0, 8, 8, 8, SUIT_SHELL, rng, None, "helmet", bounds)   # helmet
        _paint_box_uv(px, 16, 16, 8, 12, 4, SUIT_SHELL, rng, None, "chest", bounds)  # body
        _paint_box_uv(px, 40, 16, 4, 12, 4, SUIT_SHELL, rng, None, "arm", bounds)    # arms
        _paint_box_uv(px, 0, 16, 4, 12, 4, SUIT_SHELL, rng, None, "boot", bounds)    # boots
        # Visor: dark glass band with a cyan reflection across the helmet FRONT (u 8..16, v 8..16).
        for y in range(10, 14):
            for x in range(9, 15):
                px[x, y] = SUIT_VISOR
        for x in range(9, 13):
            px[x, 10] = SUIT_GLASS
        px[9, 11] = SUIT_GLASS
        # Chest console on the body front (u 20..28, v 20..32): a small status cluster.
        for i, c in enumerate(((88, 224, 128, 255), (255, 196, 64, 255), SUIT_GLASS, SUIT_ACCENT)):
            px[21 + i, 22] = c
        for x in range(21, 27):
            px[x, 24] = SUIT_VISOR
        # Backpack tank on the body BACK (u 32..40): twin cylinders.
        for y in range(21, 30):
            for x in (33, 34):
                px[x, y] = SUIT_SHELL[4]
            for x in (37, 38):
                px[x, y] = SUIT_SHELL[3]
        for x in (33, 34, 37, 38):
            px[x, 20] = SUIT_ACCENT
        # Accent cuff seals: helmet rim, arm wrists, boot tops.
        for x in range(8, 16):
            px[x, 15] = SUIT_ACCENT
        for x in range(40, 52):
            px[x, 27] = SUIT_ACCENT
        for x in range(0, 12):
            px[x, 20] = SUIT_ACCENT
        img.save(path)
        print("wrote", os.path.relpath(path, ROOT))
    else:
        print("skip (exists)", os.path.relpath(path, ROOT))

    # humanoid_leggings layer: legs + belt.
    path = out("humanoid_leggings")
    if not (os.path.exists(path) and "--force" not in sys.argv):
        img = Image.new("RGBA", (64, 32), CLEAR)
        px = img.load()
        _paint_box_uv(px, 0, 16, 4, 12, 4, SUIT_SHELL, rng, None, "leg", bounds)      # legs
        _paint_box_uv(px, 16, 16, 8, 12, 4, SUIT_SHELL, rng, None, "belt", bounds)    # belt body
        for x in range(16, 40):                                                       # belt seal
            px[x, 21] = SUIT_ACCENT
        for x in range(0, 12):                                                        # knee seals
            px[x, 25] = SUIT_ACCENT
        img.save(path)
        print("wrote", os.path.relpath(path, ROOT))
    else:
        print("skip (exists)", os.path.relpath(path, ROOT))

    # Matching item icons: helmet dome+visor, chest shell+console, layered legs, boot pair.
    def icon(name, draw):
        ipath = os.path.join(ITEM_DIR, "oxygen_suit_%s.png" % name)
        if os.path.exists(ipath) and "--force" not in sys.argv:
            print("skip (exists)", os.path.relpath(ipath, ROOT))
            return
        img = new_img()
        draw(img.load())
        save(img, ipath)

    def _shell(px, x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                t = (y - y0) / max(1, y1 - 1 - y0)
                px[x, y] = SUIT_SHELL[3 - min(3, int(t * 3))]

    def helmet(px):
        _shell(px, 3, 3, 13, 12)
        for x in range(4, 12):
            px[x, 2] = SUIT_SHELL[4]
        for y in range(6, 10):
            for x in range(4, 12):
                px[x, y] = SUIT_VISOR
        for x in range(4, 8):
            px[x, 6] = SUIT_GLASS
        for x in range(3, 13):
            px[x, 12] = SUIT_ACCENT

    def chestplate(px):
        _shell(px, 3, 2, 13, 13)
        for y in range(2, 6):    # shoulder cut
            for x in range(6, 10):
                px[x, y] = CLEAR
        for i, c in enumerate(((88, 224, 128, 255), (255, 196, 64, 255), SUIT_GLASS)):
            px[6 + i, 8] = c
        for x in range(3, 13):
            px[x, 12] = SUIT_ACCENT

    def leggings(px):
        _shell(px, 3, 3, 13, 6)
        _shell(px, 3, 6, 7, 14)
        _shell(px, 9, 6, 13, 14)
        for x in range(3, 13):
            px[x, 5] = SUIT_ACCENT

    def boots(px):
        _shell(px, 2, 8, 7, 13)
        _shell(px, 9, 8, 14, 13)
        for x in list(range(2, 7)) + list(range(9, 14)):
            px[x, 8] = SUIT_ACCENT
            px[x, 13] = SUIT_VISOR

    icon("helmet", helmet)
    icon("chestplate", chestplate)
    icon("leggings", leggings)
    icon("boots", boots)


# ---- Suit variants: Tier 2 / Thermal / Cryo (derived from the Tier 1 art) ---
#
# Derived art, but a FULL re-style (suits rework): the old chain only recoloured saturated trim,
# which left the steel shell — 95% of the suit — identical across tiers, so T1/T2 read as the
# same suit on adjacent armor stands. Now every variant remaps the WHOLE family: the grey shell
# is value-mapped onto a per-variant plating ramp, accents and visor glass get per-variant
# colours, and the dark visor void stays dark. Additive like the rest: skipped when the output
# PNG already exists.
#
#  - Tier 2:  dark gunmetal plating + cindrite-ember trim + amber visor ("upgraded hardware").
#  - Thermal: charred obsidian plating + furnace-bright seams + hot visor (Cindara-grade).
#  - Cryo:    frost-blue plating + deep-blue trim + icy visor (Glacira-grade).

SUIT_VARIANTS = {
    "t2": {
        "shell": [(34, 38, 48), (56, 62, 76), (82, 90, 108), (112, 122, 142), (146, 158, 180)],
        "accent": (236, 108, 32), "glass": (255, 190, 80),
    },
    "heat": {
        "shell": [(20, 14, 14), (40, 26, 22), (66, 40, 30), (94, 56, 38), (124, 76, 48)],
        "accent": (255, 150, 60), "glass": (255, 110, 60),
    },
    "cold": {
        "shell": [(96, 130, 168), (140, 176, 206), (180, 212, 234), (212, 236, 248), (238, 250, 255)],
        "accent": (50, 110, 170), "glass": (180, 240, 255),
    },
}


def _suit_variant(src_path, dst_path, spec):
    """Re-style a Tier 1 suit texture into a variant: grey shell pixels are value-mapped onto the
    variant's plating ramp (keeping the painted shading/noise structure), saturated trim becomes
    the variant accent, visor glass becomes the variant glass, and the near-black visor void is
    left untouched so every helmet keeps its dark window."""
    if os.path.exists(dst_path) and "--force" not in sys.argv:
        print("skip (exists)", os.path.relpath(dst_path, ROOT))
        return
    if not os.path.exists(src_path):
        print("MISSING source", os.path.relpath(src_path, ROOT))
        return
    shell = spec["shell"]
    ar, ag, ab = spec["accent"]
    gr, gg, gb = spec["glass"]
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
            if lum < 40:
                continue  # visor void / inked outlines stay dark
            if b > 180 and g > 150 and r < 170:
                # visor glass reflection -> variant glass, keeping the highlight shading
                shade = max(0.7, min(1.15, lum / 200.0))
                px[x, y] = (min(255, int(gr * shade)), min(255, int(gg * shade)),
                            min(255, int(gb * shade)), a)
            elif sat > 0.3:
                # coloured trim / seals / status lights -> variant accent
                shade = max(0.45, min(1.25, lum / 140.0))
                px[x, y] = (min(255, int(ar * shade)), min(255, int(ag * shade)),
                            min(255, int(ab * shade)), a)
            else:
                # grey shell -> the variant plating ramp, indexed by source luminance
                idx = min(len(shell) - 1, int(lum / 52.0))
                px[x, y] = (shell[idx][0], shell[idx][1], shell[idx][2], a)
    img.save(dst_path)
    print("wrote", os.path.relpath(dst_path, ROOT))


def _gen_suit_variant(variant):
    spec = SUIT_VARIANTS[variant]
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        _suit_variant(os.path.join(ITEM_DIR, "oxygen_suit_%s.png" % piece),
                      os.path.join(ITEM_DIR, "oxygen_suit_%s_%s.png" % (variant, piece)), spec)
    equip = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures/entity/equipment")
    for layer in ("humanoid", "humanoid_leggings"):
        _suit_variant(os.path.join(equip, layer, "oxygen_suit.png"),
                      os.path.join(equip, layer, "oxygen_suit_%s.png" % variant), spec)


def gen_oxygen_suit_t2():
    _gen_suit_variant("t2")


def gen_oxygen_suit_heat():
    _gen_suit_variant("heat")


# ---- Multiple stations (MULTI_STATION_DESIGN.md) ----------------------------

def gen_station_core():
    """Station Core: station-family steel panel with a glowing cyan beacon core + seam cross."""
    rng = random.Random(0x57A7)
    img = new_img()
    noise_fill(img, [(176, 176, 192, 255), (158, 158, 176, 255), (146, 146, 164, 255)], rng)
    px = img.load()
    CYAN_D = (24, 96, 120, 255)
    CYAN = (60, 190, 220, 255)
    CYAN_HI = (170, 240, 255, 255)
    for i in range(S):  # seam cross
        px[i, 8] = R_GRAY
        px[8, i] = R_GRAY
    # Beacon core: a 4x4 glowing diamond at the centre.
    for (cx, cy) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        px[cx, cy] = CYAN_HI
    for (cx, cy) in [(6, 7), (6, 8), (9, 7), (9, 8), (7, 6), (8, 6), (7, 9), (8, 9)]:
        px[cx, cy] = CYAN
    for (cx, cy) in [(5, 7), (5, 8), (10, 7), (10, 8), (7, 5), (8, 5), (7, 10), (8, 10)]:
        px[cx, cy] = CYAN_D
    bevel(img, R_WHITE, R_DARK)
    for (bx, by) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[bx, by] = R_DARK  # corner rivets
    save(img, os.path.join(BLOCK_DIR, "station_core.png"))


def gen_station_charter():
    """Station Charter: a rolled blueprint — pale scroll, cyan station seal, steel end-caps."""
    img = new_img()
    px = img.load()
    PAPER = (228, 232, 238, 255)
    PAPER_D = (188, 196, 206, 255)
    SEAL = (60, 190, 220, 255)
    SEAL_D = (24, 96, 120, 255)
    for y in range(3, 13):  # scroll body
        for x in range(4, 12):
            px[x, y] = PAPER if (x + y) % 5 else PAPER_D
    for y in range(3, 13):  # rolled edges
        px[4, y] = PAPER_D
        px[11, y] = PAPER_D
    for x in range(3, 13):  # steel end-caps
        px[x, 2] = R_GRAY
        px[x, 13] = R_GRAY
    px[3, 2] = R_DARK; px[12, 2] = R_DARK
    px[3, 13] = R_DARK; px[12, 13] = R_DARK
    # Station seal: a small cyan diamond.
    px[7, 7] = SEAL; px[8, 7] = SEAL
    px[7, 8] = SEAL; px[8, 8] = SEAL
    px[7, 6] = SEAL_D; px[8, 9] = SEAL_D
    px[6, 7] = SEAL_D; px[9, 8] = SEAL_D
    save(img, os.path.join(ITEM_DIR, "station_charter.png"))


def gen_oxygen_suit_cold():
    _gen_suit_variant("cold")


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
# Layout matches the Rocket*Model classes (128x128). Every part owns a DISJOINT UV region — the
# old 64x64 sheet had the T2/T4 boosters, T3 skirts/long nose and T4 wide body sampling outside
# the texture, which entityCutout discards as transparent = invisible "missing" parts:
#   body @ (0,0)      nose @ (0,56)     tip @ (44,56)     console @ (44,68)   bell @ (0,80)
#   fins @ (64,0)     boosters W/E @ (80,0)   boosters N/S @ (104,0)
#   skirts N/S @ (64,32)   skirts W/E @ (64,48)
# Each body side face gets a DARK-FRAMED, fully TRANSPARENT window (entityCutout discards alpha
# = a real hole the standing rider sees out of), an accent stripe per tier, and the console
# carries the interior gauge art. Geometry per tier (w,h,d) mirrors the Java models.

ROCKET_TIER_SPECS = {
    "rocket_t1": {"accent": (224, 58, 58, 255), "trim": (160, 36, 36, 255),
                  "nose": (8, 8, 8)},
    "rocket_t2": {"accent": (179, 39, 158, 255), "trim": (106, 31, 140, 255),
                  "nose": (8, 8, 8), "boosters": (4, 18, 5)},
    "rocket_t3": {"accent": (240, 200, 80, 255), "trim": (60, 170, 90, 255),
                  "nose": (8, 14, 8), "tip": True, "skirts": True},
    # Tier 4 — Glacira run: ice-cyan accent + glacial blue trim (palette rule: steel + per-tier accent).
    "rocket_t4": {"accent": (120, 210, 240, 255), "trim": (60, 130, 200, 255),
                  "body": (14, 36, 14), "nose": (10, 10, 10), "bell": (10, 3, 10),
                  "fins": False, "boosters": (4, 18, 6), "boosters_ns": (6, 18, 4)},
}


def gen_rocket_tier_entity(name, spec):
    rng = random.Random(hash(name) & 0xffffffff)
    img = Image.new("RGBA", (128, 128), CLEAR)
    px = img.load()
    whites = [(232, 232, 244, 255), (214, 214, 228, 255), (196, 196, 212, 255)]
    steel = [(150, 150, 168, 255), (132, 132, 150, 255), (118, 118, 136, 255)]
    bell_metal = [(70, 70, 86, 255), (56, 56, 70, 255), (44, 44, 56, 255)]
    dark = (70, 70, 86, 255)
    ink = (28, 28, 36, 255)
    accent, trim = spec["accent"], spec["trim"]

    def plate(x0, y0, x1, y1, ramp=whites):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[x, y] = rng.choice(ramp)

    # --- Body @ (0,0): top/bottom caps + the four side faces (the side row is [d][w][d][w]
    # columns; body w == d for every tier, so equal quarters).
    bw, bh, bd = spec.get("body", (12, 36, 12))
    side_w = 2 * (bw + bd)
    y0, y1 = bd, bd + bh
    plate(bd, 0, bd + 2 * bw, bd)
    plate(0, y0, side_w, y1)
    # Panel lines (art overhaul §4.2): vertical hull seams + rivet rows so the hull reads plated.
    for x in range(0, side_w, 6):
        for y in range(y0 + 1, y1 - 1):
            if rng.random() < 0.8:
                px[x, y] = (174, 176, 194, 255)
    for y in (y0 + 4, y0 + 24):
        for x in range(1, side_w, 3):
            px[x, y] = dark
    for col in range(4):
        u0 = col * side_w // 4
        u1 = (col + 1) * side_w // 4
        for x in range(u0, u1):
            for y in range(y0 + 16, y0 + 21):
                px[x, y] = accent
            px[x, y0] = dark
            px[x, y1 - 1] = dark
            px[x, y1 - 4] = trim
        # Window band at the standing rider's eye line: dark frame, transparent glass.
        wx0 = u0 + (u1 - u0 - 6) // 2
        for y in range(y0 + 8, y0 + 15):
            for x in range(wx0, wx0 + 6):
                px[x, y] = ink
        for y in range(y0 + 9, y0 + 14):
            for x in range(wx0 + 1, wx0 + 5):
                px[x, y] = CLEAR

    # --- Nose @ (0,56): accent cap ring + dark base seam; T3's long cone gets a mid trim ring
    # and the separate tip cube @ (44,56).
    nw, nh, nd = spec["nose"]
    nose_w = 2 * (nw + nd)
    plate(nd, 56, nd + 2 * nw, 56 + nd)
    plate(0, 56 + nd, nose_w, 56 + nd + nh)
    for x in range(0, nose_w):
        px[x, 56 + nd] = accent
        px[x, 56 + nd + nh - 1] = dark
    if spec.get("tip"):
        for x in range(0, nose_w):
            px[x, 56 + nd + nh // 2] = trim
        plate(48, 56, 56, 60)
        plate(44, 60, 60, 64)
        for x in range(44, 60):
            px[x, 60] = accent

    # --- Console @ (44,68): ink panel + the interior gauge cluster.
    for y in range(68, 74):
        for x in range(44, 62):
            px[x, y] = (16, 22, 30, 255)
    g = (88, 224, 128, 255)
    a = (255, 196, 64, 255)
    r = (236, 80, 80, 255)
    b = (96, 196, 255, 255)
    for i, c in enumerate((g, g, a, g, r, b, g, a)):
        px[46 + i, 70] = c
    for i in range(6):
        px[54 + i, 72] = b if i % 2 == 0 else (40, 90, 130, 255)
    for i in range(4):
        px[46 + i * 2, 72] = a

    # --- Bell @ (0,80): dark engine metal, accent throat ring, ink nozzle lip.
    blw, blh, bld = spec.get("bell", (8, 3, 8))
    bell_w = 2 * (blw + bld)
    plate(bld, 80, bld + 2 * blw, 80 + bld, ramp=bell_metal)
    plate(0, 80 + bld, bell_w, 80 + bld + blh, ramp=bell_metal)
    for x in range(0, bell_w):
        px[x, 80 + bld] = accent
        px[x, 80 + bld + blh - 1] = ink

    # --- Fins @ (64,0) (T1/T2/T3 — T4 swaps them for the four boosters).
    if spec.get("fins", True):
        plate(64, 0, 76, 14)
        for x in range(64, 76):
            px[x, 4] = accent
            px[x, 13] = dark

    # --- Strap-on boosters: W/E @ (80,0) (T2 + T4), N/S @ (104,0) (T4). Accent nose cap,
    # trim tail band, dark nozzle rows, rivet ticks.
    def booster(u, dims):
        w, h, d = dims
        full = 2 * (w + d)
        plate(u + d, 0, u + d + 2 * w, d)
        plate(u, d, u + full, d + h)
        for x in range(u, u + full):
            px[x, d] = accent
            px[x, d + 1] = accent
            px[x, d + h - 5] = trim
            px[x, d + h - 2] = dark
            px[x, d + h - 1] = ink
        for y in range(d + 4, d + h - 6, 4):
            for x in range(u + 1, u + full, 3):
                px[x, y] = dark

    if "boosters" in spec:
        booster(80, spec["boosters"])
    if "boosters_ns" in spec:
        booster(104, spec["boosters_ns"])

    # --- T3 ring skirt: N/S slabs @ (64,32), W/E slabs @ (64,48) — steel with an accent crown.
    if spec.get("skirts"):
        def skirt(u, v, dims):
            w, h, d = dims
            full = 2 * (w + d)
            plate(u + d, v, u + d + 2 * w, v + d, ramp=steel)
            plate(u, v + d, u + full, v + d + h, ramp=steel)
            for x in range(u, u + full):
                px[x, v + d] = accent
                px[x, v + d + h - 1] = dark

        skirt(64, 32, (14, 4, 2))
        skirt(64, 48, (2, 4, 10))

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


# ---------------- MINER_DESIGN: QUARRY / MINER ----------------

def gen_quarry_controller():
    rng = random.Random(1301)
    img = new_img()
    px = img.load()
    noise_fill(img, METAL, rng)
    bevel(img, METAL_L, METAL_D)
    # recessed core
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = METAL_D
    # tier-1 red drill cross
    for i in range(4, 12):
        px[i, 8] = N_RED
        px[8, i] = N_RED
    px[8, 8] = N_GLOW
    # cyan projector corner accents
    for (bx, by) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[bx, by] = I_CYAN
    save(img, os.path.join(BLOCK_DIR, "quarry_controller.png"))


def gen_quarry_landmark():
    rng = random.Random(1302)
    img = new_img()
    px = img.load()
    # opaque dark-steel body (rendered as a full cube)
    noise_fill(img, [METAL[2], METAL[0], METAL_D], rng)
    bevel(img, METAL_L, METAL_D)
    # central purple emitter column with a bright top
    for y in range(2, 14):
        for x in range(6, 10):
            px[x, y] = N_PURPLE if (x + y) % 2 else N_MAG
    for x in range(6, 10):
        px[x, 2] = N_GLOW
    px[7, 1] = N_BRIGHT
    px[8, 1] = N_BRIGHT
    save(img, os.path.join(BLOCK_DIR, "quarry_landmark.png"))


def _gen_quarry_strut(name, rail, node, seed):
    # Energised strut texture: steel body + coloured energy rails + glow weld nodes. Shared shape for
    # the quarry frame block model AND (via entity render types) the moving gantry + drill head, each
    # in its own colour. Narrow beam faces sample a thin UV slice, so a rail sits every 4px on both
    # axes so any slice lights up.
    rng = random.Random(seed)
    img = new_img()
    px = img.load()
    noise_fill(img, METAL, rng)
    bevel(img, METAL_L, METAL_D)
    for i in range(S):
        for k in range(0, S, 4):
            px[k, i] = rail
            px[i, k] = rail
    for y in range(0, S, 4):
        for x in range(0, S, 4):
            px[x, y] = node
    save(img, os.path.join(BLOCK_DIR, name + ".png"))


def gen_quarry_frame():
    # Frame ring — BLUE.
    _gen_quarry_strut("quarry_frame", (52, 120, 246, 255), (150, 198, 255, 255), 1303)


def gen_quarry_gantry():
    # Moving gantry (bridge / carriage / support shaft) — PURPLE.
    _gen_quarry_strut("quarry_gantry", N_PURPLE, (200, 130, 255, 255), 1304)


def gen_quarry_drill():
    # Drill head — keeps the original red/steel look.
    _gen_quarry_strut("quarry_drill", N_RED, N_GLOW, 1305)


def gen_frame_casing():
    img = new_img()
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            if x in (3, 12) or y in (3, 12):
                px[x, y] = METAL_L if (x + y) % 2 else METAL[1]
    for y in range(4, 12):
        for x in range(4, 12):
            if x in (4, 11) or y in (4, 11):
                px[x, y] = METAL_D
    for (bx, by) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        px[bx, by] = METAL_L
    save(img, os.path.join(ITEM_DIR, "frame_casing.png"))


def gen_quarry_module(name, accent):
    img = new_img()
    px = img.load()
    for y in range(2, 14):
        for x in range(3, 13):
            px[x, y] = (40, 44, 54, 255)
    for x in range(3, 13):
        px[x, 2] = accent
        px[x, 13] = accent
    for y in range(2, 14):
        px[3, y] = accent
        px[12, y] = accent
    for y in range(5, 9):
        for x in range(6, 10):
            px[x, y] = accent
    px[7, 6] = (255, 255, 255, 255)
    for x in range(5, 11, 2):
        px[x, 13] = METAL_L
    save(img, os.path.join(ITEM_DIR, name + ".png"))


def gen_gui_quarry(accent, accent_d):
    """A 176x210 quarry-screen hull: frame + module sockets on top, a 2x6 output grid, a readout
    band, and the standard player inventory at (8,126)."""
    W, H = 176, 210
    img = Image.new("RGBA", (256, 256), CLEAR)
    px = img.load()
    rng = random.Random(hash("quarry") & 0xFFFF)
    INK = (5, 8, 13, 255)
    HULL = [(13, 17, 25, 255), (15, 20, 29, 255), (11, 15, 22, 255)]
    PANEL = (8, 11, 17, 255)
    SOCKET = (20, 26, 36, 255)
    SOCKET_HI = (44, 56, 74, 255)
    for y in range(H):
        for x in range(W):
            px[x, y] = rng.choice(HULL)
    for i in range(W):
        px[i, 0] = accent
        px[i, H - 1] = accent_d
    for i in range(H):
        px[0, i] = accent
        px[W - 1, i] = accent_d

    def socket(sx, sy):
        for y in range(sy - 1, sy + 17):
            for x in range(sx - 1, sx + 17):
                px[x, y] = SOCKET
        for x in range(sx - 1, sx + 17):
            px[x, sy - 1] = INK
            px[x, sy + 16] = SOCKET_HI
        for y in range(sy - 1, sy + 17):
            px[sx - 1, y] = INK
            px[sx + 16, y] = SOCKET_HI

    def recess(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[x, y] = PANEL

    socket(8, 20)                       # frame casing
    for i in range(4):                  # module sockets (room for up to 4 — Tier 3)
        socket(26 + i * 18, 20)
    for r in range(2):                  # output grid
        for c in range(6):
            socket(8 + c * 18, 42 + r * 18)
    recess(6, 78, 170, 114)             # readout band
    for row in range(3):                # player inventory
        for col in range(9):
            socket(8 + col * 18, 126 + row * 18)
    for col in range(9):                # hotbar
        socket(8 + col * 18, 184)
    save(img, os.path.join(GUI_DIR, "quarry.png"))


def gen_trash_can():
    rng = random.Random(1401)
    img = new_img()
    px = img.load()
    noise_fill(img, METAL, rng)
    bevel(img, METAL_L, METAL_D)
    # vertical ribs
    for x in (4, 8, 11):
        for y in range(2, 14):
            px[x, y] = METAL_D
    # dark open mouth across the top
    for x in range(2, 14):
        px[x, 2] = (12, 12, 16, 255)
        px[x, 3] = (20, 20, 26, 255)
    # hazard band under the rim
    for x in range(2, 14):
        px[x, 4] = HAZ_Y if (x // 2) % 2 == 0 else HAZ_K
    for (rx, ry) in [(2, 13), (13, 13)]:
        px[rx, ry] = METAL_L
    save(img, os.path.join(BLOCK_DIR, "trash_can.png"))


def gen_solar_panel(name, edge):
    """Futuristic photovoltaic deck: neon blue-and-green cells on a near-black lattice, filling the
    sprite edge-to-edge (pixel-perfect — no steel padding), wrapped in a single tier-coloured pixel
    border. The {@code edge} colour codes the tier (T1/T2/T3); the cells are the same for every tier."""
    img = new_img()
    px = img.load()
    sub     = (10, 16, 30, 255)    # near-black blue substrate = the grid wires/gaps
    blue_d  = (22, 60, 104, 255)
    blue    = (40, 112, 172, 255)
    blue_hi = (96, 184, 238, 255)
    grn     = (50, 206, 112, 255)  # bright green mixed in with the blue cells
    grn_hi  = (132, 255, 176, 255)
    for y in range(S):
        for x in range(S):
            if x % 3 == 0 or y % 3 == 0:
                px[x, y] = sub                      # thin dark lattice between cells
            else:
                cx, cy = x // 3, y // 3
                node = (x % 3 == 2 and y % 3 == 2)  # cell centre = highlight node
                if (cx + cy) % 3 == 1:              # ~a third of cells are bright green
                    px[x, y] = grn_hi if node else grn
                else:
                    px[x, y] = blue_hi if node else (blue if (x + y) % 2 == 0 else blue_d)
    # A brighter green core node so the deck reads as "alive".
    px[7, 7] = grn_hi
    px[8, 8] = grn_hi
    # Tier-coloured edge ring: the outermost pixels, all four sides (pixel-perfect, no padding).
    for i in range(S):
        px[0, i] = edge
        px[S - 1, i] = edge
        px[i, 0] = edge
        px[i, S - 1] = edge
    save(img, os.path.join(BLOCK_DIR, name + ".png"))


def gen_solar_panel_base(name, edge):
    """The housing the deck sits on — a neutral blued-steel plate (NOT the green steel, NOT the PV
    sprite) with a recessed bay and tier-coloured corner bolts, so the static base reads as a distinct
    futuristic mount and still identifies its tier."""
    img = new_img()
    px = img.load()
    base = METAL[1]
    dk = METAL[2]
    lt = METAL_L
    recess = METAL[0]
    for y in range(S):
        for x in range(S):
            px[x, y] = base
    bevel(img, lt, dk)
    # Recessed central bay (where the deck rests when folded flat).
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = dk if (x + y) % 2 == 0 else recess
    # Two vent ridges + tier-coloured corner bolts.
    for x in range(4, 12):
        px[x, 5] = lt
        px[x, 10] = lt
    for (bx, by) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[bx, by] = edge
    save(img, os.path.join(BLOCK_DIR, name + ".png"))


if __name__ == "__main__":
    # Solar panels (SOLAR_PANEL_DESIGN): shared futuristic blue+green PV deck; the edge ring colour
    # codes the tier — T1 signal red, T2 nerosium magenta, T3 gold.
    gen_solar_panel("solar_panel_t1", N_RED)
    gen_solar_panel_base("solar_panel_t1_base", N_RED)
    gen_solar_panel("solar_panel_t2", N_MAG)
    gen_solar_panel_base("solar_panel_t2_base", N_MAG)
    gen_solar_panel("solar_panel_t3", GOLD)
    gen_solar_panel_base("solar_panel_t3_base", GOLD)
    # Trash Can (logistics void sink).
    gen_trash_can()
    # Quarry / Miner (MINER_DESIGN).
    gen_quarry_controller()
    gen_quarry_landmark()
    gen_quarry_frame()
    gen_quarry_gantry()
    gen_quarry_drill()
    gen_frame_casing()
    gen_quarry_module("speed_module", (90, 200, 255, 255))
    gen_quarry_module("efficiency_module", (120, 230, 140, 255))
    gen_quarry_module("fortune_module", (120, 180, 255, 255))
    gen_quarry_module("silk_touch_module", (230, 180, 255, 255))
    gen_gui_quarry((224, 64, 90, 255), (90, 22, 28, 255))
    # Heavy Launch Complex + cockpit rework.
    gen_launch_gantry()
    gen_rocket_tier_entities()
    # Star Guide (progression block, 1.0).
    gen_star_guide()
    gen_star_guide_book()
    gen_gui_star_guide()
    gen_oxygen_particle()
    gen_oxygen_hud_icon()
    gen_rocket_fuel_block()
    gen_rocket_fuel_fluid()
    gen_terraformer()
    # Deeper terraforming (DEEPER_TERRAFORM_DESIGN.md).
    gen_hydration_module()
    gen_gui_machine_panel("hydration_module", (120, 210, 240, 255), (40, 90, 130, 255))
    gen_terraform_monitor()
    gen_gui_machine_panel("terraform_monitor", (84, 212, 106, 255), (30, 90, 44, 255), slots=())
    # Art overhaul A2: per-face machine textures + tank cores + gantry platform.
    gen_machine_faces()
    # Art overhaul A4 (sign-off Q6): every standard machine GUI on the shared hull generator.
    gen_gui_machine_panel("nerosium_grinder", (248, 56, 64, 255), (90, 22, 28, 255),
                          slots=((56, 35), (116, 35)))
    gen_gui_machine_panel("combustion_generator", (255, 158, 36, 255), (110, 62, 14, 255))
    gen_gui_machine_panel("passive_generator", (84, 212, 106, 255), (30, 90, 44, 255))
    gen_gui_machine_panel("terraformer", (84, 212, 106, 255), (30, 90, 44, 255))
    gen_gui_machine_panel("oxygen_generator", (120, 210, 240, 255), (40, 90, 130, 255), slots=())
    gen_gui_machine_panel("fuel_tank", (255, 158, 36, 255), (110, 62, 14, 255), slots=())
    gen_gui_machine_panel("fuel_refinery", (255, 158, 36, 255), (110, 62, 14, 255),
                          slots=((56, 35), (104, 35)))
    # Creature base textures (additive; (re)render only under the creature-scoped --creatures flag).
    gen_creatures()
    gen_spawn_eggs()
    gen_loper_haunch()
    gen_strutter_drumstick()
    gen_drift_fleece()
    for _name in ("xertz_stalker", "quartz_crawler", "greenling", "cinder_stalker", "frost_strider",
                  "meadow_loper", "ember_strutter", "woolly_drift"):
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
    gen_fuel_refinery()
    gen_oxygen_generator()
    # Storage endpoints + creative sources.
    gen_storage_endpoints()
    # Universal pipe translucent tube.
    gen_universal_pipe_glass()
    # Suit-and-station integration — Tier 2 (cindrite-upgraded) oxygen suit, derived from Tier 1 art.
    gen_oxygen_suit_t1()
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
    # Multiple stations (MULTI_STATION_DESIGN.md).
    gen_station_core()
    gen_station_charter()
    # Developer diagnostics — hidden Sentry test block: a steel panel with a red warning glyph.
    gen_panel_block("sentry_test", N_RED, SYM_BANG)