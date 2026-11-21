#!/usr/bin/env python3
"""Give Luna, Mars, Nyxara and Cindara their own terrain noise.

Vanilla seeds every ``worldgen/noise`` instance from the hash of its **registry id**, so two dimensions whose noise
routers name the same noise ids get the same heightmap for the same world seed. These four planets used the vanilla
overworld router (or a copy of it), which made their hills line up with the Overworld's at the same x/z.

This script copies every vanilla density function and noise the planet's router reaches (transitively) under a
per-planet id (``nerospace:<planet>/<vanilla path>``) with the **vanilla parameters unchanged**, and repoints the
router (and the 26.3 ``debug_functions`` readout) at the copies. The copies are taken from the matching game jar for each resource tree (26.2 for the shared
tree, 26.3 for the 26.3 overlay), so both trees hold the same file set and every file parses on its line. Density
functions that read no noise (``minecraft:y`` and the like) stay shared. Surface rules are not touched.

Glacira and Greenxertz keep the shared router on purpose (their terrain continues across old chunks).

Europa is different: its terrain is **authored** here (``europa_tree``), not copied. One abstract density tree is
emitted in both trees' spellings, with its own noises, and its noise settings get their router from it.

Usage (from anywhere; needs the Loom cache jars, i.e. one Fabric build per line):
    python tools/gen_planet_noise.py           # write the copies and repoint the routers
    python tools/gen_planet_noise.py --check   # exit 1 if anything is missing or stale (writes nothing)
"""

import argparse
import glob
import json
import math
import sys
import zipfile
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
MAIN = REPO / "common" / "src" / "main"
TREES = {"resources": "26.2", "resources-26.3": "26.3"}
VERIFY = {"resources": ["26.1.2", "26.2"], "resources-26.3": ["26.3"]}
PLANETS = ["luna", "mars", "nyxara", "cindara"]
# Planets whose terrain is authored here rather than copied from vanilla (see europa_tree()).
AUTHORED = ["europa"]
# The vanilla climate noises an authored planet's biome source reads, copied under the planet's id.
AUTHORED_CLIMATE = {"continents": "continentalness", "erosion": "erosion", "ridges": "ridge",
                    "temperature": "temperature", "vegetation": "vegetation"}
SHIFT_TYPES = {"minecraft:shift_a", "minecraft:shift_b", "minecraft:shift"}
DF = "data/minecraft/worldgen/density_function/"
NOISE = "data/minecraft/worldgen/noise/"


def jar(version):
    hits = sorted(glob.glob(str(REPO / ".gradle" / "loom-cache" / "minecraftMaven" / "net" / "minecraft"
                                / "minecraft-merged-*" / version / ("*-" + version + ".jar"))))
    if not hits:
        sys.exit("no Loom-cache jar for " + version + " (run a Fabric build for that line first)")
    return zipfile.ZipFile(hits[0])


class Vanilla:
    def __init__(self, version):
        self.zip = jar(version)
        names = self.zip.namelist()
        self.dfs = {n[len(DF):-5] for n in names if n.startswith(DF) and n.endswith(".json")}
        self.noises = {n[len(NOISE):-5] for n in names if n.startswith(NOISE) and n.endswith(".json")}
        self.cache = {}
        self.types = set()
        for n in names:
            if n.startswith("data/minecraft/worldgen/") and n.endswith(".json"):
                self._collect_types(json.loads(self.zip.read(n)))

    def _collect_types(self, node):
        if isinstance(node, dict):
            t = node.get("type")
            if isinstance(t, str):
                self.types.add(t)
            for v in node.values():
                self._collect_types(v)
        elif isinstance(node, list):
            for v in node:
                self._collect_types(v)

    def df(self, path):
        key = ("df", path)
        if key not in self.cache:
            self.cache[key] = json.loads(self.zip.read(DF + path + ".json"))
        return self.cache[key]

    def noise(self, path):
        return json.loads(self.zip.read(NOISE + path + ".json"))


_VANILLA = {}


def vanilla_for(version):
    if version not in _VANILLA:
        _VANILLA[version] = Vanilla(version)
    return _VANILLA[version]


def strip(ref):
    return ref[len("minecraft:"):] if ref.startswith("minecraft:") else None


