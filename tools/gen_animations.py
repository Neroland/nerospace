#!/usr/bin/env python3
"""In-game animated textures: smooth flowing rocket fuel + blinking machine lights / live screens.

Output goes into textures/block and is picked up by the atlas via a sibling .png.mcmeta (no model
or Java change). Kinds:

  * FUEL  -- generates a CLEAN, seamless vertically-scrolling liquid strip for rocket_fuel
            still/flow (16 frames, rolls exactly one tile so it loops with no jump) from the
            committed rocket_fuel palette. Slow frametime so it reads as molten fuel, not strobing.
  * PULSE -- turns a static 16x16 machine texture into a frame strip whose accent pixels
            (LEDs / glow) breathe in brightness (HSV value, keeps colour -> no white-out).
  * GRAPH -- bespoke live screen for the terraform monitor: three bars rise/fall like telemetry,
            a scan line sweeps, the status LED blinks. Only the screen interior is repainted; the
            bezel art is kept.

Frame 0 of PULSE is the untouched original (idempotent, resting look unchanged). FUEL/GRAPH are
fully generated each run.

Usage: python tools/gen_animations.py --multiloader
Deps: Pillow
"""
import colorsys, json, math, os, sys
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nerospace_target import resolve  # noqa: E402

def BLOCK(): return resolve("common/src/main/resources/assets/nerospace/textures/block")

# name, kind, frames, frametime(ticks), amplitude, hue-window
PULSE = [
    ("combustion_generator_front", 8, 3, 0.30, (0.00, 0.13)),
    ("nerosium_grinder_front",     8, 3, 0.28, None),
    ("fuel_refinery",              8, 3, 0.28, None),
    ("oxygen_generator",           8, 3, 0.28, None),
    ("passive_generator",          8, 4, 0.24, None),
    ("hydration_module_front",     8, 3, 0.28, None),
    ("terraformer_front",          8, 3, 0.30, None),
    ("battery",                    8, 3, 0.30, None),
    ("creative_battery",           8, 3, 0.30, None),
    ("star_guide",                10, 3, 0.35, None),
    ("quarry_controller",          8, 3, 0.40, None),
    ("quarry_landmark",            8, 3, 0.40, None),
    ("quarry_drill",               8, 2, 0.30, None),
]


def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def write_mcmeta(path, frametime, interpolate=False):
    anim = {"frametime": frametime}
    if interpolate:
        anim["interpolate"] = True
    with open(path + ".mcmeta", "w", encoding="utf-8") as fh:
        json.dump({"animation": anim}, fh, indent=2); fh.write("\n")


