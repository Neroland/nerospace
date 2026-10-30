#!/usr/bin/env python3
"""
Sky and particle textures for the Nerospace 2.0.0 planet skies.

Generates every texture the seven ``assets/nerospace/sky/<planet>.json`` profiles reference, plus the sprite
sheets for the six custom particle types, into the resource tree:

    textures/sky/      galaxy.png, galaxy_nyx.png, aurora_0.png, aurora_1.png, stars.png, horizon_ramp.png,
                       sun_white.png, sun_small.png, sun_pale.png, sun_amber.png, red_giant.png, dark_moon.png,
                       earth.png, earth_disk.png, phobos.png, deimos.png, moon_small.png
    textures/particle/ dust_0..3, ash_0..3, spore_0..3, diamond_dust_0..3, ember_0..3, nebula_mote_0..3 (8x8)

Everything is deterministic: each output seeds its own ``numpy`` generator from the md5 of its name (same rule as
``gen_planet_textures.seed_of``), so a re-render is byte-identical. Pillow + numpy only.

Conventions
-----------
* Body textures are **luminance-first**: discs are white/pale so ``Body.color`` in the sky profile does the tinting
  (the red giant and Earth are the exceptions - they carry their own colour because their surface detail is the
  identity).
* The galaxy bands and aurora strips tile horizontally (periodic value noise), so the renderer can wrap them
  around the sky dome with ``yaw``.
* ``stars.png`` is a 4x4 sheet of 16x16 sprites (point, soft, cross, 6-ray at two brightnesses); the renderer
  picks a sprite per star by hash and tints with ``stars.tint``.
* Particle sprites are 8x8 like vanilla's and come in four frames per type (``<type>_0..3``), listed in that order
  by ``particles/<type>.json`` so vanilla's animated sprite set cycles them over the particle lifetime.

Usage (from anywhere; the default root is the parent of this ``tools`` folder, so inside the repo it writes to
``common/src/main/resources/assets/nerospace/textures``; ``--root`` points it at another resource tree):

    python tools/gen_sky_textures.py                 # write all textures (additive: existing files are overwritten
                                                     #   only with --force, like gen_textures.save)
    python tools/gen_sky_textures.py --force
    python tools/gen_sky_textures.py --root <dir>    # another resource root
    python tools/gen_sky_textures.py --preview <dir> # also write a contact sheet of every texture
    python tools/gen_sky_textures.py --check         # re-render to a temp dir; exit 1 if a committed texture drifted
"""
import argparse
import hashlib
import math
import os
import shutil
import sys
import tempfile

try:
    from PIL import Image, ImageFilter
    import numpy as np
except ModuleNotFoundError as e:  # mirrors gen_textures: committed art, deps optional
    print("gen_sky_textures: %s not installed; skipping (pip install pillow numpy)." % e.name)
    sys.exit(0)

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_ROOT = os.path.dirname(HERE)
ASSET_DIR = os.path.join("common", "src", "main", "resources", "assets", "nerospace", "textures")


# ======================================================================================= helpers

def seed_of(name):
    return int(hashlib.md5(name.encode("utf-8")).hexdigest()[:8], 16)


def rng_of(name):
    return np.random.default_rng(seed_of(name))


def hexc(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32) / 255.0


def smoothstep(t):
    return t * t * (3.0 - 2.0 * t)


def value_noise(rng, w, h, fx, fy, wrap_x=False):
    """Bilinear value noise with ``fx`` x ``fy`` lattice cells over the image; periodic in x when wrap_x."""
    lat = rng.random((fy + 1, fx + 1)).astype(np.float32)
    if wrap_x:
        lat[:, -1] = lat[:, 0]
    xs = np.linspace(0.0, fx, w, endpoint=False, dtype=np.float32)
    ys = np.linspace(0.0, fy, h, endpoint=False, dtype=np.float32)
    x0 = np.floor(xs).astype(int)
    y0 = np.floor(ys).astype(int)
    tx = smoothstep(xs - x0)[None, :]
    ty = smoothstep(ys - y0)[:, None]
    x1 = np.minimum(x0 + 1, fx)
    y1 = np.minimum(y0 + 1, fy)
    a = lat[y0][:, x0]
    b = lat[y0][:, x1]
    c = lat[y1][:, x0]
    d = lat[y1][:, x1]
    return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty


def fbm(rng, w, h, base=4, octaves=5, persistence=0.5, wrap_x=False, aspect=1.0):
    out = np.zeros((h, w), dtype=np.float32)
    amp = 1.0
    total = 0.0
    f = base
    for _ in range(octaves):
        fy = max(1, int(round(f * aspect)))
        out += amp * value_noise(rng, w, h, max(1, int(f)), fy, wrap_x)
        total += amp
        amp *= persistence
        f *= 2
    return out / total


def radial(size):
    """(dx, dy, r) in [-1, 1] from the centre of a size x size image."""
    ys, xs = np.mgrid[0:size, 0:size].astype(np.float32)
    c = (size - 1) / 2.0
    dx = (xs - c) / c
    dy = (ys - c) / c
    return dx, dy, np.sqrt(dx * dx + dy * dy)


def to_image(rgb, alpha):
    rgb = np.clip(rgb, 0.0, 1.0)
    alpha = np.clip(alpha, 0.0, 1.0)
    arr = np.concatenate([rgb, alpha[..., None]], axis=-1)
    return Image.fromarray((arr * 255.0 + 0.5).astype(np.uint8), "RGBA")


def save(img, path, force):
    if os.path.exists(path) and not force:
        return False
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)
    return True


# ======================================================================================= suns

def sun_disc(name, size, radius, glow, rays=0, rim=None):
    rng = rng_of(name)
    dx, dy, r = radial(size)
    disc = np.clip((radius - r) / 0.04, 0, 1)
    halo = np.exp(-(np.maximum(r - radius, 0.0) / glow) ** 2)
    a = np.maximum(disc, halo * 0.9)
    lum = np.ones((size, size), dtype=np.float32)
    # faint limb darkening so the disc is not a flat cut-out
    lum = np.where(r < radius, 1.0 - 0.12 * (r / radius) ** 2, lum)
    if rays:
        ang = np.arctan2(dy, dx)
        ray = np.abs(np.cos(ang * rays / 2.0)) ** 60.0
        ray *= np.exp(-((r - radius) / (glow * 2.4))) * (r > radius)
        a = np.clip(a + ray * 0.8, 0, 1)
    grain = fbm(rng, size, size, base=6, octaves=3) * 0.05
    rgb = np.stack([lum - grain * (r < radius)] * 3, axis=-1)
    if rim is not None:
        rimc = hexc(rim)
        w = np.clip(halo * 1.5, 0, 1)[..., None]
        rgb = rgb * (1 - w) + rimc * w
    return to_image(rgb, a)


def red_giant(name="red_giant", size=256):
    rng = rng_of(name)
    dx, dy, r = radial(size)
    radius = 0.72
    gran = fbm(rng, size, size, base=12, octaves=4, persistence=0.55)
    cells = fbm(rng, size, size, base=5, octaves=2)
    spots = (fbm(rng, size, size, base=4, octaves=3) > 0.66).astype(np.float32)
    spots = np.array(Image.fromarray((spots * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2))) / 255.0
    limb = 1.0 - 0.55 * np.clip(r / radius, 0, 1) ** 1.6
    lum = (0.78 + 0.22 * gran) * (0.85 + 0.15 * cells) * limb
    lum *= (1.0 - 0.6 * spots)
    # surface colour: white-yellow core to orange limb; Body.color (#FF6B35) multiplies this
    core = hexc("#FFF1C2")
    edge = hexc("#FFB070")
    t = np.clip(r / radius, 0, 1)[..., None]
    rgb = (core * (1 - t) + edge * t) * lum[..., None]
    disc = np.clip((radius - r) / 0.012, 0, 1)
    corona = np.exp(-(np.abs(r - radius) / 0.16) ** 1.4) * (r > radius)
    wisps = fbm(rng, size, size, base=16, octaves=3)
    corona *= 0.55 + 0.45 * wisps
    rgb = np.where((r > radius)[..., None], hexc("#FF8A4A")[None, None, :] * (0.7 + 0.3 * wisps[..., None]), rgb)
    a = disc + corona * 0.85
    return to_image(rgb, a)