class Rewriter:
    """Rewrites one planet's router against one vanilla version, collecting the files to write."""

    def __init__(self, vanilla, planet, verify=()):
        self.v = vanilla
        self.planet = planet
        self.verify = [v for v in verify if v is not vanilla]
        self.out_dfs = {}
        self.out_noises = set()
        self.reads_noise = {}
        self.unknown = set()

    def df_reads_noise(self, path, stack=()):
        if path in self.reads_noise:
            return self.reads_noise[path]
        if path in stack:
            return False
        found = self._reads(self.v.df(path), None, stack + (path,))
        self.reads_noise[path] = found
        return found

    def _reads(self, node, parent_type, stack):
        if isinstance(node, dict):
            t = node.get("type")
            for k, val in node.items():
                if k == "type":
                    continue
                if isinstance(val, str):
                    if k == "noise" or (k == "argument" and t in SHIFT_TYPES):
                        return True
                    p = strip(val)
                    if p in self.v.dfs and self.df_reads_noise(p, stack):
                        return True
                elif self._reads(val, t, stack):
                    return True
        elif isinstance(node, list):
            return any(self._reads(x, parent_type, stack) for x in node)
        return False

    def portable(self, path):
        """A density function is copied only if every type it uses exists on every line the tree serves. The cave
        functions changed shape at 26.2 (``weird_scaled_sampler`` out, ``interval_select`` in), so on the shared
        tree they stay vanilla references (their noises stay shared; only caves, not the terrain shape)."""
        used = types_in(self.v.df(path), set())
        return all(t in v.types for v in self.verify for t in used)

    def own(self, path):
        return "nerospace:" + self.planet + "/" + path

    def rewrite(self, node):
        if isinstance(node, str):
            p = strip(node)
            if p in self.v.dfs:
                if self.df_reads_noise(p) and self.portable(p):
                    self.copy_df(p)
                    return self.own(p)
                return node
            if node.startswith("minecraft:"):
                self.unknown.add(node)
            return node
        if isinstance(node, list):
            return [self.rewrite(x) for x in node]
        if isinstance(node, dict):
            t = node.get("type")
            out = {}
            for k, val in node.items():
                if k == "type":
                    out[k] = val
                elif isinstance(val, str) and (k == "noise" or (k == "argument" and t in SHIFT_TYPES)):
                    p = strip(val)
                    if p is None or p not in self.v.noises:
                        sys.exit("unknown noise " + val)
                    self.out_noises.add(p)
                    out[k] = self.own(p)
                else:
                    out[k] = self.rewrite(val)
            return out
        return node

    def copy_df(self, path):
        if path in self.out_dfs:
            return
        self.out_dfs[path] = None  # cycle guard
        self.out_dfs[path] = self.rewrite(self.v.df(path))


def dump(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False) + "\n"


def types_in(node, acc):
    if isinstance(node, dict):
        t = node.get("type")
        if isinstance(t, str):
            acc.add(t)
        for v in node.values():
            types_in(v, acc)
    elif isinstance(node, list):
        for v in node:
            types_in(v, acc)
    return acc


# --- authored terrain ------------------------------------------------------------------------------------------------
# An authored planet's density functions are written once as an abstract tree and emitted in each tree's spelling:
#   number                      a constant
#   ("ref", name)               another density function of the same planet
#   ("noise", name, xz_scale)   a 2D noise of the same planet (y_scale 0)
#   ("ramp", y0, v0, y1, v1)    the clamped Y gradient
#   ("add"|"mul"|"min"|"max", a, b), ("abs", x), ("clamp", x, lo, hi), ("interp", x)

def _n(name):
    """A 2D noise clamped to [-1, 1] before it is scaled, so every layer boundary has an exact Y interval."""
    return ("clamp", ("noise", name, 1.0), -1.0, 1.0)


