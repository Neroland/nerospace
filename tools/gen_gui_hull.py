#!/usr/bin/env python3
"""
Nerospace GUI panel family generator (the shared GUI language).

One hull, many accents: every container screen's 256x256 PNG is produced by ``gen_gui_hull`` from the
``GUI_PANELS`` table below, so the whole family is reviewable as data. Also produces:

* ``hud_glyphs.png``  128x32 — four rows of 8x8 white-on-transparent glyphs, tinted at draw time
  (row 0 hazards/status, row 1 weather + clock quarters, row 2 UI arrows/pips, row 3 station modules).
* ``widgets.png``     256x64 — 9-slice button/tab/well sprites in every state, chevrons, gauge cells,
  scrollbar, the two clock-dial faces, the grinder progress arrow and the combustion flame.

Deterministic: the hull noise is seeded from the md5 of the panel name (NOT Python's salted ``hash``),
so a re-render is byte-identical on any machine.

Usage
    python3 tools/gen_gui_hull.py                 # writes missing files into the multiloader common tree
    python3 tools/gen_gui_hull.py --force         # overwrites every GUI PNG (only the GUI dir)
    python3 tools/gen_gui_hull.py --out DIR       # render everything into DIR (review / staging)
    python3 tools/gen_gui_hull.py --check         # structural diff of each repo panel vs the generator
    python3 tools/gen_gui_hull.py --only star_guide,station_console

``--check`` ignores the hull noise body: it compares only the pixels the generator paints
deliberately (frame, header, wells, sockets), so a 1.6.0 panel rendered with the old salted seed still
reports "structure OK" when its geometry matches the table.
"""
import argparse
import hashlib
import os
import random
import sys

try:
    from PIL import Image
except ModuleNotFoundError:
    print("gen_gui_hull: Pillow not installed; skipping (pip install pillow).")
    sys.exit(0)

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.dirname(HERE)
DEFAULT_GUI_DIR = os.path.join(REPO_ROOT, "common", "src", "main", "resources", "assets", "nerospace",
                               "textures", "gui")

# --- colour tokens (RGBA) -----------------------------------------------------------------
CLEAR = (0, 0, 0, 0)
GUI_INK = (5, 8, 13, 255)          # #05080D rim / trough border / socket top-left
GUI_TROUGH = (11, 17, 25, 255)     # #0B1119 gauge backing
GUI_HULL = [(13, 17, 25, 255), (15, 20, 29, 255), (11, 15, 22, 255)]   # #0D1119 ±2 noise body
GUI_HEADER = (20, 40, 60, 255)     # #14283C header band
GUI_WELL = (10, 16, 24, 255)       # #0A1018 recessed well (replaces the 1.6.0 PANEL #080B11)
GUI_SOCKET = (20, 26, 36, 255)     # #141A24 slot socket body
GUI_SOCKET_HI = (44, 56, 74, 255)  # #2C384A socket / well bottom-right highlight
GUI_RIVET = (53, 80, 106, 255)     # #35506A corner rivets
GUI_TITLE = (214, 236, 255, 255)
GUI_SUBTLE = (141, 160, 180, 255)
# SpaceButton states (read from SpaceButton.java)
BTN_IDLE = ((46, 74, 90, 255), (12, 30, 43, 255))
BTN_HOVER = (None, (22, 52, 74, 255))          # border = accent
BTN_SELECTED = (None, (18, 48, 66, 255))       # border = accent
BTN_DISABLED = ((38, 43, 51, 255), (18, 22, 31, 255))
SHEEN = (255, 255, 255, 0x22)
WHITE = (255, 255, 255, 255)


def hexrgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def seed_for(name):
    return int(hashlib.md5(name.encode("utf-8")).hexdigest()[:8], 16)


