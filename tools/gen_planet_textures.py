#!/usr/bin/env python3
"""
Planet & station texture families for the Nerospace 2.0.0 planets rework.

This module extends ``gen_textures.py`` with the texture *families* the new planet block sets need
(noise stone, ore blebs, glow cracks, crystal facets, strata stripes, holes, layers, frost edges,
footprints, hue-shift / drip / pulse / shimmer animations) and a data table,
``PLANET_BLOCK_TEXTURES``, that maps every new block / item id to a family + palette + seed +
frame spec.  ``render(id)`` produces the texture(s) for one id; ``write_all()`` writes the whole
table into the resource tree, additive-only (an existing file is never overwritten unless
``--force`` is on the command line, exactly like ``gen_textures.save``).

Everything is deterministic: each id seeds its own ``random.Random`` from the md5 of the id, so a
re-render is byte-identical.  Pillow only (no numpy).

Entry points
------------
* ``gen_textures.py`` imports this module and calls ``write_all(BLOCK_DIR, ITEM_DIR)`` at the end of
  its ``__main__`` (so ``./gradlew genAssets`` picks the planet set up with no Gradle change).
  The call is gated by ``ENABLED`` below: it is ``False`` until the planet blocks are registered
  (Stage 3 flips it to ``True`` — or passes ``--planets`` on the command line).  Until then the
  table is inert and ``genAssets`` leaves the resource tree exactly as it was.
* ``python tools/gen_planet_textures.py --multiloader [--force] [--planets]`` renders the table
  directly (same gate, same additive semantics).
* ``python tools/gen_planet_textures.py --preview <dir>`` renders the whole table into a planning
  folder as per-planet contact sheets (16x and 4x) without touching the resource tree.

Output naming
-------------
A table row's ``outputs`` lists the files it produces, relative to ``textures/``:
``block/<id>.png`` for the main face and ``block/<id>_<suffix>.png`` for extra faces/states
(``_top``, ``_side``, ``_footprint``, ``_stage0..3``, ``_part``, ``_tip``), ``item/<id>.png`` for
items.  Animated outputs are vertical frame strips plus ``<file>.png.mcmeta`` written from the
``mcmeta`` dict the family returns, so ``gen_animations.py`` does not need a PULSE entry for them.
"""
import colorsys
import hashlib
import json
import math
import os
import random
import sys

try:
    from PIL import Image
except ModuleNotFoundError:  # mirrors gen_textures: committed art, Pillow optional
    print("gen_planet_textures: Pillow not installed; skipping (pip install pillow).")
    sys.exit(0)

S = 16
CLEAR = (0, 0, 0, 0)

# Stage 3 flips this to True once the planet blocks/items are registered. Until then the table is
# only reachable through --planets (resource write) or --preview (planning folder).
ENABLED = False


# ======================================================================================= helpers

def hexc(h, a=255):
    """'#RRGGBB' -> RGBA tuple (the concept-sheet hexes are the inputs)."""
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (a[3] if len(a) > 3 else 255,)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))),
            max(0, min(255, int(c[2] * f))), c[3] if len(c) > 3 else 255)


def seed_of(name):
    return int(hashlib.md5(name.encode()).hexdigest(), 16) & 0xffffffff


def new_img(fill=CLEAR):
    return Image.new("RGBA", (S, S), fill)


def _smooth_field(rng, chunk=0.35):
    """Value field in [0,1): per-pixel jitter biased by a coarse 4x4 lattice. `chunk` is the weight
    of the coarse term (0 = pure speckle like the 1.6.0 noise_fill, 1 = large soft patches)."""
    coarse = [[rng.random() for _ in range(4)] for _ in range(4)]
    out = [[0.0] * S for _ in range(S)]
    for y in range(S):
        for x in range(S):
            gx, gy = x / 4.0, y / 4.0
            x0, y0 = int(gx) % 4, int(gy) % 4
            x1, y1 = (x0 + 1) % 4, (y0 + 1) % 4
            tx, ty = gx - int(gx), gy - int(gy)
            v = (coarse[y0][x0] * (1 - tx) * (1 - ty) + coarse[y0][x1] * tx * (1 - ty)
                 + coarse[y1][x0] * (1 - tx) * ty + coarse[y1][x1] * tx * ty)
            out[y][x] = min(0.9999, max(0.0, v * chunk + rng.random() * (1.0 - chunk)))
    return out


def ramp_pick(ramp, t):
    return ramp[min(len(ramp) - 1, int(t * len(ramp)))]


def bevel(img, light, dark):
    px = img.load()
    for i in range(S):
        px[i, 0] = light
        px[0, i] = light
        px[i, S - 1] = dark
        px[S - 1, i] = dark


def blob(img, cx, cy, r, colors, rng, density=0.9):
    """Rough circular cluster (same primitive the 1.6.0 ores use, so ores stay in-family)."""
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r and rng.random() < density * (1 - d / (r + 0.001)):
                t = 1 - d / (r + 0.001)
                px[x, y] = colors[min(len(colors) - 1, int(t * len(colors)))]


def strip_of(frames):
    """List of 16x16 frames -> vertical strip (Minecraft animation layout)."""
    st = Image.new("RGBA", (S, S * len(frames)), CLEAR)
    for i, f in enumerate(frames):
        st.paste(f, (0, i * S))
    return st


def mcmeta(frametime, interpolate=False):
    anim = {"frametime": frametime}
    if interpolate:
        anim["interpolate"] = True
    return {"animation": anim}


def _lit(px, xy, f, sat_boost=0.12):
    r, g, b, a = px[xy]
    hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    vv = min(1.0, vv * f)
    ss = min(1.0, ss * (1.0 + sat_boost * (f - 1)))
    r, g, b = (int(c * 255) for c in colorsys.hsv_to_rgb(hh, ss, vv))
    px[xy] = (r, g, b, a)


# ============================================================================ static families

def noise_stone(palette, seed, *, grain=0.35, veins=0, vein_colour=None, chunk=0.6, bev=None):
    """Three-tone stone: base / shade / grain from `palette` (base, shade, grain[, extra...]).
    `grain` is the share of grain pixels, `veins` adds that many 1px random-walk veins in
    `vein_colour`, `chunk` controls clump size, `bev` = (light, dark) adds a storage-block bevel."""
    rng = random.Random(seed)
    base, dark, light = palette[0], palette[1], palette[2]
    field = _smooth_field(rng, chunk)
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            v = field[y][x]
            if v < 0.30:
                px[x, y] = dark
            elif v > 1.0 - grain * 0.75:
                px[x, y] = light
            else:
                px[x, y] = base
            if len(palette) > 3 and rng.random() < 0.04:
                px[x, y] = palette[3]
    for _ in range(veins):
        x, y = rng.randrange(S), rng.randrange(S)
        for _ in range(rng.randint(5, 10)):
            px[x % S, y % S] = vein_colour or light
            x += rng.choice((-1, 0, 1))
            y += rng.choice((0, 1, 1))
    if bev:
        bevel(img, bev[0], bev[1])
    return img


def ore_blebs(base_img, ore_palette, seed, *, count=4, size=2.6, glint=None, density=0.95):
    """Scatter `count` ore clusters (dark->light ramp `ore_palette`) over a copy of `base_img`."""
    rng = random.Random(seed ^ 0xB1EB)
    img = base_img.copy()
    spots = [(4, 5), (11, 6), (7, 12), (13, 12), (2, 11), (9, 2)][:count]
    for (cx, cy) in spots:
        blob(img, cx + rng.randint(-1, 1), cy + rng.randint(-1, 1), size + rng.random() * 0.6,
             ore_palette, rng, density)
    if glint:
        px = img.load()
        for (cx, cy) in spots[:2]:
            px[cx, cy] = glint
    return img


def holes(base_img, hole_palette, seed, *, count=5, radius=1.8, rim=True):
    """Rounded holes punched into `base_img`: (rim, floor, highlight). Cheese, regolith craters."""
    rng = random.Random(seed ^ 0x4015)
    img = base_img.copy()
    px = img.load()
    rim_c, floor_c, hi_c = hole_palette
    for _ in range(count):
        cx, cy = rng.uniform(2, 13), rng.uniform(2, 13)
        r = radius * rng.uniform(0.7, 1.2)
        for y in range(S):
            for x in range(S):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d <= r:
                    px[x, y] = floor_c
                    if x < cx and y < cy and d > r - 0.9:
                        px[x, y] = hi_c
                elif rim and d <= r + 0.8:
                    px[x, y] = rim_c
    return img


def crystal_facets(palette, seed, *, cells=4, bev=True):
    """Full-cube faceted crystal: triangular facets tiled `cells` x `cells`, lit top-left.
    `palette` = (base/dark, body, tips/light)."""
    rng = random.Random(seed ^ 0xFACE)
    dark, body, light = palette[0], palette[1], palette[2]
    img = new_img()
    px = img.load()
    cs = S // cells
    for cy in range(cells):
        for cx in range(cells):
            flip = rng.random() < 0.5
            tone = rng.choice((body, body, mix(body, light, 0.3)))
            for y in range(cs):
                for x in range(cs):
                    u, v = (x, y) if not flip else (cs - 1 - x, y)
                    if u + v < cs - 1:
                        px[cx * cs + x, cy * cs + y] = mix(tone, light, 0.35)
                    elif u + v == cs - 1:
                        px[cx * cs + x, cy * cs + y] = light
                    else:
                        px[cx * cs + x, cy * cs + y] = mix(tone, dark, 0.35)
    if bev:
        bevel(img, light, dark)
    return img


