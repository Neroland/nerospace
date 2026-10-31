#!/usr/bin/env python3
"""
Nyxara crystal texture set (Nerospace 2.0.x: Nyxara reads as glassy crystal, not stone).

Extends ``gen_planet_textures.py`` (same helpers, same md5-seeded determinism, same output naming)
with the glassy families the crystal surface needs and a table, ``NYXARA_CRYSTAL_TEXTURES``, that
maps every new Nyxara block / item id to a family + palette + frame spec.

Three kinds of texture come out of here, and the kind decides the chunk render layer on every
26.x line (the layer is taken from texture alpha):

* ``opaque`` surfaces remain solid, including the loose shard drift.
* ``translucent`` crystals have partially transparent facets and brighter edges.
* Cross-shaped clusters retain transparent gaps and translucent crystal facets.

Entry points
------------
* ``python tools/gen_nyxara_crystal.py``            writes the set into the resource tree
  (additive: an existing file is skipped unless ``--force``), like ``gen_planet_textures.py``.
* ``python tools/gen_nyxara_crystal.py --check``    re-renders everything in memory and compares
  byte-for-byte with the files on disk; exits 1 on any missing or drifted file (``--pixels``
  compares decoded pixels instead, for a machine with a different zlib build).
* ``python tools/gen_nyxara_crystal.py --root <textures dir>``  targets another ``textures/``
  folder (used for staging); ``--preview <dir>`` writes the per-id contact sheet there.

Everything is deterministic: ``random.Random(seed_of(id))`` per id, Pillow only, no numpy, and
the PNGs are written with the default encoder so a re-render is byte-identical.
"""
import io
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    from PIL import Image
except ModuleNotFoundError:
    print("gen_nyxara_crystal: Pillow not installed; skipping (pip install pillow).")
    sys.exit(0)

from gen_planet_textures import (S, CLEAR, hexc, mix, shade, seed_of, new_img, strip_of, mcmeta,
                                 _smooth_field, crystal_cluster)

# ================================================================================ palette
# Listed Nyxara sheet hexes: #2B2440 nyx stone, #9B5DE5 violet crystal, #C77DFF crystal light,
# #FFB86B dawnband gold, #0D0A14 shadow, #5B2D8E nebula, #FF6EC7 nebula pink, #4DD0E1 nebula cyan.
# Derived tones (marked *) are mixes/shades of listed ones and are recorded in the design note.
NYX_DEEP = hexc("#0D0A14")
NYX_STONE = hexc("#2B2440")
VIOLET_DEEP = hexc("#5B2D8E")
VIOLET = hexc("#9B5DE5")
VIOLET_LT = hexc("#C77DFF")
VIOLET_PALE = hexc("#E6D2FF")     # * (already the voidglass ore glint)
GOLD = hexc("#FFB86B")
ROSE = hexc("#FF6EC7")
CYAN = hexc("#4DD0E1")
WHITE = hexc("#FFFFFF")

# dayside *
SUN_CORE = hexc("#F4E4C4")        # * clear-amber core
SUN_BODY = hexc("#E8CFA0")        # *
SUN_DEEP = hexc("#B98B4E")        # *
SUN_LIGHT = hexc("#FFF3DC")       # *
CLEAR_CORE = hexc("#EEF3F9")      # *
CLEAR_BODY = hexc("#D5DFEA")      # *
CLEAR_DEEP = hexc("#8FA3BD")      # *
CRAZE_SHADOW = hexc("#7F91AB")    # *
DRIFT_BED = hexc("#D8C7A3")       # *

# band *
ROSE_DEEP = hexc("#B2448F")       # *
ROSE_LT = hexc("#FFA8E0")         # *
ROSE_PALE = hexc("#FFE6F6")       # *

# nightside *
SMOKE_BODY = hexc("#1A1424")      # *
SMOKE_LT = hexc("#4A3D6A")        # *
MIRROR_BODY = hexc("#1B1528")     # *


# ================================================================================ families

def _depth_field(rng, cx, cy):
    """0 at the lit core (cx, cy), 1 at the far rim; the refracted-interior cue."""
    out = [[0.0] * S for _ in range(S)]
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - cx, y - cy) / 12.0
            out[y][x] = max(0.0, min(1.0, d))
    return out