# --- the panel family (slot/gauge positions are the menus' coordinates — none move) -------
GUI_PANELS = [
    dict(name="nerosium_grinder", w=176, h=166, accent="#D23A8C", accent_d="#5A1A3C",
         wells=((6, 16, 170, 80),), sockets=((56, 35), (116, 35), (134, 35), (116, 53), (134, 53))),
    dict(name="combustion_generator", w=176, h=166, accent="#F0A83C", accent_d="#6E3E0E",
         wells=((6, 16, 170, 80),), sockets=((80, 35),)),
    dict(name="passive_generator", w=176, h=166, accent="#B327A0", accent_d="#5A1450",
         wells=((6, 16, 170, 80),), sockets=((80, 35),)),
    dict(name="fuel_tank", w=176, h=166, accent="#F0A030", accent_d="#6E3E0E",
         wells=((6, 16, 170, 80),), sockets=()),
    dict(name="fuel_refinery", w=176, h=166, accent="#F0A030", accent_d="#6E3E0E",
         wells=((6, 16, 170, 80),), sockets=((56, 35), (104, 35))),
    dict(name="oxygen_generator", w=176, h=166, accent="#3CC8E6", accent_d="#285A82",
         wells=((6, 16, 170, 80),), sockets=()),
    dict(name="terraformer", w=176, h=166, accent="#54D46A", accent_d="#1E5A2C",
         wells=((6, 16, 170, 80),), sockets=((80, 46),)),
    dict(name="terraform_monitor", w=176, h=166, accent="#54D46A", accent_d="#1E5A2C",
         wells=((6, 16, 170, 80),), sockets=()),
    dict(name="hydration_module", w=176, h=166, accent="#78D2F0", accent_d="#285A82",
         wells=((6, 16, 170, 80),), sockets=((80, 46),)),
    dict(name="advanced_filter", w=176, h=166, accent="#5AC8E0", accent_d="#1E5064",
         wells=((6, 16, 170, 80),),
         sockets=tuple((26 + 18 * c, 17 + 18 * r) for r in range(3) for c in range(3))),
    dict(name="quarry", w=176, h=210, accent="#E0405A", accent_d="#701E2C", header=0,
         wells=((6, 78, 170, 114),),
         sockets=((8, 20),) + tuple((26 + 18 * i, 20) for i in range(4))
         + tuple((8 + 18 * c, 42 + 18 * r) for r in range(2) for c in range(6)),
         inventory=(8, 126), hotbar_y=184),
    # Star Guide: rail widened to x 6..94 so every chapter
    # title fits at 1x; step canvas 96..234 x 19..95; text well 96..234 x 97..194.
    dict(name="star_guide", w=240, h=200, accent="#B05AE0", accent_d="#582D70",
         wells=((96, 19, 234, 95), (96, 97, 234, 194)), sockets=(), inventory=None,
         rail=(6, 19, 94, 194)),
    # Rocket: procedural screens (Rocket, CargoPad, LaunchController, StationCharter) paint over it.
    dict(name="rocket", w=212, h=244, accent="#E0506A", accent_d="#701E2C",
         wells=((8, 20, 72, 116), (76, 20, 172, 104)), sockets=((186, 30),),
         inventory=(26, 164), hotbar_y=222),
    # Station Console (new): horizontal tab rail + page well.
    dict(name="station_console", w=240, h=200, accent="#2EC4B6", accent_d="#17625B",
         wells=((6, 36, 234, 194),), sockets=(), inventory=None, rail=(6, 19, 234, 34)),
]

# Existing 1.6.0 header rule: the machine panels have no 16px header band in their PNG (the recess
# starts at y 16), so ``header`` defaults to 0 for the 176-wide machines and 16 for the big screens.
for _p in GUI_PANELS:
    _p.setdefault("header", 16 if _p["w"] >= 212 else 0)
    _p.setdefault("inventory", (8, 84))
    _p.setdefault("hotbar_y", None)
    _p.setdefault("rail", None)
    _p.setdefault("rivets", False)


