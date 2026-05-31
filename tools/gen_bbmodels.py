#!/usr/bin/env python3
"""
Generate Blockbench .bbmodel source files for the Nerospace textures.

- Blocks  -> a full 16x16x16 cube, all six faces UV-mapped to the texture.
- Items   -> a flat 1px-thick plate (north/south faces) showing the sprite.

Each texture is embedded as a base64 data URL (so the file opens self-contained)
AND linked by relative_path back to the live mod resource, so painting in
Blockbench and hitting "Save All Textures" writes straight into the mod.

Output:
  art/blockbench/block/<name>.bbmodel
  art/blockbench/item/<name>.bbmodel
"""
import base64
import json
import os
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures")
OUT_BLOCK = os.path.join(ROOT, "art/blockbench/block")
OUT_ITEM = os.path.join(ROOT, "art/blockbench/item")
os.makedirs(OUT_BLOCK, exist_ok=True)
os.makedirs(OUT_ITEM, exist_ok=True)

BLOCKS = ["nerosium_ore", "deepslate_nerosium_ore", "nerosium_block",
          "raw_nerosium_block", "nerosium_grinder"]
ITEMS = ["nerosium_ingot", "nerosium_dust", "raw_nerosium", "nerosium_pickaxe"]


def data_url(png_path):
    with open(png_path, "rb") as fh:
        b = base64.b64encode(fh.read()).decode("ascii")
    return "data:image/png;base64," + b


def texture_entry(name, folder):
    png = os.path.join(TEX, folder, name + ".png")
    rel = "../../../src/main/resources/assets/nerospace/textures/%s/%s.png" % (folder, name)
    return {
        "path": os.path.normpath(png),
        "name": name + ".png",
        "folder": "block" if folder == "block" else "item",
        "namespace": "nerospace",
        "id": "0",
        "particle": True,
        "render_mode": "default",
        "render_sides": "auto",
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "mode": "bitmap",
        "saved": True,
        "uuid": str(uuid.uuid4()),
        "relative_path": rel,
        "source": data_url(png),
    }


def cube_faces():
    f = {}
    for side in ("north", "east", "south", "west", "up", "down"):
        f[side] = {"uv": [0, 0, 16, 16], "texture": 0}
    return f


def plate_faces():
    # flat sprite: only front (north) and back (south) carry the texture
    return {
        "north": {"uv": [0, 0, 16, 16], "texture": 0},
        "south": {"uv": [16, 0, 0, 16], "texture": 0},
        "east": {"uv": [0, 0, 0, 16], "texture": None},
        "west": {"uv": [0, 0, 0, 16], "texture": None},
        "up": {"uv": [0, 0, 16, 0], "texture": None},
        "down": {"uv": [0, 0, 16, 0], "texture": None},
    }


def make_bbmodel(name, folder, kind):
    el_uuid = str(uuid.uuid4())
    if kind == "block":
        frm, to, faces = [0, 0, 0], [16, 16, 16], cube_faces()
        model_format = "java_block"
    else:
        # 1px-thick plate centred on z, standing upright like an item sprite
        frm, to, faces = [0, 0, 7.5], [16, 16, 8.5], plate_faces()
        model_format = "java_block"

    element = {
        "name": name,
        "box_uv": False,
        "rescale": False,
        "locked": False,
        "render_order": "default",
        "allow_mirror_modeling": True,
        "from": frm,
        "to": to,
        "autouv": 0,
        "color": 0,
        "origin": [8, 8, 8],
        "uv_offset": [0, 0],
        "faces": faces,
        "type": "cube",
        "uuid": el_uuid,
    }

    doc = {
        "meta": {
            "format_version": "4.10",
            "model_format": model_format,
            "box_uv": False,
        },
        "name": name,
        "model_identifier": "",
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 16, "height": 16},
        "elements": [element],
        "outliner": [el_uuid],
        "textures": [texture_entry(name, folder)],
    }
    out_dir = OUT_BLOCK if kind == "block" else OUT_ITEM
    path = os.path.join(out_dir, name + ".bbmodel")
    with open(path, "w") as fh:
        json.dump(doc, fh, indent=2)
    print("wrote", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    for n in BLOCKS:
        make_bbmodel(n, "block", "block")
    for n in ITEMS:
        make_bbmodel(n, "item", "item")
    print("done")
