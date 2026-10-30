#!/usr/bin/env python3
"""
Entity skins for the Nerospace 2.0.0 planets rework.

Paints the 64x64 box-UV skins for the planet creatures and the three new villager cultures
*per cube face* (not per texOffs region like the 1.6.0 ``gen_textures.py`` creature painters),
so every face of every part gets its own read: fur direction on a flank, a face on the front
of a head, a hazard stripe on a rim.  Each skin is painted against the Java ``LayerDefinition``
of the model the renderer actually uses today (see ``MODELS`` below) — the placeholder models
the planet creatures shipped with — plus, where a dedicated model exists, extra UV
islands in the unused parts of the sheet so the SAME png serves the new geometry.

Deterministic: each skin seeds its own ``random.Random`` from the md5 of its name.  Pillow only.

Usage
-----
    python3 gen_entity_skins.py <entity-texture-dir>        # writes every skin + glow layer
    python3 gen_entity_skins.py --preview <dir>             # 4x contact sheet per skin
    python3 gen_entity_skins.py --check <entity-texture-dir> # re-render to a temp dir; exit 1 on drift

Outputs (all under textures/entity/):
    alien_villager_cindara.png  (+ _glow)   Ember-kin   (REPLACES the 1.6.0 tinted placeholder)
    alien_villager_glacira.png  (+ _glow)   Frostbound  (REPLACES the 1.6.0 tinted placeholder)
    alien_villager_duskborn.png (+ _glow)   Duskborn
    moon_hopper.png                         Luna passive (GreenlingModel geometry)
    dust_strider.png                        Mars grazer  (MeadowLoperModel geometry)
    dusk_moth.png (+ _glow)                 Nyxara ambient (GreenlingModel geometry + wing islands)
    salvage_drone.png (+ _glow)             Mars merchant (QuartzCrawlerModel geometry + hover islands)
    dust_devil.png                          Mars weather entity: 4-frame translucent vortex strip
"""
import hashlib
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

ES = 64
CLEAR = (0, 0, 0, 0)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))),
            max(0, min(255, int(c[2] * f))), 255)


def seed_of(name):
    return int(hashlib.md5(name.encode()).hexdigest(), 16) & 0xffffffff


# ------------------------------------------------------------------ box-UV geometry

def faces(u, v, w, h, d):
    """Minecraft box UV: {face: (x, y, width, height)} for a cube of size w x h x d at texOffs(u, v).
    Face names follow the texture layout, not world direction: 'front' is the -Z face."""
    w, h, d = int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d))
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


class Sheet:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (ES, ES), CLEAR)
        self.px = self.img.load()
        self.rng = random.Random(seed)
        self.glow = Image.new("RGBA", (ES, ES), CLEAR)
        self.gpx = self.glow.load()

    # -- primitives ---------------------------------------------------------------------
    def put(self, x, y, c, glow=False):
        if 0 <= x < ES and 0 <= y < ES:
            self.px[x, y] = c
            if glow:
                self.gpx[x, y] = c

    def rect(self, r, c, glow=False):
        x, y, w, h = r
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.put(xx, yy, c, glow)

    def noise(self, r, palette, weights=None, *, grain=0.5, stripes=None):
        """Fill rect with a speckle of `palette` (first entry dominant). `stripes` = (period,
        colour, axis) draws a faint directional grain (fur / plating seams)."""
        x, y, w, h = r
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                c = palette[0]
                if self.rng.random() < grain:
                    c = self.rng.choice(palette)
                if stripes:
                    period, sc, axis = stripes
                    k = (yy - y) if axis == "y" else (xx - x)
                    if k % period == 0:
                        c = mix(c, sc, 0.6)
                self.put(xx, yy, c)

    def outline(self, r, c):
        x, y, w, h = r
        for xx in range(x, x + w):
            self.put(xx, y, c)
            self.put(xx, y + h - 1, c)
        for yy in range(y, y + h):
            self.put(x, yy, c)
            self.put(x + w - 1, yy, c)

    def hline(self, r, row, c, glow=False, x0=0, x1=None):
        x, y, w, h = r
        x1 = w if x1 is None else x1
        for xx in range(x + x0, x + x1):
            self.put(xx, y + row, c, glow)

    def vline(self, r, col, c, glow=False, y0=0, y1=None):
        x, y, w, h = r
        y1 = h if y1 is None else y1
        for yy in range(y + y0, y + y1):
            self.put(x + col, yy, c, glow)

    def dot(self, r, dx, dy, c, glow=False):
        x, y, w, h = r
        self.put(x + dx, y + dy, c, glow)

    def shade_edges(self, r, light, dark, *, top=True, bottom=True):
        """One-pixel lit top / shadowed bottom on a face (cheap volume)."""
        x, y, w, h = r
        if top:
            for xx in range(x, x + w):
                self.put(xx, y, mix(self.px[xx, y], light, 0.35))
        if bottom:
            for xx in range(x, x + w):
                self.put(xx, y + h - 1, mix(self.px[xx, y + h - 1], dark, 0.45))

    def cube(self, u, v, w, h, d, palette, *, grain=0.5, stripes=None, light=None, dark=None, skip=()):
        f = faces(u, v, w, h, d)
        for name, r in f.items():
            if name in skip:
                continue
            self.noise(r, palette, grain=grain, stripes=stripes)
            if light is not None and name not in ("top", "bottom"):
                self.shade_edges(r, light, dark or shade(palette[0], 0.6))
            if name == "top" and light is not None:
                self.rect(r, mix(palette[0], light, 0.25))
                self.noise(r, [mix(palette[0], light, 0.25)] + palette[1:], grain=grain * 0.6)
            if name == "bottom" and dark is not None:
                self.noise(r, [mix(palette[0], dark, 0.4)] + palette[1:], grain=grain * 0.6)
        return f

    def eyes(self, front, *, y, inset, size=1, eye=None, socket=None, glint=None, glow=True):
        """Pair of eyes on a front face: `inset` from each side, at row `y`."""
        x, fy, w, h = front
        for sgn, ex in ((-1, x + inset), (1, x + w - inset - size)):
            for dx in range(size):
                for dy in range(size):
                    self.put(ex + dx, fy + y + dy, socket or eye)
            self.put(ex, fy + y, eye, glow)
            if size > 1:
                self.put(ex + size - 1, fy + y, eye, glow)
                self.put(ex, fy + y + size - 1, eye, glow)
            if glint is not None:
                self.put(ex + (size - 1 if sgn < 0 else 0), fy + y, glint, glow)

    def save(self, path, glow_path=None):
        self.img.save(path)
        if glow_path:
            self.glow.save(glow_path)