# --- hull painter -------------------------------------------------------------------------------------
def gen_gui_hull(name, w, h, accent, accent_d, *, header=16, wells=(), sockets=(), inventory=(8, 84),
                 hotbar_y=None, rail=None, rivets=False, seed=None, structural=None):
    """One 256x256 sheet, panel (w x h) top-left. Draws, in order: HULL noise body (seeded by name);
    accent top/left + accent_d bottom/right 1px frame; INK inset line; HEADER band of ``header`` px with
    a 1px accent rule under it; every ``wells`` rect (x0,y0,x1,y1) as WELL with INK top/left +
    SOCKET_HI bottom/right; every ``sockets`` (sx,sy) as an 18x18 vanilla-style slot (item at sx+1,sy+1);
    the 3x9 player inventory at ``inventory`` and the hotbar at hotbar_y (default inventory_y+58);
    ``rail`` an optional WELL rail; rivets=True stamps 2x2 RIVET at the four corners.
    ``structural`` (a set) collects every (x, y) painted deliberately, for --check."""
    accent = hexrgb(accent) if isinstance(accent, str) else accent
    accent_d = hexrgb(accent_d) if isinstance(accent_d, str) else accent_d
    img = Image.new("RGBA", (256, 256), CLEAR)
    px = img.load()
    rng = random.Random(seed_for(name) if seed is None else seed)
    mark = structural.add if structural is not None else (lambda p: None)

    for y in range(h):
        for x in range(w):
            px[x, y] = rng.choice(GUI_HULL)

    def put(x, y, c):
        px[x, y] = c
        mark((x, y))

    for i in range(w):                      # outer frame
        put(i, 0, accent)
        put(i, h - 1, accent_d)
    for i in range(h):
        put(0, i, accent)
        put(w - 1, i, accent_d)
    for y in range(1, h - 1):               # inset shadow line
        put(1, y, GUI_INK)
        put(w - 2, y, GUI_INK)
    for x in range(1, w - 1):
        put(x, 1, GUI_INK)
        put(x, h - 2, GUI_INK)

    if header:
        for y in range(2, header):
            for x in range(2, w - 2):
                put(x, y, GUI_HEADER)
        for x in range(2, w - 2):           # 1px accent rule under the header band
            put(x, header, accent)

    def recess(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                put(x, y, GUI_WELL)
        for x in range(x0, x1):
            put(x, y0, GUI_INK)
            put(x, y1 - 1, GUI_SOCKET_HI)
        for y in range(y0, y1):
            put(x0, y, GUI_INK)
            put(x1 - 1, y, GUI_SOCKET_HI)

    def socket(sx, sy):
        for y in range(sy - 1, sy + 17):
            for x in range(sx - 1, sx + 17):
                put(x, y, GUI_SOCKET)
        for x in range(sx - 1, sx + 17):
            put(x, sy - 1, GUI_INK)
            put(x, sy + 16, GUI_SOCKET_HI)
        for y in range(sy - 1, sy + 17):
            put(sx - 1, y, GUI_INK)
            put(sx + 16, y, GUI_SOCKET_HI)

    if rail:
        recess(*rail)
    for well in wells:
        recess(*well)
    for (sx, sy) in sockets:
        socket(sx, sy)
    if inventory is not None:
        ix, iy = inventory
        for row in range(3):
            for col in range(9):
                socket(ix + col * 18, iy + row * 18)
        hy = hotbar_y if hotbar_y is not None else iy + 58
        for col in range(9):
            socket(ix + col * 18, hy)
    if rivets:
        for (rx, ry) in ((2, 2), (w - 4, 2), (2, h - 4), (w - 4, h - 4)):
            for dy in range(2):
                for dx in range(2):
                    put(rx + dx, ry + dy, GUI_RIVET)
    return img


# --- 8x8 glyph stamps (white on transparent, tinted at draw time) -----------------------------------
def _g(*rows):
    assert len(rows) == 8 and all(len(r) == 8 for r in rows), rows
    return rows


GLYPHS_ROW0 = [  # hazards + status (row 0)
    ("HEAT", _g("........", ".#..#..#", "#..#..#.", ".#..#..#", "#..#..#.", "........", "########", "........")),
    ("COLD", _g("...#....", ".#.#.#..", "..###...", "#######.", "..###...", ".#.#.#..", "...#....", "........")),
    ("TOXIC", _g("..###...", ".#...#..", ".#...#..", "#.###.#.", "#.#.#.#.", "#.###.#.", ".#...#..", "..###...")),
    ("RADIATION", _g("..###...", ".#...#..", ".#.#.#..", "#..#..#.", "#.###.#.", "#.....#.", ".#...#..", "..###...")),
    ("AIRLESS", _g("..####..", ".#....#.", "#.....##", "#....#.#", "#...#..#", "#..#...#", ".##...#.", "..####..")),
    ("NONE", _g("........", "........", "..###...", ".#####..", ".#####..", ".#####..", "..###...", "........")),
    ("SUIT", _g("..####..", ".#....#.", "#..##..#", "#.#..#.#", "#.#..#.#", "#..##..#", "########", "..#..#..")),
    ("LOCK", _g("..###...", ".#...#..", ".#...#..", "#######.", "#######.", "###.###.", "#######.", ".#####..")),
    ("GATE", _g("...#....", "..###...", ".#.#.#..", "#######.", "#######.", "###.###.", "#######.", ".#####..")),
    ("PAD", _g("........", ".######.", "#......#", "#..##..#", "#..##..#", "#......#", ".######.", "........")),
    ("DOCK", _g("#.....#.", ".#...#..", "..#.#...", "...#....", "..#.#...", ".#...#..", "#.....#.", "........")),
    ("GRAVITY_1", _g("........", "........", "........", "...#....", "..###...", "........", "#######.", "........")),
    ("GRAVITY_2", _g("........", "........", "...#....", "...#....", ".#####..", "..###...", "#######.", "........")),
    ("GRAVITY_3", _g("........", "...#....", "...#....", "...#....", ".#####..", "..###...", "#######.", "........")),
    ("GRAVITY_4", _g("...#....", "...#....", "...#....", "...#....", ".#####..", "..###...", "...#....", "#######.")),
    ("MEMBER", _g("..#..#..", ".###.###", "..#..#..", ".###.###", "#######.", "#######.", "#######.", "........")),
]
GLYPHS_ROW1 = [  # weather + clock quarters (row 1)
    ("W_NONE", _g("........", "........", "........", "........", "........", "........", "........", "........")),
    ("DUST_STORM", _g("........", "#####...", "........", ".######.", "......#.", "####..#.", ".....##.", "........")),
    ("ASH_FALL", _g("..###...", ".#####..", "#######.", "........", ".#..#..#", "........", "#..#..#.", "........")),
    ("DIAMOND_DUST", _g(".#...#..", "###.###.", ".#...#..", "........", "...#....", "#.###.#.", "...#....", "#.....#.")),
    ("SPORE_BLOOM", _g(".....##.", "....#..#", "..#..##.", ".#.#....", "..#.....", "##......", "#.#.....", ".#......")),
    ("FLARE", _g("...#....", "...#....", "..###...", ".#####..", ".#####..", "..###...", "........", "...#....")),
    ("CLOCK_SUNRISE", _g("..####..", ".#....#.", "#......#", "#...###.", "#......#", "#......#", ".#....#.", "..####..")),
    ("CLOCK_NOON", _g("..####..", ".#.#..#.", "#..#...#", "#..#...#", "#......#", "#......#", ".#....#.", "..####..")),
    ("CLOCK_SUNSET", _g("..####..", ".#....#.", "#......#", ".###...#", "#......#", "#......#", ".#....#.", "..####..")),
    ("CLOCK_MIDNIGHT", _g("..####..", ".#....#.", "#......#", "#......#", "#..#...#", "#..#...#", ".#.#..#.", "..####..")),
    ("RESERVED_10", _g(*(["........"] * 8))),
    ("RESERVED_11", _g(*(["........"] * 8))),
    ("RESERVED_12", _g(*(["........"] * 8))),
    ("RESERVED_13", _g(*(["........"] * 8))),
    ("RESERVED_14", _g(*(["........"] * 8))),
    ("RESERVED_15", _g(*(["........"] * 8))),
]
GLYPHS_ROW2 = [  # UI glyphs (arrows, chevrons, pips)
    ("ARROW_L", _g("......#.", "....##..", "..####..", "#######.", "#######.", "..####..", "....##..", "......#.")),
    ("ARROW_R", _g(".#......", "..##....", "..####..", ".#######", ".#######", "..####..", "..##....", ".#......")),
    ("CHEV_UP", _g("........", "...##...", "..####..", ".##..##.", "##....##", "........", "........", "........")),
    ("CHEV_DOWN", _g("........", "........", "........", "##....##", ".##..##.", "..####..", "...##...", "........")),
    ("CHECK", _g("........", "......#.", ".....##.", "#...##..", "##.##...", ".###....", "..#.....", "........")),
    ("STAR", _g("...#....", "...#....", "#.###.#.", ".#####..", "..###...", ".##.##..", "#.....#.", "........")),
    ("DOT_FILLED", _g("........", "........", "..###...", ".#####..", ".#####..", ".#####..", "..###...", "........")),
    ("DOT_HOLLOW", _g("........", "........", "..###...", ".#...#..", ".#...#..", ".#...#..", "..###...", "........")),
    ("ROCKET", _g("...#....", "..###...", "..###...", ".#####..", ".#####..", "#.###.#.", "#..#..#.", "..#.#...")),
    ("SCROLL", _g("...#....", "..###...", ".#.#.#..", "...#....", "...#....", ".#.#.#..", "..###...", "...#....")),
    ("TOOLTIP", _g("..###...", ".#...#..", "....#...", "...#....", "...#....", "........", "...#....", "........")),
    ("REQUIRED", _g("........", "...#....", "#..#..#.", ".#.#.#..", "..###...", ".#.#.#..", "#..#..#.", "...#....")),
    ("OPTIONAL", _g("........", "..###...", ".#...#..", "#.....#.", "#.....#.", "#.....#.", ".#...#..", "..###...")),
    ("WALL", _g("########", "#..#..#.", "########", ".#..#..#", "########", "#..#..#.", "########", "........")),
    ("GANTRY", _g("...##...", "..#..#..", "..####..", "..#..#..", "..####..", ".#....#.", ".######.", "#......#")),
    ("RESERVED_15", _g(*(["........"] * 8))),
]
GLYPHS_ROW3 = [  # station modules, ModuleKind tile order (Core 0 .. Observatory 6)
    ("MOD_CORE", _g("..####..", ".#....#.", "#..##..#", "#.####.#", "#.####.#", "#..##..#", ".#....#.", "..####..")),
    ("MOD_HABITAT", _g("..####..", ".#....#.", "#......#", "#.####.#", "#.#..#.#", "#.####.#", ".#....#.", "..####..")),
    ("MOD_DOCKING", _g("#..##..#", ".#.##.#.", "..####..", "########", "########", "..####..", ".#.##.#.", "#..##..#")),
    ("MOD_SOLAR", _g("########", "#.#.#.##", "########", "#.#.#.##", "########", "...##...", "...##...", ".######.")),
    ("MOD_HYDRO", _g("...#....", "..###...", ".#####..", ".#####..", "..###...", "###.###.", "#.....#.", "#######.")),
    ("MOD_GRAVITY", _g("...#....", "...#....", "...#....", ".#####..", "..###...", "...#....", "########", "########")),
    ("MOD_OBSERVATORY", _g("..####..", ".#....#.", "#..##..#", "#.#..#.#", "#..##..#", ".#....#.", "..####..", "...##...")),
    ("FUEL", _g("...#....", "...#....", "..###...", "..###...", ".#####..", ".##.##..", ".#####..", "..###...")),
    ("O2", _g("..###...", ".#...#..", "#.#...#.", "#.....#.", "#.....#.", ".#...#..", "..###...", "........")),
    ("POWER", _g("....#...", "...#....", "..#.....", ".#####..", "....#...", "...#....", "..#.....", "........")),
    ("RESERVED_10", _g(*(["........"] * 8))),
    ("RESERVED_11", _g(*(["........"] * 8))),
    ("RESERVED_12", _g(*(["........"] * 8))),
    ("RESERVED_13", _g(*(["........"] * 8))),
    ("RESERVED_14", _g(*(["........"] * 8))),
    ("RESERVED_15", _g(*(["........"] * 8))),
]
GLYPH_ROWS = [GLYPHS_ROW0, GLYPHS_ROW1, GLYPHS_ROW2, GLYPHS_ROW3]


def glyph_index():
    """name -> (u, v) on hud_glyphs.png."""
    out = {}
    for v, row in enumerate(GLYPH_ROWS):
        for u, (name, _) in enumerate(row):
            out[name] = (u * 8, v * 8)
    return out


def stamp(px, ox, oy, rows, colour=WHITE):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == "#":
                px[ox + x, oy + y] = colour


def gen_hud_glyphs():
    img = Image.new("RGBA", (128, 32), CLEAR)
    px = img.load()
    for v, row in enumerate(GLYPH_ROWS):
        assert len(row) == 16, "each glyph row holds 16 cells"
        for u, (_, rows) in enumerate(row):
            stamp(px, u * 8, v * 8, rows)
    return img


# --- widgets sheet -----------------------------------------------------------------------------------
# Layout (u, v, w, h) — the 9-slice sheet. 9-slice sprites are 12x12 with 2px corners.
WIDGETS = {}


def _rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = c


def _box(px, x, y, w, h, border, body, sheen=True, open_bottom=False):
    _rect(px, x, y, x + w, y + h, border)
    _rect(px, x + 1, y + 1, x + w - 1, y + h - 1, body)
    if sheen:
        _rect(px, x + 1, y + 1, x + w - 1, y + 2, (255, 255, 255, 0x22))
    if open_bottom:
        _rect(px, x + 1, y + h - 1, x + w - 1, y + h, body)


def gen_widgets(accent=(46, 196, 182, 255)):
    """256x64 sprite sheet. Accent-coloured states use ``accent`` (station teal) as a placeholder: the
    screens tint hover/selected borders with their own accent at draw time; the sheet is a reference
    + resource-pack hook, not the only source of truth (SpaceButton keeps its fills)."""
    img = Image.new("RGBA", (256, 64), CLEAR)
    px = img.load()
    x = 0
    # buttons 12x12 (9-slice: corners 2px, edges stretch)
    for name, (border, body) in (("btn_idle", BTN_IDLE), ("btn_hover", (accent, BTN_HOVER[1])),
                                 ("btn_selected", (accent, BTN_SELECTED[1])), ("btn_disabled", BTN_DISABLED)):
        _box(px, x, 0, 12, 12, border, body)
        WIDGETS[name] = (x, 0, 12, 12)
        x += 12
    # tabs 12x12: selected tab loses its bottom border (joined to the page well)
    for name, (border, body, open_b) in (("tab_idle", (BTN_IDLE[0], BTN_IDLE[1], False)),
                                         ("tab_hover", (accent, BTN_HOVER[1], False)),
                                         ("tab_selected", (accent, GUI_WELL, True))):
        _box(px, x, 0, 12, 12, border, body, open_bottom=open_b)
        WIDGETS[name] = (x, 0, 12, 12)
        x += 12
    # well 12x12 9-slice (INK top/left, SOCKET_HI bottom/right)
    _rect(px, x, 0, x + 12, 12, GUI_WELL)
    _rect(px, x, 0, x + 12, 1, GUI_INK); _rect(px, x, 0, x + 1, 12, GUI_INK)
    _rect(px, x, 11, x + 12, 12, GUI_SOCKET_HI); _rect(px, x + 11, 0, x + 12, 12, GUI_SOCKET_HI)
    WIDGETS["well"] = (x, 0, 12, 12); x += 12
    # socket 18x18
    _rect(px, x, 0, x + 18, 18, GUI_SOCKET)
    _rect(px, x, 0, x + 18, 1, GUI_INK); _rect(px, x, 0, x + 1, 18, GUI_INK)
    _rect(px, x, 17, x + 18, 18, GUI_SOCKET_HI); _rect(px, x + 17, 0, x + 18, 18, GUI_SOCKET_HI)
    WIDGETS["socket"] = (x, 0, 18, 18); x += 18
    # chevrons 28x8 up/down (idle border + glyph), disabled variant below
    for i, (name, rows) in enumerate((("chevron_up", GLYPHS_ROW2[2][1]), ("chevron_down", GLYPHS_ROW2[3][1]))):
        _box(px, x, i * 8 + 0, 28, 8, BTN_IDLE[0], BTN_IDLE[1], sheen=False)
        stamp(px, x + 10, i * 8, rows, GUI_TITLE)
        WIDGETS[name] = (x, i * 8, 28, 8)
    x += 28
    # gauge cells: trough 10x6, lit 10x6 (white, tint), intensity 4x4 off/on
    _rect(px, x, 0, x + 10, 6, GUI_TROUGH); WIDGETS["gauge_trough"] = (x, 0, 10, 6)
    _rect(px, x, 8, x + 10, 14, WHITE); _rect(px, x, 8, x + 10, 9, (255, 255, 255, 0x55))
    WIDGETS["gauge_lit"] = (x, 8, 10, 6); x += 10
    _rect(px, x, 0, x + 4, 4, GUI_TROUGH); WIDGETS["cell_off"] = (x, 0, 4, 4)
    _rect(px, x, 8, x + 4, 12, WHITE); WIDGETS["cell_on"] = (x, 8, 4, 4); x += 4
    # scrollbar track 2x8 + thumb 2x8
    _rect(px, x, 0, x + 2, 8, GUI_TROUGH); WIDGETS["scroll_track"] = (x, 0, 2, 8)
    _rect(px, x, 8, x + 2, 16, GUI_SUBTLE); WIDGETS["scroll_thumb"] = (x, 8, 2, 8); x += 2
    x = (x + 1) // 2 * 2 + 2
    # clock dial faces: 16x16 (r 7) and 12x12 (r 5): INK rim, TROUGH face, daylight half = white 40 %
    for name, size, r in (("dial16", 16, 7), ("dial12", 12, 5)):
        cx = cy = size / 2.0 - 0.5
        for yy in range(size):
            for xx in range(size):
                d = ((xx - cx) ** 2 + (yy - cy) ** 2) ** 0.5
                if d <= r - 1.0:
                    px[x + xx, yy] = (255, 255, 255, 0x66) if yy < cy else GUI_TROUGH
                elif d <= r:
                    px[x + xx, yy] = GUI_INK
        WIDGETS[name] = (x, 0, size, size)
        x += size
    # row 2 (v 20): progress arrow 22x16 trough + fill, combustion flame 14x14 off/on, fluid frame 12x12
    y0 = 20
    x = 0
    arrow = ["......#........", "......##.......", "......###......", "#########......", "##########.....",
             "###########....", "##########.....", "#########......", "......###......", "......##.......",
             "......#........"]
    for i, colour in enumerate((GUI_TROUGH, WHITE)):
        for yy, row in enumerate(arrow):
            for xx, c in enumerate(row):
                if c == "#":
                    px[x + xx + 2, y0 + yy + 2] = colour
        WIDGETS["arrow_trough" if i == 0 else "arrow_fill"] = (x, y0, 20, 16)
        x += 20
    flame = ["....#....", "....#....", "...##....", "...###...", "..####...", "..#####..", ".######..",
             ".#######.", "#########", "#########", ".#######.", "..#####..", "...###...", "....#...."]
    for i, colour in enumerate((GUI_TROUGH, WHITE)):
        for yy, row in enumerate(flame):
            for xx, c in enumerate(row):
                if c == "#":
                    px[x + xx + 2, y0 + yy] = colour
        WIDGETS["flame_off" if i == 0 else "flame_on"] = (x, y0, 14, 14)
        x += 14
    # radio / check pips 8x8
    for name, rows in (("pip_done", GLYPHS_ROW2[4][1]), ("pip_star", GLYPHS_ROW2[5][1]),
                       ("pip_filled", GLYPHS_ROW2[6][1]), ("pip_hollow", GLYPHS_ROW2[7][1])):
        stamp(px, x, y0, rows, WHITE)
        WIDGETS[name] = (x, y0, 8, 8)
        x += 8
    # selector arrow buttons 12x12 (idle / hover / disabled) with the arrow glyph centred
    for side, rows in (("l", GLYPHS_ROW2[0][1]), ("r", GLYPHS_ROW2[1][1])):
        for state, (border, body, col) in (("idle", (BTN_IDLE[0], BTN_IDLE[1], (207, 231, 255, 255))),
                                           ("hover", (accent, BTN_HOVER[1], WHITE)),
                                           ("disabled", (BTN_DISABLED[0], BTN_DISABLED[1], (106, 114, 128, 255)))):
            _box(px, x, y0, 12, 12, border, body)
            stamp(px, x + 2, y0 + 2, rows, col)
            WIDGETS["arrow_%s_%s" % (side, state)] = (x, y0, 12, 12)
            x += 12
    # row 3 (v 40): rename edit box 12x12 9-slice (INK border, TROUGH body), notice banner 12x12
    y0 = 40
    x = 0
    _box(px, x, y0, 12, 12, GUI_INK, GUI_TROUGH, sheen=False); WIDGETS["editbox"] = (x, y0, 12, 12); x += 12
    _box(px, x, y0, 12, 12, (240, 168, 60, 255), (20, 12, 4, 240), sheen=False)
    WIDGETS["notice"] = (x, y0, 12, 12); x += 12
    _box(px, x, y0, 12, 12, (240, 96, 96, 255), (20, 12, 4, 240), sheen=False)
    WIDGETS["notice_bad"] = (x, y0, 12, 12); x += 12
    # card borders 12x12: available, selected (2px + sheen), locked dotted, gate dotted
    for name, border in (("card_idle", BTN_IDLE[0]), ("card_selected", accent)):
        if name == "card_selected":
            _rect(px, x, y0, x + 12, y0 + 12, border)
            _rect(px, x + 2, y0 + 2, x + 10, y0 + 10, (255, 255, 255, 0x55))
            _rect(px, x + 3, y0 + 3, x + 9, y0 + 9, GUI_HULL[0])
        else:
            _box(px, x, y0, 12, 12, border, GUI_HULL[0], sheen=False)
        WIDGETS[name] = (x, y0, 12, 12); x += 12
    for name, border in (("card_locked", BTN_DISABLED[0]), ("card_gate", (88, 45, 112, 255))):
        _rect(px, x + 1, y0 + 1, x + 11, y0 + 11, GUI_HULL[0])
        for i in range(0, 12, 2):
            px[x + i, y0] = border; px[x + i, y0 + 11] = border; px[x, y0 + i] = border; px[x + 11, y0 + i] = border
        WIDGETS[name] = (x, y0, 12, 12); x += 12
    return img


# --- I/O ---------------------------------------------------------------------------------------------
def save(img, path, force):
    if os.path.exists(path) and not force:
        print("  keep   %s (exists; --force to overwrite)" % os.path.relpath(path))
        return False
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=False)
    print("  wrote  %s" % os.path.relpath(path))
    return True


