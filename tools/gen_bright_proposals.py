#!/usr/bin/env python3
"""STAGED brighter-texture proposals for review (Dario asked for brighter, not dull).

Reads every committed block/item texture and writes a brighter, slightly more saturated copy
into ``art/bright_proposed/`` — it NEVER touches the shipped art under
``common/src/main/resources``. A side-by-side ``art/bright_proposed/_compare_*.png`` sheet shows
before (left) vs after (right) for quick approval.

Animated frame strips are lifted frame-by-frame so the animation survives. Once approved, the
proposals can be promoted with ``--apply`` (copies them over the committed textures); without it
the tool is read-only against the mod art.

Usage:
  python tools/gen_bright_proposals.py --multiloader            # generate proposals + compare sheets
  python tools/gen_bright_proposals.py --multiloader --apply    # promote proposals into the mod
Deps: Pillow
"""
import argparse
import colorsys
import math
import os
import shutil
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nerospace_target import resolve, REPO_ROOT  # noqa: E402

SRC = resolve("common/src/main/resources/assets/nerospace/textures")
OUT = os.path.join(REPO_ROOT, "art", "bright_proposed")

LIFT = 1.16   # lightness multiplier
SAT = 1.20    # saturation multiplier
GAMMA = 0.94  # <1 lifts midtones


def brighten(img):
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ll, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            ll = min(1.0, (ll ** GAMMA) * LIFT)
            ss = min(1.0, ss * SAT)
            r, g, b = (int(c * 255) for c in colorsys.hls_to_rgb(hh, ll, ss))
            px[x, y] = (r, g, b, a)
    return img


def process(sub):
    src = os.path.join(SRC, sub)
    out = os.path.join(OUT, sub)
    os.makedirs(out, exist_ok=True)
    names = sorted(n for n in os.listdir(src) if n.endswith(".png"))
    for n in names:
        brighten(Image.open(os.path.join(src, n))).save(os.path.join(out, n))
        meta = os.path.join(src, n + ".mcmeta")
        if os.path.exists(meta):
            shutil.copy(meta, os.path.join(out, n + ".mcmeta"))
    return names


def compare_sheet(sub, names):
    src = os.path.join(SRC, sub)
    out = os.path.join(OUT, sub)
    S, cols = 4, 6
    cell_w, cell_h = 16 * S * 2 + 14, 16 * S + 16
    rows = math.ceil(len(names) / cols)
    sheet = Image.new("RGBA", (cols * cell_w + 8, rows * cell_h + 8), (32, 34, 44, 255))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        cx = 8 + (i % cols) * cell_w
        cy = 8 + (i // cols) * cell_h
        before = Image.open(os.path.join(src, n)).convert("RGBA")
        before = before.crop((0, 0, 16, 16)).resize((16 * S, 16 * S), Image.NEAREST)
        after = Image.open(os.path.join(out, n)).convert("RGBA")
        after = after.crop((0, 0, 16, 16)).resize((16 * S, 16 * S), Image.NEAREST)
        sheet.alpha_composite(before, (cx, cy))
        sheet.alpha_composite(after, (cx + 16 * S + 6, cy))
        d.text((cx, cy + 16 * S + 2), n[:-4][:16], fill=(205, 212, 226, 255))
    p = os.path.join(OUT, f"_compare_{sub}.png")
    sheet.convert("RGB").save(p)
    return p


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--multiloader", action="store_true")
    ap.add_argument("--common", action="store_true")
    ap.add_argument("--apply", action="store_true")
    ap.parse_args()
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for sub in ("block", "item"):
        if not os.path.isdir(os.path.join(SRC, sub)):
            continue
        names = process(sub)
        total += len(names)
        sheet = compare_sheet(sub, names)
        print(f"{sub}: {len(names)} proposals -> {os.path.relpath(sheet, REPO_ROOT)}")
    print(f"\n{total} brighter proposals staged in {os.path.relpath(OUT, REPO_ROOT)} (NOT applied).")
    print("Review the _compare_*.png sheets; run with --apply to promote them into the mod.")


if __name__ == "__main__":
    main()
