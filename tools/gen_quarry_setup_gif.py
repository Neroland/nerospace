#!/usr/bin/env python3
"""Animated wiki diagram: how a Quarry Controller is set up and how its frame is built.

Renders ``wiki/images/quarry_setup.gif`` -- a top-down plan of an 8x8 claim plus a side panel
(GUI-style status + a cross-section of the pit) and a caption bar whose step numbers match
``wiki/Quarry-Setup-Guide.md``. The sequence mirrors the real controller logic:

  1. flat ground                      5. Frame Casing loaded -> frame placed one block at a time,
  2. three landmarks in an L,            in QuarryRegion.framePositions() order (28 casings)
     projecting marker lasers         6. power + chest connected
  3. controller beside the south side 7. interior mined layer by layer (perimeter skipped), x-fast
  4. claim -> landmarks consumed         sweep like the cursor in QuarryControllerBlockEntity
                                      8. bedrock -> frame dismantled, casings back in the controller

Only the mod's own block textures are used (read from the resources tree); vanilla things
(ground, chest, generator surroundings) are drawn as flat pixel-art shapes, so no Mojang assets
end up in the wiki. Output is deterministic: re-running produces the same GIF.

Usage: python tools/gen_quarry_setup_gif.py [--textures DIR] [--out FILE]
Deps: Pillow (10.1+ for the scalable default font)
"""
import argparse
import os
import sys

from PIL import Image, ImageDraw, ImageFont

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_TEXTURES = os.path.join(REPO_ROOT, "common", "src", "main", "resources", "assets",
                                "nerospace", "textures", "block")
DEFAULT_OUT = os.path.join(REPO_ROOT, "wiki", "images", "quarry_setup.gif")

# --- Layout ------------------------------------------------------------------------------------
CELL = 32                      # 16px textures at 2x
GW, GH = 11, 12                # plan grid (cells)
PLAN_X, PLAN_Y = 8, 8
SIDE_X = PLAN_X + GW * CELL + 12
SIDE_W = 200
W = SIDE_X + SIDE_W + 8
CAPTION_H = 52
H = PLAN_Y + GH * CELL + 8 + CAPTION_H

# Claim: landmark-to-landmark 8x8 at grid x/z 1..8; interior 2..7 (6x6 = 36 columns).
MIN_X, MAX_X, MIN_Z, MAX_Z = 1, 8, 1, 8
LANDMARKS = [(1, 8), (8, 8), (1, 1)]          # corner, X-mate, Z-mate (the L)
CONTROLLER = (5, 9)                           # beside the south side, same Y
CHEST = (6, 9)
PIPE = (5, 10)
GENERATOR = (5, 11)

# Cross-section: layers 0..8 are minable (grass, 2 dirt, 6 stone), row 9 is bedrock.
LAYERS = 9
SEC_CELL = 14

BG = (24, 26, 33)
PANEL = (36, 39, 49)
PANEL_EDGE = (70, 76, 94)
TEXT = (232, 236, 245)
MUTED = (150, 158, 178)
ACCENT = (255, 82, 104)        # landmark / Tier-1 accent red
ENERGY = (255, 196, 64)
LASER = (255, 64, 90)


def font(size):
    try:
        return ImageFont.load_default(size=size)
    except TypeError:  # Pillow < 10.1
        return ImageFont.load_default()


F_CAP = font(17)
F_SMALL = font(13)
F_TINY = font(11)