# ================================================================== palettes (concept sheets)

# Cindara (sheet 03): ash #3B3237 / #2B2328 / #4E434A, ember #D94E1F #FF5A1F #FF8A4A #FFB347, obsidian #1A1420
CIND = dict(ash=hexc("#3B3237"), ash_d=hexc("#2B2328"), ash_l=hexc("#4E434A"), ember=hexc("#FF5A1F"),
            ember_d=hexc("#D94E1F"), ember_l=hexc("#FF8A4A"), gold=hexc("#FFB347"), obs=hexc("#1A1420"),
            sulfur=hexc("#E8C547"))
# Glacira (sheet 04): ice #A9E3FF #6CC2EF #F2FBFF, permafrost #2D6F9E #1F4F73, salt #E8ECEF #C9D6DD, methane #6B4AA3
GLAC = dict(fur=hexc("#E8ECEF"), fur_d=hexc("#C9D6DD"), fur_l=hexc("#F2FBFF"), ice=hexc("#A9E3FF"),
            ice_d=hexc("#6CC2EF"), deep=hexc("#2D6F9E"), deep_d=hexc("#1F4F73"), skin=hexc("#8AC8EC"),
            violet=hexc("#6B4AA3"))
# Nyxara (sheet 06): nyx #2B2440 / #0D0A14 / #5B2D8E, crystal #9B5DE5 #C77DFF, voidglass #4DD0E1, dawn #FFB86B
NYX = dict(skin=hexc("#2B2440"), skin_d=hexc("#0D0A14"), skin_l=hexc("#5B2D8E"), mark=hexc("#C77DFF"),
           mark_d=hexc("#9B5DE5"), glass=hexc("#4DD0E1"), dawn=hexc("#FFB86B"), pale=hexc("#E6D2FF"))
# Luna (sheet 01): regolith #9A9A94 #7D7D78 #C9C9C2, anorthosite #E0E0D8, basalt #4A4B50 #2F2F36
LUNA = dict(fur=hexc("#C9C9C2"), fur_l=hexc("#E0E0D8"), fur_d=hexc("#9A9A94"), shade=hexc("#7D7D78"),
            dark=hexc("#4A4B50"), eye=hexc("#2F2F36"), nose=hexc("#B3A6A6"))
# Mars (sheet 02): rust #B5532A #9A4322 #C8683D, hematite #6B2F1E #4A2016, sand #D9A066, salt #E8ECEF
MARS = dict(coat=hexc("#B5532A"), coat_d=hexc("#9A4322"), coat_l=hexc("#C8683D"), stripe=hexc("#6B2F1E"),
            stripe_d=hexc("#4A2016"), belly=hexc("#D9A066"), salt=hexc("#E8ECEF"), dust=hexc("#7A4A2E"))
# Station steel (sheet 07) for the drone: #8D949B #5A6168, teal #2EC4B6, hazard #FF8C42 / #1A1420, lens #4DD0E1
STEEL = dict(hull=hexc("#8D949B"), seam=hexc("#5A6168"), hull_l=hexc("#A7AEB8"), hazard=hexc("#FF8C42"),
             black=hexc("#1A1420"), lens=hexc("#4DD0E1"), beacon=hexc("#FF5A1F"), rust=hexc("#6B2F1E"),
             rust_l=hexc("#8C3C2A"), teal=hexc("#2EC4B6"))


# ================================================================== model geometry (from Java)

# AlienVillagerModel: head (0,28) 8x8x8 | body (0,0) 8x12x4 | crystal_* (44,0) 2x5x2 | arm_* (44,8) 2x11x4 | leg_* (44,24) 3x10x4
VILLAGER = dict(head=(0, 28, 8, 8, 8), body=(0, 0, 8, 12, 4), crystal=(44, 0, 2, 5, 2), arm=(44, 8, 2, 11, 4), leg=(44, 24, 3, 10, 4))
# GreenlingModel: body (0,0) 7x6x6 | belly (0,0) 6x3x5 | head (0,28) 8x8x8 | cheeks (0,28) 9x3x7 | frond_mid (44,0) 1x6x1 |
#                 leg_* (44,0) 2.5x3x3 | frond_l/r (44,0) 1x5x1 | arm_* (44,0) 2x5x2      (all small parts share (44,0))
GREENLING = dict(body=(0, 0, 7, 6, 6), belly=(0, 0, 6, 3, 5), head=(0, 28, 8, 8, 8), cheeks=(0, 28, 9, 3, 7),
                 small=(44, 0, 3, 6, 3))