def europa_tree():
    """Europa: a sea floor, an ocean, an ice shell with an underside and a top. Three linear Y ramps, each offset by
    clamped 2D noise. Returns ({density function name: tree}, {noise name: parameters})."""
    topside = ("add",
               ("add",
                ("add", ("ramp", 160, 1.0, 224, -1.0), ("mul", 0.375, _n("surface"))),
                # chaos rafts: 0 .. +0.25 where erosion is above the chaos threshold
                ("mul", ("mul", 0.4, ("clamp", ("add", ("ref", "erosion"), -0.35), 0.0, 0.625)),
                 ("abs", _n("rafts")))),
               # lineae trough: -0.125 .. 0 along the ridges zero line
               ("mul", -0.125, ("clamp", ("add", 1.0, ("mul", -20.0, ("abs", ("ref", "ridges")))), 0.0, 1.0)))
    underside = ("add", ("ramp", 72, -1.0, 112, 1.0), ("mul", 0.4, _n("underside")))
    seabed = ("add", ("ramp", -56, 1.0, -8, -1.0), ("mul", 0.5, _n("seabed")))
    dfs = {
        "topside": topside,
        "underside": underside,
        "seabed": seabed,
        "final_density": ("interp", ("max", ("ref", "seabed"), ("min", ("ref", "underside"), ("ref", "topside")))),
        "depth": ("ramp", -64, 2.0, 320, -1.0),
    }
    for df, noise in AUTHORED_CLIMATE.items():
        dfs[df] = ("noise", noise, 0.25)
    noises = {
        "surface": {"firstOctave": -7, "amplitudes": [1.0, 1.0, 0.5]},
        "rafts": {"firstOctave": -5, "amplitudes": [1.0, 0.5]},
        "underside": {"firstOctave": -6, "amplitudes": [1.0, 1.0]},
        "seabed": {"firstOctave": -7, "amplitudes": [1.0, 1.0, 1.0]},
    }
    return dfs, noises


AUTHORED_TREES = {"europa": europa_tree}


def _parity_octaves(amplitudes):
    """26.3 NormalNoise.buildOctaves at base amplitude 1 with normalisation: the absolute octave amplitudes."""
    n = len(amplitudes)
    amp = math.pow(0.5, -(n - 1)) / (math.pow(0.5, -n) - 1.0)
    out = []
    for m in amplitudes:
        if m != 0.0:
            out.append(abs(amp * m))
        amp *= 0.5
    return out


def noise_263(params):
    """A noise in the old spelling ({firstOctave, amplitudes}) in the 26.3 spelling, the way vanilla 26.3's
    NormalNoise.createParity converts it, so the noise is unchanged: base_octave, octave_count,
    amplitude_modifiers and the parity base_amplitude. Verified against every converted noise in this repo."""
    first, amps = params["firstOctave"], [float(a) for a in params["amplitudes"]]
    octaves = _parity_octaves(amps)
    deviation = math.sqrt(sum((0.2702247831245211 * a) ** 2 for a in octaves))
    base = 1.0
    if deviation != 0.0:
        new_factor = (sum(octaves) * 0.3333333333333333) / (deviation * math.sqrt(2.0))
        live = [i for i, a in enumerate(amps) if a != 0.0]
        old_factor = 0.5 * 0.3333333333333333 / (0.1 * (1.0 + 1.0 / ((max(live) - min(live)) + 1)))
        if new_factor != 0.0:
            base = old_factor / new_factor
    out = {"base_amplitude": base, "base_octave": first}
    if len(amps) != 1:
        out["octave_count"] = len(amps)
    if any(a != 1.0 for a in amps):
        out["amplitude_modifiers"] = amps
    return out


def emit(node, dialect, planet):
    """One abstract node in a tree's spelling: dialect "resources" (26.1/26.2 and older) or "resources-26.3"."""
    new = dialect == "resources-26.3"
    if isinstance(node, (int, float)):
        return float(node)
    op = node[0]
    if op == "ref":
        return "nerospace:" + planet + "/" + node[1]
    if op == "noise":
        return {"type": "minecraft:noise", "noise": "nerospace:" + planet + "/" + node[1],
                "xz_scale": float(node[2]), "y_scale": 0.0}
    if op == "ramp":
        if new:
            return {"type": "minecraft:gradient", "axis": "y", "from_coordinate": node[1],
                    "from_value": float(node[2]), "to_coordinate": node[3], "to_value": float(node[4])}
        return {"type": "minecraft:y_clamped_gradient", "from_y": node[1], "to_y": node[3],
                "from_value": float(node[2]), "to_value": float(node[4])}
    if op in ("add", "mul", "min", "max"):
        a, b = ("left", "right") if new else ("argument1", "argument2")
        return {"type": "minecraft:" + op, a: emit(node[1], dialect, planet), b: emit(node[2], dialect, planet)}
    if op == "abs":
        return {"type": "minecraft:abs", ("input" if new else "argument"): emit(node[1], dialect, planet)}
    if op == "clamp":
        return {"type": "minecraft:clamp", "input": emit(node[1], dialect, planet),
                "min": float(node[2]), "max": float(node[3])}
    if op == "interp":
        if new:
            return {"type": "minecraft:interpolated", "cell_size_xz": 4, "cell_size_y": 8,
                    "input": emit(node[1], dialect, planet)}
        return {"type": "minecraft:interpolated", "argument": emit(node[1], dialect, planet)}
    sys.exit("unknown authored node " + str(op))


