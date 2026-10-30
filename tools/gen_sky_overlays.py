#!/usr/bin/env python3
"""Generate the NeoForge 26.3 copy of every Nerospace dimension type, with the planet skybox renderer selected.

On NeoForge 26.3 a dimension type picks a custom sky renderer through the ``neoforge:custom_skybox`` environment
attribute. Only NeoForge registers that attribute, so it cannot go in the shared resources (Fabric and Forge would
reject the whole dimension type). This writes
``neoforge/src/main/resources-26.3/data/nerospace/dimension_type/<name>.json`` (a NeoForge-only overlay for 26.3 and
later, staged after the shared overlays by ``stonecutter.gradle``) as the effective 26.3 dimension type (the shared
``common/src/main/resources-26.3`` copy when there is one, else the base file) plus
``attributes["neoforge:custom_skybox"] = "nerospace:planet"``. Nothing else changes.

Edit only the shared dimension types, then run this script. ``SkyAssetsTest`` fails the build if an overlay is
missing, stale or orphaned.

Usage (from anywhere):
    python tools/gen_sky_overlays.py           # write the overlays
    python tools/gen_sky_overlays.py --check   # exit 1 if an overlay is missing, stale or orphaned (writes nothing)
"""

import argparse
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
REL = Path("data") / "nerospace" / "dimension_type"
BASE = REPO / "common" / "src" / "main" / "resources" / REL
SHARED_263 = REPO / "common" / "src" / "main" / "resources-26.3" / REL
OUT = REPO / "neoforge" / "src" / "main" / "resources-26.3" / REL

ATTRIBUTE = "neoforge:custom_skybox"
SKY_ID = "nerospace:planet"


def expected(name):
    src = SHARED_263 / name if (SHARED_263 / name).exists() else BASE / name
    obj = json.loads(src.read_text(encoding="utf-8"))
    attrs = dict(obj.get("attributes", {}))
    attrs[ATTRIBUTE] = SKY_ID
    obj["attributes"] = dict(sorted(attrs.items()))
    if "attributes" in obj and list(obj).index("attributes") != 1 and "ambient_light" in obj:
        obj = {k: obj[k] for k in ["ambient_light", "attributes"] + [k for k in obj if k not in ("ambient_light", "attributes")]}
    return json.dumps(obj, indent=2, ensure_ascii=False) + "\n"


def main(argv=None):
    ap = argparse.ArgumentParser(description="NeoForge 26.3 dimension type overlays (planet sky renderer)")
    ap.add_argument("--check", action="store_true", help="verify only; exit 1 on a missing/stale/orphaned overlay")
    args = ap.parse_args(argv)
    names = sorted(p.name for p in BASE.glob("*.json"))
    problems = []
    for name in names:
        want = expected(name)
        target = OUT / name
        have = target.read_text(encoding="utf-8").replace("\r\n", "\n") if target.exists() else None
        if have == want:
            continue
        if args.check:
            problems.append(("missing " if have is None else "stale ") + str(target.relative_to(REPO)))
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(want, encoding="utf-8", newline="\n")
            print("gen_sky_overlays: wrote", target.relative_to(REPO))
    for orphan in sorted(OUT.glob("*.json")) if OUT.exists() else []:
        if orphan.name not in names:
            problems.append("orphaned " + str(orphan.relative_to(REPO)))
    if problems:
        for p in problems:
            print("gen_sky_overlays:", p)
        return 1
    if args.check:
        print("gen_sky_overlays: %d overlay(s) up to date" % len(names))
    return 0


if __name__ == "__main__":
    sys.exit(main())