def _facet_planes(rng, n=2):
    """`n` random diagonal cuts; each pixel's region id = bitmask of which side of each cut it is on."""
    cuts = []
    for _ in range(n):
        a = rng.uniform(0.6, 1.6) * rng.choice((-1, 1))
        b = rng.uniform(-4, 18)
        cuts.append((a, b))
    region = [[0] * S for _ in range(S)]
    for y in range(S):
        for x in range(S):
            r = 0
            for i, (a, b) in enumerate(cuts):
                if y > a * x + b:
                    r |= 1 << i
            region[y][x] = r
    return region, cuts


def glassy_block(pal, seed, *, alpha=255, facets=2, caustic=True, glow=None, glow_dots=0,
                 streaks=None, streak_count=0, rim=True, alt_pal=None, alt_share=0.4):
    """Glassy crystal cube. `pal` = (deep, body, core, spec). alpha 255 = opaque glassy (SOLID);
    lower = translucent (edges stay 255). `glow` paints `glow_dots` inner points; `streaks`
    paints `streak_count` 2-3 px inclusions (amber veins in the dayside crystal)."""
    rng = random.Random(seed ^ 0x61A55)
    deep, body, core, spec = pal
    cx, cy = rng.uniform(4, 7), rng.uniform(4, 7)
    depth = _depth_field(rng, cx, cy)
    region, _ = _facet_planes(rng, facets)
    # every facet plane is a flat tone step (crystal is planar, not cloudy); the plane's level is
    # its distance from the lit core plus a per-plane offset, quantised to five steps
    def ramp(p):
        dp, bd, cr, sp = p
        return (mix(cr, sp, 0.25), cr, mix(cr, bd, 0.5), bd, mix(bd, dp, 0.5), dp)
    main_levels = ramp(pal)
    alt_levels = ramp(alt_pal) if alt_pal is not None else None
    nreg = 1 << facets
    offs = [rng.uniform(-0.42, 0.42) for _ in range(nreg)]
    # with an alternate palette some planes take it (clear planes among amber ones)
    use_alt = [alt_levels is not None and rng.random() < alt_share for _ in range(nreg)]
    img = new_img()
    px = img.load()
    mask = []
    lvl = [[0] * S for _ in range(S)]
    for y in range(S):
        for x in range(S):
            t = 0.2 + depth[y][x] * 0.5 + offs[region[y][x]] + rng.uniform(-0.03, 0.03)
            t = max(0.0, min(0.999, t))
            levels = alt_levels if use_alt[region[y][x]] else main_levels
            k = int(t * (len(levels) - 1) + 0.5)
            lvl[y][x] = k
            c = levels[k]
            a = alpha
            if alpha < 255:
                a = max(0, min(255, int(alpha + (k - 2.5) * 14 + rng.randint(-5, 5))))
            px[x, y] = (c[0], c[1], c[2], a)
    # facet edges: the lighter side of a cut gets a spec line, the darker side a deep line, so each
    # plane reads as a hard crystal face
    for y in range(1, S - 1):
        for x in range(1, S - 1):
            for (nx, ny) in ((x + 1, y), (x, y + 1)):
                if region[y][x] != region[ny][nx]:
                    here, there = lvl[y][x], lvl[ny][nx]
                    c = px[x, y]
                    if here <= there:
                        lit = mix((c[0], c[1], c[2], 255), spec, 0.5)
                        px[x, y] = (lit[0], lit[1], lit[2], 255)
                    else:
                        dk = mix((c[0], c[1], c[2], 255), deep, 0.6)
                        px[x, y] = (dk[0], dk[1], dk[2], 255)
                    break
    # two or three dark inclusions (flaws) so the planes are not sterile
    for _ in range(rng.randint(2, 3)):
        ix, iy = rng.randint(2, 13), rng.randint(2, 13)
        c = px[ix, iy]
        d = mix((c[0], c[1], c[2], 255), deep, 0.5)
        px[ix, iy] = (d[0], d[1], d[2], c[3])
    if streaks is not None:
        for _ in range(streak_count):
            sx, sy = rng.randint(2, 12), rng.randint(2, 12)
            ln = rng.randint(2, 4)
            dx, dy = rng.choice(((1, 1), (1, 0), (1, -1)))
            for k in range(ln):
                xx, yy = sx + dx * k, sy + dy * k
                if 1 <= xx < S - 1 and 1 <= yy < S - 1:
                    c = px[xx, yy]
                    s = mix(streaks, (c[0], c[1], c[2], 255), 0.25)
                    px[xx, yy] = (s[0], s[1], s[2], c[3])
    if caustic:
        # a short bright diagonal streak near the core plus two specular points
        sx = int(cx) - 2
        sy = int(cy) - 2
        for k in range(4):
            xx, yy = sx + k, sy + k
            if 1 <= xx < S - 1 and 1 <= yy < S - 1:
                px[xx, yy] = (spec[0], spec[1], spec[2], 255)
        for (xx, yy) in ((int(cx) + 4, int(cy) - 3), (int(cx) - 4, int(cy) + 4)):
            if 1 <= xx < S - 1 and 1 <= yy < S - 1:
                px[xx, yy] = (spec[0], spec[1], spec[2], 255)
    if glow is not None:
        for _ in range(glow_dots):
            gx, gy = rng.randint(2, 13), rng.randint(2, 13)
            c = px[gx, gy]
            g = mix((c[0], c[1], c[2], 255), glow, 0.75)
            px[gx, gy] = (g[0], g[1], g[2], c[3])
            mask.append((gx, gy))
            for (ox, oy) in ((1, 1), (-1, -1)):
                nx, ny = gx + ox, gy + oy
                if 1 <= nx < S - 1 and 1 <= ny < S - 1 and (nx, ny) not in mask:
                    c = px[nx, ny]
                    g = mix((c[0], c[1], c[2], 255), glow, 0.3)
                    px[nx, ny] = (g[0], g[1], g[2], c[3])
    if rim:
        for i in range(S):
            for (x, y, tone) in ((i, 0, spec), (0, i, spec), (i, S - 1, deep), (S - 1, i, deep)):
                c = px[x, y]
                r = mix((c[0], c[1], c[2], 255), tone, 0.55 if tone is spec else 0.7)
                px[x, y] = (r[0], r[1], r[2], 255)
    return img, mask