def dark_moon(name="dark_moon", size=256):
    rng = rng_of(name)
    dx, dy, r = radial(size)
    radius = 0.82
    n = fbm(rng, size, size, base=8, octaves=4)
    lum = 0.10 + 0.08 * n
    # a thin lit rim so the silhouette reads against the red giant during totality
    rim = np.exp(-((radius - r) / 0.025) ** 2) * (r < radius)
    rgb = np.stack([lum] * 3, axis=-1)
    rgb += rim[..., None] * hexc("#6E3A4A")[None, None, :] * 0.9
    a = np.clip((radius - r) / 0.01, 0, 1)
    return to_image(rgb, a)


# ======================================================================================= moons / planets

def rocky_moon(name, size, radius, base_grey, crater_count, potato=0.0):
    rng = rng_of(name)
    dx, dy, r = radial(size)
    ang = np.arctan2(dy, dx)
    bump = 1.0
    if potato > 0:
        bump = 1.0 + potato * (0.5 * np.sin(ang * 3 + 0.7) + 0.5 * np.cos(ang * 5 + 2.1)) * 0.5
    rr = r / bump
    n = fbm(rng, size, size, base=6, octaves=4)
    lum = base_grey * (0.75 + 0.5 * n)
    lum *= 1.0 - 0.5 * np.clip(rr / radius, 0, 1) ** 2  # limb darkening
    for _ in range(crater_count):
        cx, cy = rng.uniform(-radius * 0.8, radius * 0.8, 2)
        cr = rng.uniform(0.06, 0.22) * radius
        d = np.sqrt((dx - cx) ** 2 + (dy - cy) ** 2)
        lum *= 1.0 - 0.35 * np.clip(1 - d / cr, 0, 1)
        lum += 0.25 * np.exp(-((d - cr) / (cr * 0.25)) ** 2) * (dx - cx < 0)
    a = np.clip((radius - rr) / 0.03, 0, 1)
    rgb = np.stack([lum] * 3, axis=-1)
    return to_image(rgb, a)


def earth(name, size):
    rng = rng_of(name)
    dx, dy, r = radial(size)
    radius = 0.86
    land = fbm(rng, size, size, base=3, octaves=6, persistence=0.55)
    landmask = np.clip((land - 0.52) / 0.035, 0, 1)
    ocean = hexc("#2B6FD6")
    ocean_deep = hexc("#1B4FA8")
    veg = hexc("#4F8F3A")
    dirt = hexc("#A58B55")
    terrain = fbm(rng, size, size, base=9, octaves=3)
    landc = veg * (1 - terrain[..., None]) + dirt * terrain[..., None]
    oceanc = ocean * (1 - terrain[..., None] * 0.6) + ocean_deep * (terrain[..., None] * 0.6)
    rgb = oceanc * (1 - landmask[..., None]) + landc * landmask[..., None]
    # polar ice
    lat = np.abs(dy) / radius
    ice = np.clip((lat - 0.78) / 0.1, 0, 1) * (1 - 0.3 * fbm(rng, size, size, base=10, octaves=2))
    rgb = rgb * (1 - ice[..., None]) + hexc("#F2F7FF")[None, None, :] * ice[..., None]
    # clouds: streaky bands + swirls
    cl = fbm(rng, size, size, base=5, octaves=5, persistence=0.6, aspect=0.5)
    cloud = np.clip((cl - 0.5) / 0.12, 0, 1)
    rgb = rgb * (1 - cloud[..., None] * 0.95) + hexc("#FFFFFF")[None, None, :] * cloud[..., None] * 0.95
    # day-side shading (light from upper-left) and limb darkening
    light = np.clip(0.55 + 0.45 * (-(dx + dy) / 1.414), 0.15, 1.0)
    limb = 1.0 - 0.35 * np.clip(r / radius, 0, 1) ** 3
    rgb *= (light * limb)[..., None]
    # atmosphere rim inside + glow outside
    rim = np.exp(-((radius - r) / 0.05) ** 2) * (r < radius)
    rgb = rgb * (1 - rim[..., None] * 0.5) + hexc("#9FD2FF")[None, None, :] * rim[..., None] * 0.5
    glow = np.exp(-(np.abs(r - radius) / 0.07) ** 1.5) * (r > radius)
    rgb = np.where((r > radius)[..., None], hexc("#7FB8FF")[None, None, :], rgb)
    a = np.clip((radius - r) / 0.008, 0, 1) + glow * 0.8
    return to_image(rgb, a)


