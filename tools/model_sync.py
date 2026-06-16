#!/usr/bin/env python3
"""
Bidirectional sync between Blockbench entity models and their Java LayerDefinition code.

Each entity model has two representations:
  - a Blockbench `.bbmodel` (the visual editor surface, embeds + links its texture), and
  - a Java `EntityModel` class whose `createBodyLayer()` builds the mesh.

This tool keeps them in sync in BOTH directions:
  - edit geometry in Blockbench  -> run this (or build) -> the Java `createBodyLayer()` is rewritten;
  - edit the Java cube block      -> run this (or build) -> the `.bbmodel` is rewritten.

How it stays robust:
  * The Java geometry lives in a NORMALISED, marker-delimited block (between `// model_sync:begin`
    and `// model_sync:end`). Only that block is ever rewritten; the rest of the class is untouched.
    The block is plain `addOrReplaceChild(... addBox ... PartPose.offset ...)` calls you can still
    hand-edit; just keep that shape.
  * Java entity space is Y-down; Blockbench's modded-entity editor is Y-up. We apply `bb_y = 24 - java_y`
    (and the inverse) so both display upright. Cube origins (`PartPose.offset`) are folded into absolute
    `addBox` coordinates, so each part is one bone at origin 0 with one cube — a clean 1:1 mapping.

Direction is chosen by file mtime (newest wins) unless forced with --to-java / --to-bbmodel.

Limitations (by design, for robustness): one cube per bone, no per-bone rotation, box-UV only. These
cover the current Nerospace models; Blockbench remains the place to do richer modelling, but then the
Java must be exported via Blockbench's own "Export Java Entity" rather than this tool.

Usage:
  python3 tools/model_sync.py                # auto (newest side drives), all models
  python3 tools/model_sync.py --to-java      # force .bbmodel -> Java
  python3 tools/model_sync.py --to-bbmodel   # force Java -> .bbmodel
  python3 tools/model_sync.py --check        # report drift, change nothing (exit 1 if out of sync)
  python3 tools/model_sync.py --watch        # poll and sync continuously
"""
import base64
import json
import os
import re
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_ENTITY = "src/main/resources/assets/nerospace/textures/entity"

BEGIN = "// model_sync:begin"
END = "// model_sync:end"
GROUND = 24  # java y of the model's ground contact; bb_y = GROUND - java_y

# Each entry: the Java model class <-> its authoritative .bbmodel, plus geometry-mirror .bbmodels
# (same shape, different texture) that follow the authoritative geometry but keep their own texture.
#
# Art overhaul A3 (ART_OVERHAUL_DESIGN.md §4.1): every creature and rocket tier has its OWN source
# now — the legacy shared GreenxertzCreatureModel and its geometry mirrors are retired. Models with
# rotated or multi-cube parts keep those parts OUTSIDE their marker block (Java-authoritative); the
# bbmodel carries the syncable core only.
def _entry(java_class, name, texture=None):
    return {
        "java": "src/main/java/za/co/neroland/nerospace/client/" + java_class + ".java",
        "bbmodel": "art/blockbench/entity/" + name + ".bbmodel",
        "mirrors": [],
        "texture": texture or name,
    }


REGISTRY = [
    _entry("XertzStalkerModel", "xertz_stalker"),
    _entry("QuartzCrawlerModel", "quartz_crawler"),
    _entry("GreenlingModel", "greenling"),
    _entry("AlienVillagerModel", "alien_villager"),
    _entry("CinderStalkerModel", "cinder_stalker"),
    _entry("FrostStriderModel", "frost_strider"),
    _entry("MeadowLoperModel", "meadow_loper"),
    _entry("EmberStrutterModel", "ember_strutter"),
    _entry("WoollyDriftModel", "woolly_drift"),
    _entry("RocketModel", "rocket_t1"),
    _entry("RocketT2Model", "rocket_t2"),
    _entry("RocketT3Model", "rocket_t3"),
    _entry("RocketT4Model", "rocket_t4"),
]

# A part = dict(name, uv=[u,v], box=[x,y,z,w,h,d])  (box in ABSOLUTE java coords)

_NUM = r"(-?\d+(?:\.\d+)?)F?"
_CUBE_RE = re.compile(
    r'addOrReplaceChild\(\s*"([^"]+)"\s*,\s*'
    r'CubeListBuilder\.create\(\)\s*\.texOffs\(\s*(-?\d+)\s*,\s*(-?\d+)\s*\)\s*'
    r'\.addBox\(\s*' + r"\s*,\s*".join([_NUM] * 6) + r'\s*\)\s*,\s*'
    r'PartPose\.offset\(\s*' + r"\s*,\s*".join([_NUM] * 3) + r'\s*\)\s*\)\s*;',
    re.DOTALL,
)


