#!/usr/bin/env python3
"""
Generate Blockbench .bbmodel source files for the Nerospace textures.

- Blocks   -> a full 16x16x16 cube, all six faces UV-mapped to the texture.
- Items    -> a flat 1px-thick plate (north/south faces) showing the sprite.
- Entities -> a `modded_entity` multi-cube model whose geometry MIRRORS the Java
              LayerDefinition (GreenxertzCreatureModel for the mobs, RocketModel for
              the rocket), with box-UV cubes at the same texOffs as the Java code.

Each texture is embedded as a base64 data URL (so the file opens self-contained)
AND linked by relative_path back to the live mod resource, so painting in
Blockbench and choosing "Save All Textures" writes straight into the mod.

When you add a new block/item: append its id to BLOCKS or ITEMS and rerun.
When you add/adjust an entity model: keep the cube lists below in sync with the
Java LayerDefinition (from = part.offset + box.origin; to = from + box.size).
"""
import base64
import json
import os
import sys
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/nerospace/textures")
OUT_BLOCK = os.path.join(ROOT, "art/blockbench/block")
OUT_ITEM = os.path.join(ROOT, "art/blockbench/item")
OUT_ENTITY = os.path.join(ROOT, "art/blockbench/entity")
os.makedirs(OUT_BLOCK, exist_ok=True)
os.makedirs(OUT_ITEM, exist_ok=True)
os.makedirs(OUT_ENTITY, exist_ok=True)

BLOCKS = ["nerosium_ore", "deepslate_nerosium_ore", "nerosium_block",
          "raw_nerosium_block", "nerosium_grinder",
          "nerosteel_ore", "xertz_quartz_ore", "nerosteel_block",
          "rocket_launch_pad", "fuel_tank", "oxygen_generator",
          "cindrite_ore", "cindrite_block",
          "station_floor", "station_wall"]
ITEMS = ["nerosium_ingot", "nerosium_dust", "raw_nerosium", "nerosium_pickaxe",
         "raw_nerosteel", "nerosteel_ingot", "xertz_quartz", "greenxertz_navigator",
         "rocket_fuel_canister", "rocket_tier_1", "rocket_tier_2", "rocket_tier_3",
         "cindrite", "rocket_fuel_bucket",
         "station_compass", "greenxertz_compass", "cindara_compass"]

# Entity cube lists — keep in sync with the Java LayerDefinitions. Each cube is
# (part_name, from[x,y,z], to[x,y,z], uv_offset[u,v]).  64x64 sheets.
CREATURE_CUBES = [  # GreenxertzCreatureModel (shared by the three mobs)
    ("body",            [-4, 8, -3], [4, 16, 3],   [0, 0]),
    ("head",            [-3, 6, -7], [3, 12, -1],  [0, 14]),
    ("leg_front_left",  [-4, 16, -3], [-2, 24, -1], [28, 0]),
    ("leg_front_right", [2, 16, -3], [4, 24, -1],  [28, 0]),
    ("leg_back_left",   [-4, 16, 1], [-2, 24, 3],  [28, 0]),
    ("leg_back_right",  [2, 16, 1],  [4, 24, 3],   [28, 0]),
]
ROCKET_CUBES = [  # RocketModel
    ("body",      [-4, -16, -4], [4, 20, 4],  [0, 0]),
    ("nose",      [-3, -24, -3], [3, -16, 3], [0, 46]),
    ("fin_north", [-1, 14, -8],  [1, 24, -4], [40, 0]),
    ("fin_south", [-1, 14, 4],   [1, 24, 8],  [40, 0]),
    ("fin_west",  [-8, 14, -1],  [-4, 24, 1], [40, 0]),
    ("fin_east",  [4, 14, -1],   [8, 24, 1],  [40, 0]),
]
# entity bbmodel name -> (entity texture name, cube list)
ENTITIES = {
    "xertz_stalker": ("xertz_stalker", CREATURE_CUBES),
    "quartz_crawler": ("quartz_crawler", CREATURE_CUBES),
    "greenling": ("greenling", CREATURE_CUBES),
    "cinder_stalker": ("cinder_stalker", CREATURE_CUBES),
    "rocket": ("rocket", ROCKET_CUBES),
}


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
        "folder": folder,
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
    return {s: {"uv": [0, 0, 16, 16], "texture": 0}
            for s in ("north", "east", "south", "west", "up", "down")}