# --- Textures ----------------------------------------------------------------------------------
class Tex:
    def __init__(self, folder):
        self.folder = folder
        self.cache = {}

    def frames(self, name):
        if name not in self.cache:
            path = os.path.join(self.folder, name + ".png")
            if not os.path.isfile(path):
                sys.exit("missing texture: " + path)
            strip = Image.open(path).convert("RGBA")
            n = max(1, strip.height // strip.width)
            s = strip.width
            self.cache[name] = [strip.crop((0, i * s, s, (i + 1) * s)).resize((CELL, CELL), Image.NEAREST)
                                for i in range(n)]
        return self.cache[name]

    def get(self, name, tick=0, size=CELL):
        fr = self.frames(name)
        img = fr[tick % len(fr)]
        return img if size == CELL else img.resize((size, size), Image.NEAREST)


# --- Deterministic pixel-art ground ------------------------------------------------------------
def _hash(x, y, salt):
    h = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return (h ^ (h >> 16)) & 0xFF


def _noise_tile(base, gx, gy, salt, amp=14, speckle=None):
    tile = Image.new("RGB", (16, 16))
    px = tile.load()
    for y in range(16):
        for x in range(16):
            n = _hash(gx * 16 + x, gy * 16 + y, salt)
            d = (n % (2 * amp + 1)) - amp
            c = tuple(max(0, min(255, v + d)) for v in base)
            if speckle and n > 248:
                c = speckle
            px[x, y] = c
    return tile.resize((CELL, CELL), Image.NEAREST)


# floor colour seen from above after `d` layers have been removed from a column
DEPTH_LOOK = {
    0: ((92, 156, 58), None),            # grass
    1: ((134, 96, 67), None),            # dirt
    2: ((121, 85, 58), None),            # dirt
    3: ((125, 125, 125), None),          # stone
    4: ((116, 116, 118), (70, 200, 220)),
    5: ((106, 106, 110), None),
    6: ((96, 96, 102), (70, 200, 220)),
    7: ((86, 86, 94), None),
    8: ((76, 76, 86), None),
    9: ((48, 48, 52), (20, 20, 22)),     # bedrock
}
_ground_cache = {}


def ground(gx, gy, d):
    key = (gx, gy, d)
    if key not in _ground_cache:
        base, speck = DEPTH_LOOK[d]
        amp = 14 if d == 9 else 6
        tile = _noise_tile(base, gx, gy, salt=d + 1, amp=amp, speckle=speck)
        if d > 0:  # pit walls read as depth: darken towards the floor
            shade = Image.new("RGB", tile.size, (0, 0, 0))
            tile = Image.blend(tile, shade, min(0.45, 0.04 * d))
        _ground_cache[key] = tile
    return _ground_cache[key]


def chest_tile():
    t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(t)
    d.rectangle((1, 1, 14, 14), fill=(158, 106, 52), outline=(70, 44, 20))
    d.line((1, 6, 14, 6), fill=(70, 44, 20))
    d.rectangle((7, 5, 8, 8), fill=(200, 200, 210))
    for y in (3, 10, 12):
        d.line((3, y, 12, y), fill=(140, 92, 44))
    return t.resize((CELL, CELL), Image.NEAREST)


CHEST_TILE = None


# --- State -------------------------------------------------------------------------------------
class State:
    def __init__(self):
        self.landmarks = []          # placed landmarks
        self.lasers = False
        self.controller = False
        self.claim_flash = 0
        self.frame = set()           # placed frame cells
        self.casings = 0             # in controller frame slots
        self.power = False
        self.energy = 0.0
        self.chest = False
        self.depth = {}              # (x, z) -> layers removed
        self.drill = None            # (x, z, layer)
        self.mined = 0
        self.status = ""
        self.caption = ""
        self.sub = ""
        self.highlight = None        # cell to ring-highlight
        self.casing_fly = None       # 0..1 progress of the casing stack flying into the controller
        self.materials = None        # Step 1: how many parts of the shopping list are shown (None = hidden)

    def interior(self):
        return [(x, z) for z in range(MIN_Z, MAX_Z + 1) for x in range(MIN_X, MAX_X + 1)
                if MIN_X < x < MAX_X and MIN_Z < z < MAX_Z]


def frame_positions():
    """Same walk as QuarryRegion.framePositions()."""
    out = [(x, MIN_Z) for x in range(MIN_X, MAX_X + 1)]
    out += [(MAX_X, z) for z in range(MIN_Z + 1, MAX_Z + 1)]
    out += [(x, MAX_Z) for x in range(MAX_X - 1, MIN_X - 1, -1)]
    out += [(MIN_X, z) for z in range(MAX_Z - 1, MIN_Z, -1)]
    return out


# --- Rendering ---------------------------------------------------------------------------------
def cell_xy(x, z):
    return PLAN_X + x * CELL, PLAN_Y + z * CELL


def draw_text_center(d, xy, text, f, fill):
    l, t, r, b = d.textbbox((0, 0), text, font=f)
    d.text((xy[0] - (r - l) / 2, xy[1] - (b - t) / 2 - t), text, font=f, fill=fill)


def render(st, tex, tick):
    img = Image.new("RGBA", (W, H), BG + (255,))
    d = ImageDraw.Draw(img)

    # ground
    for gz in range(GH):
        for gx in range(GW):
            img.paste(ground(gx, gz, st.depth.get((gx, gz), 0)), cell_xy(gx, gz))
    # subtle grid
    grid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grid)
    for gx in range(GW + 1):
        x = PLAN_X + gx * CELL
        gd.line((x, PLAN_Y, x, PLAN_Y + GH * CELL), fill=(0, 0, 0, 34))
    for gz in range(GH + 1):
        y = PLAN_Y + gz * CELL
        gd.line((PLAN_X, y, PLAN_X + GW * CELL, y), fill=(0, 0, 0, 34))
    img.alpha_composite(grid)

    # claim outline hint while lasers show
    if st.lasers:
        glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        g = ImageDraw.Draw(glow)
        pulse = 150 + int(80 * ((tick % 6) / 5.0))
        for (lx, lz) in st.landmarks:
            cx, cy = cell_xy(lx, lz)
            cx += CELL // 2
            cy += CELL // 2
            reach = 7 * CELL
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ex = max(PLAN_X, min(PLAN_X + GW * CELL, cx + dx * reach))
                ey = max(PLAN_Y, min(PLAN_Y + GH * CELL, cy + dz * reach))
                g.line((cx, cy, ex, ey), fill=LASER + (70,), width=6)
                g.line((cx, cy, ex, ey), fill=LASER + (pulse,), width=2)
        img.alpha_composite(glow)

    # frame
    for (fx, fz) in st.frame:
        img.alpha_composite(tex.get("quarry_frame"), cell_xy(fx, fz))

    # landmarks
    for (lx, lz) in st.landmarks:
        img.alpha_composite(tex.get("quarry_landmark", tick), cell_xy(lx, lz))

    # power line + chest
    if st.power:
        img.alpha_composite(tex.get("universal_pipe"), cell_xy(*PIPE))
        img.alpha_composite(tex.get("combustion_generator_top"), cell_xy(*GENERATOR))
    if st.chest:
        img.alpha_composite(CHEST_TILE, cell_xy(*CHEST))
        if st.mined:
            cx, cy = cell_xy(*CHEST)
            label = "%d" % st.mined
            l, t, rr, b = d.textbbox((0, 0), label, font=F_TINY)
            bx, by = cx + CELL - 4, cy + CELL - 14
            d.rectangle((bx - 2, by - 1, bx + rr - l + 2, by + b - t + 2), fill=(16, 17, 22))
            d.text((bx - l, by - t), label, font=F_TINY, fill=TEXT)

    # controller
    if st.controller:
        img.alpha_composite(tex.get("quarry_controller", tick), cell_xy(*CONTROLLER))

    # gantry + drill (above everything on the plan)
    if st.drill:
        dx_, dz_, _layer = st.drill
        over = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        for x in range(MIN_X, MAX_X + 1):
            g = tex.get("quarry_gantry").copy()
            g.putalpha(g.getchannel("A").point(lambda a: int(a * 0.82)))
            over.alpha_composite(g, cell_xy(x, dz_))
        img.alpha_composite(over)
        big = tex.get("quarry_drill", tick, size=CELL + 8)
        cx, cy = cell_xy(dx_, dz_)
        img.alpha_composite(big, (cx - 4, cy - 4))
        ImageDraw.Draw(img).rectangle((cx - 4, cy - 4, cx + CELL + 3, cy + CELL + 3), outline=(255, 255, 255))

    # placement highlight
    if st.highlight:
        hx, hy = cell_xy(*st.highlight)
        ImageDraw.Draw(img).rectangle((hx - 2, hy - 2, hx + CELL + 1, hy + CELL + 1), outline=(255, 255, 255), width=2)

    # claim flash across the area
    if st.claim_flash:
        fl = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        x0, y0 = cell_xy(MIN_X, MIN_Z)
        x1, y1 = cell_xy(MAX_X + 1, MAX_Z + 1)
        ImageDraw.Draw(fl).rectangle((x0, y0, x1 - 1, y1 - 1), fill=(120, 190, 255, 18 * st.claim_flash),
                                     outline=(160, 210, 255, 60 * st.claim_flash), width=3)
        img.alpha_composite(fl)

    # flying casing stack
    if st.casing_fly is not None:
        t = st.casing_fly
        sx, sy = cell_xy(9, 6)
        ex, ey = cell_xy(*CONTROLLER)
        px = int(sx + (ex - sx) * t)
        py = int(sy + (ey - sy) * t)
        img.alpha_composite(tex.get("quarry_frame", size=24), (px + 4, py + 4))
        ImageDraw.Draw(img).text((px + 22, py - 6), "x28", font=F_SMALL, fill=TEXT)

    if st.materials is not None:
        draw_materials(img, st, tex, tick)
    draw_side_panel(img, st, tex, tick)
    draw_caption(img, st)
    return img.convert("RGB")


