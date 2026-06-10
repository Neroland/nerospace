#!/usr/bin/env python3
"""
Contact-sheet renderer (ART_OVERHAUL_DESIGN.md §8): composes labelled atlases of the committed
textures into art/preview/ so each art phase can be reviewed at a glance before runClient.

Sheets:
  blocks_sheet.png   — every textures/block PNG at 4x with a name label
  items_sheet.png    — every textures/item PNG at 4x with a name label
  gui_sheet.png      — the GUI panels at 1x in a vertical strip

Deterministic; safe to re-run (preview output is not committed mod art, so it always overwrites).

Usage: python3 tools/render_contact_sheets.py
Deps:  Pillow
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures")
OUT = os.path.join(ROOT, "art", "preview")
os.makedirs(OUT, exist_ok=True)

SCALE = 4          # 16x art shown at 64px
CELL_W = 16 * SCALE + 8
CELL_H = 16 * SCALE + 18   # room for the label
BG = (24, 26, 32, 255)
LABEL = (200, 210, 224, 255)


def _atlas(subdir, out_name, columns=8):
    folder = os.path.join(TEX, subdir)
    names = sorted(n for n in os.listdir(folder) if n.endswith(".png"))
    if not names:
        return
    rows = math.ceil(len(names) / columns)
    sheet = Image.new("RGBA", (columns * CELL_W + 8, rows * CELL_H + 8), BG)
    draw = ImageDraw.Draw(sheet)
    for i, name in enumerate(names):
        col, row = i % columns, i // columns
        x = 8 + col * CELL_W
        y = 8 + row * CELL_H
        img = Image.open(os.path.join(folder, name)).convert("RGBA")
        img = img.resize((img.width * SCALE, img.height * SCALE), Image.NEAREST)
        # crop oversized art (e.g. animated strips) to its first frame for the sheet
        img = img.crop((0, 0, min(img.width, 16 * SCALE), min(img.height, 16 * SCALE)))
        sheet.alpha_composite(img, (x, y))
        draw.text((x, y + 16 * SCALE + 2), name[:-4][:18], fill=LABEL)
    path = os.path.join(OUT, out_name)
    sheet.save(path)
    print("wrote", os.path.relpath(path, ROOT), f"({len(names)} tiles)")


def _gui_strip():
    folder = os.path.join(TEX, "gui")
    names = sorted(n for n in os.listdir(folder) if n.endswith(".png"))
    if not names:
        return
    imgs = [Image.open(os.path.join(folder, n)).convert("RGBA") for n in names]
    width = max(i.width for i in imgs) + 16
    height = sum(i.height + 22 for i in imgs) + 8
    sheet = Image.new("RGBA", (width, height), BG)
    draw = ImageDraw.Draw(sheet)
    y = 8
    for name, img in zip(names, imgs):
        draw.text((8, y), name[:-4], fill=LABEL)
        sheet.alpha_composite(img, (8, y + 14))
        y += img.height + 22
    path = os.path.join(OUT, "gui_sheet.png")
    sheet.save(path)
    print("wrote", os.path.relpath(path, ROOT), f"({len(names)} panels)")


if __name__ == "__main__":
    _atlas("block", "blocks_sheet.png")
    _atlas("item", "items_sheet.png", columns=10)
    _gui_strip()