# MeadowLoperModel: body (0,0) 10x9x16 | head (0,28) 6x6x6 | muzzle (0,28) 4x3x3 | horn_* (44,0) 1x2x1 | tail (44,0) 1x7x1 | leg_* (44,0) 3x7x3
LOPER = dict(body=(0, 0, 10, 9, 16), head=(0, 28, 6, 6, 6), muzzle=(0, 28, 4, 3, 3), small=(44, 0, 3, 7, 3))
# QuartzCrawlerModel: dome (0,0) 8x3x8 | shell (0,0) 10x4x10 | rim (0,0) 11x2x11 | head (0,28) 6x4x4 | crystal_* (44,0) 2x5/6/4x2 | leg_* (44,0) 2x10x2
CRAWLER = dict(dome=(0, 0, 8, 3, 8), shell=(0, 0, 10, 4, 10), rim=(0, 0, 11, 2, 11), head=(0, 28, 6, 4, 4), small=(44, 0, 2, 10, 2))


# ================================================================== villagers

def villager_base(sh, skin, skin_d, robe, robe_d, robe_l):
    """Shared humanoid read: skin head, robed torso, sleeves, boots."""
    head = sh.cube(*VILLAGER["head"], [skin, skin_d, mix(skin, skin_d, 0.5)], grain=0.35, light=mix(skin, (255, 255, 255, 255), 0.4), dark=skin_d)
    body = sh.cube(*VILLAGER["body"], [robe, robe_d, robe_l], grain=0.4, stripes=(4, robe_d, "y"), light=robe_l, dark=robe_d)
    arm = sh.cube(*VILLAGER["arm"], [robe, robe_d, robe_l], grain=0.4, light=robe_l, dark=robe_d)
    leg = sh.cube(*VILLAGER["leg"], [robe_d, robe, robe_d], grain=0.3, light=robe, dark=shade(robe_d, 0.7))
    # hands: last two rows of the arm cube are skin
    for name in ("front", "back", "left", "right"):
        r = arm[name]
        sh.hline(r, r[3] - 1, skin)
        sh.hline(r, r[3] - 2, skin_d)
    sh.rect(arm["bottom"], skin)
    return head, body, arm, leg


def ember_kin():
    """Cindara Ember-kin: stocky (renderer scales 1.15 x 0.88), soot-marked ash skin, a
    charcoal robe whose hem and seams glow like cooling lava, ember-lit eyes, and obsidian
    shoulder spikes with hot tips.  Glow layer: eyes, robe cracks, spike tips."""
    sh = Sheet(seed_of("alien_villager_cindara"))
    P = CIND
    head, body, arm, leg = villager_base(sh, P["ash_l"], P["ash_d"], P["ash"], P["ash_d"], P["ash_l"])
    # face: heavy brow, soot smudges under the eyes, ember eyes
    f = head["front"]
    sh.hline(f, 2, P["ash_d"], x0=1, x1=7)
    sh.eyes(f, y=3, inset=1, size=2, eye=P["ember_l"], socket=P["obs"], glint=P["gold"])
    for (dx, dy) in ((1, 5), (2, 6), (5, 6), (6, 5), (3, 7)):
        sh.dot(f, dx, dy, P["ash_d"])
    sh.hline(f, 6, P["obs"], x0=3, x1=5)  # mouth
    # soot on the crown and cheeks
    for name in ("top", "left", "right"):
        r = head[name]
        for _ in range(5):
            sh.dot(r, sh.rng.randrange(r[2]), sh.rng.randrange(r[3]), P["ash_d"])
    # robe: lava hem (two rows) + crack seams running down the front/back
    for name in ("front", "back", "left", "right"):
        r = body[name]
        sh.hline(r, r[3] - 1, P["ember_d"], glow=True)
        sh.hline(r, r[3] - 2, P["ember"], glow=True)
        sh.hline(r, r[3] - 3, mix(P["ash_d"], P["ember_d"], 0.4))
    for name, cols in (("front", (2, 5)), ("back", (3, 6))):
        r = body[name]
        for c in cols:
            y = 1
            x = c
            while y < r[3] - 3:
                sh.dot(r, x, y, P["ember"] if y % 3 else P["ember_l"], glow=True)
                y += 1
                if sh.rng.random() < 0.35:
                    x = max(0, min(r[2] - 1, x + sh.rng.choice((-1, 1))))
    # belt in sulfur
    for name in ("front", "back", "left", "right"):
        sh.hline(body[name], 7, P["sulfur"])
        sh.hline(body[name], 8, shade(P["sulfur"], 0.7))
    # sleeve cuffs glow
    for name in ("front", "back", "left", "right"):
        sh.hline(arm[name], arm[name][3] - 3, P["ember_d"], glow=True)
    # boots: obsidian with an ember sole
    for name in ("front", "back", "left", "right"):
        r = leg[name]
        sh.rect((r[0], r[1] + r[3] - 3, r[2], 3), P["obs"])
        sh.hline(r, r[3] - 1, P["ember_d"], glow=True)
    # shoulder "crystals" -> obsidian spikes, hot tip
    cr = sh.cube(*VILLAGER["crystal"], [P["obs"], shade(P["obs"], 1.4)], grain=0.3)
    for name in ("front", "back", "left", "right"):
        sh.hline(cr[name], 0, P["ember"], glow=True)
    sh.rect(cr["top"], P["gold"], glow=True)
    return sh