MATERIALS = [
    ("quarry_controller", "Quarry Controller", "x1"),
    ("quarry_landmark", "Quarry Landmark", "x3  (one craft)"),
    ("quarry_frame", "Frame Casing", "x28  (8 x 8 claim)"),
    ("combustion_generator_top", "Generator", "x1  (any power)"),
    ("universal_pipe", "Universal Pipe", "a few"),
    ("chest", "Chest", "x1"),
]


def draw_materials(img, st, tex, tick):
    """Step 1 card over the plan: the parts to craft before you start."""
    shade = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    x0, y0 = PLAN_X + 20, PLAN_Y + 22
    x1, y1 = PLAN_X + GW * CELL - 20, PLAN_Y + GH * CELL - 22
    ImageDraw.Draw(shade).rectangle((PLAN_X, PLAN_Y, PLAN_X + GW * CELL - 1, PLAN_Y + GH * CELL - 1),
                                    fill=(10, 11, 15, 150))
    img.alpha_composite(shade)
    d = ImageDraw.Draw(img)
    d.rectangle((x0, y0, x1, y1), fill=PANEL, outline=PANEL_EDGE, width=2)
    d.text((x0 + 16, y0 + 14), "You need", font=F_CAP, fill=TEXT)
    d.line((x0 + 16, y0 + 40, x1 - 16, y0 + 40), fill=PANEL_EDGE)
    row_h = 48
    for i, (name, label, qty) in enumerate(MATERIALS[:st.materials]):
        ry = y0 + 54 + i * row_h
        icon = CHEST_TILE if name == "chest" else tex.get(name, tick)
        img.alpha_composite(icon, (x0 + 18, ry))
        d.text((x0 + 62, ry + 1), label, font=F_SMALL, fill=TEXT)
        d.text((x0 + 62, ry + 18), qty, font=F_TINY, fill=MUTED)


