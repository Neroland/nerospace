#!/usr/bin/env python3
"""
Auto-fix the Markdown violations the gradle-mcp `markdown_check` tool reports, in place.

Companion to that linter: it applies the same markdownlint rule subset the repo's
.markdownlint.json leaves enabled, so `markdown_check` comes back clean afterwards.

Fixes:
  MD009  strip trailing whitespace (keeping a valid 2-space hard break)
  MD012  collapse runs of blank lines to a single blank
  MD022  surround ATX headings with blank lines
  MD031  surround fenced code blocks with blank lines
  MD040  give a bare ``` fence a language (text)
  MD047  end the file with exactly one newline

Idempotent: run it as often as you like. Skips build/vcs directories.
Usage: python3 tools/fix_markdown.py            # whole repo
       python3 tools/fix_markdown.py wiki a.md   # specific files/dirs
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKIP_DIRS = {'node_modules', '.git', '.gradle', 'build', 'out', 'bin', '.idea', '.vscode', 'run'}

FENCE_RE = re.compile(r'^(\s{0,3})(`{3,}|~{3,})(.*)$')
HEADING_RE = re.compile(r'^#{1,6}(\s|$)')


def fix_text(text):
    raw = text.split('\n')

    # MD009: strip trailing whitespace, but preserve an intentional 2-space hard break.
    lines = []
    for ln in raw:
        stripped = ln.rstrip()
        trail = ln[len(stripped):]
        lines.append(stripped + '  ' if stripped != '' and trail == '  ' else stripped)

    out = []
    in_fence = False
    i, n = 0, len(lines)
    while i < n:
        ln = lines[i]
        fence = FENCE_RE.match(ln)

        if fence and not in_fence:
            if fence.group(3).strip() == '':          # MD040: bare fence -> add a language
                ln = fence.group(1) + fence.group(2) + 'text'
            if out and out[-1] != '':                 # MD031: blank line before
                out.append('')
            out.append(ln)
            in_fence = True
            i += 1
            continue

        if fence and in_fence:                        # closing fence
            out.append(ln)
            in_fence = False
            if i + 1 < n and lines[i + 1] != '':      # MD031: blank line after
                out.append('')
            i += 1
            continue

        if in_fence:
            out.append(ln)
            i += 1
            continue

        if ln == '':                                  # MD012: collapse blank runs
            if not out or out[-1] != '':
                out.append('')
            i += 1
            continue

        if HEADING_RE.match(ln):                       # MD022: surround headings with blanks
            if out and out[-1] != '':
                out.append('')
            out.append(ln)
            if i + 1 < n and lines[i + 1] != '':
                out.append('')
            i += 1
            continue

        out.append(ln)
        i += 1

    while out and out[-1] == '':                       # MD047: exactly one trailing newline
        out.pop()
    return '\n'.join(out) + '\n'


def fix_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    fixed = fix_text(text)
    if fixed != text:
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(fixed)
        return True
    return False


def collect(target):
    if os.path.isfile(target):
        return [target] if target.lower().endswith(('.md', '.markdown')) else []
    found = []
    for dirpath, dirnames, filenames in os.walk(target):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            if name.lower().endswith(('.md', '.markdown')):
                found.append(os.path.join(dirpath, name))
    return found


def main():
    targets = sys.argv[1:] or [ROOT]
    files = []
    for t in targets:
        p = t if os.path.isabs(t) else os.path.join(ROOT, t)
        files.extend(collect(p))
    files = sorted(set(files))
    changed = 0
    for f in files:
        if fix_file(f):
            changed += 1
            print('fixed', os.path.relpath(f, ROOT))
    print(f'\n{changed}/{len(files)} file(s) changed.')


if __name__ == '__main__':
    main()