# ---------------------------------------------------------------- FUEL
def fuel_palette(path):
    im = Image.open(path).convert("RGBA")
    op = [p for p in im.getdata() if p[3]]
    lum = lambda p: p[0] * .3 + p[1] * .59 + p[2] * .11
    s = sorted(op, key=lum)
    return s[len(s) // 5][:3], s[4 * len(s) // 5][:3], s[-1][:3]  # dark, light, hi


def fuel_strip(dark, light, hi, frames=16, bands=1.0, sparkle=True):
    """Seamless vertical scroll: a 16x16 tile periodic in y, rolled 1px/frame -> loops cleanly."""
    W = 16
    tile = Image.new("RGBA", (W, W))
    tp = tile.load()
    for y in range(W):
        for x in range(W):
            wave = 0.5 + 0.5 * math.sin(2 * math.pi * bands * y / W + x * 0.55)
            checker = ((x // 2) + (y // 2)) & 1
            t = (0.30 if checker else 0.0) + 0.55 * wave
            col = lerp(dark, light, min(1.0, t))
            if sparkle and wave > 0.90 and (x * 5 + y * 3) % 7 == 0:
                col = lerp(col, hi, 0.8)
            tp[x, y] = (*col, 255)
    strip = Image.new("RGBA", (W, W * frames))
    for i in range(frames):
        rolled = Image.new("RGBA", (W, W))
        for y in range(W):
            rolled.paste(tile.crop((0, (y - i) % W, W, (y - i) % W + 1)), (0, y))
        strip.paste(rolled, (0, i * W))
    return strip


# ---------------------------------------------------------------- GRAPH (terraform monitor)
def screen_rect(front):
    p = front.load(); w = front.size[0]
    pts = [(x, y) for y in range(w) for x in range(w)
           if p[x, y][3] and max(p[x, y][:3]) < 25]
    xs = [a for a, _ in pts]; ys = [b for _, b in pts]
    return min(xs), min(ys), max(xs), max(ys)


def monitor_strip(front, frames=24):
    base = front.crop((0, 0, 16, 16))
    rx0, ry0, rx1, ry1 = screen_rect(base)
    ix0, iy0, ix1, iy1 = rx0 + 1, ry0 + 1, rx1 - 1, ry1 - 1
    aw = ix1 - ix0 + 1; ah = iy1 - iy0 + 1
    bg = (8, 12, 18, 255); grid = (20, 30, 40, 255)
    bars = [(60, 210, 110), (96, 200, 240), (150, 235, 175)]
    nb = len(bars); bw = max(1, aw // (nb + 1)); gap = max(1, (aw - nb * bw) // (nb + 1))
    strip = Image.new("RGBA", (16, 16 * frames))
    for i in range(frames):
        fr = base.copy(); px = fr.load()
        for y in range(iy0, iy1 + 1):
            for x in range(ix0, ix1 + 1):
                px[x, y] = grid if ((y - iy0) % 3 == 0) else bg
        for k, c in enumerate(bars):
            ph = 2 * math.pi * i / frames + k * 2.1
            hh = int(round(ah * (0.30 + 0.62 * (0.5 + 0.5 * math.sin(ph)))))
            hh = max(1, min(ah, hh))
            bx = ix0 + gap + k * (bw + gap)
            top = iy1 - hh + 1
            for y in range(top, iy1 + 1):
                shade = c if y > top else lerp(c, (255, 255, 255), 0.35)  # bright cap
                for x in range(bx, min(bx + bw, ix1 + 1)):
                    px[x, y] = (*shade, 255)
        # scan line sweeping down
        sy = iy0 + (i % ah)
        for x in range(ix0, ix1 + 1):
            r, g, b, a = px[x, sy]
            px[x, sy] = (min(255, r + 60), min(255, g + 70), min(255, b + 60), 255)
        # status LED (top-left inside screen) blink
        on = (i % frames) < frames // 2
        px[ix0, iy0] = ((90, 255, 130, 255) if on else (30, 80, 45, 255))
        strip.paste(fr, (0, i * 16))
    return strip


# ---------------------------------------------------------------- PULSE
def accent_mask(img, hue):
    px = img.load(); w, h = img.size
    vals = [max(px[x, y][:3]) / 255 for y in range(h) for x in range(w) if px[x, y][3]]
    med = sorted(vals)[len(vals) // 2] if vals else .5
    m = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if not a:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if ss > 0.32 and vv > max(0.42, med * 1.05) and (hue is None or hue[0] <= hh <= hue[1]):
                m.append((x, y))
    return m


def litpx(px, xy, f):
    r, g, b, a = px[xy]
    hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    vv = min(1.0, vv * f); ss = min(1.0, ss * (1.0 + 0.12 * (f - 1)))
    r, g, b = (int(c * 255) for c in colorsys.hsv_to_rgb(hh, ss, vv)); px[xy] = (r, g, b, a)


def pulse_strip(base, frames, amp, hue):
    w = base.width; mask = accent_mask(base, hue)
    strip = Image.new("RGBA", (w, w * frames))
    for i in range(frames):
        fr = base.copy(); px = fr.load()
        fac = 1.0 + amp * math.sin(2 * math.pi * i / frames)
        for xy in mask:
            litpx(px, xy, fac)
        strip.paste(fr, (0, i * w))
    return strip


def main():
    B = BLOCK(); made = []
    # fuel
    dark, light, hi = fuel_palette(os.path.join(B, "rocket_fuel.png"))
    fuel_strip(dark, light, hi, 16, bands=1.0).save(os.path.join(B, "rocket_fuel_still.png"))
    write_mcmeta(os.path.join(B, "rocket_fuel_still.png"), 4, interpolate=True)
    fuel_strip(dark, light, hi, 16, bands=2.0).save(os.path.join(B, "rocket_fuel_flow.png"))
    write_mcmeta(os.path.join(B, "rocket_fuel_flow.png"), 2, interpolate=True)
    made += ["rocket_fuel_still (scroll 16f @4)", "rocket_fuel_flow (scroll 16f @2)"]
    # monitor
    mp = os.path.join(B, "terraform_monitor_front.png")
    monitor_strip(Image.open(mp).convert("RGBA")).save(mp)
    write_mcmeta(mp, 2)
    made.append("terraform_monitor_front (graph 24f @2)")
    # pulse machines
    for name, frames, ft, amp, hue in PULSE:
        p = os.path.join(B, name + ".png")
        if not os.path.exists(p):
            print("  miss", name); continue
        base = Image.open(p).convert("RGBA"); base = base.crop((0, 0, base.width, base.width))
        pulse_strip(base, frames, amp, hue).save(p)
        write_mcmeta(p, ft)
        made.append(f"{name} (pulse {frames}f)")
    print(f"animated {len(made)} textures:")
    for m in made:
        print("  ", m)


if __name__ == "__main__":
    main()