def frostbound():
    """Glacira Frostbound: fur-wrapped (renderer scales 1.08 x 1.04), a tall fur hood that
    frames a pale-blue face, strapped fur wraps on the torso and arms, deep-ice boots, ice
    spike shoulders.  Glow layer: eyes and the spike tips (faint cold light)."""
    sh = Sheet(seed_of("alien_villager_glacira"))
    P = GLAC
    head, body, arm, leg = villager_base(sh, P["skin"], P["ice_d"], P["fur"], P["fur_d"], P["fur_l"])
    # hood: every head face except the front opening is fur
    for name in ("top", "left", "right", "back", "bottom"):
        sh.noise(head[name], [P["fur"], P["fur_d"], P["fur_l"]], grain=0.6, stripes=(3, P["fur_d"], "y"))
    f = head["front"]
    sh.noise(f, [P["fur"], P["fur_d"], P["fur_l"]], grain=0.6)
    # face opening: a 4x5 window of skin inside the fur
    sh.rect((f[0] + 2, f[1] + 2, 4, 5), P["skin"])
    sh.noise((f[0] + 2, f[1] + 2, 4, 5), [P["skin"], P["ice_d"]], grain=0.3)
    sh.eyes(f, y=3, inset=2, size=1, eye=P["fur_l"], socket=P["deep_d"], glow=True)
    sh.dot(f, 3, 6, P["deep"]); sh.dot(f, 4, 6, P["deep"])  # mouth
    # fur ruff: the top two body rows are brighter fur
    for name in ("front", "back", "left", "right"):
        r = body[name]
        sh.rect((r[0], r[1], r[2], 2), P["fur_l"])
        sh.noise((r[0], r[1], r[2], 2), [P["fur_l"], P["fur"]], grain=0.4)
        # leather straps across the wraps
        for row in (4, 8):
            sh.hline(r, row, P["deep"])
            sh.hline(r, row + 1, P["deep_d"])
        # fur hem
        sh.hline(r, r[3] - 1, P["fur_d"])
    # strap buckles (methane-violet beads) on the front
    sh.dot(body["front"], 3, 4, P["violet"]); sh.dot(body["front"], 4, 8, P["violet"])
    # arm wraps: strap mid-arm
    for name in ("front", "back", "left", "right"):
        sh.hline(arm[name], 5, P["deep"])
        sh.hline(arm[name], 6, P["deep_d"])
    # boots: deep ice
    for name in ("front", "back", "left", "right"):
        r = leg[name]
        sh.rect((r[0], r[1] + r[3] - 4, r[2], 4), P["deep"])
        sh.noise((r[0], r[1] + r[3] - 4, r[2], 4), [P["deep"], P["deep_d"], P["ice_d"]], grain=0.4)
        sh.hline(r, r[3] - 4, P["ice"])
    # legs above boots: fur wraps
    for name in ("front", "back", "left", "right"):
        r = leg[name]
        sh.noise((r[0], r[1], r[2], r[3] - 4), [P["fur"], P["fur_d"]], grain=0.5, stripes=(2, P["fur_d"], "y"))
    # shoulder ice spikes
    cr = sh.cube(*VILLAGER["crystal"], [P["ice"], P["ice_d"], P["fur_l"]], grain=0.5)
    for name in ("front", "back", "left", "right"):
        sh.hline(cr[name], 0, P["fur_l"], glow=True)
    sh.rect(cr["top"], P["fur_l"], glow=True)
    return sh