def check(gui_dir, only):
    """Structural diff: compare deliberately painted pixels of each table panel against the repo PNG."""
    ok = True
    for spec in GUI_PANELS:
        if only and spec["name"] not in only:
            continue
        path = os.path.join(gui_dir, spec["name"] + ".png")
        structural = set()
        img = gen_gui_hull(**{k: v for k, v in spec.items()}, structural=structural)
        if not os.path.exists(path):
            print("  MISSING  %-22s (new file)" % spec["name"])
            ok = False
            continue
        ref = Image.open(path).convert("RGBA")
        if ref.size != (256, 256):
            print("  SIZE     %-22s %s" % (spec["name"], ref.size)); ok = False; continue
        rp, gp = ref.load(), img.load()
        w, h = spec["w"], spec["h"]
        hull = set(GUI_HULL)
        ref_mask = {(x, y) for y in range(h) for x in range(w) if rp[x, y] not in hull}
        geom = ref_mask ^ structural
        colour = [p for p in structural & ref_mask if rp[p] != gp[p]]
        exact = list(img.get_flattened_data()) == list(ref.get_flattened_data()) if hasattr(img, "get_flattened_data") \
            else list(img.getdata()) == list(ref.getdata())
        if exact:
            print("  IDENT    %-22s byte-identical" % spec["name"])
        elif not geom:
            print("  TONE     %-22s geometry identical; %d painted px differ in colour only (accent/well tone)"
                  % (spec["name"], len(colour)))
        else:
            print("  GEOM     %-22s %d px of layout differ (e.g. %s); %d colour-only"
                  % (spec["name"], len(geom), sorted(geom)[:3], len(colour)))
            ok = False
    return ok


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", default=None, help="output directory (default: the repo GUI dir)")
    ap.add_argument("--force", action="store_true", help="overwrite existing PNGs")
    ap.add_argument("--check", action="store_true", help="structural diff against the repo GUI dir")
    ap.add_argument("--only", default="", help="comma-separated panel names")
    ap.add_argument("--no-sheets", action="store_true", help="skip hud_glyphs.png / widgets.png")
    ap.add_argument("--gui-dir", default=None, help="repo GUI dir to --check against (default: this repo's)")
    args = ap.parse_args(argv)
    only = set(filter(None, args.only.split(",")))
    gui_dir = args.out or DEFAULT_GUI_DIR
    if args.check:
        return 0 if check(args.gui_dir or args.out or DEFAULT_GUI_DIR, only) else 1
    print("gen_gui_hull: -> %s" % gui_dir)
    for spec in GUI_PANELS:
        if only and spec["name"] not in only:
            continue
        save(gen_gui_hull(**spec), os.path.join(gui_dir, spec["name"] + ".png"), args.force)
    if not args.no_sheets:
        save(gen_hud_glyphs(), os.path.join(gui_dir, "hud_glyphs.png"), args.force)
        save(gen_widgets(), os.path.join(gui_dir, "widgets.png"), args.force)
    return 0


if __name__ == "__main__":
    sys.exit(main())
