#!/usr/bin/env python3
"""
Asset resolution checker (ART_OVERHAUL_DESIGN.md §2 / RELEASE_CHECKLIST §2 "every model resolves").

Cross-references the datagen output against the committed art:
  1. every texture referenced by a generated model JSON must exist as a PNG;
  2. every model referenced by a generated blockstate must exist as a model JSON;
  3. every model referenced by a generated client-item definition must exist as a model JSON.

Run AFTER `gradlew runData`. Exits non-zero on any miss, listing every offender — the automated
half of the "zero missing-texture placeholders" gate (the visual half stays a runClient check).

Usage: python3 tools/check_assets.py
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GEN_ASSETS = os.path.join(ROOT, "src/generated/resources/assets/nerospace")
MAIN_ASSETS = os.path.join(ROOT, "src/main/resources/assets/nerospace")

MODELS_DIR = os.path.join(GEN_ASSETS, "models")
BLOCKSTATES_DIR = os.path.join(GEN_ASSETS, "blockstates")
ITEMS_DIR = os.path.join(GEN_ASSETS, "items")


def _walk_json(root):
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if name.endswith(".json"):
                path = os.path.join(dirpath, name)
                with open(path, encoding="utf-8") as f:
                    yield path, json.load(f)


def _collect_strings(node, key, out):
    """Collect every string value under any occurrence of `key` (recursively)."""
    if isinstance(node, dict):
        for k, v in node.items():
            if k == key and isinstance(v, str):
                out.append(v)
            else:
                _collect_strings(v, key, out)
    elif isinstance(node, list):
        for item in node:
            _collect_strings(item, key, out)


def _texture_exists(ref):
    # "#slot" indirections resolve within the model chain; only concrete refs are checked.
    if ref.startswith("#"):
        return True
    ns, _, path = ref.partition(":")
    if not path:
        ns, path = "minecraft", ns
    if ns != "nerospace":
        return True  # vanilla/other-namespace textures are the loader's problem, not ours
    return os.path.exists(os.path.join(MAIN_ASSETS, "textures", path + ".png"))


def _model_exists(ref):
    ns, _, path = ref.partition(":")
    if not path:
        ns, path = "minecraft", ns
    if ns != "nerospace":
        return True
    return os.path.exists(os.path.join(MODELS_DIR, path + ".json"))


def main():
    problems = []

    if not os.path.isdir(MODELS_DIR):
        print("check_assets: no generated models found — run `gradlew runData` first")
        return 1

    # 1. model JSON -> texture PNGs
    for path, data in _walk_json(MODELS_DIR):
        textures = data.get("textures", {})
        for slot, ref in textures.items():
            if isinstance(ref, str) and not _texture_exists(ref):
                problems.append(f"{os.path.relpath(path, ROOT)}: texture '{ref}' (slot {slot}) has no PNG")

    # 2. blockstate -> models
    for path, data in _walk_json(BLOCKSTATES_DIR):
        refs = []
        _collect_strings(data, "model", refs)
        for ref in refs:
            if not _model_exists(ref):
                problems.append(f"{os.path.relpath(path, ROOT)}: model '{ref}' has no JSON")

    # 3. client item definitions -> models
    if os.path.isdir(ITEMS_DIR):
        for path, data in _walk_json(ITEMS_DIR):
            refs = []
            _collect_strings(data, "model", refs)
            for ref in refs:
                if isinstance(ref, str) and ref.count(":") <= 1 and "/" in ref and not _model_exists(ref):
                    problems.append(f"{os.path.relpath(path, ROOT)}: model '{ref}' has no JSON")

    if problems:
        print(f"check_assets: {len(problems)} unresolved reference(s):")
        for p in problems:
            print("  " + p)
        return 1
    print("check_assets: all generated model/blockstate/item references resolve")
    return 0


if __name__ == "__main__":
    sys.exit(main())