# ======================================================================================= bands

def galaxy_band(name, w, h, core, outer, lane, star_tint, density=1.0, nebula=None):
    rng = rng_of(name)
    ys = np.linspace(-1, 1, h, dtype=np.float32)[:, None]
    wobble = (fbm(rng, w, 1, base=2, octaves=3, wrap_x=True)[0] - 0.5) * 0.5
    profile = np.exp(-((ys - wobble[None, :]) / 0.42) ** 2)
    cloud = fbm(rng, w, h, base=6, octaves=6, persistence=0.55, wrap_x=True, aspect=0.5)
    lanes = fbm(rng, w, h, base=10, octaves=4, persistence=0.6, wrap_x=True, aspect=0.4)
    lane_mask = np.clip((lanes - 0.56) / 0.08, 0, 1) * profile
    inten = profile * (0.35 + 0.65 * cloud) * density
    inten *= 1.0 - 0.75 * lane_mask
    t = np.clip(np.abs(ys - wobble[None, :]) / 0.5, 0, 1)
    rgb = hexc(core) * (1 - t[..., None]) + hexc(outer) * t[..., None]
    if nebula:
        n1 = fbm(rng, w, h, base=4, octaves=4, wrap_x=True, aspect=0.5)
        n2 = fbm(rng, w, h, base=7, octaves=4, wrap_x=True, aspect=0.5)
        c1, c2 = hexc(nebula[0]), hexc(nebula[1])
        m1 = np.clip((n1 - 0.45) / 0.25, 0, 1)[..., None]
        m2 = np.clip((n2 - 0.5) / 0.2, 0, 1)[..., None]
        rgb = rgb * (1 - m1) + c1 * m1
        rgb = rgb * (1 - m2 * 0.8) + c2 * m2 * 0.8
    rgb = rgb * (1 - lane_mask[..., None] * 0.6) + hexc(lane) * lane_mask[..., None] * 0.6
    # star specks inside the band
    n_stars = int(w * h * 0.004 * density)
    sx = rng.integers(0, w, n_stars)
    sy = np.clip((rng.normal(0, 0.28, n_stars) + wobble[sx] * 0.5 + 1) * 0.5 * h, 0, h - 1).astype(int)
    sb = rng.uniform(0.3, 1.0, n_stars)
    stars = np.zeros((h, w), dtype=np.float32)
    stars[sy, sx] = sb
    bright = rng.random(n_stars) > 0.9
    for x, y in zip(sx[bright], sy[bright]):
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, yy = (x + ox) % w, y + oy
            if 0 <= yy < h:
                stars[yy, xx] = max(stars[yy, xx], 0.5)
    rgb = rgb * (1 - stars[..., None]) + hexc(star_tint) * stars[..., None]
    a = np.clip(inten * 0.9 + stars, 0, 1)
    return to_image(rgb, a)


def aurora_strip(name, w, h, phase):
    rng = rng_of(name)
    xs = np.linspace(0, 1, w, endpoint=False, dtype=np.float32)
    ys = np.linspace(0, 1, h, dtype=np.float32)[:, None]
    rays = fbm(rng, w, 1, base=24, octaves=4, persistence=0.6, wrap_x=True)[0]
    rays2 = fbm(rng, w, 1, base=9, octaves=3, wrap_x=True)[0]
    ray = np.clip((rays - 0.35) / 0.4, 0, 1) * (0.4 + 0.6 * rays2)
    base_y = 0.75 + 0.15 * (fbm(rng, w, 1, base=3, octaves=2, wrap_x=True)[0] - 0.5)
    vert = np.exp(-((ys - base_y[None, :]) / 0.08) ** 2) * 1.0 + np.clip(1 - (base_y[None, :] - ys) / 0.6, 0, 1) * (ys < base_y[None, :]) * 0.6
    wisps = fbm(rng, w, h, base=12, octaves=3, wrap_x=True, aspect=0.6)
    inten = ray[None, :] * vert * (0.6 + 0.4 * wisps)
    inten = np.clip(inten, 0, 1)
    # the strip is a luminance/alpha mask; the renderer tints it with aurora.colors over time
    rgb = np.ones((h, w, 3), dtype=np.float32)
    return to_image(rgb, inten)


