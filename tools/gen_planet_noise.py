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

Usage (from anywhere; needs the Loom cache jars, i.e. one Fabric build per line):
    python tools/gen_planet_noise.py           # write the copies and repoint the routers
    python tools/gen_planet_noise.py --check   # exit 1 if anything is missing or stale (writes nothing)
"""

import argparse
import glob
import json
import sys
import zipfile
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
MAIN = REPO / "common" / "src" / "main"
TREES = {"resources": "26.2", "resources-26.3": "26.3"}
VERIFY = {"resources": ["26.1.2", "26.2"], "resources-26.3": ["26.3"]}
PLANETS = ["luna", "mars", "nyxara", "cindara"]
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