def duskborn():
    """Nyxara Duskborn: tall and slender (renderer scales 0.9 x 1.14), indigo skin with
    bioluminescent violet markings (#C77DFF) that run from the brow down the arms and torso,
    a near-black robe with violet seams, voidglass shoulder shards.  Glow layer: every marking,
    the eyes and the shard glints — the 'slow idle glow' is the eyes layer at full bright."""
    sh = Sheet(seed_of("alien_villager_duskborn"))
    P = NYX
    head, body, arm, leg = villager_base(sh, P["skin"], P["skin_d"], P["skin_d"], hexc("#070510"), P["skin"])
    f = head["front"]
    # brow marking: a chevron over the eyes
    for (dx, dy) in ((1, 2), (2, 1), (3, 0), (4, 0), (5, 1), (6, 2)):
        sh.dot(f, dx, dy, P["mark"], glow=True)
    sh.eyes(f, y=3, inset=1, size=2, eye=P["mark"], socket=P["skin_d"], glint=P["pale"])
    # cheek dots and chin line
    for (dx, dy) in ((0, 5), (7, 5), (3, 7), (4, 7)):
        sh.dot(f, dx, dy, P["mark_d"], glow=True)
    # markings over the crown and down the back of the head
    for name, pts in (("top", ((3, 2), (4, 2), (3, 5), (4, 5))), ("back", ((3, 1), (4, 1), (3, 4), (4, 4), (3, 7), (4, 7)))):
        for (dx, dy) in pts:
            sh.dot(head[name], dx, dy, P["mark"], glow=True)
    for name in ("left", "right"):
        for dy in (2, 4, 6):
            sh.dot(head[name], 3, dy, P["mark_d"], glow=True)
    # robe: violet seams down the centre and along the shoulders, voidglass clasp
    for name in ("front", "back"):
        r = body[name]
        sh.vline(r, 3, P["skin_l"]); sh.vline(r, 4, P["skin_l"])
        sh.hline(r, 0, P["mark_d"], glow=True)          # glowing shoulder seam
        sh.hline(r, r[3] - 1, P["mark_d"], glow=True)  # glowing hem
        # a chain of marking beads down the centre seam (the robe is open at the chest)
        for dy in range(2, r[3] - 2, 3):
            sh.dot(r, 3, dy, P["mark"], glow=True); sh.dot(r, 4, dy, P["mark"], glow=True)
    for name in ("left", "right"):
        sh.hline(body[name], 0, P["skin_l"])
        sh.hline(body[name], body[name][3] - 1, P["mark_d"], glow=True)
    sh.dot(body["front"], 3, 2, P["glass"], glow=True); sh.dot(body["front"], 4, 2, P["glass"], glow=True)
    # arms: bare skin with a marking line running the length of the outer face
    for name in ("front", "back", "left", "right"):
        r = arm[name]
        sh.noise(r, [P["skin"], P["skin_d"]], grain=0.35)
        sh.shade_edges(r, P["skin_l"], P["skin_d"])
    for name in ("left", "right"):
        r = arm[name]
        for dy in range(0, r[3], 2):
            sh.dot(r, r[2] // 2, dy, P["mark"] if dy % 4 == 0 else P["mark_d"], glow=True)
    # legs: robe skirt tone with a marking at the ankle
    for name in ("front", "back", "left", "right"):
        r = leg[name]
        sh.noise(r, [hexc("#070510"), P["skin_d"]], grain=0.3)
        sh.hline(r, r[3] - 2, P["mark_d"], glow=True)
    # shoulder shards: voidglass
    cr = sh.cube(*VILLAGER["crystal"], [P["mark_d"], P["skin_l"], P["mark"]], grain=0.5)
    for name in ("front", "back", "left", "right"):
        sh.dot(cr[name], 0, 0, P["glass"], glow=True)
        sh.dot(cr[name], 1, 2, P["pale"], glow=True)
    sh.rect(cr["top"], P["glass"], glow=True)
    return sh


# ================================================================== planet creatures

def moon_hopper():
    """Luna Moon Hopper on the Greenling geometry (renderer scale 0.9): a round, pale
    regolith-grey hopper with three ear-fronds (mid + two splayed), big dark eyes with a
    glint, a pale belly and dusty paws.  No glow layer (renderer registers none)."""
    sh = Sheet(seed_of("moon_hopper"))
    P = LUNA
    fur = [P["fur"], P["fur_d"], P["fur_l"]]
    body = sh.cube(*GREENLING["body"], fur, grain=0.5, stripes=(3, P["fur_d"], "x"), light=P["fur_l"], dark=P["shade"])
    # belly samples the top-left of the body region: paint that sub-area paler
    belly = faces(*GREENLING["belly"])
    for name in ("front", "bottom"):
        sh.noise(belly[name], [P["fur_l"], P["fur"]], grain=0.3)
    head = sh.cube(*GREENLING["head"], fur, grain=0.5, light=P["fur_l"], dark=P["shade"])
    f = head["front"]
    sh.eyes(f, y=2, inset=1, size=2, eye=P["eye"], socket=P["dark"], glint=P["fur_l"], glow=False)
    sh.dot(f, 3, 5, P["nose"]); sh.dot(f, 4, 5, P["nose"])
    sh.dot(f, 3, 6, P["shade"]); sh.dot(f, 4, 6, P["shade"])
    # cheeks share the head region (0,28): the cheek band rows 28..31 (its front = rows 35..37 of the
    # head's front face area) — brighten the lower cheeks
    for dx in (0, 1, 6, 7):
        sh.dot(f, dx, 5, P["fur_l"]); sh.dot(f, dx, 6, P["fur_l"])
    # dust on the crown
    for _ in range(4):
        sh.dot(head["top"], sh.rng.randrange(8), sh.rng.randrange(8), P["shade"])
    # small parts region (44,0) 3x6x3 max: ears/legs/arms all sample here — fur with darker tips
    sm = sh.cube(*GREENLING["small"], [P["fur"], P["fur_d"]], grain=0.5)
    for name in ("front", "back", "left", "right"):
        sh.hline(sm[name], 0, P["shade"])     # ear tip / toe tone
        sh.hline(sm[name], 1, P["fur_d"])
    sh.rect(sm["top"], P["shade"])
    # ---- UV islands for MoonHopperModel: hind legs 3x5x6 at (0,46) / (18,46),
    #      a 2x3x2 tail at (36,46)
    for u in (0, 18):
        sh.cube(u, 46, 3, 5, 6, fur, grain=0.5, light=P["fur_l"], dark=P["shade"])
    tail = sh.cube(36, 46, 2, 3, 2, [P["fur_l"], P["fur"]], grain=0.4)
    sh.rect(tail["top"], P["fur_l"])
    return sh


def dust_strider():
    """Mars Dust Strider on the Meadow Loper geometry (renderer scale 0.85 x 1.2 x 0.85): a
    long-legged rust-red grazer with a dust-tan belly, three dark dorsal stripes, a pale
    salt-dust muzzle and dark eyes under a brow ridge; horns, tail and legs in the shared
    (44,0) region.  No glow layer."""
    sh = Sheet(seed_of("dust_strider"))
    P = MARS
    coat = [P["coat"], P["coat_d"], P["coat_l"]]
    body = sh.cube(*LOPER["body"], coat, grain=0.5, stripes=(4, P["coat_d"], "x"), light=P["coat_l"], dark=P["stripe"])
    # belly (bottom face) and the lower third of the flanks are tan
    sh.noise(body["bottom"], [P["belly"], mix(P["belly"], P["coat"], 0.4)], grain=0.4)
    for name in ("left", "right"):
        r = body[name]
        sh.noise((r[0], r[1] + r[3] - 3, r[2], 3), [P["belly"], mix(P["belly"], P["coat"], 0.5)], grain=0.5)
    # dorsal stripes: three dark bands across the top and the upper flank
    top = body["top"]
    for k in (3, 8, 13):
        for dx in range(top[2]):
            if (dx + k) % 2:
                sh.dot(top, dx, k, P["stripe"])
                sh.dot(top, dx, k + 1, P["stripe_d"])
    for name in ("left", "right"):
        r = body[name]
        for k in (3, 8, 13):
            sh.dot(r, k, 0, P["stripe"]); sh.dot(r, k, 1, P["stripe"]); sh.dot(r, k + 1, 1, P["stripe_d"]); sh.dot(r, k, 2, P["stripe_d"])
    # rump dust
    for _ in range(6):
        sh.dot(body["back"], sh.rng.randrange(10), sh.rng.randrange(9), P["dust"])
    head = sh.cube(*LOPER["head"], coat, grain=0.5, light=P["coat_l"], dark=P["stripe"])
    f = head["front"]
    sh.hline(f, 1, P["stripe"], x0=1, x1=5)  # brow ridge
    sh.eyes(f, y=2, inset=1, size=1, eye=P["stripe_d"], socket=P["stripe_d"], glint=P["belly"], glow=False)
    # the muzzle samples the same region at a smaller size: its front = head front rows 3..5, cols 3..6
    mz = faces(*LOPER["muzzle"])
    sh.noise(mz["front"], [P["salt"], P["belly"]], grain=0.5)
    sh.dot(mz["front"], 0, 1, P["stripe_d"]); sh.dot(mz["front"], 3, 1, P["stripe_d"])  # nostrils
    for name in ("left", "right", "bottom", "top"):
        sh.noise(mz[name], [P["belly"], P["salt"]], grain=0.5)
    # small parts (44,0): horns/tail/legs share it (and the body's back face overlaps x44..52, y4..13):
    # paint it coat-coloured with dark tips so horns read as horn, legs as leg, back as back
    sm = sh.cube(*LOPER["small"], coat, grain=0.4)
    for name in ("front", "back", "left", "right"):
        sh.hline(sm[name], 0, P["stripe_d"])
        sh.hline(sm[name], sm[name][3] - 1, P["dust"])
        sh.hline(sm[name], sm[name][3] - 2, P["dust"])
    sh.rect(sm["top"], P["stripe_d"])
    sh.rect(sm["bottom"], P["dust"])
    return sh


def dusk_moth():
    """Nyxara Dusk Moth on the Greenling geometry (renderer scale 0.7 x 0.5 x 0.7): a fuzzy
    indigo body with violet lamp-spots along the abdomen, a head that is mostly two large
    violet compound eyes, feathered antennae (the frond parts) and wing stubs (the arm parts)
    carrying violet eye-spots.  Glow layer: eyes, lamp-spots, antenna tips, eye-spots.
    Wing islands for DuskMothModel are painted at (0,46)/(28,46)."""
    sh = Sheet(seed_of("dusk_moth"))
    P = NYX
    fuzz = [P["skin"], P["skin_d"], mix(P["skin"], P["skin_l"], 0.4)]
    body = sh.cube(*GREENLING["body"], fuzz, grain=0.6, stripes=(2, P["skin_d"], "y"), light=P["skin_l"], dark=P["skin_d"])
    # abdomen lamps on the back / sides / bottom
    for name in ("back", "left", "right", "bottom", "top"):
        r = body[name]
        for dy in range(1, r[3], 2):
            sh.dot(r, r[2] // 2, dy, P["mark"] if dy % 4 == 1 else P["mark_d"], glow=True)
    belly = faces(*GREENLING["belly"])
    sh.noise(belly["front"], fuzz, grain=0.6)
    for dy in (0, 2):
        sh.dot(belly["front"], 3, dy, P["mark_d"], glow=True)
    head = sh.cube(*GREENLING["head"], fuzz, grain=0.6, light=P["skin_l"], dark=P["skin_d"])
    f = head["front"]
    # compound eyes: two 3x3 violet domes with a cyan glint
    for ex in (0, 5):
        for (dx, dy) in ((0, 0), (1, 0), (2, 0), (0, 1), (2, 1), (0, 2), (1, 2), (2, 2)):
            sh.gpx[f[0] + ex + dx, f[1] + 1 + dy] = P["mark_d"]
            sh.px[f[0] + ex + dx, f[1] + 1 + dy] = P["mark_d"]
        sh.put(f[0] + ex + 1, f[1] + 2, P["mark"], True)
        sh.put(f[0] + ex + 1, f[1] + 1, P["glass"], True)
    # proboscis / mouthparts
    sh.dot(f, 3, 5, P["skin_l"]); sh.dot(f, 4, 5, P["skin_l"]); sh.dot(f, 3, 6, P["skin_d"]); sh.dot(f, 4, 6, P["skin_d"])
    # eyes wrap onto the side faces
    for name in ("left", "right"):
        r = head[name]
        sh.rect((r[0] + (r[2] - 3 if name == "left" else 0), r[1] + 1, 3, 3), P["mark_d"])
        sh.put(r[0] + (r[2] - 2 if name == "left" else 1), r[1] + 2, P["mark"], True)
    # small parts (44,0): antennae (1x6x1 / 1x5x1), legs (2.5x3x3), arms (2x5x2) -> fuzzy indigo,
    # feathered antenna tips in violet (top rows), eye-spot on the arm/wing-stub faces
    sm = sh.cube(*GREENLING["small"], fuzz, grain=0.6)
    for name in ("front", "back", "left", "right"):
        sh.hline(sm[name], 0, P["mark"], glow=True)
        sh.hline(sm[name], 1, P["mark_d"], glow=True)
        sh.dot(sm[name], 1, 3, P["glass"], glow=True)
    sh.rect(sm["top"], P["mark"], glow=True)
    # ---- wing islands for DuskMothModel: fore wing 8x1x6 at (0,46) and (28,46),
    #      hind wing 6x1x4 at (0,54) and (20,54).  Top/bottom faces carry the pattern.
    def wing(u, v, w, d, mirror):
        fw = sh.cube(u, v, w, 1, d, [P["skin"], P["skin_d"]], grain=0.5)
        for name in ("top", "bottom"):
            r = fw[name]
            sh.noise(r, [P["skin"], P["skin_d"], P["skin_l"]], grain=0.5)
            # scalloped violet trailing edge + a big eye-spot
            for dx in range(r[2]):
                if dx % 2 == 0:
                    sh.dot(r, dx, r[3] - 1, P["mark_d"], glow=True)
            cx = (r[2] - 3) if mirror else 1
            sh.rect((r[0] + cx, r[1] + 1, 2, 2), P["mark_d"])
            sh.put(r[0] + cx, r[1] + 1, P["mark"], True)
            sh.put(r[0] + cx + 1, r[1] + 2, P["glass"], True)
            # veins
            for dy in range(0, r[3], 2):
                sh.dot(r, r[2] // 2, dy, P["skin_l"])
    wing(0, 46, 8, 6, False); wing(28, 46, 8, 6, True)
    wing(0, 54, 6, 4, False); wing(20, 54, 6, 4, True)
    return sh


def salvage_drone():
    """Mars Salvage Drone on the Quartz Crawler geometry (renderer scale 0.7): a weathered
    titanium hull (station greys) with rust bloom on the dome, a hazard-striped rim, a
    sensor head with a cyan lens, antenna masts (the crystal parts) with a red beacon and
    strut legs.  Glow layer: lens, beacon, teal status LEDs.  Hover-chassis islands for the
    SalvageDroneModel at (0,40) and (32,40)."""
    sh = Sheet(seed_of("salvage_drone"))
    P = STEEL
    hull = [P["hull"], P["seam"], P["hull_l"]]
    rim = sh.cube(*CRAWLER["rim"], hull, grain=0.4, stripes=(3, P["seam"], "x"), light=P["hull_l"], dark=P["seam"])
    # shell and dome share the (0,0) origin with the rim: their faces are sub-rects of the rim's.
    # Paint the rim's side faces with hazard stripes along the bottom row band (rim is 2 tall) and
    # plating above; the shell (4 tall) and dome (3 tall) read the plating rows.
    for name in ("front", "back", "left", "right"):
        r = rim[name]
        for dx in range(r[2]):
            c = P["hazard"] if (dx // 2) % 2 == 0 else P["black"]
            sh.put(r[0] + dx, r[1] + r[3] - 1, c)
    shell = faces(*CRAWLER["shell"])
    dome = faces(*CRAWLER["dome"])
    # top of the shell/dome: rust bloom + a teal status LED pair + panel seams
    for fname in ("top",):
        r = shell[fname]
        sh.noise(r, hull, grain=0.4)
        for _ in range(9):
            sh.dot(r, sh.rng.randrange(r[2]), sh.rng.randrange(r[3]), sh.rng.choice((P["rust"], P["rust_l"])))
        sh.hline(r, r[3] // 2, P["seam"]); sh.vline(r, r[2] // 2, P["seam"])
    r = dome["top"]
    sh.noise(r, hull, grain=0.4)
    sh.outline(r, P["seam"])
    sh.put(r[0] + 2, r[1] + 2, P["teal"], True); sh.put(r[0] + 5, r[1] + 2, P["teal"], True)
    for _ in range(4):
        sh.dot(r, sh.rng.randrange(r[2]), sh.rng.randrange(r[3]), P["rust"])
    # shell sides: rivets and a rust streak
    for name in ("front", "back", "left", "right"):
        r = shell[name]
        for dx in range(1, r[2], 3):
            sh.dot(r, dx, 0, P["seam"])
        sh.dot(r, r[2] - 2, 1, P["rust"]); sh.dot(r, r[2] - 2, 2, P["rust_l"])
    # sensor head: dark housing with a cyan lens on the front, vents on the sides
    head = sh.cube(*CRAWLER["head"], [P["seam"], P["black"], P["hull"]], grain=0.4, light=P["hull"], dark=P["black"])
    f = head["front"]
    sh.rect((f[0] + 1, f[1] + 1, 4, 2), P["black"])
    sh.put(f[0] + 2, f[1] + 1, P["lens"], True); sh.put(f[0] + 3, f[1] + 1, P["lens"], True)
    sh.put(f[0] + 2, f[1] + 2, mix(P["lens"], P["black"], 0.5), True); sh.put(f[0] + 3, f[1] + 2, mix(P["lens"], P["black"], 0.5), True)
    sh.put(f[0] + 2, f[1] + 1, hexc("#FFFFFF"), True)
    for name in ("left", "right"):
        r = head[name]
        for dy in range(0, r[3], 2):
            sh.hline(r, dy, P["black"])
    # small parts (44,0): antenna masts (2x5/6/4x2) + legs (2x10x2) share the region.
    # dark steel struts with a red beacon on row 0 (mast tips) and hazard feet on the last rows (legs)
    sm = sh.cube(*CRAWLER["small"], [P["seam"], P["black"], P["hull"]], grain=0.4, stripes=(2, P["black"], "y"))
    for name in ("front", "back", "left", "right"):
        r = sm[name]
        sh.hline(r, 0, P["beacon"], glow=True)
        sh.hline(r, r[3] - 1, P["hazard"]); sh.hline(r, r[3] - 2, P["black"])
    sh.rect(sm["top"], P["beacon"], glow=True)
    sh.rect(sm["bottom"], P["hazard"])
    # ---- hover-chassis islands for SalvageDroneModel: chassis 10x4x10 at (0,40) is the
    #      shell above; thruster pods 3x3x3 at (0,56),(12,56),(24,56),(36,56); manipulator arm 2x8x2 at (48,40)
    for u in (0, 12, 24, 36):
        pod = sh.cube(u, 56, 3, 3, 3, hull, grain=0.4, light=P["hull_l"], dark=P["seam"])
        sh.rect(pod["bottom"], P["lens"], glow=True)
    armf = sh.cube(48, 40, 2, 8, 2, [P["seam"], P["black"], P["hull"]], grain=0.4)
    for name in ("front", "back", "left", "right"):
        sh.hline(armf[name], armf[name][3] - 1, P["hazard"])
    return sh


def dust_devil():
    """Mars Dust Devil: four 16x64 frames (columns) of a translucent, tapering vortex in the
    red-regolith palette — a dark throat near the ground widening into loose dust at the top,
    with spiral bands that advance one row per frame.  Intended for a crossed-quad renderer
    (DustDevilRenderer); the entity's own FALLING_DUST / CLOUD particles stay as they are."""
    rng = random.Random(seed_of("dust_devil"))
    img = Image.new("RGBA", (ES, ES), CLEAR)
    px = img.load()
    P = MARS
    bands = [P["coat"], P["coat_l"], P["coat_d"], P["belly"], P["dust"]]
    for frame in range(4):
        x0 = frame * 16
        for y in range(ES):
            t = y / (ES - 1)                       # 0 = top, 1 = ground
            half = 1.0 + (1 - t) * 6.5             # half-width in px: ~1.5 at the foot, ~7.5 at the top
            cx = 7.5 + 0.8 * math.sin((t * 6.0 + frame * 0.9))
            for dx in range(16):
                dist = abs(dx - cx)
                if dist > half:
                    continue
                edge = dist / half                  # 0 centre .. 1 edge
                # spiral band: phase moves with height and frame
                band = int((y * 0.45 + (dx - cx) * 0.35 + frame) % len(bands))
                c = bands[band]
                alpha = int(60 + 110 * (1 - edge) * (0.55 + 0.45 * t))
                if t > 0.82:                        # throat: dark and dense
                    c = mix(c, P["stripe"], 0.5)
                    alpha = min(230, alpha + 60)
                if rng.random() < 0.18:             # loose grains
                    alpha = max(0, alpha - 70)
                    c = P["belly"]
                if edge > 0.8 and rng.random() < 0.5:
                    alpha = int(alpha * 0.5)
                px[x0 + dx, y] = (c[0], c[1], c[2], alpha)
    return img


SKINS = {
    "alien_villager_cindara": (ember_kin, True),
    "alien_villager_glacira": (frostbound, True),
    "alien_villager_duskborn": (duskborn, True),
    "moon_hopper": (moon_hopper, False),
    "dust_strider": (dust_strider, False),
    "dusk_moth": (dusk_moth, True),
    "salvage_drone": (salvage_drone, True),
}


def write_all(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for name, (fn, has_glow) in SKINS.items():
        sh = fn()
        sh.save(os.path.join(out_dir, name + ".png"), os.path.join(out_dir, name + "_glow.png") if has_glow else None)
        print("wrote", name, "(+glow)" if has_glow else "")
    dust_devil().save(os.path.join(out_dir, "dust_devil.png"))
    print("wrote dust_devil")


def write_preview(out_dir, scale=4):
    from PIL import ImageDraw
    os.makedirs(out_dir, exist_ok=True)
    names = list(SKINS) + ["dust_devil"]
    cell = ES * scale
    sheet = Image.new("RGBA", (4 * (cell + 8) + 8, ((len(names) + 3) // 4) * (cell + 20) + 8), (30, 30, 34, 255))
    d = ImageDraw.Draw(sheet)
    for i, name in enumerate(names):
        if name == "dust_devil":
            img = dust_devil()
        else:
            sh = SKINS[name][0]()
            img = sh.img.copy()
            img.alpha_composite(sh.glow)
        big = img.resize((cell, cell), Image.NEAREST)
        bg = Image.new("RGBA", (cell, cell), (70, 70, 76, 255))
        bg.alpha_composite(big)
        x = 8 + (i % 4) * (cell + 8)
        y = 8 + (i // 4) * (cell + 20)
        d.text((x, y), name, fill=(230, 230, 230, 255))
        sheet.paste(bg, (x, y + 12))
    sheet.save(os.path.join(out_dir, "entity_sheets_x%d.png" % scale))
    print("preview", os.path.join(out_dir, "entity_sheets_x%d.png" % scale))


def check(out_dir):
    """Renders every skin into a temp dir and compares bytes with ``out_dir``; returns 1 on any drift."""
    tmp = tempfile.mkdtemp(prefix="gen_skins_check_")
    try:
        write_all(tmp)
        bad = []
        names = sorted(os.listdir(tmp))
        for f in names:
            target = os.path.join(out_dir, f)
            if not os.path.isfile(target):
                bad.append(f + " (missing)")
                continue
            with open(os.path.join(tmp, f), "rb") as a, open(target, "rb") as b:
                if a.read() != b.read():
                    bad.append(f)
        for b in bad:
            print("gen_entity_skins: stale " + b)
        print("gen_entity_skins: %d of %d skin file(s) ok" % (len(names) - len(bad), len(names)))
        return 1 if bad else 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    if "--check" in sys.argv:
        sys.exit(check(sys.argv[sys.argv.index("--check") + 1]))
    if "--preview" in sys.argv:
        write_preview(sys.argv[sys.argv.index("--preview") + 1])
    else:
        write_all(sys.argv[1])
