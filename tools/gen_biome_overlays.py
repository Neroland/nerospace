#!/usr/bin/env python3
"""Generate the Minecraft 26.3 copy of every Nerospace biome.

Minecraft 26.3 moved a biome's mob spawns out of the top-level ``spawners`` / ``spawn_costs`` fields and into the
``minecraft:gameplay/natural_mob_spawns`` environment attribute. 26.3 ignores the old fields without an error, so a
biome written for 26.1.2 / 26.2 loads fine on 26.3 but spawns no mobs at all.

For every ``common/src/main/resources/data/<namespace>/worldgen/biome/<name>.json`` this writes
``common/src/main/resources-26.3/data/<namespace>/worldgen/biome/<name>.json`` (the 26.3 resource overlay, which
replaces the shared file on 26.3 only). The overlay is the base biome with:

* ``spawners`` converted to ``attributes["minecraft:gameplay/natural_mob_spawns"]`` as
  ``{"argument": {"spawn_costs": ..., "spawns_by_category": ...}, "modifier": "overlay"}`` (the shape vanilla 26.3
  writes). Each entry ``{type, minCount, maxCount, weight}`` becomes ``{type, count, weight}``: ``count`` is a plain
  int when ``minCount == maxCount``, else a ``minecraft:uniform`` int provider. Every spawn category is written, empty
  ones as ``[]``, so the biome never inherits a category from anywhere else (as vanilla ``the_void`` does);
* ``spawn_costs`` moved into that attribute;
* everything else (``attributes``, ``effects``, ``carvers``, ``features``, climate) unchanged.

Edit only the shared biome, then run this script. ``BiomeOverlay26_3Test`` fails the build if an overlay is missing
or its spawns differ from the shared biome.

Usage (from anywhere):
    python tools/gen_biome_overlays.py           # write the overlays
    python tools/gen_biome_overlays.py --check   # exit 1 if an overlay is missing, stale or orphaned (writes nothing)
"""

import argparse
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
MAIN = REPO / "common" / "src" / "main"
BASE = MAIN / "resources" / "data"
OVERLAY = MAIN / "resources-26.3" / "data"

ATTRIBUTE = "minecraft:gameplay/natural_mob_spawns"

# The mob categories a 26.3 biome lists (vanilla MobCategory serialized names).
CATEGORIES = (
    "ambient", "axolotls", "creature", "misc", "monster",
    "underground_water_creature", "water_ambient", "water_creature",
)


def count(entry, where):
    low, high = entry["minCount"], entry["maxCount"]
    if low > high:
        raise SystemExit(f"{where}: minCount {low} > maxCount {high} for {entry['type']}")
    if low == high:
        return low
    return {"type": "minecraft:uniform", "min_inclusive": low, "max_inclusive": high}


def convert(biome, where):
    out = dict(biome)
    spawners = out.pop("spawners", {}) or {}
    spawn_costs = out.pop("spawn_costs", {}) or {}
    unknown = sorted(set(spawners) - set(CATEGORIES))
    if unknown:
        raise SystemExit(f"{where}: unknown spawn categories {unknown}")
    by_category = {}
    for category in CATEGORIES:
        by_category[category] = [
            {"type": e["type"], "count": count(e, where), "weight": e["weight"]}
            for e in spawners.get(category, [])
        ]
    attributes = dict(out.get("attributes", {}))
    if ATTRIBUTE in attributes:
        raise SystemExit(f"{where}: already has {ATTRIBUTE}; keep spawns in 'spawners' in the shared biome")
    attributes[ATTRIBUTE] = {
        "argument": {"spawn_costs": spawn_costs, "spawns_by_category": by_category},
        "modifier": "overlay",
    }
    out["attributes"] = attributes
    return out


def namespaces():
    return sorted(d.name for d in BASE.iterdir() if (d / "worldgen" / "biome").is_dir())


def expected():
    out = {}
    for ns in namespaces():
        biomes = BASE / ns / "worldgen" / "biome"
        for path in sorted(biomes.rglob("*.json")):
            rel = path.relative_to(biomes)
            biome = json.loads(path.read_text(encoding="utf-8"))
            target = OVERLAY / ns / "worldgen" / "biome" / rel
            out[target] = json.dumps(convert(biome, f"{ns}:{rel.with_suffix('').as_posix()}"),
                                     indent=2, sort_keys=True) + "\n"
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = expected()
    existing = set()
    if OVERLAY.exists():
        for ns in OVERLAY.iterdir():
            biomes = ns / "worldgen" / "biome"
            if biomes.is_dir():
                existing |= set(biomes.rglob("*.json"))
    problems = []
    for path, text in files.items():
        current = path.read_text(encoding="utf-8") if path.exists() else None
        if current != text:
            if args.check:
                problems.append(f"stale or missing: {path.relative_to(REPO)}")
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                with open(path, "w", encoding="utf-8", newline="\n") as out:
                    out.write(text)
    for path in sorted(existing - set(files)):
        problems.append(f"orphan (no shared biome): {path.relative_to(REPO)}")
    for line in problems:
        print(line)
    if problems:
        return 1
    print(f"{len(files)} 26.3 biome overlays {'ok' if args.check else 'written'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