def crystal_cluster(palette, seed, stage, *, max_stage=3):
    """Cross-model cluster on a transparent ground: 1 + stage tapered spikes (3 px wide at the
    foot, 1 px at the tip), lit on the left flank, with a sparkle at every tip. Heights grow with
    the stage so STAGE 0..3 read as small -> full cluster."""
    rng = random.Random(seed ^ (0xC7 + stage))
    dark, body, light = palette[0], palette[1], palette[2]
    img = new_img()
    px = img.load()
    n = min(4, 1 + stage)
    spikes = [(8, 0), (4, -1), (12, 1), (6, -2)][:n]
    hmax = [5, 8, 11, 14][min(stage, max_stage)]
    for i, (cx, lean) in enumerate(spikes):
        hh = max(3, hmax - i * 2)
        foot = S - 1
        for k in range(hh):
            y = foot - k
            t = k / float(hh)
            x = cx + int(round(lean * t))
            half = 1 if t < 0.65 else 0
            for dx in range(-half, half + 1):
                xx = x + dx
                if 0 <= xx < S:
                    px[xx, y] = light if dx < 0 else (body if dx == 0 else dark)
        tipx = cx + lean
        if 0 <= tipx < S:
            px[tipx, foot - hh + 1] = (255, 255, 255, 255)
    for x in range(2, 14):
        if rng.random() < 0.6:
            px[x, S - 1] = rng.choice((dark, body))
    return img


def glass_facets(edge, pane, sparkle, seed, *, alpha=150, tint=None, tint_t=0.0, frame=2):
    """Translucent pane with a `frame`-px edge, soft inner noise and a diagonal sparkle run."""
    rng = random.Random(seed ^ 0x61A5)
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            c = pane if rng.random() > 0.15 else mix(pane, edge, 0.25)
            if tint is not None:
                c = mix(c, tint, tint_t)
            px[x, y] = (c[0], c[1], c[2], alpha + rng.randint(-8, 8))
    for i in range(S):
        for f in range(frame):
            for (x, y) in ((i, f), (i, S - 1 - f), (f, i), (S - 1 - f, i)):
                px[x, y] = edge if f == 0 else mix(edge, pane, 0.4)
    for i in range(4, 12):
        if (i * 3 + 1) % 4 != 0:
            px[i, S - 1 - i] = sparkle
    px[5, 4] = sparkle
    px[11, 10] = sparkle
    return img


def strata_stripes(palette, seed, *, pattern="banded", rotation=0):
    """Sediment bands cycling through `palette`. `pattern`:
    thin   - 1-2 px laminae, every colour in turn (fine lamination);
    banded - 2-4 px beds (the default sandstone face);
    wide   - two broad 6-8 px beds with a single dark parting;
    cross  - banded beds sheared one pixel per row (cross-bedding / dune foresets).
    `rotation` 90 turns the bands vertical for an AXIS-rotated strata block."""
    rng = random.Random(seed ^ 0x57A7)
    img = new_img()
    px = img.load()
    if pattern == "thin":
        bands = [1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1]
        order = [0, 1, 3, 1, 0, 2, 1, 3, 0, 1, 2, 1]
    elif pattern == "wide":
        bands = [7, 1, 8]
        order = [0, 2, 1]
    elif pattern == "cross":
        bands = [3, 2, 3, 2, 3, 3]
        order = [0, 1, 3, 0, 2, 1]
    else:
        bands = [2, 3, 2, 2, 3, 2, 2]
        order = [0, 1, 3, 0, 2, 1, 3]
    y = 0
    i = 0
    while y < S:
        h = bands[i % len(bands)]
        col = palette[order[i % len(order)] % len(palette)]
        for yy in range(y, min(S, y + h)):
            for x in range(S):
                c = col
                if rng.random() < (0.18 if pattern != "wide" else 0.10):
                    c = mix(c, palette[(order[i % len(order)] + 1) % len(palette)], 0.45)
                if yy == y and h > 1:
                    c = mix(c, (255, 255, 255, 255), 0.08)
                if yy == min(S, y + h) - 1 and h > 2:
                    c = shade(c, 0.9)
                xx = (x + yy) % S if pattern == "cross" else x
                px[xx, yy] = c
        y += h
        i += 1
    if rotation == 90:
        img = img.rotate(90)
    return img


