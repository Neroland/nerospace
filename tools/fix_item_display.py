#!/usr/bin/env python3
"""Give block items the standard 3D inventory tilt.

A block model written as raw ``elements`` with no ``parent`` inherits no ``display``
transforms, so its inventory/menu icon renders FLAT (face-on) instead of the vanilla
3D tilt that ``solar_panel`` / ``launch_gantry`` / ``rocket_launch_pad`` get from
parenting ``minecraft:block/block``.

This tool finds every block model that actually serves as an inventory icon (referenced
from ``assets/nerospace/items/<id>.json``) and that has ``elements`` but neither a
``parent`` nor a ``display`` block, and inserts ``"parent": "minecraft:block/block"`` as
the first key. The model's own ``elements`` still win; it only gains the GUI display
transforms. Idempotent and additive — re-running changes nothing once applied.

Usage: python tools/fix_item_display.py --multiloader
Deps: stdlib only.
"""
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nerospace_target import resolve  # noqa: E402

RES = "common/src/main/resources/assets/nerospace"


def referenced_block_models():
    ids = set()

    def walk(n):
        if isinstance(n, dict):
            m = n.get("model")
            if isinstance(m, str):
                yield m
            for v in n.values():
                yield from walk(v)
        elif isinstance(n, list):
            for v in n:
                yield from walk(v)

    for j in glob.glob(os.path.join(resolve(f"{RES}/items"), "*.json")):
        with open(j, encoding="utf-8") as fh:
            for m in walk(json.load(fh)):
                if m.startswith("nerospace:block/"):
                    ids.add(m.split("/")[-1])
    return ids


def main():
    mb = resolve(f"{RES}/models/block")
    fixed = []
    for mid in sorted(referenced_block_models()):
        p = os.path.join(mb, f"{mid}.json")
        if not os.path.exists(p):
            continue
        with open(p, encoding="utf-8") as fh:
            d = json.load(fh)
        if d.get("elements") and "parent" not in d and "display" not in d:
            new = {"parent": "minecraft:block/block"}
            new.update(d)
            with open(p, "w", encoding="utf-8") as fh:
                json.dump(new, fh, indent=2)
                fh.write("\n")
            fixed.append(mid)
    print(f"added inventory display parent to {len(fixed)} models:")
    for m in fixed:
        print("  ", m)
    if not fixed:
        print("  (nothing to do — already 3D)")


if __name__ == "__main__":
    main()