def crazed_block(pal, seed, *, craze, shadow, cracks=4):
    """Clear glassy cube with a white craze network (thermal-shock lines), each line shadowed."""
    img, _ = glassy_block(pal, seed, alpha=165, facets=1, caustic=True)
    rng = random.Random(seed ^ 0xC2A2E)
    px = img.load()
    for _ in range(cracks):
        x, y = rng.randint(1, 14), rng.randint(1, 14)
        dx, dy = rng.choice(((1, 0), (0, 1), (1, 1), (1, -1)))
        for k in range(rng.randint(4, 9)):
            if rng.random() < 0.3:
                dx, dy = rng.choice(((1, 0), (0, 1), (1, 1), (1, -1), (-1, 1)))
            if 0 <= x < S and 0 <= y < S:
                px[x, y] = craze
                sx, sy = x + 1, y + 1
                if sx < S and sy < S and px[sx, sy] != craze:
                    c = px[sx, sy]
                    s = mix((c[0], c[1], c[2], 255), shadow, 0.5)
                    px[sx, sy] = (s[0], s[1], s[2], 255)
            x, y = x + dx, y + dy
    return img


def shard_drift(bed, chips, seed, *, side=False):
    """Loose crystal chips on a bleached bed: 2-3 px triangular shards, each with a lit edge."""
    rng = random.Random(seed ^ (0xD21F if not side else 0x51DE))
    img = new_img()
    px = img.load()
    field = _smooth_field(rng, chunk=0.5)
    for y in range(S):
        for x in range(S):
            t = field[y][x]
            px[x, y] = mix(shade(bed, 0.88), bed, t) if t < 0.85 else mix(bed, WHITE, 0.15)
    n = 9 if not side else 6
    for _ in range(n):
        cx, cy = rng.randint(0, 14), rng.randint(0, 14)
        deep, body, light = rng.choice(chips)
        size = rng.choice((2, 2, 3))
        flip = rng.random() < 0.5
        for yy in range(size):
            for xx in range(size - yy):
                x = cx + (xx if not flip else size - 1 - xx)
                y = cy + yy
                if 0 <= x < S and 0 <= y < S:
                    px[x, y] = light if yy == 0 and xx == 0 else (body if yy == 0 else deep)
    if side:
        # the drift's edge: darker bottom rows so the layer model reads as a lip
        for x in range(S):
            px[x, S - 1] = shade(bed, 0.7)
            px[x, S - 2] = mix(px[x, S - 2], shade(bed, 0.7), 0.5)
    return img


