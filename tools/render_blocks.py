#!/usr/bin/env python3
"""Isometric block renderer -- fakes the in-game inventory/GUI block icon from the committed
model JSON + textures, for the wiki.

Parses assets/nerospace/models/block/<id>.json (resolving common vanilla parents cube_all /
cube_column / cube_bottom_top / cube / orientable, and raw elements), back-face culls, projects
every visible box face into Minecraft's GUI isometric view with vanilla per-face shading
(top 1.0, sides 0.8/0.62, bottom 0.5) and composites back-to-front. Crisp NEAREST pixel art.

Animated textures (vertical frame strips, optionally with a .png.mcmeta) are detected and emitted
as a looping GIF beside the static PNG. --spin adds a turntable GIF. A gentle brightness/saturation
lift (default on) keeps icons punchy without touching the committed art.

Usage:
  python tools/render_blocks.py --multiloader
  python tools/render_blocks.py --multiloader --only solar_panel combustion_generator
  python tools/render_blocks.py --multiloader --spin
Deps: Pillow
"""
import argparse, colorsys, json, math, os, sys
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nerospace_target import resolve, REPO_ROOT  # noqa: E402

RES = "common/src/main/resources/assets/nerospace"
def MODELS_BLOCK(): return resolve(f"{RES}/models/block")
def TEX_DIR(): return resolve(f"{RES}/textures")
OUT_DIR = os.path.join(REPO_ROOT, "wiki", "images")

YAW = math.radians(45.0)
PITCH = math.radians(30.0)
TEXEL = 24
CANVAS = 256
MARGIN = 12
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.62, "west": 0.62}
NORMAL = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1),
          "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}
FULL = ([0, 0, 0], [16, 16, 16])

def _box(faces): return {"from": FULL[0], "to": FULL[1], "faces": faces}
def _cube(t): return {f: {"texture": t[f]} for f in ("down", "up", "north", "south", "east", "west")}
PARENTS = {
    "minecraft:block/cube_all": lambda: [_box(_cube({f: "#all" for f in SHADE}))],
    "minecraft:block/cube_column": lambda: [_box(_cube(
        {"up": "#end", "down": "#end", "north": "#side", "south": "#side", "east": "#side", "west": "#side"}))],
    "minecraft:block/cube_bottom_top": lambda: [_box(_cube(
        {"up": "#top", "down": "#bottom", "north": "#side", "south": "#side", "east": "#side", "west": "#side"}))],
    "minecraft:block/cube": lambda: [_box(_cube(
        {"up": "#up", "down": "#down", "north": "#north", "south": "#south", "east": "#east", "west": "#west"}))],
    "minecraft:block/orientable": lambda: [_box(_cube(
        {"up": "#top", "down": "#top", "north": "#front", "south": "#side", "east": "#side", "west": "#side"}))],
}

def load_model(model_id):
    for base in (f"{model_id}_item", model_id):
        p = os.path.join(MODELS_BLOCK(), f"{base}.json")
        if os.path.exists(p):
            with open(p, encoding="utf-8") as fh:
                m = json.load(fh)
            break
    else:
        return None
    textures = dict(m.get("textures", {}))
    elements = m.get("elements")
    parent = m.get("parent")
    if not elements and parent in PARENTS:
        elements = PARENTS[parent]()
    return elements or [], textures

def resolve_tex(ref, textures):
    seen = 0
    while isinstance(ref, str) and ref.startswith("#") and seen < 8:
        ref = textures.get(ref[1:]); seen += 1
    if not ref:
        return None
    name = ref.split(":")[-1].split("/")[-1]
    return os.path.join(TEX_DIR(), "block", f"{name}.png")