def plate_faces():
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
    else:
        frm, to, faces = [0, 0, 7.5], [16, 16, 8.5], plate_faces()

    element = {
        "name": name, "box_uv": False, "rescale": False, "locked": False,
        "render_order": "default", "allow_mirror_modeling": True,
        "from": frm, "to": to, "autouv": 0, "color": 0,
        "origin": [8, 8, 8], "uv_offset": [0, 0],
        "faces": faces, "type": "cube", "uuid": el_uuid,
    }
    doc = {
        "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
        "name": name, "model_identifier": "", "visible_box": [1, 1, 0],
        "variable_placeholders": "", "variable_placeholder_buttons": [],
        "timeline_setups": [], "unhandled_root_fields": {},
        "resolution": {"width": 16, "height": 16},
        "elements": [element], "outliner": [el_uuid],
        "textures": [texture_entry(name, folder)],
    }
    out_dir = OUT_BLOCK if kind == "block" else OUT_ITEM
    path = os.path.join(out_dir, name + ".bbmodel")
    # ADDITIVE-ONLY: never clobber an existing .bbmodel (pass --force to override).
    if os.path.exists(path) and "--force" not in sys.argv:
        print("skip (exists)", os.path.relpath(path, ROOT))
        return
    with open(path, "w") as fh:
        json.dump(doc, fh, indent=2)
    print("wrote", os.path.relpath(path, ROOT))


def box_uv_faces():
    # In box-UV mode Blockbench derives face UVs from uv_offset + cube size on load.
    return {s: {"uv": [0, 0, 0, 0], "texture": 0}
            for s in ("north", "east", "south", "west", "up", "down")}


def to_blockbench_y(frm, to):
    """Java entity models are Y-down (rendered with a flip); Blockbench's modded-entity editor is
    Y-up. Mapping bb_y = 24 - java_y (both Nerospace models rest on the ground at java y=24) keeps the
    cube lists in readable Java coordinates while making the .bbmodel display upright on the grid."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    return [x0, 24 - y1, z0], [x1, 24 - y0, z1]


def make_entity_bbmodel(name, texname, cubes):
    """A `modded_entity` model: one box-UV cube per part, each in its own bone."""
    path = os.path.join(OUT_ENTITY, name + ".bbmodel")
    if os.path.exists(path) and "--force" not in sys.argv:
        print("skip (exists)", os.path.relpath(path, ROOT))
        return
    elements, outliner = [], []
    for (pname, frm, to, uv_offset) in cubes:
        bb_from, bb_to = to_blockbench_y(frm, to)
        cu = str(uuid.uuid4())
        elements.append({
            "name": pname, "box_uv": True, "uv_offset": uv_offset, "rescale": False,
            "locked": False, "render_order": "default", "allow_mirror_modeling": True,
            "from": bb_from, "to": bb_to, "autouv": 0, "color": 0, "origin": [0, 0, 0],
            "faces": box_uv_faces(), "type": "cube", "uuid": cu,
        })
        outliner.append({
            "name": pname, "origin": [0, 0, 0], "color": 0, "uuid": str(uuid.uuid4()),
            "export": True, "isOpen": False, "visibility": True, "autouv": 0, "children": [cu],
        })
    doc = {
        "meta": {"format_version": "4.10", "model_format": "modded_entity", "box_uv": True},
        "name": name, "model_identifier": "", "visible_box": [2, 3, 0],
        "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [],
        "unhandled_root_fields": {}, "resolution": {"width": 64, "height": 64},
        "elements": elements, "outliner": outliner,
        "textures": [texture_entry(texname, "entity")],
    }
    with open(path, "w") as fh:
        json.dump(doc, fh, indent=2)
    print("wrote", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    for n in BLOCKS:
        make_bbmodel(n, "block", "block")
    for n in ITEMS:
        make_bbmodel(n, "item", "item")
    for name, (texname, cubes) in ENTITIES.items():
        make_entity_bbmodel(name, texname, cubes)
    print("done")