def surface_level(planet, dialect):
    """The top-surface search over the planet's topside function. The +/-0.390625 pair is the shape the build's
    1.21.x rewrite recognises (it turns the inner sum into the old engine's initial density)."""
    top = "nerospace:" + planet + "/topside"
    if dialect == "resources-26.3":
        return {"type": "minecraft:interpolated", "cell_size_xz": 16, "cell_size_y": 1,
                "input": {"type": "minecraft:find_top_surface", "cell_height": 8, "lower_bound": -64,
                          "upper_bound": 224.0,
                          "density": {"type": "minecraft:add",
                                      "left": {"type": "minecraft:add", "left": 0.390625, "right": top},
                                      "right": -0.390625}}}
    return {"type": "minecraft:find_top_surface", "cell_height": 8, "lower_bound": -64, "upper_bound": 224.0,
            "density": {"type": "minecraft:add", "argument1": -0.390625,
                        "argument2": {"type": "minecraft:add", "argument1": 0.390625, "argument2": top}}}


def write_authored(planet, tree, vanilla, writes):
    """Adds an authored planet's noise, density-function and noise-settings files for one resource tree to
    ``writes``. Returns the used types (checked against the vanilla jars by the caller)."""
    dfs, noises = AUTHORED_TREES[planet]()
    base = MAIN / tree / "data" / "nerospace" / "worldgen"
    own = "nerospace:" + planet + "/"
    used = set()
    for name, node in dfs.items():
        body = emit(node, tree, planet)
        types_in(body, used)
        writes[base / "density_function" / planet / (name + ".json")] = dump(body)
    for name, params in noises.items():
        body = noise_263(params) if tree == "resources-26.3" else params
        writes[base / "noise" / planet / (name + ".json")] = dump(body)
    for noise in AUTHORED_CLIMATE.values():
        writes[base / "noise" / planet / (noise + ".json")] = dump(vanilla.noise(noise))
    ns_path = base / "noise_settings" / (planet + ".json")
    settings = json.loads(ns_path.read_text(encoding="utf-8"))
    climate = {k: own + k for k in ("continents", "depth", "erosion", "final_density", "ridges", "temperature",
                                    "vegetation")}
    if tree == "resources-26.3":
        level = surface_level(planet, tree)
        types_in(level, used)
        writes[base / "density_function" / planet / "chunk_surface_level.json"] = dump(level)
        router = dict(climate)
        router["chunk_surface_level"] = own + "chunk_surface_level"
        settings["debug_functions"] = [{"function": own + f, "label": label} for label, f in (
            ("N", "final_density"), ("T", "temperature"), ("V", "vegetation"), ("C", "continents"),
            ("E", "erosion"), ("D", "depth"))]
    else:
        router = dict(climate)
        for k in ("barrier", "fluid_level_floodedness", "fluid_level_spread", "lava", "vein_toggle", "vein_ridged",
                  "vein_gap"):
            router[k] = 0.0
        router["preliminary_surface_level"] = surface_level(planet, tree)
        types_in(router["preliminary_surface_level"], used)
    settings["noise_router"] = dict(sorted(router.items()))
    writes[ns_path] = dump(settings)
    return used


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    problems = []
    writes = {}
    file_sets = {}
    verify = {}
    for tree, version in TREES.items():
        vanilla = vanilla_for(version)
        verify[tree] = [vanilla_for(v) for v in VERIFY[tree]]
        files = set()
        for planet in PLANETS:
            ns_path = MAIN / tree / "data" / "nerospace" / "worldgen" / "noise_settings" / (planet + ".json")
            settings = json.loads(ns_path.read_text(encoding="utf-8"))
            rw = Rewriter(vanilla, planet, verify[tree])
            # Restore the vanilla ids first so the script is idempotent, then rewrite.
            router = json.loads(json.dumps(settings["noise_router"]).replace("nerospace:" + planet + "/", "minecraft:"))
            settings["noise_router"] = rw.rewrite(router)
            if "debug_functions" in settings:  # 26.3 F3 readout: show the planet's own values
                dbg = json.loads(json.dumps(settings["debug_functions"]).replace("nerospace:" + planet + "/", "minecraft:"))
                settings["debug_functions"] = rw.rewrite(dbg)
            if rw.unknown:
                problems.append(tree + " " + planet + ": unresolved minecraft refs " + str(sorted(rw.unknown)))
            writes[ns_path] = dump(settings)
            for p, body in rw.out_dfs.items():
                f = MAIN / tree / "data" / "nerospace" / "worldgen" / "density_function" / planet / (p + ".json")
                writes[f] = dump(body)
                files.add(("density_function", planet, p))
            for p in rw.out_noises:
                f = MAIN / tree / "data" / "nerospace" / "worldgen" / "noise" / planet / (p + ".json")
                writes[f] = dump(vanilla.noise(p))
                files.add(("noise", planet, p))
            # Every "type" written must exist in the vanilla worldgen data of every line this tree serves.
            used = types_in(settings["noise_router"], set())
            for body in rw.out_dfs.values():
                types_in(body, used)
            for v in verify[tree]:
                missing = sorted(t for t in used if t not in v.types)
                if missing:
                    problems.append(tree + " " + planet + ": types not in vanilla jar: " + str(missing))
        for planet in AUTHORED:
            used = write_authored(planet, tree, vanilla, writes)
            for v in verify[tree]:
                missing = sorted(t for t in used if t not in v.types)
                if missing:
                    problems.append(tree + " " + planet + ": types not in vanilla jar: " + str(missing))
        file_sets[tree] = files
    if file_sets["resources"] != file_sets["resources-26.3"]:
        only_base = sorted(file_sets["resources"] - file_sets["resources-26.3"])
        only_263 = sorted(file_sets["resources-26.3"] - file_sets["resources"])
        # A base-only file would load on 26.3 too (the overlay only replaces same paths): give it a 26.3 copy.
        v263 = vanilla_for("26.3")
        for kind, planet, p in only_base:
            f = MAIN / "resources-26.3" / "data" / "nerospace" / "worldgen" / kind / planet / (p + ".json")
            if (kind == "noise" and p not in v263.noises) or (kind != "noise" and p not in v263.dfs):
                problems.append("no 26.3 vanilla twin for shared-tree " + kind + " " + p)
            elif kind == "noise":
                writes[f] = dump(v263.noise(p))
            else:
                rw = Rewriter(v263, planet)
                writes[f] = dump(rw.rewrite(v263.df(p)))
                for q in rw.out_noises:
                    g = MAIN / "resources-26.3" / "data" / "nerospace" / "worldgen" / "noise" / planet / (q + ".json")
                    writes.setdefault(g, dump(v263.noise(q)))
                for q, body in rw.out_dfs.items():
                    g = MAIN / "resources-26.3" / "data" / "nerospace" / "worldgen" / "density_function" / planet / (q + ".json")
                    writes.setdefault(g, dump(body))
        # 26.3-only files (the 26.3 router reaches more functions) need no shared-tree twin: the overlay only ever
        # adds them on 26.3, and a shared-tree copy would have to parse on 26.1.2 and 26.2 as well.
    if problems:
        print("\n".join(problems))
        sys.exit(1)
    stale = [f for f, body in writes.items() if not f.exists() or f.read_text(encoding="utf-8") != body]
    if args.check:
        for f in stale:
            print("stale or missing:", f.relative_to(REPO))
        print(str(len(writes)) + " files, " + str(len(stale)) + " stale")
        sys.exit(1 if stale else 0)
    for f in stale:
        f.parent.mkdir(parents=True, exist_ok=True)
        with open(f, "w", encoding="utf-8", newline="\n") as h:
            h.write(writes[f])
    print(str(len(writes)) + " files, " + str(len(stale)) + " written")


if __name__ == "__main__":
    main()