def spire_side(pal, seed, *, alpha=175):
    """Columnar translucent crystal: 3-4 vertical facet bands, a bright vertical caustic, dark
    outer edges at alpha 255 so neighbouring spire faces cull cleanly."""
    rng = random.Random(seed ^ 0x5B12E)
    deep, body, core, spec = pal
    img = new_img()
    px = img.load()
    cuts = sorted(rng.sample(range(3, 13), 3))
    bands = [0] + cuts + [S]
    tones = [rng.uniform(-0.25, 0.25) for _ in range(len(bands) - 1)]
    bright = rng.randint(1, len(bands) - 2)
    for y in range(S):
        for x in range(S):
            b = max(i for i in range(len(bands) - 1) if x >= bands[i])
            t = 0.5 + tones[b] + rng.uniform(-0.04, 0.04)
            if b == bright:
                t -= 0.25
            t = max(0.0, min(1.0, t))
            c = mix(core, body, t * 2) if t < 0.5 else mix(body, deep, (t - 0.5) * 2)
            a = max(0, min(255, int(alpha + (t - 0.5) * 50 + rng.randint(-6, 6))))
            px[x, y] = (c[0], c[1], c[2], a)
    for b in cuts:
        for y in range(S):
            c = px[b, y]
            l = mix((c[0], c[1], c[2], 255), spec, 0.4)
            px[b, y] = (l[0], l[1], l[2], 255)
    vx = bands[bright] + 1
    for y in range(2, 14):
        px[vx, y] = (spec[0], spec[1], spec[2], 255)
    # two angled fractures across the column (a crystal is not a pipe)
    for (sx, sy, ln) in ((rng.randint(1, 6), rng.randint(1, 6), 5), (rng.randint(7, 11), rng.randint(6, 10), 4)):
        for k in range(ln):
            xx, yy = sx + k, sy + k
            if 1 <= xx < S - 1 and 1 <= yy < S - 1:
                c = px[xx, yy]
                l = mix((c[0], c[1], c[2], 255), spec, 0.5)
                px[xx, yy] = (l[0], l[1], l[2], 255)
                c = px[xx, yy + 1]
                d = mix((c[0], c[1], c[2], 255), deep, 0.5)
                px[xx, yy + 1] = (d[0], d[1], d[2], c[3])
    for y in range(S):
        for x in (0, S - 1):
            c = px[x, y]
            d = mix((c[0], c[1], c[2], 255), deep, 0.7)
            px[x, y] = (d[0], d[1], d[2], 255)
    return img


def prism_block(pal, seed, *, alpha=165, dispersion=(ROSE, CYAN, GOLD), flaring=False):
    """Triangular facets with dispersion colours on the facet edges (the prism tints light).
    Flaring: brighter core, gold-dominant dispersion, higher alpha."""
    rng = random.Random(seed ^ 0x9215 + (1 if flaring else 0))
    deep, body, core, spec = pal
    if flaring:
        body = mix(body, GOLD, 0.35)
        core = mix(core, WHITE, 0.5)
        alpha = min(255, alpha + 40)
    img = new_img()
    px = img.load()
    cs = 4
    for cy in range(4):
        for cx in range(4):
            flip = rng.random() < 0.5
            tone = rng.choice((body, body, mix(body, core, 0.4)))
            disp = rng.choice(dispersion) if (rng.random() < (0.7 if flaring else 0.5)) else None
            if flaring and rng.random() < 0.5:
                disp = GOLD
            for y in range(cs):
                for x in range(cs):
                    u, v = (x, y) if not flip else (cs - 1 - x, y)
                    X, Y = cx * cs + x, cy * cs + y
                    if u + v < cs - 1:
                        c = mix(tone, core, 0.45)
                        a = alpha + 10
                    elif u + v == cs - 1:
                        c = disp if disp is not None else mix(tone, spec, 0.5)
                        a = 255
                    else:
                        c = mix(tone, deep, 0.4)
                        a = alpha - 15
                    px[X, Y] = (c[0], c[1], c[2], max(0, min(255, a + rng.randint(-5, 5))))
    for i in range(S):
        for (x, y, tone) in ((i, 0, spec), (0, i, spec), (i, S - 1, deep), (S - 1, i, deep)):
            c = mix(px[x, y], tone, 0.6)
            px[x, y] = (c[0], c[1], c[2], 255)
    if flaring:
        for (x, y) in ((7, 7), (8, 8), (8, 7), (7, 8), (6, 7), (9, 8)):
            px[x, y] = (255, 255, 255, 255)
    return img


