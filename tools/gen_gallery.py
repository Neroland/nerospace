#!/usr/bin/env python3
"""
Composite raw in-game captures into CurseForge gallery images (1920x1080).

Reads a shot list (art/gallery/gallery.json) of entries
    {"raw": "<path under art/gallery/>", "out": "<path under art/gallery/>", "headline": "...", "subline": "..."}
and for each one: cover-crops the raw screenshot to 16:9 at 1920x1080, lays a bottom gradient scrim, draws a
large glowing headline and a smaller subline (legible down to ~400 px wide thumbnails), a thin accent bar,
and the NEROSPACE logo badge in the top-right corner.

Raw captures come from the Fabric 26.3 client gametest (./gradlew :fabric:26.3:runClientGameTest), which
writes them to art/gallery/raw/.

Same family conventions as tools/gen_logo.py (Nerospace crimson/magenta/violet palette, DejaVu Sans Bold,
glow-then-outline-then-fill wordmark); the helpers it needs are copied here, gen_logo.py is untouched.

Usage:  python tools/gen_gallery.py [path/to/gallery.json]
Needs:  Pillow + numpy.
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GALLERY = os.path.join(ROOT, "art/gallery")
LOGO = os.path.join(ROOT, "art/logo/nerospace_logo.png")

W, H = 1920, 1080

# palette (copied from tools/gen_logo.py)
N_DARK   = (43, 13, 58)
N_PURPLE = (106, 31, 140)
N_MAG    = (179, 39, 158)
N_RED    = (224, 58, 58)
N_GLOW   = (255, 138, 216)
N_BRIGHT = (255, 214, 242)

FONT_CANDIDATES = (
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",            # same face as gen_logo.py
    "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    "C:/Windows/Fonts/segoeuib.ttf",                                   # Windows fallbacks
    "C:/Windows/Fonts/arialbd.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",               # macOS fallback
)


def _font(size):
    for p in FONT_CANDIDATES:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    raise SystemExit("gen_gallery: no bold TrueType font found (tried %s)" % ", ".join(FONT_CANDIDATES))


def cover_crop(img, w=W, h=H):
    """Scale to cover w x h, then centre-crop (no letterboxing, no distortion)."""
    sw, sh = img.size
    scale = max(w / sw, h / sh)
    nw, nh = max(w, round(sw * scale)), max(h, round(sh * scale))
    img = img.resize((nw, nh), Image.LANCZOS)
    x0, y0 = (nw - w) // 2, (nh - h) // 2
    return img.crop((x0, y0, x0 + w, y0 + h))


def scrim(base, start=0.40, strength=0.96):
    """Bottom gradient: transparent at `start` of the height, deep violet-black at the bottom edge."""
    t = np.clip((np.linspace(0, 1, H) - start) / (1 - start), 0, 1) ** 1.15 * strength
    a = np.repeat((t * 255).astype(np.uint8)[:, None], W, axis=1)
    rgb = np.zeros((H, W, 3), np.uint8)
    rgb[:] = (10, 4, 18)
    layer = Image.fromarray(np.dstack([rgb, a]), "RGBA")
    base.alpha_composite(layer)
    # faint magenta bloom low on the left, behind the text block
    Y, X = np.mgrid[0:H, 0:W].astype(float)
    d = np.sqrt(((X - W * 0.18) / (W * 0.45)) ** 2 + ((Y - H * 0.95) / (H * 0.35)) ** 2)
    g = (np.clip(1 - d, 0, 1) ** 2 * 70).astype(np.uint8)
    glow = np.zeros((H, W, 4), np.uint8)
    glow[..., :3] = N_MAG
    glow[..., 3] = g
    base.alpha_composite(Image.fromarray(glow, "RGBA"))
    return base


def glow_text(base, xy, text, font, fill, glow_col, glow_r, outline=3):
    """gen_logo.py wordmark recipe: soft coloured glow, dark outline, bright fill."""
    gl = Image.new("RGBA", base.size, (0, 0, 0, 0))
    ImageDraw.Draw(gl).text(xy, text, font=font, fill=glow_col + (255,))
    base.alpha_composite(gl.filter(ImageFilter.GaussianBlur(glow_r)))
    base.alpha_composite(gl.filter(ImageFilter.GaussianBlur(max(1, glow_r // 3))))
    ImageDraw.Draw(base).text(xy, text, font=font, fill=fill, stroke_width=outline, stroke_fill=(16, 6, 26, 255))
    return base


def fit_font(text, max_w, start, minimum):
    size = start
    while size > minimum:
        f = _font(size)
        if ImageDraw.Draw(Image.new("RGB", (1, 1))).textlength(text, font=f) <= max_w:
            return f
        size -= 4
    return _font(minimum)


def logo_badge(base, size=196, margin=48):
    """The square mod logo as a rounded badge with a glowing magenta rim, top-right."""
    if not os.path.exists(LOGO):
        print("gen_gallery: logo not found, skipping badge:", os.path.relpath(LOGO, ROOT))
        return base
    logo = Image.open(LOGO).convert("RGBA").resize((size, size), Image.LANCZOS)
    radius = size // 6
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    x, y = W - size - margin, margin
    halo = Image.new("RGBA", base.size, (0, 0, 0, 0))
    ImageDraw.Draw(halo).rounded_rectangle([x - 6, y - 6, x + size + 6, y + size + 6], radius=radius + 6,
                                           fill=N_MAG + (200,))
    base.alpha_composite(halo.filter(ImageFilter.GaussianBlur(14)))
    base.paste(logo, (x, y), mask)
    ImageDraw.Draw(base).rounded_rectangle([x, y, x + size - 1, y + size - 1], radius=radius,
                                           outline=N_GLOW + (255,), width=3)
    return base


def compose(entry):
    raw_path = os.path.join(GALLERY, entry["raw"])
    out_path = os.path.join(GALLERY, entry["out"])
    img = cover_crop(Image.open(raw_path).convert("RGB")).convert("RGBA")
    img = scrim(img)

    left = 112
    max_w = W - left - 120
    head_font = fit_font(entry["headline"], max_w, 112, 56)
    sub_font = fit_font(entry.get("subline", ""), max_w, 54, 28)
    d = ImageDraw.Draw(img)
    hb = d.textbbox((0, 0), entry["headline"], font=head_font)
    sb = d.textbbox((0, 0), entry.get("subline", "") or " ", font=sub_font)
    head_h, sub_h = hb[3] - hb[1], sb[3] - sb[1]
    gap = 26
    bottom = H - 96
    sub_y = bottom - sub_h - sb[1]
    head_y = sub_y + sb[1] - gap - head_h - hb[1]

    # thin accent bar: crimson -> magenta -> violet, to the left of the text block
    bar_top, bar_bot = head_y + hb[1] - 4, bottom + 4
    bar = np.zeros((bar_bot - bar_top, 10, 4), np.uint8)
    for i, row in enumerate(np.linspace(0, 1, bar.shape[0])):
        c = np.array(N_RED) * (1 - row) + np.array(N_PURPLE) * row if row > 0.5 else \
            np.array(N_RED) * (1 - row * 2) + np.array(N_MAG) * (row * 2)
        bar[i, :, :3] = c
        bar[i, :, 3] = 255
    bar_img = Image.fromarray(bar, "RGBA")
    bar_glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    bar_glow.paste(bar_img, (left - 40, bar_top))
    img.alpha_composite(bar_glow.filter(ImageFilter.GaussianBlur(8)))
    img.alpha_composite(bar_glow)

    img = glow_text(img, (left, head_y), entry["headline"], head_font, (248, 242, 255, 255), (255, 80, 160), 16, 4)
    if entry.get("subline"):
        img = glow_text(img, (left + 2, sub_y), entry["subline"], sub_font, N_BRIGHT + (255,), N_PURPLE, 10, 3)

    img = logo_badge(img)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    img.convert("RGB").save(out_path, optimize=True)
    print("wrote", os.path.relpath(out_path, ROOT), img.size)


def main():
    shots = sys.argv[1] if len(sys.argv) > 1 else os.path.join(GALLERY, "gallery.json")
    with open(shots, encoding="utf-8") as fh:
        entries = json.load(fh)["shots"]
    for entry in entries:
        compose(entry)


if __name__ == "__main__":
    main()