def bar(d, x, y, w, h, frac, colour):
    d.rectangle((x, y, x + w, y + h), fill=(20, 22, 28), outline=PANEL_EDGE)
    if frac > 0:
        d.rectangle((x + 2, y + 2, x + 2 + int((w - 4) * frac), y + h - 2), fill=colour)


def draw_side_panel(img, st, tex, tick):
    d = ImageDraw.Draw(img)
    x0, y0 = SIDE_X, PLAN_Y
    d.rectangle((x0, y0, x0 + SIDE_W, y0 + 158), fill=PANEL, outline=PANEL_EDGE)
    d.text((x0 + 10, y0 + 8), "Quarry Controller", font=F_SMALL, fill=TEXT)
    d.text((x0 + 10, y0 + 28), st.status or "-", font=F_TINY, fill=ACCENT if st.status.startswith("Paused") else MUTED)

    # casing slots (4) like the GUI's top-left
    for i in range(4):
        sx = x0 + 10 + i * 22
        d.rectangle((sx, y0 + 50, sx + 19, y0 + 69), fill=(20, 22, 28), outline=PANEL_EDGE)
    left = st.casings
    for i in range(4):
        n = min(64, left)
        left -= n
        if n:
            sx = x0 + 10 + i * 22
            img.alpha_composite(tex.get("quarry_frame", size=16), (sx + 2, y0 + 52))
            l, t, rr, b = d.textbbox((0, 0), str(n), font=F_TINY)
            tx, ty = sx + 19 - (rr - l), y0 + 69 - (b - t)
            d.text((tx + 1 - l, ty + 1 - t), str(n), font=F_TINY, fill=(0, 0, 0))
            d.text((tx - l, ty - t), str(n), font=F_TINY, fill=TEXT)
    d.text((x0 + 104, y0 + 54), "Frame Casing", font=F_TINY, fill=MUTED)

    d.text((x0 + 10, y0 + 80), "Energy", font=F_TINY, fill=MUTED)
    bar(d, x0 + 60, y0 + 80, SIDE_W - 72, 12, st.energy, ENERGY)
    depth = st.drill[2] + 1 if st.drill else (LAYERS if st.status.startswith("Finished") else 0)
    d.text((x0 + 10, y0 + 102), "Depth", font=F_TINY, fill=MUTED)
    d.text((x0 + 60, y0 + 102), "%d layer%s" % (depth, "" if depth == 1 else "s"), font=F_TINY, fill=TEXT)
    d.text((x0 + 10, y0 + 122), "Mined", font=F_TINY, fill=MUTED)
    d.text((x0 + 60, y0 + 122), "%d blocks" % st.mined, font=F_TINY, fill=TEXT)
    d.text((x0 + 10, y0 + 140), "Frame", font=F_TINY, fill=MUTED)
    d.text((x0 + 60, y0 + 140), "%d / 28" % len(st.frame), font=F_TINY, fill=TEXT)

    # cross-section (slice through the middle of the claim)
    sy0 = y0 + 172
    d.rectangle((x0, sy0, x0 + SIDE_W, sy0 + 212), fill=PANEL, outline=PANEL_EDGE)
    d.text((x0 + 10, sy0 + 6), "Side view (cross-section)", font=F_TINY, fill=MUTED)
    ox = x0 + (SIDE_W - 10 * SEC_CELL) // 2
    oy = sy0 + 48
    mid_z = 4
    # sky row above refY
    for col in range(10):
        gx = col  # grid x 0..9
        for row in range(LAYERS + 1):
            px, py = ox + col * SEC_CELL, oy + row * SEC_CELL
            in_claim = MIN_X <= gx <= MAX_X
            interior = MIN_X < gx < MAX_X
            removed = st.depth.get((gx, mid_z), 0) if interior else 0
            look = None if row < removed else DEPTH_LOOK[row][0]
            if look:
                d.rectangle((px, py, px + SEC_CELL - 1, py + SEC_CELL - 1), fill=look)
            else:
                d.rectangle((px, py, px + SEC_CELL - 1, py + SEC_CELL - 1), fill=(30, 33, 42))
            if row == 0 and in_claim and not interior and (gx, mid_z) in st.frame:
                img.alpha_composite(tex.get("quarry_frame", size=SEC_CELL), (px, py))
    # gantry beam + drill in section
    if st.drill:
        bx0 = ox + MIN_X * SEC_CELL
        bx1 = ox + (MAX_X + 1) * SEC_CELL
        d.rectangle((bx0, oy - 14, bx1 - 1, oy - 9), fill=(150, 70, 200))
        dx_, _dz, layer = st.drill
        cx = ox + dx_ * SEC_CELL + SEC_CELL // 2
        tip = oy + layer * SEC_CELL + SEC_CELL - 3
        d.line((cx, oy - 9, cx, tip - 6), fill=(190, 190, 205), width=3)
        d.polygon([(cx - 5, tip - 8), (cx + 5, tip - 8), (cx, tip)], fill=(255, 90, 110))
    d.text((ox + 10 * SEC_CELL + 4, oy + 1), "top", font=F_TINY, fill=MUTED)
    d.text((ox, oy + (LAYERS + 1) * SEC_CELL + 3), "bedrock", font=F_TINY, fill=MUTED)