def star_sheet(name="stars", cell=16, n=4):
    rng = rng_of(name)
    size = cell * n
    rgb = np.ones((size, size, 3), dtype=np.float32)
    a = np.zeros((size, size), dtype=np.float32)
    kinds = ["point", "point2", "soft", "soft2", "cross", "cross2", "ray6", "ray6b",
             "point", "soft", "pointw", "softw", "cross", "point2", "soft", "point"]
    for i, kind in enumerate(kinds):
        cx = (i % n) * cell
        cy = (i // n) * cell
        dx, dy, r = radial(cell)
        if kind == "point":
            spr = (r < 0.08).astype(np.float32) * 0.8
        elif kind == "point2":
            spr = np.clip(1 - r / 0.16, 0, 1)
        elif kind == "pointw":
            spr = (r < 0.08).astype(np.float32) * 0.5
        elif kind == "soft":
            spr = np.exp(-(r / 0.22) ** 2)
        elif kind == "soft2":
            spr = np.exp(-(r / 0.3) ** 2) * 0.9
        elif kind == "softw":
            spr = np.exp(-(r / 0.18) ** 2) * 0.6
        elif kind.startswith("cross"):
            arm = (np.abs(dx) < 0.07) | (np.abs(dy) < 0.07)
            spr = arm * np.clip(1 - r / (0.5 if kind == "cross" else 0.75), 0, 1) + np.exp(-(r / 0.16) ** 2)
        else:  # ray6
            ang = np.arctan2(dy, dx)
            ray = np.abs(np.cos(ang * 3)) ** 40
            spr = ray * np.clip(1 - r / 0.8, 0, 1) * 0.8 + np.exp(-(r / 0.2) ** 2)
        a[cy:cy + cell, cx:cx + cell] = np.clip(spr, 0, 1)
        # a touch of colour temperature per sprite (blue-white / warm) baked at 10 % so tint still rules
        temp = rng.uniform(-1, 1)
        rgb[cy:cy + cell, cx:cx + cell] = np.array([1.0 - 0.08 * max(temp, 0), 1.0 - 0.04 * abs(temp), 1.0 - 0.08 * max(-temp, 0)])
    return to_image(rgb, a)


# ======================================================================================= particles

def particle_frames(kind):
    """Four 8x8 RGBA frames per type. Returns a list of PIL images."""
    frames = []
    for f in range(4):
        rng = rng_of("%s_%d" % (kind, f))
        dx, dy, r = radial(8)
        if kind == "dust":
            col = [hexc("#C8683D"), hexc("#B5532A"), hexc("#7A4A2E"), hexc("#D9A066")][f]
            n = value_noise(rng, 8, 8, 3, 3)
            a = np.clip((0.9 - r) / 0.45, 0, 1) * (0.35 + 0.65 * n) * 0.9
            a *= (r < 0.8 + 0.3 * (n - 0.5))
            rgb = col[None, None, :] * (0.85 + 0.3 * n[..., None])
        elif kind == "ash":
            grey = [0.62, 0.5, 0.42, 0.34][f]
            n = value_noise(rng, 8, 8, 3, 3)
            shape = (r < 0.55 + 0.35 * (n - 0.5)) & (np.abs(dx * 0.6 + dy) < 0.75)
            a = shape.astype(np.float32) * (0.7 + 0.3 * n)
            rgb = np.stack([grey * (0.8 + 0.4 * n)] * 3, axis=-1) * np.array([1.0, 0.97, 0.95])
        elif kind == "spore":
            size_ = [0.55, 0.7, 0.6, 0.45][f]
            a = np.exp(-(r / size_) ** 2) * 0.9
            core = np.exp(-(r / (size_ * 0.45)) ** 2)
            rgb = hexc("#FF7AC8")[None, None, :] * (1 - core[..., None] * 0.5) + hexc("#FFD6EC")[None, None, :] * core[..., None] * 0.5
        elif kind == "diamond_dust":
            arm = [0.9, 0.55, 0.3, 0.7][f]
            cross = ((np.abs(dx) < 0.3) | (np.abs(dy) < 0.3)) * np.clip(1 - r / arm, 0, 1)
            a = np.clip(cross + np.exp(-(r / 0.3) ** 2), 0, 1)
            rgb = hexc("#F2FBFF")[None, None, :] * np.ones((8, 8, 1)) - np.array([0.0, 0.0, 0.0])
            rgb[..., 2] = 1.0
            rgb[..., 1] = 0.97 + 0.03 * a
        elif kind == "ember":
            cols = [("#FFE08A", "#FF6B35"), ("#FFC860", "#FF5A1F"), ("#FF9A4A", "#D94E1F"), ("#D94E1F", "#5A2416")][f]
            core = np.exp(-(r / 0.28) ** 2)
            halo = np.exp(-(r / 0.6) ** 2) * (0.9 - 0.2 * f)
            a = np.clip(core + halo * 0.7, 0, 1)
            rgb = hexc(cols[1])[None, None, :] * (1 - core[..., None]) + hexc(cols[0])[None, None, :] * core[..., None]
        else:  # nebula_mote
            cols = ["#C77DFF", "#9B5DE5", "#4DD0E1", "#FF6EC7"]
            col = hexc(cols[f])
            a = np.exp(-(r / 0.5) ** 2) * (0.75 + 0.25 * math.sin(f * 1.7)) * 0.8
            core = np.exp(-(r / 0.22) ** 2)
            rgb = col[None, None, :] * (1 - core[..., None] * 0.4) + np.ones((8, 8, 3)) * core[..., None] * 0.4
        frames.append(to_image(rgb, a))
    return frames


# ======================================================================================= table

def sky_textures():
    return {
        "sun_white": lambda: sun_disc("sun_white", 64, 0.34, 0.16, rays=4),
        "sun_small": lambda: sun_disc("sun_small", 32, 0.42, 0.18),
        "sun_pale": lambda: sun_disc("sun_pale", 64, 0.26, 0.28),
        "sun_amber": lambda: sun_disc("sun_amber", 64, 0.36, 0.2, rim="#FFD27A"),
        "red_giant": lambda: red_giant(),
        "dark_moon": lambda: dark_moon(),
        "earth": lambda: earth("earth", 128),
        "earth_disk": lambda: earth("earth_disk", 512),
        "phobos": lambda: rocky_moon("phobos", 32, 0.7, 0.58, 4, potato=0.35),
        "deimos": lambda: rocky_moon("deimos", 32, 0.62, 0.66, 2, potato=0.25),
        "moon_small": lambda: rocky_moon("moon_small", 32, 0.8, 0.85, 5),
        "galaxy": lambda: galaxy_band("galaxy", 1024, 256, "#FFF4E0", "#CFE4FF", "#1A1420", "#FFFFFF"),
        "galaxy_nyx": lambda: galaxy_band("galaxy_nyx", 1024, 256, "#FF9EDC", "#5B2D8E", "#0D0A14", "#FFF0FF",
                                          density=1.25, nebula=("#9B5DE5", "#4DD0E1")),
        "aurora_0": lambda: aurora_strip("aurora_0", 512, 128, 0.0),
        "aurora_1": lambda: aurora_strip("aurora_1", 512, 128, 0.5),
        "stars": lambda: star_sheet(),
        "horizon_ramp": lambda: horizon_ramp(),
    }


def horizon_ramp(name="horizon_ramp", w=4, h=64):
    """White strip whose alpha rises from 0 (top) to 1 (bottom) on a t^1.6 curve. The renderer wraps it round the
    horizon and tints it with the profile's horizon colour (and again with the haze colour), so one texture makes
    the zenith-to-horizon gradient of every sky."""
    t = (np.arange(h, dtype=np.float32) + 0.5) / h
    alpha = np.repeat((t ** 1.6)[:, None], w, axis=1)
    rgb = np.ones((h, w, 3), dtype=np.float32)
    return to_image(rgb, alpha)


PARTICLE_TYPES = ["dust", "ash", "spore", "diamond_dust", "ember", "nebula_mote"]


def write_all(root, force=False, preview=None):
    base = os.path.join(root, ASSET_DIR)
    written = 0
    sheet = []
    for name, fn in sky_textures().items():
        img = fn()
        if save(img, os.path.join(base, "sky", name + ".png"), force):
            written += 1
        sheet.append((name, img))
    for kind in PARTICLE_TYPES:
        for i, img in enumerate(particle_frames(kind)):
            fname = "%s_%d" % (kind, i)
            if save(img, os.path.join(base, "particle", fname + ".png"), force):
                written += 1
            sheet.append((fname, img))
    print("gen_sky_textures: %d file(s) written under %s" % (written, base))
    if preview:
        contact_sheet(sheet, os.path.join(preview, "sky_textures_contact.png"))


def contact_sheet(items, path):
    cell = 136
    cols = 8
    rows = (len(items) + cols - 1) // cols
    from PIL import ImageDraw
    out = Image.new("RGBA", (cols * cell, rows * (cell + 14)), (24, 28, 36, 255))
    d = ImageDraw.Draw(out)
    for i, (name, img) in enumerate(items):
        x = (i % cols) * cell
        y = (i // cols) * (cell + 14)
        # checker so alpha is visible
        for cy in range(0, cell, 8):
            for cx in range(0, cell, 8):
                c = (52, 56, 64, 255) if ((cx + cy) // 8) % 2 else (40, 44, 52, 255)
                d.rectangle([x + cx, y + cy, x + cx + 7, y + cy + 7], fill=c)
        im = img.copy()
        scale = max(1, min(cell // im.width, cell // im.height))
        if im.width < 32:
            scale = 128 // im.width
        im = im.resize((min(cell, im.width * scale), min(cell, im.height * scale)), Image.NEAREST if im.width <= 32 else Image.BILINEAR)
        out.alpha_composite(im, (x + (cell - im.width) // 2, y + (cell - im.height) // 2))
        d.text((x + 3, y + cell + 1), name, fill=(220, 230, 240, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)
    print("gen_sky_textures: contact sheet -> %s" % path)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[1])
    ap.add_argument("--root", default=DEFAULT_ROOT, help="resource root (default: parent of tools/)")
    ap.add_argument("--force", action="store_true", help="overwrite existing files")
    ap.add_argument("--preview", default=None, help="also write a contact sheet into this folder")
    ap.add_argument("--check", action="store_true", help="compare a fresh render with the committed files (writes nothing)")
    args = ap.parse_args(argv)
    if args.check:
        sys.exit(check(args.root))
    write_all(args.root, args.force, args.preview)


def check(root):
    """Renders every texture into a temp root and compares bytes with ``root``; returns 1 on any drift."""
    tmp = tempfile.mkdtemp(prefix="gen_sky_check_")
    try:
        write_all(tmp, True)
        fresh = os.path.join(tmp, ASSET_DIR)
        bad = []
        count = 0
        for dirpath, _, files in os.walk(fresh):
            for f in sorted(files):
                rel = os.path.relpath(os.path.join(dirpath, f), fresh)
                count += 1
                target = os.path.join(root, ASSET_DIR, rel)
                if not os.path.isfile(target):
                    bad.append(rel + " (missing)")
                    continue
                with open(os.path.join(dirpath, f), "rb") as a, open(target, "rb") as b:
                    if a.read() != b.read():
                        bad.append(rel)
        for b in bad:
            print("gen_sky_textures: stale " + b)
        print("gen_sky_textures: %d of %d texture(s) ok" % (count - len(bad), count))
        return 1 if bad else 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