def mirror_frames(pal, seed, *, alpha=226, frames=8, frametime=6):
    """Near-black glossy crystal with a cyan sheen that drifts across (the lake 'reflects' the
    galaxy); the base is static, only the sheen band moves."""
    rng = random.Random(seed ^ 0x3122)
    deep, body, sheen, spec = pal
    base = new_img()
    px = base.load()
    field = _smooth_field(rng, chunk=0.8)
    for y in range(S):
        for x in range(S):
            c = mix(body, deep, field[y][x] * 0.8)
            px[x, y] = (c[0], c[1], c[2], max(0, min(255, alpha + rng.randint(-4, 4))))
    for i in range(S):
        for (x, y, tone) in ((i, 0, mix(body, sheen, 0.3)), (0, i, mix(body, sheen, 0.3)), (i, S - 1, deep), (S - 1, i, deep)):
            px[x, y] = (tone[0], tone[1], tone[2], 255)
    out = []
    for f in range(frames):
        im = base.copy()
        p = im.load()
        phase = f / float(frames)
        # diagonal sheen band x + y == k, drifting with the phase; width 3, strongest in the middle
        k = int(round(4 + phase * 22)) % 30 - 4
        for y in range(1, S - 1):
            for x in range(1, S - 1):
                d = min(abs((x + y) - k), abs((x + y) - 19) + 1)
                if d <= 2:
                    w = (0.5, 0.3, 0.12)[d]
                    c = p[x, y]
                    s = mix((c[0], c[1], c[2], 255), sheen, w)
                    p[x, y] = (s[0], s[1], s[2], c[3])
        for (x, y) in ((4, 11), (11, 4), (12, 12)):
            c = p[x, y]
            s = mix((c[0], c[1], c[2], 255), spec, 0.6 if f % 4 == 0 else 0.35)
            p[x, y] = (s[0], s[1], s[2], 255)
        out.append(im)
    return strip_of(out), mcmeta(frametime, interpolate=True), out[0]


def glow_pulse(img, mask, glow, *, frames=8, frametime=8, amp=0.25):
    """Pulse only the masked inner-glow pixels toward `glow`; everything else stays put."""
    out = []
    for f in range(frames):
        t = 0.5 + 0.5 * math.sin(2 * math.pi * f / frames)
        im = img.copy()
        p = im.load()
        for (x, y) in mask:
            c = p[x, y]
            g = mix((c[0], c[1], c[2], 255), glow, amp * t)
            p[x, y] = (g[0], g[1], g[2], c[3])
        out.append(im)
    return strip_of(out), mcmeta(frametime, interpolate=True), out[0]


def budding_block(pal, seed, *, buds):
    """Dark crystal matrix with 5-6 bright bud points (the nucleation sites)."""
    img, _ = glassy_block(pal, seed, alpha=175, facets=2, caustic=False)
    rng = random.Random(seed ^ 0xB0DD)
    px = img.load()
    pts = []
    for _ in range(6):
        x, y = rng.randint(2, 12), rng.randint(2, 12)
        if any(abs(x - a) < 3 and abs(y - b) < 3 for a, b in pts):
            continue
        pts.append((x, y))
        bd, bb, bl = buds
        px[x, y] = bl
        px[x + 1, y] = bb
        px[x, y + 1] = bb
        px[x + 1, y + 1] = bd
        if rng.random() < 0.5:
            px[x - 1, y] = bd
    return img


