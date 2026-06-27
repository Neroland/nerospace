#!/usr/bin/env python3
"""Embed the rendered block icons into the GitHub wiki pages.

For every ``wiki/<Page>.md`` that corresponds to a block, insert (or refresh) a managed image
block just under the page's H1 title, pointing at ``images/<id>.gif`` if an animated render exists
else ``images/<id>.png``. The block is wrapped in ``<!-- nerospace:render -->`` markers so re-running
is idempotent (it replaces, never stacks).

Page -> block id is derived by lowercasing + hyphen->underscore, with a small alias table for the
"Block of X" -> "x_block" style names. Pages with no matching render are left untouched and listed.

Usage: python tools/embed_wiki_images.py
Deps: stdlib only.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nerospace_target import REPO_ROOT  # noqa: E402

WIKI = os.path.join(REPO_ROOT, "wiki")
IMAGES = os.path.join(WIKI, "images")
BEGIN, END = "<!-- nerospace:render -->", "<!-- /nerospace:render -->"

ALIAS = {
    "block_of_nerosium": "nerosium_block",
    "block_of_nerosteel": "nerosteel_block",
    "block_of_cindrite": "cindrite_block",
    "block_of_glacite": "glacite_block",
    "block_of_raw_nerosium": "raw_nerosium_block",
    "launch_controller": "launch_controller",
    "rocket_launch_pad": "rocket_launch_pad",
    "universal_pipe": "universal_pipe",
}


def page_to_id(page):
    base = page[:-3].lower().replace("-", "_")
    if base in ALIAS:
        return ALIAS[base]
    return base


def image_for(block_id):
    for ext in ("gif", "png"):
        p = os.path.join(IMAGES, f"{block_id}.{ext}")
        if os.path.exists(p):
            return f"images/{block_id}.{ext}", ext
    return None, None


def block_html(rel, title):
    return (f"{BEGIN}\n<p align=\"right\">"
            f"<img src=\"{rel}\" alt=\"{title}\" width=\"150\" align=\"right\">"
            f"</p>\n{END}")


def main():
    embedded, skipped = [], []
    for page in sorted(os.listdir(WIKI)):
        if not page.endswith(".md") or page.startswith("_"):
            continue
        bid = page_to_id(page)
        rel, ext = image_for(bid)
        if not rel:
            skipped.append(page[:-3])
            continue
        path = os.path.join(WIKI, page)
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        title = page[:-3].replace("-", " ")
        html = block_html(rel, title)
        if BEGIN in text:  # refresh existing
            text = re.sub(re.escape(BEGIN) + r".*?" + re.escape(END), html, text,
                          flags=re.S)
        else:              # insert after the first H1, else at top
            m = re.match(r"(#[^\n]*\n)", text)
            if m:
                text = text[:m.end()] + "\n" + html + "\n" + text[m.end():]
            else:
                text = html + "\n\n" + text
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text)
        embedded.append(f"{page[:-3]} -> {rel}")

    print(f"embedded {len(embedded)} pages:")
    for e in embedded:
        print("  ", e)
    print(f"\nno render for {len(skipped)} pages (left untouched):")
    print("  ", ", ".join(skipped))


if __name__ == "__main__":
    main()