def draw_caption(img, st):
    d = ImageDraw.Draw(img)
    y0 = H - CAPTION_H
    d.rectangle((0, y0, W, H), fill=(16, 17, 22))
    d.line((0, y0, W, y0), fill=ACCENT, width=2)
    d.text((12, y0 + 7), st.caption, font=F_CAP, fill=TEXT)
    if st.sub:
        d.text((12, y0 + 30), st.sub, font=F_SMALL, fill=MUTED)


# --- Timeline ----------------------------------------------------------------------------------
def build(tex):
    frames = []
    st = State()
    tick = [0]

    def snap(ms, n=1):
        for _ in range(n):
            frames.append((render(st, tex, tick[0]), ms))
            tick[0] += 1

    st.caption = "Step 1 - Gather the materials"
    st.sub = "Everything is crafted from Nerosteel Ingots plus a few vanilla items."
    st.materials = 0
    snap(500, 2)
    for k in range(1, len(MATERIALS) + 1):
        st.materials = k
        snap(450)
    snap(500, 6)
    st.materials = None

    st.caption = "Step 2 - Pick a flat spot"
    st.sub = "The landmark height becomes the frame level: it digs that level and everything below."
    snap(500, 6)

    st.caption = "Step 3 - Place 3 Quarry Landmarks in an L"
    st.sub = "Corner + one along X + one along Z, all at the same height."
    for lm in LANDMARKS:
        st.landmarks.append(lm)
        st.highlight = lm
        snap(220, 2)
        st.highlight = None
        snap(220, 1)
    st.lasers = True
    st.sub = "Their marker lasers show the edges of the rectangle (here 8 x 8)."
    snap(160, 10)

    st.caption = "Step 4 - Place the controller beside one side"
    st.sub = "Same height as the landmarks, touching a side (not a corner)."
    st.controller = True
    st.status = "Idle - set landmarks or frame"
    st.highlight = CONTROLLER
    snap(220, 3)
    st.highlight = None
    snap(220, 2)

    st.caption = "Step 4 - The controller claims the area"
    st.sub = "The landmarks are used up."
    st.lasers = False
    st.landmarks = []
    st.status = "Paused - out of Frame Casing"
    for k in (3, 2, 1, 0):
        st.claim_flash = k
        snap(140)
    snap(400, 2)

    st.caption = "Step 5 - Load Frame Casing"
    st.sub = "One casing per frame block: 2 x 8 + 2 x 8 - 4 = 28 for this claim."
    for i in range(6):
        st.casing_fly = i / 5.0
        snap(90)
    st.casing_fly = None
    st.casings = 28
    st.status = "Building frame"
    snap(300, 2)

    st.sub = "The frame builds itself, one block every half-second in-game."
    for pos in frame_positions():
        st.frame.add(pos)
        st.casings -= 1
        snap(90)
    snap(300, 2)

    st.caption = "Step 6-7 - Connect power and a chest"
    st.sub = "Power on any side (40 FE per block); mined items are pushed into a touching chest."
    st.power = True
    st.status = "Paused - out of power"
    snap(300, 2)
    st.chest = True
    for i in range(6):
        st.energy = (i + 1) / 6.0
        snap(110)
    st.status = "Mining"

    st.caption = "Mining - the inside only, layer by layer"
    st.sub = "The frame ring is never dug, so it keeps its footing."
    cells = st.interior()
    for layer in range(LAYERS):
        step = 1 if layer == 0 else (3 if layer == 1 else 9)
        if layer == 3:
            st.sub = "...all the way down to bedrock. Items keep flowing into the chest."
        for i in range(0, len(cells), step):
            for (x, z) in cells[i:i + step]:
                st.depth[(x, z)] = layer + 1
                st.mined += 1
            st.drill = (cells[min(i + step, len(cells)) - 1][0], cells[min(i + step, len(cells)) - 1][1], layer)
            st.energy = 0.75 + 0.25 * ((tick[0] % 4) / 3.0)
            snap(70 if layer == 0 else 80)
    st.drill = None

    st.caption = "Step 9 - Bedrock reached: frame reclaimed"
    st.sub = "Every casing goes back into the controller, ready for the next site."
    st.status = "Finished - frame reclaimed"
    for pos in reversed(frame_positions()):
        st.frame.discard(pos)
        st.depth[pos] = 1  # the frame had replaced the top block; the dirt beneath shows
        st.casings += 1
        if len(st.frame) % 2 == 0:
            snap(70)
    snap(450, 7)
    return frames


def shared_palette(frames):
    """One global palette for every frame so the GIF doesn't shimmer between frames."""
    picks = [f for i, (f, _ms) in enumerate(frames) if i % max(1, len(frames) // 12) == 0]
    w, h = picks[0].size
    sheet = Image.new("RGB", (w, h * len(picks)))
    for i, f in enumerate(picks):
        sheet.paste(f, (0, i * h))
    return sheet.quantize(colors=255, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--textures", default=DEFAULT_TEXTURES)
    ap.add_argument("--out", default=DEFAULT_OUT)
    args = ap.parse_args()

    global CHEST_TILE
    CHEST_TILE = chest_tile()
    tex = Tex(args.textures)
    frames = build(tex)
    pal = shared_palette(frames)
    images = [f.quantize(palette=pal, dither=Image.Dither.NONE) for f, _ in frames]
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    images[0].save(args.out, save_all=True, append_images=images[1:],
                   duration=[ms for _, ms in frames], loop=0, disposal=1, optimize=True)
    total = sum(ms for _, ms in frames) / 1000.0
    print("wrote %s (%d frames, %.1fs, %d KB)" % (args.out, len(images), total,
                                                os.path.getsize(args.out) // 1024))


if __name__ == "__main__":
    main()