def item_shard(pal, seed):
    """Item: a single faceted shard, lit on the left flank (same silhouette rule as item_gem)."""
    deep, body, light = pal
    img = new_img()
    px = img.load()
    pts = [(7, 2), (8, 2), (9, 3), (10, 5), (10, 7), (9, 10), (8, 12), (7, 13), (6, 12), (5, 10), (5, 7), (6, 4)]
    # fill the polygon by scanline
    for y in range(S):
        xs = []
        n = len(pts)
        for i in range(n):
            (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
            if (y0 <= y < y1) or (y1 <= y < y0):
                xs.append(x0 + (y - y0) * (x1 - x0) / float(y1 - y0))
        xs.sort()
        for i in range(0, len(xs) - 1, 2):
            for x in range(int(math.ceil(xs[i])), int(math.floor(xs[i + 1])) + 1):
                px[x, y] = light if x < 7 else (body if x < 9 else deep)
    px[7, 4] = WHITE
    px[6, 6] = WHITE
    for y in range(3, 12):
        if px[7, y][3]:
            px[7, y] = mix(px[7, y], light, 0.5)
    return img


# ================================================================================ table

def _row(id_, family, **kw):
    kw.update(id=id_, family=family, seed=seed_of(id_))
    return kw


SUN = (SUN_DEEP, SUN_BODY, SUN_CORE, SUN_LIGHT)
CLR = (CLEAR_DEEP, CLEAR_BODY, CLEAR_CORE, WHITE)
TWI = (VIOLET_DEEP, VIOLET, VIOLET_LT, VIOLET_PALE)
RSE = (ROSE_DEEP, ROSE, ROSE_LT, ROSE_PALE)
SMK = (NYX_DEEP, SMOKE_BODY, NYX_STONE, SMOKE_LT)
MIR = (NYX_DEEP, MIRROR_BODY, CYAN, WHITE)
PRM = (VIOLET_DEEP, VIOLET, VIOLET_PALE, WHITE)


def _f_sunglass(spec):
    img, _ = glassy_block(SUN, spec["seed"], alpha=170, facets=3, streaks=GOLD, streak_count=4, alt_pal=CLR, alt_share=0.45)
    return {"block/sunglass_crystal.png": img}


def _f_crazed(spec):
    return {"block/crazed_crystal.png": crazed_block(CLR, spec["seed"], craze=WHITE, shadow=CRAZE_SHADOW)}


def _f_drift(spec):
    chips = ((CLEAR_DEEP, CLEAR_BODY, WHITE), (SUN_DEEP, GOLD, SUN_LIGHT), (VIOLET_DEEP, VIOLET_LT, VIOLET_PALE))
    return {"block/shard_drift.png": shard_drift(DRIFT_BED, chips, spec["seed"]),
            "block/shard_drift_side.png": shard_drift(DRIFT_BED, chips, spec["seed"], side=True)}


def _f_twilight(spec):
    img, _ = glassy_block(TWI, spec["seed"], alpha=170, facets=3)
    return {"block/twilight_crystal.png": img}


def _f_rose(spec):
    img, _ = glassy_block(RSE, spec["seed"], alpha=170, facets=3)
    lit, _ = glassy_block((ROSE, ROSE_LT, ROSE_PALE, WHITE), spec["seed"], alpha=170, facets=3)
    return {"block/rose_crystal.png": img, "block/rose_crystal_lit.png": lit}


def _f_smoke(spec):
    img, mask = glassy_block(SMK, spec["seed"], alpha=170, facets=3, glow=CYAN, glow_dots=3)
    return {"block/smoke_crystal.png": glow_pulse(img, mask, CYAN, frames=8, frametime=8, amp=0.35)}


def _f_mirror(spec):
    return {"block/mirror_crystal.png": mirror_frames(MIR, spec["seed"])}


def _f_budding(spec):
    return {"block/budding_nyx_crystal.png": budding_block((NYX_STONE, VIOLET_DEEP, mix(VIOLET_DEEP, VIOLET, 0.4), VIOLET_LT),
                                                            spec["seed"], buds=(VIOLET_DEEP, VIOLET_LT, ROSE_PALE))}


def _f_cluster(spec):
    out = {}
    for st in range(4):
        img = crystal_cluster((VIOLET_DEEP, VIOLET_LT, VIOLET_PALE), spec["seed"], st)
        if st == 3:  # cyan glints at the full-grown tips
            px = img.load()
            for y in range(S):
                for x in range(S):
                    if px[x, y] == (255, 255, 255, 255):
                        px[x, y] = CYAN
        px = img.load()
        for y in range(S):
            for x in range(S):
                r, g, b, a = px[x, y]
                if a:
                    px[x, y] = (r, g, b, 190)
        out["block/nyx_crystal_cluster_stage%d.png" % st] = img
    return out


def _f_spire(spec):
    side = spire_side(TWI, spec["seed"])
    top, _ = glassy_block(TWI, spec["seed"] ^ 0x70, alpha=185, facets=3)
    return {"block/crystal_spire.png": side, "block/crystal_spire_top.png": top}


def _f_prism(spec):
    return {"block/nyx_prism.png": prism_block(PRM, spec["seed"]),
            "block/nyx_prism_flaring.png": prism_block(PRM, spec["seed"], flaring=True)}


def _f_shard_item(spec):
    return {"item/crystal_shard.png": item_shard((VIOLET_DEEP, VIOLET_LT, VIOLET_PALE), spec["seed"])}


FAMILIES = {
    "sunglass": _f_sunglass, "crazed": _f_crazed, "drift": _f_drift, "twilight": _f_twilight,
    "rose": _f_rose, "smoke": _f_smoke, "mirror": _f_mirror, "budding": _f_budding,
    "cluster": _f_cluster, "spire": _f_spire, "prism": _f_prism, "shard_item": _f_shard_item,
}

# One row per id; `layer` documents the intended chunk layer (ChunkLayerTexturesTest pins it).
NYXARA_CRYSTAL_TEXTURES = [
    _row("sunglass_crystal", "sunglass", layer="translucent"),
    _row("crazed_crystal", "crazed", layer="translucent"),
    _row("shard_drift", "drift", layer="solid"),
    _row("twilight_crystal", "twilight", layer="translucent"),
    _row("rose_crystal", "rose", layer="translucent"),
    _row("smoke_crystal", "smoke", layer="translucent"),
    _row("mirror_crystal", "mirror", layer="translucent"),
    _row("budding_nyx_crystal", "budding", layer="translucent"),
    _row("nyx_crystal_cluster", "cluster", layer="translucent"),
    _row("crystal_spire", "spire", layer="translucent"),
    _row("nyx_prism", "prism", layer="translucent"),
    _row("crystal_shard", "shard_item", layer="item"),
]
BY_ID = {r["id"]: r for r in NYXARA_CRYSTAL_TEXTURES}


def render(id_):
    spec = BY_ID[id_]
    return FAMILIES[spec["family"]](spec)


def render_all():
    return {r["id"]: render(r["id"]) for r in NYXARA_CRYSTAL_TEXTURES}


# ================================================================================ layer audit

def alpha_layer(img):
    """'solid' | 'cutout' | 'translucent' from the pixels, the way the chunk layer is picked."""
    vals = set(img.convert("RGBA").tobytes()[3::4])
    if vals == {255}:
        return "solid"
    if vals <= {0, 255}:
        return "cutout"
    return "translucent"


def audit():
    """Every output's actual alpha class must match its row's `layer`; returns a list of errors."""
    errs = []
    for r in NYXARA_CRYSTAL_TEXTURES:
        want = r["layer"]
        for rel, art in render(r["id"]).items():
            img = art[2] if isinstance(art, tuple) else art
            if want == "item":
                continue
            got = alpha_layer(img)
            if got != want:
                errs.append("%s: expected %s texture, alpha says %s" % (rel, want, got))
    return errs


# ================================================================================ writing / check

def _png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def _mcmeta_text(meta):
    return json.dumps(meta, indent=2) + "\n"


def expected_files():
    """{relative path under textures/: bytes} for the whole set."""
    out = {}
    for id_, outputs in render_all().items():
        for rel, art in outputs.items():
            if isinstance(art, tuple):
                strip, meta, _still = art
                out[rel] = _png_bytes(strip)
                out[rel + ".mcmeta"] = _mcmeta_text(meta).encode("utf-8")
            else:
                out[rel] = _png_bytes(art)
    return out


def write_all(root, force=False):
    n = 0
    for rel, data in expected_files().items():
        path = os.path.join(root, rel)
        if os.path.exists(path) and not force:
            print("skip (exists)", path)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as fh:
            fh.write(data)
        print("wrote", path)
        n += 1
    return n


def check(root, pixels=False):
    """Byte-for-byte comparison of the on-disk set with an in-memory render. Returns error list.
    PNG bytes depend on the zlib build (zlib vs zlib-ng encode the same pixels differently), so a
    machine other than the one that rendered may see 'drift' with identical pixels; `pixels=True`
    (``--pixels``) compares decoded RGBA instead, which is build-independent."""
    errs = list(audit())
    for rel, data in expected_files().items():
        path = os.path.join(root, rel)
        if not os.path.exists(path):
            errs.append("missing: " + rel)
            continue
        with open(path, "rb") as fh:
            disk = fh.read()
        if disk == data:
            continue
        if pixels and rel.endswith(".png"):
            a = Image.open(io.BytesIO(disk)).convert("RGBA")
            b = Image.open(io.BytesIO(data)).convert("RGBA")
            if a.size == b.size and a.tobytes() == b.tobytes():
                continue
        errs.append("drift: " + rel)
    return errs


# ================================================================================ preview

def write_preview(out_dir, scale=8):
    from PIL import ImageDraw
    os.makedirs(out_dir, exist_ok=True)
    rendered = render_all()
    entries = []
    for r in NYXARA_CRYSTAL_TEXTURES:
        for rel, art in rendered[r["id"]].items():
            if isinstance(art, tuple):
                strip = art[0]
                frames = [strip.crop((0, i * S, S, (i + 1) * S)) for i in range(strip.height // S)]
                frames = [frames[0], frames[len(frames) // 2]]
            else:
                frames = [art]
            entries.append((rel.split("/", 1)[1][:-4], frames))
    cell = S * scale
    pad = 6
    cols = 8
    rows = (len(entries) + cols - 1) // cols
    w = cols * (cell + pad) + pad
    h = rows * (cell + pad + 14) + pad + 20
    sheet = Image.new("RGBA", (w, h), (24, 24, 28, 255))
    draw = ImageDraw.Draw(sheet)
    draw.text((pad, 4), "nyxara crystal set x%d" % scale, fill=(230, 230, 230, 255))
    for i, (name, frames) in enumerate(entries):
        gx, gy = i % cols, i // cols
        x0 = pad + gx * (cell + pad)
        y0 = 20 + gy * (cell + pad + 14)
        draw.text((x0, y0), name, fill=(230, 230, 230, 255))
        f = frames[0]
        big = f.resize((cell, cell), Image.NEAREST)
        bg = Image.new("RGBA", (cell, cell), (60, 60, 66, 255))
        bp = bg.load()
        q = cell // 8
        for yy in range(cell):
            for xx in range(cell):
                if ((xx // q) + (yy // q)) % 2:
                    bp[xx, yy] = (44, 44, 50, 255)
        bg.alpha_composite(big)
        sheet.paste(bg, (x0, y0 + 14))
    sheet.save(os.path.join(out_dir, "nyxara_crystal_x%d.png" % scale))
    print("preview", os.path.join(out_dir, "nyxara_crystal_x%d.png" % scale))


# ================================================================================ main

def _root_from_args():
    if "--root" in sys.argv:
        return sys.argv[sys.argv.index("--root") + 1]
    from nerospace_target import MULTILOADER_COMMON, src_base
    base = src_base()
    if not os.path.isdir(os.path.join(base, "src/main/resources")):
        base = MULTILOADER_COMMON  # the flattened repo keeps the shared resources under common/
    return os.path.join(base, "src/main/resources/assets/nerospace/textures")


if __name__ == "__main__":
    if "--preview" in sys.argv:
        write_preview(sys.argv[sys.argv.index("--preview") + 1])
        sys.exit(0)
    root = _root_from_args()
    if "--check" in sys.argv:
        problems = check(root, pixels="--pixels" in sys.argv)
        for p in problems:
            print("CHECK FAIL", p)
        print("gen_nyxara_crystal --check: %s (%d files)" % ("ok" if not problems else "FAILED", len(expected_files())))
        sys.exit(1 if problems else 0)
    bad = audit()
    if bad:
        for b in bad:
            print("LAYER FAIL", b)
        sys.exit(1)
    print("gen_nyxara_crystal: wrote %d files under %s" % (write_all(root, force="--force" in sys.argv), root))