def rd(rel):
    with open(os.path.join(ROOT, rel)) as fh:
        return fh.read()


def wr(rel, text):
    with open(os.path.join(ROOT, rel), "w") as fh:
        fh.write(text)


def fnum(v):
    f = float(v)
    return int(f) if f == int(f) else f


# ---- Java <-> parts -------------------------------------------------------

def parse_java(rel):
    src = rd(rel)
    m = re.search(re.escape(BEGIN) + r"(.*?)" + re.escape(END), src, re.DOTALL)
    region = m.group(1) if m else src
    parts = []
    for mm in _CUBE_RE.finditer(region):
        name, u, v, bx, by, bz, bw, bh, bd, ox, oy, oz = mm.groups()
        x = fnum(ox) + fnum(bx)
        y = fnum(oy) + fnum(by)
        z = fnum(oz) + fnum(bz)
        parts.append({"name": name, "uv": [int(u), int(v)],
                      "box": [x, y, z, fnum(bw), fnum(bh), fnum(bd)]})
    return parts


def _f(v):
    return "%sF" % (("%g" % v))


def emit_java_block(parts, indent="        "):
    lines = []
    for p in parts:
        x, y, z, w, h, d = p["box"]
        u, v = p["uv"]
        lines.append(
            '%sroot.addOrReplaceChild("%s",\n'
            '%s        CubeListBuilder.create().texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s),\n'
            '%s        PartPose.offset(0.0F, 0.0F, 0.0F));'
            % (indent, p["name"], indent, u, v,
               _f(x), _f(y), _f(z), _f(w), _f(h), _f(d), indent)
        )
    return "\n".join(lines)


def write_java(rel, parts):
    src = rd(rel)
    marker_block = BEGIN + "\n" + emit_java_block(parts) + "\n        " + END
    if BEGIN in src and END in src:
        new = re.sub(re.escape(BEGIN) + r".*?" + re.escape(END),
                     lambda _m: marker_block, src, flags=re.DOTALL)
    else:
        # First run: replace everything between `mesh.getRoot();` and `return LayerDefinition`
        # (the existing hand-written cubes) with the marker-delimited generated block.
        anchor = re.search(r"getRoot\(\);\n", src)
        ret = re.search(r"\n\s*return LayerDefinition", src)
        if not anchor or not ret:
            raise SystemExit("Cannot find insertion point in %s" % rel)
        new = (src[:anchor.end()]
               + "\n        " + marker_block + "\n"
               + src[ret.start():])
    if new != src:
        wr(rel, new)
        return True
    return False


# ---- bbmodel <-> parts ----------------------------------------------------

def java_box_to_bb(box):
    x, y, z, w, h, d = box
    return [x, GROUND - (y + h), z], [x + w, GROUND - y, z + d]


