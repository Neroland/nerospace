#!/usr/bin/env python3
"""Shared target resolution for the Nerospace asset / model-sync tools.

The repo holds two parallel mod trees:
  * the single-loader **root** mod  -> ``src/main/{java,resources}``           (the default), and
  * the cross-loader **multiloader** -> ``multiloader/common/src/main/{java,resources}``.

Asset/model tools (gen_textures, gen_bbmodels, model_sync, check_assets) operate on ``src/main/...``;
this module lets a single ``--multiloader`` flag (or ``NEROSPACE_TARGET=multiloader``) point them at
the multiloader's ``common`` module instead. Repo-root paths (``art/``, ``tools/``) are shared and
NEVER move — only the ``src/main`` base does, so a ``.bbmodel`` under ``art/`` always lives at the repo
root while its Java/texture counterparts follow the chosen target.

Usage in a tool:

    from nerospace_target import REPO_ROOT, src_base, target_label, resolve

    SRC_BASE = src_base()            # root, or multiloader/common
    path = resolve("src/main/...")   # -> SRC_BASE/src/main/...
    art  = resolve("art/...")        # -> REPO_ROOT/art/... (always)
"""
import os
import sys

# tools/ -> repo root
REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Where the multiloader's shared module lives (holds its own src/main/{java,resources}).
# Flattened to the repo root (post_port.md Phase 2), so common/ is a top-level dir.
MULTILOADER_COMMON = os.path.join(REPO_ROOT, "common")


def _target(argv):
    t = os.environ.get("NEROSPACE_TARGET", "root").strip().lower()
    if any(a in ("--multiloader", "--common") for a in argv):
        t = "multiloader"
    if "--root" in argv:  # explicit override wins
        t = "root"
    return "multiloader" if t in ("multiloader", "common") else "root"


def is_multiloader(argv=None):
    return _target(sys.argv if argv is None else argv) == "multiloader"


def src_base(argv=None):
    """Directory that contains ``src/main/{java,resources}`` for the chosen target."""
    return MULTILOADER_COMMON if is_multiloader(argv) else REPO_ROOT


def target_label(argv=None):
    return "common" if is_multiloader(argv) else "root"


def resolve(rel, argv=None):
    """Absolute path for a repo-relative path, routing ``src/...`` to the chosen target base and
    everything else (``art/...``, ``tools/...``) to the repo root."""
    rel = rel.replace("\\", "/")
    base = src_base(argv) if rel.startswith("src/") else REPO_ROOT
    return os.path.join(base, rel)