def plating(base, seam, rivet, seed, *, panels=2, accent=None):
    """Machined hull plate: `panels` x `panels` panels with seams and corner rivets."""
    rng = random.Random(seed ^ 0x9A7E)
    img = new_img()
    px = img.load()
    tones = [base, shade(base, 0.94), shade(base, 1.05)]
    for y in range(S):
        for x in range(S):
            px[x, y] = rng.choice(tones)
    step = S // panels
    for i in range(S):
        for p in range(panels + 1):
            k = min(S - 1, p * step)
            px[i, k] = seam
            px[k, i] = seam
    for p in range(panels):
        for q in range(panels):
            x0, y0 = p * step + 1, q * step + 1
            px[x0, y0] = rivet
            px[x0 + step - 3, y0 + step - 3] = rivet
            px[x0 + 1, y0 + 1] = shade(base, 1.12)
    if accent:
        px[S // 2, S // 2] = accent
        px[S // 2 - 1, S // 2 - 1] = accent
    return img


def lattice(bar, hi, seed, *, pitch=5):
    """Open truss lattice on a transparent ground (cutout render layer)."""
    rng = random.Random(seed ^ 0x1A77)
    img = new_img()
    px = img.load()
    for i in range(S):
        for (x, y) in ((i, 0), (i, S - 1), (0, i), (S - 1, i)):
            px[x, y] = bar
        px[i, i] = bar
        px[S - 1 - i, i] = bar
    for i in range(1, S - 1):
        for (x, y) in ((i, 1), (1, i)):
            px[x, y] = hi
    for i in range(2, 14, 2):
        px[i, i] = hi if rng.random() < 0.7 else bar
    return img


def layer_overlay(top_pal, side_pal, seed):
    """Snow-layer style: a fluffy top (noise in `top_pal`) and a banded side in `side_pal`."""
    top = noise_stone(top_pal, seed, grain=0.5, chunk=0.3)
    side = new_img()
    px = side.load()
    rng = random.Random(seed ^ 0x51DE)
    for y in range(S):
        for x in range(S):
            px[x, y] = side_pal[0] if (y // 2) % 2 == 0 else side_pal[1]
            if rng.random() < 0.1:
                px[x, y] = side_pal[2] if len(side_pal) > 2 else side_pal[0]
    return top, side


def footprint_variant(base_img, dark, seed):
    """Regolith FOOTPRINT state: two boot-shaped ovals pressed into the base in `dark`."""
    img = base_img.copy()
    px = img.load()
    rng = random.Random(seed ^ 0xF007)
    for (cx, cy, ang) in ((5.5, 7, 0.35), (10.5, 9.5, 0.35)):
        for y in range(S):
            for x in range(S):
                dx, dy = (x - cx), (y - cy)
                u = dx * math.cos(ang) + dy * math.sin(ang)
                v = -dx * math.sin(ang) + dy * math.cos(ang)
                if (u / 1.6) ** 2 + (v / 3.2) ** 2 <= 1.0:
                    px[x, y] = dark if rng.random() < 0.85 else mix(dark, base_img.getpixel((x, y)), 0.5)
    return img


# ========================================================================= animated families

def glow_cracks(base_img, glow_palette, seed, *, frames=8, frametime=3, amp=0.35, cracks=3,
                light_mask_colour=None):
    """Random-walk cracks in `glow_palette` (dark->hot) whose brightness breathes; each crack
    pixel carries a position-derived phase so the pulse ripples instead of blinking.
    Returns (frame strip, mcmeta dict, static frame 0)."""
    rng = random.Random(seed ^ 0xC2AC)
    base = base_img.copy()
    px = base.load()
    crack_px = {}
    for _ in range(cracks):
        x, y = rng.randrange(S), rng.randrange(S)
        dirx = rng.choice((-1, 1))
        for i in range(rng.randint(7, 12)):
            crack_px[(x % S, y % S)] = (i * 0.5 + x * 0.3) % (2 * math.pi)
            if rng.random() < 0.35:
                crack_px[((x + 1) % S, y % S)] = ((i + 1) * 0.5) % (2 * math.pi)
            x += dirx if rng.random() < 0.7 else 0
            y += rng.choice((-1, 1, 1))
    out = []
    for f in range(frames):
        fr = base.copy()
        fp = fr.load()
        for (x, y), ph in crack_px.items():
            t = 0.5 + 0.5 * math.sin(2 * math.pi * f / frames + ph)
            t = max(0.0, min(1.0, 0.5 + (t - 0.5) * (amp / 0.35)))
            fp[x, y] = ramp_pick(glow_palette, t)
        out.append(fr)
    return strip_of(out), mcmeta(frametime, interpolate=True), out[0]


def hue_shift_anim(base_img, vein_palette, hue_lo, hue_hi, seed, *, frames=16, frametime=4,
                   veins=3, amp=0.30):
    """Aurora stone: veins drawn in `vein_palette[0]` cycle their hue from `hue_lo` to `hue_hi`
    and back, with a value ripple of `amp`; the base stone is untouched."""
    rng = random.Random(seed ^ 0xA0A0)
    vein_px = {}
    for _ in range(veins):
        x, y = rng.randrange(S), rng.randrange(S)
        for i in range(rng.randint(8, 12)):
            vein_px[(x % S, y % S)] = i / 12.0
            x += rng.choice((1, 1, 0))
            y += rng.choice((-1, 0, 1))
    out = []
    for f in range(frames):
        fr = base_img.copy()
        fp = fr.load()
        cyc = 0.5 - 0.5 * math.cos(2 * math.pi * f / frames)
        for (x, y), off in vein_px.items():
            h = hue_lo + (hue_hi - hue_lo) * ((cyc + off) % 1.0)
            v = 0.75 + amp * 0.5 * math.sin(2 * math.pi * (f / frames + off))
            r, g, b = (int(c * 255) for c in colorsys.hsv_to_rgb(h, 0.55, min(1.0, v)))
            fp[x, y] = (r, g, b, 255)
        out.append(fr)
    return strip_of(out), mcmeta(frametime, interpolate=True), out[0]


def drip_anim(base_img, drip_palette, seed, *, frames=8, frametime=6, drips=2):
    """Amber sap: `drips` droplets slide down the face one pixel per frame and loop."""
    rng = random.Random(seed ^ 0xD21B)
    cols = [rng.randrange(2, 14) for _ in range(drips)]
    starts = [rng.randrange(0, 6) for _ in range(drips)]
    out = []
    for f in range(frames):
        fr = base_img.copy()
        fp = fr.load()
        for k, cx in enumerate(cols):
            y = (starts[k] + f * 2) % S
            fp[cx, y] = drip_palette[1]
            fp[cx, (y - 1) % S] = drip_palette[0]
            if y + 1 < S:
                fp[cx, y + 1] = drip_palette[2] if len(drip_palette) > 2 else drip_palette[1]
        out.append(fr)
    return strip_of(out), mcmeta(frametime), out[0]


def pulse_anim(base_img, *, frames=8, frametime=4, amp=0.25, hue=None, mask=None):
    """Brightness breathing on the accent pixels (saturated + brighter than median, optionally
    inside the `hue` window (lo, hi) in [0,1]) or on an explicit `mask` of (x, y). Frame 0 is the
    untouched base. Same contract as gen_animations.PULSE."""
    px0 = base_img.load()
    if mask is None:
        vals = sorted(max(px0[x, y][:3]) / 255 for y in range(S) for x in range(S) if px0[x, y][3])
        med = vals[len(vals) // 2] if vals else 0.5
        mask = []
        for y in range(S):
            for x in range(S):
                r, g, b, a = px0[x, y]
                if not a:
                    continue
                hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                if ss > 0.32 and vv > max(0.42, med * 1.05) and (hue is None or hue[0] <= hh <= hue[1]):
                    mask.append((x, y))
    out = []
    for f in range(frames):
        fr = base_img.copy()
        fp = fr.load()
        fac = 1.0 + amp * math.sin(2 * math.pi * f / frames)
        for xy in mask:
            _lit(fp, xy, fac)
        out.append(fr)
    return strip_of(out), mcmeta(frametime, interpolate=True), out[0]


def shimmer_anim(base_img, mask, sparkle, seed, *, frames=8, frametime=4, amp=0.25):
    """He-3 pocket: the masked pocket pixels breathe (`amp`) and a single sparkle walks across
    them one pixel per frame, so the pocket reads as a faint cyan shimmer, not a blink."""
    rng = random.Random(seed ^ 0x5411)
    order = list(mask)
    rng.shuffle(order)
    out = []
    for f in range(frames):
        fr = base_img.copy()
        fp = fr.load()
        for i, xy in enumerate(order):
            fac = 1.0 + amp * math.sin(2 * math.pi * (f / frames + (i % 5) / 5.0))
            _lit(fp, xy, fac)
        if order:
            fp[order[f % len(order)]] = sparkle
        out.append(fr)
    return strip_of(out), mcmeta(frametime, interpolate=True), out[0]


def frost_edge(base_img, frost, seed, *, frames=8, frametime=6, amp=0.20, depth=3):
    """Hull window FROSTED state: a rim of frost crystals `depth` px deep whose opacity breathes
    by `amp`; the pane centre is untouched. Frame 0 is the resting (most frosted) look."""
    rng = random.Random(seed ^ 0xF205)
    rim = {}
    for y in range(S):
        for x in range(S):
            d = min(x, y, S - 1 - x, S - 1 - y)
            if d < depth and rng.random() < (1.0 - d / depth) * 0.9:
                rim[(x, y)] = 0.5 + 0.5 * rng.random()
    out = []
    for f in range(frames):
        fr = base_img.copy()
        fp = fr.load()
        br = 1.0 - amp * 0.5 * (1 - math.cos(2 * math.pi * f / frames))
        for (x, y), w in rim.items():
            c = fp[x, y]
            t = min(1.0, w * br)
            m = mix(c, frost, t)
            fp[x, y] = (m[0], m[1], m[2], max(c[3], int(frost[3] * t)))
        out.append(fr)
    return strip_of(out), mcmeta(frametime, interpolate=True), out[0]


# ================================================================== composite block painters
# Each painter returns {output_name: Image | (strip, mcmeta, still)}. Output names are relative to
# textures/ ("block/<id>.png"). Keeping these as data-driven functions lets Stage 3 add a face
# variant by adding one dict key, not a new generator.

def _ore(spec):
    base = noise_stone(spec["base"], spec["seed"], grain=0.35, chunk=0.6)
    img = ore_blebs(base, spec["ore"], spec["seed"], count=spec.get("count", 4),
                    size=spec.get("size", 2.6), glint=spec.get("glint"))
    return {"block/%s.png" % spec["id"]: img}


def _noise(spec):
    img = noise_stone(spec["pal"], spec["seed"], grain=spec.get("grain", 0.35),
                      veins=spec.get("veins", 0), vein_colour=spec.get("vein_colour"),
                      chunk=spec.get("chunk", 0.6), bev=spec.get("bevel"))
    out = {"block/%s.png" % spec["id"]: img}
    if spec.get("footprint"):
        out["block/%s_footprint.png" % spec["id"]] = footprint_variant(img, spec["footprint"], spec["seed"])
    return out


def _polished(spec):
    """Storage-block bevel on a smooth two-tone face (polished basalt)."""
    rng = random.Random(spec["seed"])
    img = new_img()
    px = img.load()
    base, light = spec["pal"][0], spec["pal"][1]
    for y in range(S):
        for x in range(S):
            px[x, y] = base if rng.random() > 0.08 else mix(base, light, 0.4)
    for i in range(S):
        px[i, 8] = mix(base, light, 0.5)
        px[8, i] = mix(base, light, 0.5)
    bevel(img, light, shade(base, 0.7))
    return {"block/%s.png" % spec["id"]: img}


def _glass(spec):
    img = glass_facets(spec["edge"], spec["pane"], spec["sparkle"], spec["seed"],
                       alpha=spec.get("alpha", 150), tint=spec.get("tint"), tint_t=spec.get("tint_t", 0.0))
    return {"block/%s.png" % spec["id"]: img}


def _hull_window(spec):
    base = glass_facets(spec["edge"], spec["pane"], spec["sparkle"], spec["seed"], alpha=170,
                        tint=spec["tint"], tint_t=0.25)
    strip, meta, _ = frost_edge(base, spec["frost"], spec["seed"], frames=spec["frames"],
                                frametime=spec["frametime"], amp=spec["amp"])
    return {"block/%s.png" % spec["id"]: base,
            "block/%s_frosted.png" % spec["id"]: (strip, meta, base)}


def _crystal_block(spec):
    return {"block/%s.png" % spec["id"]: crystal_facets(spec["pal"], spec["seed"])}


def _crystal_cluster(spec):
    return {"block/%s_stage%d.png" % (spec["id"], st): crystal_cluster(spec["pal"], spec["seed"], st)
            for st in range(4)}


def _strata(spec):
    out = {}
    base = strata_stripes(spec["pal"], spec["seed"], pattern="banded")
    if spec.get("frames"):
        # Slow 2-tone shimmer of the band highlight: mask = top row of each band (the lit pixels).
        px = base.load()
        mask = [(x, y) for y in range(S) for x in range(S)
                if max(px[x, y][:3]) > max(spec["pal"][0][:3]) + 10]
        strip, meta, _ = pulse_anim(base, frames=spec["frames"], frametime=spec["frametime"],
                                    amp=spec["amp"], mask=mask)
        out["block/%s.png" % spec["id"]] = (strip, meta, base)
    else:
        out["block/%s.png" % spec["id"]] = base
    for i, pat in enumerate(("thin", "banded", "wide", "cross"), start=1):
        if spec.get("variants"):
            out["block/%s_strata_%d.png" % (spec["id"], i)] = strata_stripes(spec["pal"], spec["seed"] + i, pattern=pat)
    return out


def _crust(spec):
    """Salt/sulfur crust: base noise with crystalline veins + blebs of the crust colour."""
    base = noise_stone(spec["base"], spec["seed"], grain=0.3, veins=spec.get("veins", 3),
                       vein_colour=spec["crust"][1])
    img = ore_blebs(base, spec["crust"], spec["seed"], count=spec.get("count", 3), size=2.2, density=0.8)
    return {"block/%s.png" % spec["id"]: img}


def _cheese_ore(spec):
    base = noise_stone(spec["base"], spec["seed"], grain=0.35)
    img = holes(base, spec["holes"], spec["seed"], count=5, radius=1.9)
    return {"block/%s.png" % spec["id"]: img}


def _cheese_wheel(spec):
    rind, paste, hole = spec["rind"], spec["paste"], spec["hole"]
    rng = random.Random(spec["seed"])
    top = new_img()
    px = top.load()
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d <= 7.2:
                px[x, y] = paste if d < 6.2 else rind
    top = holes(top, (shade(hole, 0.85), hole, mix(hole, paste, 0.5)), spec["seed"], count=4, radius=1.3, rim=False)
    px = top.load()
    for y in range(S):
        for x in range(S):
            if px[x, y][3] and ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 > 7.2:
                px[x, y] = CLEAR
    side = new_img()
    sp = side.load()
    for y in range(S):
        for x in range(S):
            if 4 <= y < 12:
                sp[x, y] = rind if (y in (4, 11) or rng.random() < 0.1) else mix(rind, paste, 0.35)
    return {"block/%s_top.png" % spec["id"]: top, "block/%s_side.png" % spec["id"]: side}


def _column(spec):
    """Columnar basalt: vertical fluted side + hexagonal-ish top with a capped variant."""
    rng = random.Random(spec["seed"])
    base, dark, rim = spec["pal"]
    side = new_img()
    px = side.load()
    for y in range(S):
        for x in range(S):
            k = x % 4
            c = dark if k == 0 else (rim if k == 3 else base)
            if rng.random() < 0.08:
                c = mix(c, base, 0.5)
            px[x, y] = c
    top = noise_stone((base, dark, rim), spec["seed"] ^ 0x70, grain=0.2, chunk=0.4)
    tp = top.load()
    for i in range(S):
        tp[i, 0] = rim
        tp[0, i] = rim
        tp[i, S - 1] = dark
        tp[S - 1, i] = dark
    for (x, y) in ((5, 5), (10, 5), (5, 10), (10, 10)):
        tp[x, y] = dark
    out = {"block/%s_side.png" % spec["id"]: side, "block/%s_top.png" % spec["id"]: top}
    if spec.get("capped"):
        cap = side.copy()
        cp = cap.load()
        for x in range(S):
            cp[x, 0] = rim
            cp[x, 1] = mix(rim, base, 0.5)
            cp[x, 2] = dark
        out["block/%s_capped.png" % spec["id"]] = cap
    return out


def _pit(spec):
    base = noise_stone(spec["base"], spec["seed"], grain=0.35)
    img = base.copy()
    px = img.load()
    centre, ring = spec["centre"], spec["ring"]
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.0:
                px[x, y] = centre
            elif d < 4.5:
                px[x, y] = ring if (int(d * 2) + x) % 2 else mix(ring, centre, 0.5)
            elif d < 6.5 and (x + y) % 3 == 0:
                px[x, y] = ring
    return {"block/%s.png" % spec["id"]: img}


def _glow_crack_block(spec):
    base = noise_stone(spec["base"], spec["seed"], grain=0.35)
    strip, meta, still = glow_cracks(base, spec["glow"], spec["seed"], frames=spec["frames"],
                                     frametime=spec["frametime"], amp=spec["amp"], cracks=spec.get("cracks", 3))
    return {"block/%s.png" % spec["id"]: (strip, meta, still)}


def _aurora(spec):
    base = noise_stone(spec["base"], spec["seed"], grain=0.2, chunk=0.7)
    strip, meta, still = hue_shift_anim(base, spec["vein"], spec["hue"][0], spec["hue"][1], spec["seed"],
                                        frames=spec["frames"], frametime=spec["frametime"], amp=spec["amp"])
    return {"block/%s.png" % spec["id"]: (strip, meta, still)}


def _dawnband(spec):
    base = noise_stone(spec["base"], spec["seed"], grain=0.3, veins=4, vein_colour=spec["vein"][0])
    px = base.load()
    rng = random.Random(spec["seed"] ^ 0xDA)
    for y in range(S):
        for x in range(S):
            if px[x, y] == spec["vein"][0] and rng.random() < 0.35:
                px[x, y] = spec["vein"][1]
    strip, meta, still = pulse_anim(base, frames=spec["frames"], frametime=spec["frametime"],
                                    amp=spec["amp"], hue=spec["hue"])
    return {"block/%s.png" % spec["id"]: (strip, meta, still)}


def _he3(spec):
    base = noise_stone(spec["base"], spec["seed"], grain=0.35)
    img = ore_blebs(base, spec["ore"], spec["seed"], count=3, size=2.4, density=0.9)
    px = img.load()
    mask = [(x, y) for y in range(S) for x in range(S) if px[x, y] in spec["ore"]]
    strip, meta, still = shimmer_anim(img, mask, spec["sparkle"], spec["seed"], frames=spec["frames"],
                                      frametime=spec["frametime"], amp=spec["amp"])
    return {"block/%s.png" % spec["id"]: (strip, meta, still)}


def _drip_ore(spec):
    base = _log_side(spec["bark"], spec["seed"])
    img = ore_blebs(base, spec["ore"], spec["seed"], count=3, size=2.2, density=0.9)
    strip, meta, still = drip_anim(img, spec["ore"], spec["seed"], frames=spec["frames"], frametime=spec["frametime"])
    return {"block/%s.png" % spec["id"]: (strip, meta, still)}


def _log_side(pal, seed, flecks=None):
    rng = random.Random(seed ^ 0x106)
    bark, ring = pal[0], pal[1]
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            k = (x + (y // 5)) % 5
            c = shade(bark, 0.8) if k == 0 else (ring if k == 3 else bark)
            if rng.random() < 0.1:
                c = mix(c, ring, 0.5)
            px[x, y] = c
    if flecks:
        for _ in range(5):
            px[rng.randrange(S), rng.randrange(S)] = flecks
    return img


def _log(spec):
    bark, ring = spec["pal"][0], spec["pal"][1]
    side = _log_side(spec["pal"], spec["seed"], spec.get("flecks"))
    top = new_img()
    px = top.load()
    for y in range(S):
        for x in range(S):
            d = max(abs(x - 7.5), abs(y - 7.5))
            px[x, y] = bark if d > 6.5 else (ring if int(d) % 2 == 0 else shade(ring, 0.85))
    if spec.get("flecks"):
        px[7, 7] = spec["flecks"]
        px[8, 8] = spec["flecks"]
    return {"block/%s.png" % spec["id"]: side, "block/%s_top.png" % spec["id"]: top}


def _carpet(spec):
    """Spore moss carpet: moss noise with glow dots that breathe."""
    base = noise_stone(spec["pal"], spec["seed"], grain=0.4, chunk=0.4)
    px = base.load()
    rng = random.Random(spec["seed"] ^ 0xCA)
    dots = []
    for _ in range(7):
        x, y = rng.randrange(S), rng.randrange(S)
        px[x, y] = spec["glow"]
        dots.append((x, y))
    strip, meta, still = pulse_anim(base, frames=spec["frames"], frametime=spec["frametime"], amp=spec["amp"], mask=dots)
    return {"block/%s.png" % spec["id"]: (strip, meta, still)}


def _fungus(spec):
    cap, gill, stem = spec["cap"], spec["gill"], spec["stem"]
    img = new_img()
    px = img.load()
    for y in range(3, 8):
        half = {3: 2, 4: 4, 5: 5, 6: 6, 7: 6}[y]
        for x in range(8 - half, 8 + half):
            px[x, y] = cap if y < 7 else gill
    for (x, y) in ((6, 4), (9, 5), (5, 6)):
        px[x, y] = mix(cap, (255, 255, 255, 255), 0.5)
    for y in range(8, 14):
        for x in range(7, 9):
            px[x, y] = stem
    px[6, 13] = stem
    px[9, 13] = stem
    return {"block/%s.png" % spec["id"]: img}


def _vine(spec):
    rng = random.Random(spec["seed"])
    base, tip, bud = spec["pal"]
    body = new_img()
    px = body.load()
    for x in (4, 9, 12):
        for y in range(S):
            if rng.random() < 0.9:
                px[x, y] = base if rng.random() < 0.7 else tip
        for _ in range(2):
            px[min(S - 1, x + rng.choice((-1, 1))), rng.randrange(S)] = bud
    tipimg = body.copy()
    tp = tipimg.load()
    for x in (4, 9, 12):
        for y in range(10, S):
            tp[x, y] = CLEAR if y > 12 else tip
        tp[x, 12] = bud
    return {"block/%s.png" % spec["id"]: body, "block/%s_tip.png" % spec["id"]: tipimg}


def _pod(spec):
    pod, stem, seam = spec["pal"]
    img = new_img()
    px = img.load()
    for y in range(4, 12):
        half = {4: 2, 5: 3, 6: 4, 7: 4, 8: 4, 9: 4, 10: 3, 11: 2}[y]
        for x in range(8 - half, 8 + half):
            px[x, y] = pod if x < 8 else shade(pod, 0.8)
        px[8, y] = seam if y % 2 else mix(pod, seam, 0.5)
    px[6, 5] = mix(pod, (255, 255, 255, 255), 0.5)
    for y in range(12, S):
        px[7, y] = stem
        px[8, y] = shade(stem, 0.8)
    return {"block/%s.png" % spec["id"]: img}


def _multiface(spec):
    """Glow-lichen style face: sparse lichen growth on a transparent ground."""
    rng = random.Random(spec["seed"])
    base, edge, spore = spec["pal"]
    img = new_img()
    px = img.load()
    field = _smooth_field(rng, 0.7)
    for y in range(S):
        for x in range(S):
            if field[y][x] < 0.55:
                px[x, y] = base if field[y][x] > 0.2 else edge
    for _ in range(6):
        px[rng.randrange(S), rng.randrange(S)] = spore
    return {"block/%s.png" % spec["id"]: img}


def _vent(spec):
    rim, throat, steam = spec["pal"]
    top = noise_stone((rim, shade(rim, 0.75), mix(rim, throat, 0.3)), spec["seed"], grain=0.3)
    px = top.load()
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.2:
                px[x, y] = shade(throat, 0.45)
            elif d < 3.6:
                px[x, y] = throat
            elif d < 4.6:
                px[x, y] = steam if (x + y) % 2 else mix(throat, rim, 0.5)
    side = noise_stone((rim, shade(rim, 0.75), mix(rim, throat, 0.3)), spec["seed"] ^ 0x5, grain=0.3, veins=2, vein_colour=throat)
    return {"block/%s_top.png" % spec["id"]: top, "block/%s_side.png" % spec["id"]: side}


def _layer(spec):
    top, side = layer_overlay(spec["top"], spec["side"], spec["seed"])
    return {"block/%s.png" % spec["id"]: top, "block/%s_side.png" % spec["id"]: side}


def _soil(spec):
    img = noise_stone(spec["pal"], spec["seed"], grain=spec.get("grain", 0.35), chunk=spec.get("chunk", 0.5))
    px = img.load()
    rng = random.Random(spec["seed"] ^ 0x501)
    for _ in range(6):
        px[rng.randrange(S), rng.randrange(S)] = spec["fleck"]
    return {"block/%s.png" % spec["id"]: img}


def _plating(spec):
    img = plating(spec["base"], spec["seam"], spec["rivet"], spec["seed"], panels=2)
    return {"block/%s.png" % spec["id"]: img}


def _truss(spec):
    return {"block/%s.png" % spec["id"]: lattice(spec["bar"], spec["hi"], spec["seed"])}


def _wing(spec):
    rng = random.Random(spec["seed"])
    cell, grid, glint = spec["cell"], spec["grid"], spec["glint"]
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            px[x, y] = grid if (x % 4 == 0 or y % 4 == 0) else cell
            if x % 4 == 1 and y % 4 == 1:
                px[x, y] = mix(cell, glint, 0.6)
            elif rng.random() < 0.05 and px[x, y] == cell:
                px[x, y] = mix(cell, glint, 0.3)
    for i in range(3, 13):
        if i % 3 == 0:
            px[i, S - 1 - i] = glint
    side = plating(spec["grid"], shade(spec["grid"], 0.7), glint, spec["seed"] ^ 0x1, panels=1)
    return {"block/%s.png" % spec["id"]: img, "block/%s_side.png" % spec["id"]: side}


def _ring(spec):
    """Docking ring: hull plating with hazard stripes + a teal light that pulses (centre tile),
    a plain-plating part tile and a striped edge tile for the 3x3 PART ring."""
    base = plating(spec["base"], spec["seam"], spec["rivet"], spec["seed"], panels=2)
    centre = base.copy()
    px = centre.load()
    a, b = spec["stripe"]
    for y in range(S):
        for x in range(S):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if 5.5 < d < 7.6:
                px[x, y] = a if ((x + y) // 2) % 2 == 0 else b
            elif d < 2.0:
                px[x, y] = spec["light"]
            elif d < 3.0:
                px[x, y] = mix(spec["light"], spec["base"], 0.6)
    strip, meta, still = pulse_anim(centre, frames=spec["frames"], frametime=spec["frametime"], amp=spec["amp"], hue=spec["hue"])
    edge = base.copy()
    ep = edge.load()
    for y in range(S):
        for x in range(S):
            if y < 3:
                ep[x, y] = a if ((x + y) // 2) % 2 == 0 else b
    return {"block/%s.png" % spec["id"]: (strip, meta, still),
            "block/%s_part.png" % spec["id"]: base,
            "block/%s_edge.png" % spec["id"]: edge}


def _plate(spec):
    base = plating(spec["base"], shade(spec["base"], 0.7), mix(spec["base"], spec["core"], 0.3), spec["seed"], panels=1)
    px = base.load()
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.5:
                px[x, y] = spec["core"]
            elif d < 4.0:
                px[x, y] = mix(spec["core"], spec["ring"], 0.5)
            elif d < 5.2:
                px[x, y] = spec["ring"]
    strip, meta, still = pulse_anim(base, frames=spec["frames"], frametime=spec["frametime"], amp=spec["amp"], hue=spec["hue"])
    return {"block/%s.png" % spec["id"]: (strip, meta, still)}


def _tray(spec):
    frame, bed, water = spec["frame"], spec["bed"], spec["water"]
    top = noise_stone((bed, shade(bed, 0.75), mix(bed, (255, 255, 255, 255), 0.25)), spec["seed"], grain=0.4, chunk=0.4)
    px = top.load()
    for i in range(S):
        for (x, y) in ((i, 0), (i, S - 1), (0, i), (S - 1, i)):
            px[x, y] = frame
    for x in range(2, 14):
        px[x, 2] = water if x % 2 else mix(water, bed, 0.5)
    side = plating(frame, shade(frame, 0.7), mix(frame, water, 0.5), spec["seed"] ^ 0x2, panels=1)
    sp = side.load()
    for x in range(1, 15):
        sp[x, 3] = water
    return {"block/%s_top.png" % spec["id"]: top, "block/%s_side.png" % spec["id"]: side}


def _telescope(spec):
    base = plating(spec["base"], spec["seam"], spec["rivet"], spec["seed"], panels=2)
    px = base.load()
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.0:
                px[x, y] = spec["lens"]
            elif d < 3.4:
                px[x, y] = shade(spec["lens"], 0.6)
            elif d < 4.4:
                px[x, y] = spec["seam"]
    px[6, 6] = (255, 255, 255, 255)
    top = plating(spec["base"], spec["seam"], spec["rivet"], spec["seed"] ^ 0x7, panels=1)
    tp = top.load()
    for i in range(3, 13):
        tp[i, i] = spec["lens"]
        tp[i, S - 1 - i] = spec["seam"]
    return {"block/%s.png" % spec["id"]: base, "block/%s_top.png" % spec["id"]: top}


# --------------------------------------------------------------------------------- item painters

def _item_lump(spec):
    """Raw ore lump (same silhouette as raw_nerosium so the raw items line up in a tab)."""
    rng = random.Random(spec["seed"])
    pal = spec["pal"]
    img = new_img()
    px = img.load()
    shape = [(6, 4), (7, 4), (8, 4), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5)]
    shape += [(x, y) for y in (6, 7, 8) for x in range(4, 12)]
    shape += [(x, 9) for x in range(5, 12)] + [(x, 10) for x in range(5, 11)] + [(x, 11) for x in range(6, 10)]
    for (x, y) in shape:
        px[x, y] = rng.choice(pal[:3])
    for (x, y) in shape:
        if (x <= 5 or y <= 5) and rng.random() < 0.5:
            px[x, y] = pal[-1]
        if (x >= 10 or y >= 10) and rng.random() < 0.5:
            px[x, y] = pal[0]
    return {"item/%s.png" % spec["id"]: img}


def _item_ingot(spec):
    light, mid, dark = spec["pal"]
    img = new_img()
    px = img.load()
    rows = {5: (4, 12), 6: (3, 13), 7: (3, 13), 8: (3, 13), 9: (3, 13), 10: (4, 12)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1):
            if y == 5 or x == x0:
                px[x, y] = light
            elif y == 10 or x == x1 - 1:
                px[x, y] = dark
            else:
                px[x, y] = mid if (x + y) % 3 else mix(mid, light, 0.3)
    for (rx, ry) in ((5, 6), (6, 7), (7, 8)):
        px[rx, ry] = mix(px[rx, ry], (255, 255, 255, 255), 0.35)
    for x in range(5, 13):
        px[x, 11] = (20, 20, 26, 140)
    return {"item/%s.png" % spec["id"]: img}


def _item_gem(spec):
    """Faceted shard / crystal (the family diamond with a crown sparkle), `pal` = (dark, mid, light)."""
    dark, mid, light = spec["pal"]
    img = new_img()
    px = img.load()
    shape = {3: (7, 9), 4: (6, 10), 5: (5, 11), 6: (4, 12), 7: (4, 12), 8: (4, 12), 9: (5, 11),
             10: (6, 10), 11: (7, 9), 12: (8, 8)}
    if spec.get("shard"):
        shape = {3: (8, 9), 4: (7, 10), 5: (6, 10), 6: (6, 11), 7: (5, 11), 8: (5, 11), 9: (5, 10),
                 10: (6, 10), 11: (6, 9), 12: (7, 8)}
    for y, (x0, x1) in shape.items():
        m = (x0 + x1) / 2
        for x in range(x0, x1):
            px[x, y] = light if abs(x - m) < 1 else (mid if x < m else dark)
        if y % 2 == 0 and x0 < m - 1:
            px[int(m) - 1, y] = light
    px[8, 3] = (255, 255, 255, 255)
    return {"item/%s.png" % spec["id"]: img}


def _item_dust(spec):
    rng = random.Random(spec["seed"])
    img = new_img()
    px = img.load()
    for _ in range(70):
        x, y = rng.randint(3, 12), rng.randint(6, 13)
        if rng.random() < (y - 4) / 12:
            px[x, y] = rng.choice(spec["pal"])
    for (sx, sy) in ((5, 4), (10, 5), (8, 3)):
        px[sx, sy] = spec["pal"][-1]
    return {"item/%s.png" % spec["id"]: img}


def _item_fibre(spec):
    """A coiled fibre hank: three looped strands in `pal` (dark, mid, light)."""
    dark, mid, light = spec["pal"]
    img = new_img()
    px = img.load()
    for k, (cx, cy) in enumerate(((6, 7), (9, 8), (7, 10))):
        for a in range(24):
            ang = a / 24 * 2 * math.pi
            x, y = int(round(cx + 3.2 * math.cos(ang))), int(round(cy + 2.4 * math.sin(ang)))
            if 0 <= x < S and 0 <= y < S:
                px[x, y] = light if ang < math.pi / 2 else (mid if ang < math.pi * 1.4 else dark)
    for x in range(6, 10):
        px[x, 5] = mid
    return {"item/%s.png" % spec["id"]: img}


def _item_canister(spec):
    body, band, gas = spec["pal"]
    img = new_img()
    px = img.load()
    for y in range(3, 14):
        for x in range(5, 11):
            px[x, y] = body if x > 5 else mix(body, (255, 255, 255, 255), 0.3)
            if y in (3, 13):
                px[x, y] = shade(body, 0.6)
    for x in range(6, 10):
        px[x, 2] = band
        px[x, 6] = band
        px[x, 10] = band
    for y in range(7, 10):
        for x in range(6, 10):
            px[x, y] = gas if (x + y) % 2 else mix(gas, body, 0.4)
    px[7, 1] = band
    px[8, 1] = band
    return {"item/%s.png" % spec["id"]: img}


def _item_lens(spec):
    rim, glass, glint = spec["pal"]
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 4.0:
                px[x, y] = glass if d > 1.5 else mix(glass, glint, 0.5)
            elif d < 5.2:
                px[x, y] = rim
    for i in range(5, 9):
        px[i, 12 - i] = glint
    return {"item/%s.png" % spec["id"]: img}


def _item_bomb(spec):
    body, seam, fuse = spec["pal"]
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = ((x - 8) ** 2 + (y - 9) ** 2) ** 0.5
            if d < 4.5:
                px[x, y] = body if x < 8 else shade(body, 0.75)
                if abs(x - 8) < 1:
                    px[x, y] = seam
    px[6, 6] = mix(body, (255, 255, 255, 255), 0.5)
    for (x, y) in ((8, 4), (9, 3), (10, 2)):
        px[x, y] = fuse
    px[11, 1] = (255, 200, 90, 255)
    return {"item/%s.png" % spec["id"]: img}


def _item_wedge(spec):
    """Moon Cheese wedge: paste with holes and a rind on the cut face."""
    rind, paste, hole = spec["pal"]
    img = new_img()
    px = img.load()
    for y in range(3, 13):
        x0 = 3 + (12 - y)
        for x in range(x0, 13):
            px[x, y] = paste if y < 12 and x < 12 else rind
        px[x0, y] = mix(paste, (255, 255, 255, 255), 0.4)
    for (x, y) in ((8, 7), (10, 10), (6, 11), (10, 6)):
        px[x, y] = hole
        px[x + 1, y] = shade(hole, 0.85)
    return {"item/%s.png" % spec["id"]: img}


def _item_pigment(spec):
    """Rust pigment: a small pile with a smear line (dye-like silhouette)."""
    rng = random.Random(spec["seed"])
    dark, mid, light = spec["pal"]
    img = new_img()
    px = img.load()
    for y in range(7, 13):
        half = (y - 5)
        for x in range(8 - half, 8 + half):
            px[x, y] = rng.choice((dark, mid, mid, light)) if y < 12 else dark
    for x in range(3, 13):
        px[x, 4 + (x % 3 == 0)] = light if x % 2 else mid
    return {"item/%s.png" % spec["id"]: img}


FAMILIES = {
    "ore": _ore, "noise": _noise, "polished": _polished, "glass": _glass, "hull_window": _hull_window,
    "crystal_block": _crystal_block, "crystal_cluster": _crystal_cluster, "strata": _strata,
    "crust": _crust, "cheese_ore": _cheese_ore, "cheese_wheel": _cheese_wheel, "column": _column,
    "pit": _pit, "glow_cracks": _glow_crack_block, "aurora": _aurora, "dawnband": _dawnband,
    "he3": _he3, "drip_ore": _drip_ore, "log": _log, "carpet": _carpet, "fungus": _fungus,
    "vine": _vine, "pod": _pod, "multiface": _multiface, "vent": _vent, "layer": _layer,
    "soil": _soil, "plating": _plating, "truss": _truss, "wing": _wing, "ring": _ring,
    "plate": _plate, "tray": _tray, "telescope": _telescope,
    "item_lump": _item_lump, "item_ingot": _item_ingot, "item_gem": _item_gem, "item_dust": _item_dust,
    "item_fibre": _item_fibre, "item_canister": _item_canister, "item_lens": _item_lens,
    "item_bomb": _item_bomb, "item_wedge": _item_wedge, "item_pigment": _item_pigment,
}


# ================================================================================ palettes
# Every hex below is a concept-sheet palette input for the planets rework (one palette per world).
# Intentional additions (none change a listed hex) are noted inline and in art-preview NOTES.md.

LUNA_REGOLITH = (hexc("#9A9A94"), hexc("#7D7D78"), hexc("#C9C9C2"))
LUNA_BASALT = (hexc("#4A4B50"), hexc("#3A3B40"), hexc("#5C5D63"))
LUNA_ANORTH = (hexc("#C9C9C2"), hexc("#B3B3AC"), hexc("#E0E0D8"))
MARS_SAND = (hexc("#B5532A"), hexc("#9A4322"), hexc("#C8683D"))
MARS_BANDS = (hexc("#B5532A"), hexc("#C8683D"), hexc("#6B2F1E"), hexc("#D9A066"))
CIND_ASH = (hexc("#3B3237"), hexc("#2B2328"), hexc("#4E434A"))
GLAC_ICE = (hexc("#A9E3FF"), hexc("#6CC2EF"), hexc("#F2FBFF"))
GLAC_PERMA = (hexc("#2D6F9E"), hexc("#1F4F73"), hexc("#6CC2EF"))
XERTZ_SOIL = (hexc("#4A3B2F"), hexc("#3A2D24"), hexc("#5C4A3B"))  # third tone = base lightened; moss flecks #3FBF6F separately
XERTZ_BARK = (hexc("#4A3B2F"), hexc("#6B5745"))
NYX_STONE = (hexc("#2B2440"), hexc("#0D0A14"), hexc("#5B2D8E"))
STATION_HULL = hexc("#8D949B")
STATION_SEAM = hexc("#5A6168")
STATION_TEAL = hexc("#2EC4B6")


# ============================================================================= the table
# One row per id. "planet" groups the preview sheets; "family" picks the painter; the rest are
# that painter's inputs. Seeds come from the id unless overridden. Animated rows carry
# frames/frametime/amp/hue per the block spec (frametime in ticks).

def _row(planet, id_, family, **kw):
    kw.update(id=id_, planet=planet, family=family, seed=kw.get("seed", seed_of(id_)))
    return kw


PLANET_BLOCK_TEXTURES = [
    # ---------------------------------------------------------------- Luna
    _row("luna", "regolith", "noise", pal=LUNA_REGOLITH, grain=0.4, chunk=0.35, footprint=hexc("#7D7D78")),
    _row("luna", "lunar_basalt", "noise", pal=LUNA_BASALT, grain=0.3, chunk=0.7, veins=2, vein_colour=hexc("#3A3B40")),
    _row("luna", "polished_lunar_basalt", "polished", pal=(hexc("#4A4B50"), hexc("#5C5D63"))),
    _row("luna", "anorthosite", "noise", pal=LUNA_ANORTH, grain=0.45, chunk=0.4),
    _row("luna", "crater_glass", "glass", edge=hexc("#D9D4C4"), pane=hexc("#F2EFE4"), sparkle=hexc("#FFFFFF"), alpha=140),
    _row("luna", "ilmenite_ore", "ore", base=LUNA_REGOLITH, ore=(hexc("#2F2F36"), hexc("#4D4D58"), hexc("#6E6E7C")), glint=hexc("#9A9AAE")),
    _row("luna", "helium3_pocket", "he3", base=LUNA_REGOLITH, ore=(hexc("#7FF3FF"), hexc("#B9FAFF")), sparkle=hexc("#FFFFFF"),
         frames=8, frametime=4, amp=0.25, hue=(0.48, 0.55)),
    _row("luna", "cheese_ore", "cheese_ore", base=LUNA_REGOLITH, holes=(hexc("#D9A528"), hexc("#F5C242"), hexc("#FFE27A"))),
    _row("luna", "cheese_wheel", "cheese_wheel", rind=hexc("#D9A528"), paste=hexc("#F5C242"), hole=hexc("#C68E1E")),
    _row("luna", "raw_ilmenite", "item_lump", pal=(hexc("#2F2F36"), hexc("#4D4D58"), hexc("#3A3A44"), hexc("#8A8A9C"))),
    _row("luna", "titanium_ingot", "item_ingot", pal=(hexc("#E6E9EE"), hexc("#A7AEB8"), hexc("#5A6168"))),
    _row("luna", "helium3_canister", "item_canister", pal=(hexc("#8D949B"), hexc("#5A6168"), hexc("#7FF3FF"))),
    _row("luna", "moon_cheese", "item_wedge", pal=(hexc("#D9A528"), hexc("#F5C242"), hexc("#C68E1E"))),
    # ---------------------------------------------------------------- Mars
    _row("mars", "red_regolith", "noise", pal=MARS_SAND, grain=0.4, chunk=0.45),
    _row("mars", "martian_sandstone", "strata", pal=MARS_BANDS, frames=4, frametime=20, amp=0.10, variants=True),
    _row("mars", "hematite_ore", "ore", base=MARS_SAND, ore=(hexc("#4A2016"), hexc("#6B2F1E"), hexc("#8C3C2A")), glint=hexc("#D9A066")),
    _row("mars", "perchlorate_crust", "crust", base=MARS_SAND, crust=(hexc("#C9D0D6"), hexc("#E8ECEF"), hexc("#F5F7F9")), veins=3, count=3),
    _row("mars", "dry_ice", "glass", edge=hexc("#C9D6DD"), pane=hexc("#E8ECEF"), sparkle=hexc("#FFFFFF"), alpha=235, frame=1),
    _row("mars", "basalt_column", "column", pal=(hexc("#6B2F1E"), hexc("#4A2016"), hexc("#8C3C2A")), capped=True),
    _row("mars", "dust_devil_pit", "pit", base=MARS_SAND, centre=hexc("#7A4A2E"), ring=hexc("#9A4322")),
    _row("mars", "raw_hematite", "item_lump", pal=(hexc("#6B2F1E"), hexc("#8C3C2A"), hexc("#4A2016"), hexc("#C8683D"))),
    _row("mars", "rust_pigment", "item_pigment", pal=(hexc("#6B2F1E"), hexc("#B5532A"), hexc("#C8683D"))),
    _row("mars", "perchlorate_oxidiser", "item_gem", pal=(hexc("#C9D6DD"), hexc("#E8ECEF"), hexc("#F5F7F9")), shard=True),
    # ---------------------------------------------------------------- Cindara
    _row("cindara", "ashstone", "noise", pal=CIND_ASH, grain=0.4, chunk=0.45),
    _row("cindara", "ember_cracked_ashstone", "glow_cracks", base=CIND_ASH,
         glow=(hexc("#D94E1F"), hexc("#FF5A1F"), hexc("#FF8A4A"), hexc("#FFB347")), frames=8, frametime=3, amp=0.35, cracks=3),
    _row("cindara", "sulfur_crust", "crust", base=CIND_ASH, crust=(hexc("#C9A731"), hexc("#E8C547"), hexc("#F5DC7A")), veins=2, count=4),
    _row("cindara", "magma_glass", "glass", edge=hexc("#1A1420"), pane=hexc("#FF5A1F"), sparkle=hexc("#FFB347"), alpha=165),
    _row("cindara", "ash_layer", "layer", top=(hexc("#3B3237"), hexc("#2B2328"), hexc("#4E434A")), side=(hexc("#2B2328"), hexc("#3B3237"), hexc("#4E434A"))),
    _row("cindara", "basalt_spire", "column", pal=(hexc("#1A1420"), hexc("#3B3237"), hexc("#4E434A"))),
    _row("cindara", "ash_dust", "item_dust", pal=(hexc("#2B2328"), hexc("#3B3237"), hexc("#4E434A"), hexc("#6A5C64"))),
    _row("cindara", "sulfur", "item_dust", pal=(hexc("#C9A731"), hexc("#E8C547"), hexc("#E8C547"), hexc("#F5DC7A"))),
    # ---------------------------------------------------------------- Glacira
    _row("glacira", "packed_cryo_ice", "crystal_block", pal=(hexc("#6CC2EF"), hexc("#A9E3FF"), hexc("#F2FBFF"))),
    _row("glacira", "frost_crystal_cluster", "crystal_cluster", pal=(hexc("#6CC2EF"), hexc("#A9E3FF"), hexc("#F2FBFF"))),
    _row("glacira", "methane_ice", "noise", pal=(hexc("#A9E3FF"), hexc("#8AC8EC"), hexc("#D6F1FF")), grain=0.3, chunk=0.5, veins=4, vein_colour=hexc("#6B4AA3")),
    _row("glacira", "permafrost", "noise", pal=GLAC_PERMA, grain=0.5, chunk=0.35),
    _row("glacira", "aurora_stone", "aurora", base=(hexc("#2D6F9E"), hexc("#1F4F73"), hexc("#3F86B5")), vein=(hexc("#60E0A0"),), hue=(0.35, 0.90), frames=16, frametime=4, amp=0.30),
    _row("glacira", "frozen_geyser", "vent", pal=(hexc("#2D6F9E"), hexc("#A9E3FF"), hexc("#F2FBFF"))),
    _row("glacira", "frost_shard", "item_gem", pal=(hexc("#6CC2EF"), hexc("#A9E3FF"), hexc("#F2FBFF")), shard=True),
    _row("glacira", "methane_crystal", "item_gem", pal=(hexc("#6B4AA3"), hexc("#8A6AC2"), hexc("#C9B6EC"))),
    # ---------------------------------------------------------------- Greenxertz
    _row("greenxertz", "xertz_soil", "soil", pal=XERTZ_SOIL, grain=0.45, chunk=0.35, fleck=hexc("#3FBF6F")),
    _row("greenxertz", "spore_moss", "carpet", pal=(hexc("#3FBF6F"), hexc("#1D4D2B"), hexc("#5ED88A")), glow=hexc("#A6FF4D"), frames=6, frametime=5, amp=0.20),
    _row("greenxertz", "glowcap_fungus", "fungus", cap=hexc("#A6FF4D"), gill=hexc("#3FBF6F"), stem=hexc("#4A3B2F")),
    _row("greenxertz", "glowcap_block", "noise", pal=(hexc("#A6FF4D"), hexc("#7FD63A"), hexc("#C8FF8A")), grain=0.3, chunk=0.5, bevel=(hexc("#C8FF8A"), hexc("#7FD63A"))),
    _row("greenxertz", "bio_bark_log", "log", pal=XERTZ_BARK, flecks=hexc("#FF9A2E")),
    _row("greenxertz", "stripped_bio_bark_log", "log", pal=(hexc("#6B5745"), hexc("#8A745D"))),
    _row("greenxertz", "xertz_vine", "vine", pal=(hexc("#1D4D2B"), hexc("#3FBF6F"), hexc("#A6FF4D"))),
    _row("greenxertz", "amber_sap_ore", "drip_ore", bark=XERTZ_BARK, ore=(hexc("#FF9A2E"), hexc("#FFC070"), hexc("#FFE0B0")), frames=8, frametime=6, amp=0.15, hue=(0.06, 0.11)),
    _row("greenxertz", "spore_pod", "pod", pal=(hexc("#FF7AC8"), hexc("#1D4D2B"), hexc("#A6FF4D"))),
    _row("greenxertz", "amber_sap", "item_gem", pal=(hexc("#D97A1A"), hexc("#FF9A2E"), hexc("#FFC070"))),
    _row("greenxertz", "xertz_fibre", "item_fibre", pal=(hexc("#1D4D2B"), hexc("#3FBF6F"), hexc("#A6FF4D"))),
    _row("greenxertz", "spore_bomb", "item_bomb", pal=(hexc("#FF7AC8"), hexc("#1D4D2B"), hexc("#A6FF4D"))),
    # ---------------------------------------------------------------- Nyxara
    _row("nyxara", "nyx_stone", "noise", pal=NYX_STONE, grain=0.3, chunk=0.4),
    _row("nyxara", "void_crystal", "crystal_block", pal=(hexc("#5B2D8E"), hexc("#9B5DE5"), hexc("#C77DFF"))),
    _row("nyxara", "dawnband_stone", "dawnband", base=NYX_STONE, vein=(hexc("#FFB86B"), hexc("#FFD39A")), frames=8, frametime=4, amp=0.30, hue=(0.08, 0.13)),
    _row("nyxara", "shadow_lichen", "multiface", pal=(hexc("#0D0A14"), hexc("#2B2440"), hexc("#5B2D8E"))),
    _row("nyxara", "voidglass_ore", "ore", base=NYX_STONE, ore=(hexc("#4DD0E1"), hexc("#C77DFF"), hexc("#E6D2FF")), glint=hexc("#FFFFFF"), count=3),
    _row("nyxara", "gravity_shard_block", "crystal_block", pal=(hexc("#9B5DE5"), hexc("#C77DFF"), hexc("#4DD0E1"))),
    _row("nyxara", "voidglass", "item_gem", pal=(hexc("#2B2440"), hexc("#4DD0E1"), hexc("#C77DFF")), shard=True),
    _row("nyxara", "voidglass_lens", "item_lens", pal=(hexc("#5A6168"), hexc("#4DD0E1"), hexc("#C77DFF"))),
    _row("nyxara", "gravity_shard", "item_gem", pal=(hexc("#9B5DE5"), hexc("#C77DFF"), hexc("#4DD0E1")), shard=True),
    _row("nyxara", "night_fibre", "item_fibre", pal=(hexc("#0D0A14"), hexc("#2B2440"), hexc("#5B2D8E"))),
    _row("nyxara", "void_shard", "item_gem", pal=(hexc("#5B2D8E"), hexc("#9B5DE5"), hexc("#C77DFF")), shard=True),
    # ---------------------------------------------------------------- Station
    _row("station", "hull_plating", "plating", base=STATION_HULL, seam=STATION_SEAM, rivet=STATION_TEAL),
    _row("station", "hull_window", "hull_window", edge=STATION_HULL, pane=hexc("#C8D4DC"), sparkle=hexc("#FFFFFF"),
         tint=STATION_TEAL, frost=hexc("#E8ECEF"), frames=8, frametime=6, amp=0.20),
    _row("station", "truss", "truss", bar=STATION_SEAM, hi=STATION_HULL),
    _row("station", "solar_wing", "wing", cell=hexc("#1B2A49"), grid=STATION_HULL, glint=hexc("#7FB3FF")),
    _row("station", "docking_ring", "ring", base=STATION_HULL, seam=STATION_SEAM, rivet=STATION_SEAM,
         stripe=(hexc("#FF8C42"), hexc("#1A1420")), light=STATION_TEAL, frames=8, frametime=5, amp=0.30, hue=(0.05, 0.10)),
    _row("station", "gravity_plate", "plate", base=STATION_SEAM, ring=hexc("#8D949B"), core=hexc("#7FB3FF"), frames=8, frametime=4, amp=0.25, hue=(0.55, 0.62)),
    _row("station", "hydroponics_tray", "tray", frame=STATION_SEAM, bed=hexc("#3FBF6F"), water=hexc("#7FB3FF")),
    _row("station", "telescope", "telescope", base=STATION_HULL, seam=STATION_SEAM, rivet=STATION_TEAL, lens=hexc("#4DD0E1")),
]

BY_ID = {r["id"]: r for r in PLANET_BLOCK_TEXTURES}


def render(id_):
    """Render one table row -> {relative output path: Image | (strip, mcmeta, still)}."""
    spec = BY_ID[id_]
    return FAMILIES[spec["family"]](spec)


def render_all():
    out = {}
    for r in PLANET_BLOCK_TEXTURES:
        out[r["id"]] = render(r["id"])
    return out


# =========================================================================== writing

def _save_png(img, path, force):
    if os.path.exists(path) and not force:
        print("skip (exists)", path)
        return False
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", path)
    return True


def write_all(block_dir, item_dir, force=None, enabled=None):
    """Write every table output under block_dir / item_dir, additive-only (never overwrites an
    existing file unless --force). Gated by ENABLED / --planets; returns the number of files
    written. `block_dir` and `item_dir` are the textures/block and textures/item folders."""
    if force is None:
        force = "--force" in sys.argv
    if enabled is None:
        enabled = ENABLED or "--planets" in sys.argv
    if not enabled:
        print("gen_planet_textures: planet set gated (ENABLED=False, no --planets); nothing written.")
        return 0
    n = 0
    for id_, outputs in render_all().items():
        for rel, art in outputs.items():
            kind, fname = rel.split("/", 1)
            path = os.path.join(block_dir if kind == "block" else item_dir, fname)
            if isinstance(art, tuple):
                strip, meta, _still = art
                if _save_png(strip, path, force):
                    n += 1
                mpath = path + ".mcmeta"
                if not os.path.exists(mpath) or force:
                    with open(mpath, "w", encoding="utf-8") as fh:
                        json.dump(meta, fh, indent=2)
                        fh.write("\n")
            else:
                if _save_png(art, path, force):
                    n += 1
    return n


# =========================================================================== preview sheets

def _label(draw, x, y, text):
    try:
        draw.text((x, y), text, fill=(230, 230, 230, 255))
    except Exception:
        pass


def write_preview(out_dir, scales=(16, 4)):
    """Per-planet contact sheets: every output of every row, animated ones as a horizontal
    frame strip, at each scale in `scales`. Writes <planet>_x<scale>.png into out_dir."""
    from PIL import ImageDraw
    os.makedirs(out_dir, exist_ok=True)
    rendered = render_all()
    planets = []
    for r in PLANET_BLOCK_TEXTURES:
        if r["planet"] not in planets:
            planets.append(r["planet"])
    for planet in planets:
        rows = [r for r in PLANET_BLOCK_TEXTURES if r["planet"] == planet]
        entries = []  # (label, [frames])
        for r in rows:
            for rel, art in rendered[r["id"]].items():
                if isinstance(art, tuple):
                    strip = art[0]
                    frames = [strip.crop((0, i * S, S, (i + 1) * S)) for i in range(strip.height // S)]
                else:
                    frames = [art]
                entries.append((rel.split("/", 1)[1][:-4], frames))
        for sc in scales:
            cell = S * sc
            pad = max(4, sc)
            label_h = 14
            row_cells = max(8, max(len(f) for _, f in entries))
            # flow layout: an entry occupies len(frames) cells; wrap when a row is full
            placed = []
            cx = cy = 0
            for name, frames in entries:
                n = len(frames)
                if cx + n > row_cells and cx > 0:
                    cx, cy = 0, cy + 1
                placed.append((name, frames, cx, cy))
                cx += n
            w = row_cells * (cell + pad) + pad
            h = (cy + 1) * (cell + pad + label_h) + pad + 24
            sheet = Image.new("RGBA", (w, h), (24, 24, 28, 255))
            draw = ImageDraw.Draw(sheet)
            _label(draw, pad, 6, "%s  x%d" % (planet.upper(), sc))
            q = max(1, cell // 8)
            for name, frames, gx, gy in placed:
                x0 = pad + gx * (cell + pad)
                y0 = 24 + gy * (cell + pad + label_h)
                _label(draw, x0, y0, name)
                for k, f in enumerate(frames):
                    big = f.resize((cell, cell), Image.NEAREST)
                    bg = Image.new("RGBA", (cell, cell), (60, 60, 66, 255))
                    bp = bg.load()
                    for yy in range(cell):
                        for xx in range(cell):
                            if ((xx // q) + (yy // q)) % 2:
                                bp[xx, yy] = (44, 44, 50, 255)
                    bg.alpha_composite(big)
                    sheet.paste(bg, (x0 + k * (cell + pad), y0 + label_h))
            sheet.save(os.path.join(out_dir, "%s_x%d.png" % (planet, sc)))
            print("preview", os.path.join(out_dir, "%s_x%d.png" % (planet, sc)))


# ================================================================================ main

if __name__ == "__main__":
    if "--preview" in sys.argv:
        write_preview(sys.argv[sys.argv.index("--preview") + 1])
        sys.exit(0)
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    from nerospace_target import src_base, target_label
    root = src_base()
    print("gen_planet_textures: target = %s" % target_label())
    base = os.path.join(root, "src/main/resources/assets/nerospace/textures")
    write_all(os.path.join(base, "block"), os.path.join(base, "item"))