def bb_cube_to_box(frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    return [x0, GROUND - y1, z0, x1 - x0, y1 - y0, z1 - z0]


def parse_bbmodel(rel):
    doc = json.loads(rd(rel))
    parts = []
    for el in doc.get("elements", []):
        if el.get("type", "cube") != "cube":
            continue
        parts.append({"name": el.get("name", "part"),
                      "uv": list(el.get("uv_offset", [0, 0])),
                      "box": bb_cube_to_box(el["from"], el["to"])})
    return parts


def _tex_entry(texture):
    png = os.path.join(ROOT, TEX_ENTITY, texture + ".png")
    src = ""
    if os.path.exists(png):
        with open(png, "rb") as fh:
            src = "data:image/png;base64," + base64.b64encode(fh.read()).decode("ascii")
    return {
        "path": os.path.normpath(png), "name": texture + ".png", "folder": "entity",
        "namespace": "nerospace", "id": "0", "particle": True, "render_mode": "default",
        "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
        "frame_interpolate": False, "visible": True, "mode": "bitmap", "saved": True,
        "uuid": _uuid("tex:" + texture),
        "relative_path": "../../../%s/%s.png" % (TEX_ENTITY, texture),
        "source": src,
    }


def _uuid(seed):
    import hashlib
    h = hashlib.md5(seed.encode()).hexdigest()
    return "%s-%s-%s-%s-%s" % (h[0:8], h[8:12], h[12:16], h[16:20], h[20:32])


def _png_size(texture):
    """The texture sheet's pixel size from the PNG IHDR (the bbmodel resolution must match the
    Java LayerDefinition sheet — rockets are 128x128 now, creatures stay 64x64)."""
    path = os.path.join(ROOT, TEX_ENTITY, texture + ".png")
    try:
        with open(path, "rb") as fh:
            head = fh.read(24)
        if len(head) >= 24 and head[:8] == b"\x89PNG\r\n\x1a\n":
            import struct
            w, h = struct.unpack(">II", head[16:24])
            return int(w), int(h)
    except OSError:
        pass
    return 64, 64


def write_bbmodel(rel, parts, texture):
    elements, outliner = [], []
    for p in parts:
        bf, bt = java_box_to_bb(p["box"])
        cu = _uuid(rel + ":cube:" + p["name"])
        elements.append({
            "name": p["name"], "box_uv": True, "uv_offset": list(p["uv"]), "rescale": False,
            "locked": False, "render_order": "default", "allow_mirror_modeling": True,
            "from": bf, "to": bt, "autouv": 0, "color": 0, "origin": [0, 0, 0],
            "faces": {s: {"uv": [0, 0, 0, 0], "texture": 0}
                      for s in ("north", "east", "south", "west", "up", "down")},
            "type": "cube", "uuid": cu,
        })
        outliner.append({"name": p["name"], "origin": [0, 0, 0], "color": 0,
                         "uuid": _uuid(rel + ":bone:" + p["name"]), "export": True,
                         "isOpen": False, "visibility": True, "autouv": 0, "children": [cu]})
    name = os.path.splitext(os.path.basename(rel))[0]
    tex_w, tex_h = _png_size(texture)
    doc = {
        "meta": {"format_version": "4.10", "model_format": "modded_entity", "box_uv": True},
        "name": name, "model_identifier": "", "visible_box": [2, 3, 0],
        "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [],
        "unhandled_root_fields": {}, "resolution": {"width": tex_w, "height": tex_h},
        "elements": elements, "outliner": outliner, "textures": [_tex_entry(texture)],
    }
    out = json.dumps(doc, indent=2)
    old = rd(rel) if os.path.exists(os.path.join(ROOT, rel)) else ""
    if out != old:
        wr(rel, out)
        return True
    return False


# ---- sync -----------------------------------------------------------------

def mtime(rel):
    p = os.path.join(ROOT, rel)
    return os.path.getmtime(p) if os.path.exists(p) else 0.0


def mirror_texture(rel):
    return os.path.splitext(os.path.basename(rel))[0]


def sync_entry(entry, forced, check):
    java, bb = entry["java"], entry["bbmodel"]
    if forced == "java":
        direction = "to-java"
    elif forced == "bbmodel":
        direction = "to-bbmodel"
    else:
        direction = "to-bbmodel" if mtime(java) > mtime(bb) else "to-java"

    changed = []
    if direction == "to-java":
        parts = parse_bbmodel(bb)
        if check:
            if parts != parse_java(java):
                changed.append(java + " (would update)")
        elif write_java(java, parts):
            changed.append(java)
        # mirrors always follow the authoritative geometry, keeping their own texture
        for mrel in entry["mirrors"]:
            if not check and write_bbmodel(mrel, parts, mirror_texture(mrel)):
                changed.append(mrel)
    else:  # to-bbmodel
        parts = parse_java(java)
        if check:
            if parts != parse_bbmodel(bb):
                changed.append(bb + " (would update)")
        else:
            if write_bbmodel(bb, parts, entry["texture"]):
                changed.append(bb)
            for mrel in entry["mirrors"]:
                if write_bbmodel(mrel, parts, mirror_texture(mrel)):
                    changed.append(mrel)
    return direction, changed


def run_once(forced, check):
    any_changed = False
    for entry in REGISTRY:
        direction, changed = sync_entry(entry, forced, check)
        for c in changed:
            print("%-11s %s" % (direction, os.path.relpath(c.split(" ")[0] if " " in c else c)))
            any_changed = True
        if not changed:
            print("%-11s %s (in sync)" % (direction, entry["java"]))
    return any_changed


def main():
    args = sys.argv[1:]
    forced = "java" if "--to-java" in args else ("bbmodel" if "--to-bbmodel" in args else None)
    check = "--check" in args
    if "--watch" in args:
        print("model_sync: watching (Ctrl-C to stop)...")
        seen = {}
        while True:
            for entry in REGISTRY:
                for rel in [entry["java"], entry["bbmodel"]] + entry["mirrors"]:
                    seen[rel] = mtime(rel)
            run_once(forced, False)
            time.sleep(1.5)
    else:
        changed = run_once(forced, check)
        if check and changed:
            sys.exit(1)


if __name__ == "__main__":
    main()