def tex_frames(path):
    img = Image.open(path).convert("RGBA")
    w, h = img.size
    n = max(1, h // w) if w else 1
    return [img.crop((0, i * w, w, (i + 1) * w)) for i in range(n)]

def project(p, yaw=YAW):
    x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
    cx = x * math.cos(yaw) + z * math.sin(yaw)
    cz = -x * math.sin(yaw) + z * math.cos(yaw)
    py = y * math.cos(PITCH) - cz * math.sin(PITCH)
    pz = y * math.sin(PITCH) + cz * math.cos(PITCH)
    return cx, -py, pz

def rotate_dir(d, yaw=YAW):
    x, y, z = d
    cx = x * math.cos(yaw) + z * math.sin(yaw)
    cz = -x * math.sin(yaw) + z * math.cos(yaw)
    py = y * math.cos(PITCH) - cz * math.sin(PITCH)
    pz = y * math.sin(PITCH) + cz * math.cos(PITCH)
    return cx, -py, pz

def face_geom(face, f, t):
    fx, fy, fz = f; tx, ty, tz = t
    g = {
        "up":    ((fx, ty, fz), (tx, ty, fz), (fx, ty, tz), (fx, fz, tx, tz)),
        "down":  ((fx, fy, fz), (tx, fy, fz), (fx, fy, tz), (fx, fz, tx, tz)),
        "south": ((fx, ty, tz), (tx, ty, tz), (fx, fy, tz), (fx, 16 - ty, tx, 16 - fy)),
        "north": ((tx, ty, fz), (fx, ty, fz), (tx, fy, fz), (fx, 16 - ty, tx, 16 - fy)),
        "east":  ((tx, ty, tz), (tx, ty, fz), (tx, fy, tz), (fz, 16 - ty, tz, 16 - fy)),
        "west":  ((fx, ty, fz), (fx, ty, tz), (fx, fy, fz), (fz, 16 - ty, tz, 16 - fy)),
    }
    return g[face]

def affine_coeffs(O, U, V, W, H):
    dux, duy = U[0] - O[0], U[1] - O[1]
    dvx, dvy = V[0] - O[0], V[1] - O[1]
    det = dux * dvy - duy * dvx
    if abs(det) < 1e-9:
        return None
    a = W * dvy / det; b = -W * dvx / det
    d = -H * duy / det; e = H * dux / det
    return (a, b, -(a * O[0] + b * O[1]), d, e, -(d * O[0] + e * O[1]))

def shade(img, factor, bright):
    px = img.load(); w, h = img.size; lift, sat = bright
    for y in range(h):
        for x in range(w):
            r, g, b, al = px[x, y]
            if al == 0:
                continue
            r, g, b = r * factor, g * factor, b * factor
            if lift != 1.0 or sat != 1.0:
                hh, ll, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
                ll = min(1.0, ll * lift); ss = min(1.0, ss * sat)
                r, g, b = (c * 255 for c in colorsys.hls_to_rgb(hh, ll, ss))
            px[x, y] = (int(r), int(g), int(b), al)
    return img

def render(elements, textures, frame, yaw, bright, fit):
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    sc, ox, oy = fit
    faces = []
    for el in elements:
        f = [float(v) for v in el["from"]]
        t = [float(v) for v in el["to"]]
        cen = [(f[i] + t[i]) / 2 for i in range(3)]
        for fname, fdef in el.get("faces", {}).items():
            # back-face cull: keep only faces whose rotated normal points toward the camera
            if rotate_dir(NORMAL[fname], yaw)[2] <= 1e-6:
                continue
            faces.append((project(cen, yaw)[2], el, f, t, fname, fdef))
    faces.sort(key=lambda r: r[0])  # far first, nearest last
    for _, el, f, t, fname, fdef in faces:
        path = resolve_tex(fdef.get("texture"), textures)
        if not path or not os.path.exists(path):
            continue
        framelist = tex_frames(path)
        src = framelist[frame % len(framelist)]
        O3, U3, V3, uv = face_geom(fname, f, t)
        u0, v0, u1, v1 = uv
        crop = src.crop((int(round(u0)), int(round(v0)), int(round(u1)), int(round(v1))))
        if crop.width == 0 or crop.height == 0:
            continue
        face_img = crop.resize((crop.width * TEXEL, crop.height * TEXEL), Image.NEAREST)
        face_img = shade(face_img, SHADE.get(fname, 0.8), bright)
        def scr(p):
            x, y, _ = project(p, yaw); return (x * sc + ox, y * sc + oy)
        co = affine_coeffs(scr(O3), scr(U3), scr(V3), face_img.width, face_img.height)
        if not co:
            continue
        layer = face_img.transform((CANVAS, CANVAS), Image.AFFINE, co,
                                   resample=Image.NEAREST, fillcolor=(0, 0, 0, 0))
        canvas = Image.alpha_composite(canvas, layer)
    return canvas

def compute_fit():
    pts = [project((x * 16, y * 16, z * 16)) for x in (0, 1) for y in (0, 1) for z in (0, 1)]
    xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
    span = max(max(xs) - min(xs), max(ys) - min(ys))
    sc = (CANVAS - 2 * MARGIN) / span
    cx = (min(xs) + max(xs)) / 2; cy = (min(ys) + max(ys)) / 2
    return sc, CANVAS / 2 - cx * sc, CANVAS / 2 - cy * sc

def anim_frame_count(elements, textures):
    n = 1
    for el in elements:
        for fdef in el.get("faces", {}).values():
            path = resolve_tex(fdef.get("texture"), textures)
            if path and os.path.exists(path):
                n = max(n, len(tex_frames(path)))
    return n

def save_gif(frames, path, ms):
    bg = [Image.new("RGBA", f.size, (255, 255, 255, 0)) for f in frames]
    merged = [Image.alpha_composite(b, f) for b, f in zip(bg, frames)]
    pal = [m.convert("P", palette=Image.ADAPTIVE, colors=255) for m in merged]
    pal[0].save(path, save_all=True, append_images=pal[1:], duration=ms, loop=0,
                disposal=2, transparency=255)

def gif_ms(elements, textures):
    """GIF frame duration from the animated texture's .png.mcmeta frametime (ticks*50ms)."""
    ft = 0
    for el in elements:
        for fdef in el.get("faces", {}).values():
            p = resolve_tex(fdef.get("texture"), textures)
            if p and os.path.exists(p + ".mcmeta"):
                try:
                    a = json.load(open(p + ".mcmeta", encoding="utf-8")).get("animation", {})
                    ft = max(ft, int(a.get("frametime", 1)))
                except Exception:
                    pass
    return max(120, ft * 50) if ft else 150


def render_block(model_id, spin, bright):
    loaded = load_model(model_id)
    if not loaded:
        return None
    elements, textures = loaded
    if not elements:
        return None
    fit = compute_fit()
    os.makedirs(OUT_DIR, exist_ok=True)
    outs = []
    png = render(elements, textures, 0, YAW, bright, fit)
    pp = os.path.join(OUT_DIR, f"{model_id}.png"); png.save(pp); outs.append(pp)
    n = anim_frame_count(elements, textures)
    if n > 1:
        gif = [render(elements, textures, i, YAW, bright, fit) for i in range(n)]
        gp = os.path.join(OUT_DIR, f"{model_id}.gif"); save_gif(gif, gp, gif_ms(elements, textures)); outs.append(gp)
    if spin:
        frames = [render(elements, textures, 0, YAW + math.radians(i * 15), bright, fit) for i in range(24)]
        sp = os.path.join(OUT_DIR, f"{model_id}_spin.gif"); save_gif(frames, sp, 80); outs.append(sp)
    return outs

def all_block_ids():
    ids = set()
    for fn in os.listdir(MODELS_BLOCK()):
        if fn.endswith(".json"):
            ids.add(fn[:-5].removesuffix("_item"))
    return sorted(ids)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", nargs="*")
    ap.add_argument("--spin", action="store_true")
    ap.add_argument("--no-bright", action="store_true")
    ap.add_argument("--multiloader", action="store_true")
    ap.add_argument("--common", action="store_true")
    args = ap.parse_args()
    bright = (1.0, 1.0) if args.no_bright else (1.18, 1.22)
    ids = args.only or all_block_ids()
    done = 0
    for mid in ids:
        outs = render_block(mid, args.spin, bright)
        if outs:
            done += 1
            print("rendered", mid, "->", ", ".join(os.path.basename(o) for o in outs))
        else:
            print("skip", mid)
    print(f"\n{done}/{len(ids)} blocks rendered into {os.path.relpath(OUT_DIR, REPO_ROOT)}")

if __name__ == "__main__":
    main()
