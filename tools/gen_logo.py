#!/usr/bin/env python3
"""
Generate the NEROSPACE mod logo for CurseForge (square).
Scene: starfield + nebula, ringed planet, hero nerosium crystal,
launching rocket, small moon, and the NEROSPACE wordmark.

Renders supersampled (SS) then downsamples for clean edges.
Outputs:
  art/logo/nerospace_logo.png   (1024x1024 master)
  art/logo/nerospace_logo_400.png (CurseForge-ready)
"""
import math
import os
import random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art/logo")
os.makedirs(OUT, exist_ok=True)

FINAL = 1024
SS = 2
R = FINAL * SS                      # render resolution
rng = random.Random(7)

# palette
N_DARK   = (43, 13, 58)
N_PURPLE = (106, 31, 140)
N_MAG    = (179, 39, 158)
N_RED    = (224, 58, 58)
N_REDHI  = (255, 96, 110)
N_GLOW   = (255, 138, 216)
N_BRIGHT = (255, 214, 242)
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ---------- background (numpy) ----------
def background():
    top = np.array([8, 5, 20], float)
    bot = np.array([26, 10, 40], float)
    yy = np.linspace(0, 1, R)[:, None, None]
    img = top[None, None, :] * (1 - yy) + bot[None, None, :] * yy
    img = np.repeat(img, R, axis=1)

    Y, X = np.mgrid[0:R, 0:R].astype(float)
    def glow(cx, cy, rad, color, strength):
        d = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
        f = np.clip(1 - d / rad, 0, 1) ** 2 * strength
        for c in range(3):
            img[:, :, c] += color[c] * f
    glow(R * 0.30, R * 0.70, R * 0.55, (150, 30, 90), 0.55)   # red-magenta nebula
    glow(R * 0.74, R * 0.40, R * 0.50, (70, 30, 150), 0.50)   # purple nebula
    glow(R * 0.5, R * 0.5, R * 0.40, (40, 20, 70), 0.30)

    # vignette
    d = np.sqrt((X - R / 2) ** 2 + (Y - R / 2) ** 2) / (R * 0.72)
    vig = np.clip(1 - (d ** 2) * 0.85, 0.25, 1)
    img *= vig[:, :, None]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def add_stars(base):
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for _ in range(520):
        x, y = rng.randint(0, R), rng.randint(0, R)
        s = rng.choice([1, 1, 1, 2, 2, 3]) * SS
        b = rng.randint(120, 255)
        tint = rng.choice([(b, b, b), (b, b, 255), (255, b, b), (255, 200, 255)])
        d.ellipse([x, y, x + s, y + s], fill=tint + (rng.randint(120, 255),))
    # a few hero sparkles with cross flare
    for _ in range(7):
        x, y = rng.randint(int(R*0.05), int(R*0.95)), rng.randint(int(R*0.05), int(R*0.6))
        L = rng.randint(14, 26) * SS
        col = (255, 230, 250, 230)
        d.line([x - L, y, x + L, y], fill=col, width=SS)
        d.line([x, y - L, x, y + L], fill=col, width=SS)
        d.ellipse([x-3*SS, y-3*SS, x+3*SS, y+3*SS], fill=(255,255,255,255))
    glow = layer.filter(ImageFilter.GaussianBlur(3 * SS))
    base.alpha_composite(glow)
    base.alpha_composite(layer)
    return base


def soft_glow(draw_fn, blur, size=None):
    """Render onto a transparent layer via draw_fn(draw), return blurred copy."""
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    return layer.filter(ImageFilter.GaussianBlur(blur))


# ---------- planet + ring + moon ----------
def planet(base):
    cx, cy, rad = int(R * 0.70), int(R * 0.30), int(R * 0.155)

    # atmosphere glow
    base.alpha_composite(soft_glow(
        lambda d: d.ellipse([cx-rad-18*SS, cy-rad-18*SS, cx+rad+18*SS, cy+rad+18*SS],
                            fill=(150, 60, 180, 150)), 16 * SS))

    # ring (back half) — drawn before planet
    ring_layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    rd = ImageDraw.Draw(ring_layer)
    rw, rh = int(rad * 2.15), int(rad * 0.62)
    for i, col in enumerate([(150,90,170,210),(210,140,200,230),(120,70,150,200)]):
        off = i * 6 * SS
        rd.ellipse([cx-rw+off, cy-rh//2+off, cx+rw-off, cy+rh//2-off], outline=col, width=5*SS)
    ring_rot = ring_layer.rotate(-22, center=(cx, cy), resample=Image.BICUBIC)
    base.alpha_composite(ring_rot)

    # planet body via numpy disc with bands + terminator
    pl = np.zeros((R, R, 4), np.uint8)
    Y, X = np.mgrid[0:R, 0:R].astype(float)
    dist = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
    mask = dist <= rad
    # base vertical bands
    band = (np.sin((Y - cy) / rad * 6.0) * 0.5 + 0.5)
    base_c = np.array(N_PURPLE, float)
    band_c = np.array(N_RED, float)
    body = base_c[None, None, :] * (1 - band[:, :, None] * 0.5) + band_c[None, None, :] * (band[:, :, None] * 0.5)
    # lighting from upper-left
    lx, ly = -0.6, -0.7
    nx = (X - cx) / rad; ny = (Y - cy) / rad
    nz = np.sqrt(np.clip(1 - nx**2 - ny**2, 0, 1))
    lit = np.clip(nx * lx + ny * ly + nz * 0.65, 0, 1)
    shade = 0.25 + 0.95 * lit
    body = body * shade[:, :, None]
    # rim light
    rim = np.clip((dist - rad*0.82) / (rad*0.18), 0, 1) * (lit ** 0.5)
    body += np.array(N_GLOW, float)[None,None,:] * (rim[:,:,None] * 0.5)
    pl[:, :, :3] = np.clip(body, 0, 255).astype(np.uint8)
    pl[:, :, 3] = (mask * 255).astype(np.uint8)
    base.alpha_composite(Image.fromarray(pl, "RGBA"))

    # ring (front half) over planet — redraw rotated ring, keep only lower portion
    front = ring_rot.copy()
    fa = np.array(front)
    ygrid = np.mgrid[0:R, 0:R][0]
    # keep pixels below planet center line (front of ring) and outside planet a bit
    keep = (ygrid > cy - rad*0.05)
    fa[~keep] = 0
    base.alpha_composite(Image.fromarray(fa, "RGBA"))

    # moon upper-left of planet
    mx, my, mr = int(R * 0.50), int(R * 0.17), int(rad * 0.26)
    base.alpha_composite(soft_glow(
        lambda d: d.ellipse([mx-mr-6*SS,my-mr-6*SS,mx+mr+6*SS,my+mr+6*SS], fill=(180,170,200,120)), 8*SS))
    md = ImageDraw.Draw(base)
    md.ellipse([mx-mr, my-mr, mx+mr, my+mr], fill=(196, 188, 210, 255))
    md.ellipse([mx-mr, my-mr, mx+mr, my+mr], outline=None)
    # shade + craters
    sh = Image.new("RGBA",(R,R),(0,0,0,0)); sd=ImageDraw.Draw(sh)
    sd.ellipse([mx-mr+int(mr*0.5), my-mr, mx+mr, my+mr], fill=(40,30,60,90))
    base.alpha_composite(sh)
    for _ in range(5):
        rx = mx + rng.randint(-int(mr*0.6), int(mr*0.4)); ry = my + rng.randint(-int(mr*0.6), int(mr*0.6))
        cr = rng.randint(2,5)*SS
        md.ellipse([rx-cr,ry-cr,rx+cr,ry+cr], fill=(150,140,170,200))
    return base


# ---------- rocket ----------
def rocket(base):
    rw, rh = int(R * 0.22), int(R * 0.42)
    layer = Image.new("RGBA", (rw, rh), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx = rw // 2
    bw = int(rw * 0.34)                 # body half-width
    top = int(rh * 0.16); bot = int(rh * 0.74)
    # flame (bottom) with glow added later
    fl_top = bot
    for (col, w, h) in [((255,170,40), 1.0, 1.0), ((255,90,40), 0.66, 0.8), ((255,235,150), 0.36, 0.55)]:
        fw = int(bw * w)
        d.polygon([(cx-fw, fl_top), (cx+fw, fl_top), (cx, fl_top + int((rh-bot)*h))], fill=col)
    # body
    d.rounded_rectangle([cx-bw, top, cx+bw, bot], radius=int(bw*0.5), fill=(238, 236, 248))
    # nosecone
    d.polygon([(cx-bw, top), (cx+bw, top), (cx, int(rh*0.02))], fill=N_RED)
    # accent stripe
    d.rectangle([cx-bw, int(rh*0.30), cx+bw, int(rh*0.36)], fill=N_MAG)
    # window
    wr = int(bw*0.55)
    d.ellipse([cx-wr, int(rh*0.42)-wr, cx+wr, int(rh*0.42)+wr], fill=(70,40,110))
    d.ellipse([cx-wr+SS*2, int(rh*0.42)-wr+SS*2, cx+wr-SS*2, int(rh*0.42)+wr-SS*2], fill=(150,220,255))
    d.ellipse([cx-int(wr*0.4), int(rh*0.42)-int(wr*0.4), cx+int(wr*0.1), int(rh*0.42)+int(wr*0.1)], fill=(235,250,255))
    # fins
    d.polygon([(cx-bw, int(rh*0.55)), (cx-bw-int(bw*0.8), bot), (cx-bw, bot)], fill=N_PURPLE)
    d.polygon([(cx+bw, int(rh*0.55)), (cx+bw+int(bw*0.8), bot), (cx+bw, bot)], fill=N_PURPLE)

    angle = 20
    rot = layer.rotate(angle, expand=True, resample=Image.BICUBIC)
    # position: rising on the left, mid height
    px = int(R * 0.30) - rot.width // 2
    py = int(R * 0.46) - rot.height // 2

    # launch trail glow beneath
    trail = Image.new("RGBA", (R, R), (0,0,0,0)); td = ImageDraw.Draw(trail)
    ang = math.radians(angle + 90)
    sx, sy = int(R*0.30), int(R*0.62)
    ex, ey = int(sx + math.cos(ang)*R*0.22), int(sy + math.sin(ang)*R*0.22)
    td.line([sx, sy, ex, ey], fill=(255,150,60,200), width=14*SS)
    base.alpha_composite(trail.filter(ImageFilter.GaussianBlur(14*SS)))

    # rocket flame glow
    glowL = Image.new("RGBA",(R,R),(0,0,0,0))
    glowL.alpha_composite(rot, (px, py))
    base.alpha_composite(glowL.filter(ImageFilter.GaussianBlur(7*SS)))
    base.alpha_composite(rot, (px, py))
    return base


# ---------- hero crystal ----------
def crystal(base):
    cx, cy = int(R * 0.40), int(R * 0.62)
    sx = int(R * 0.155)            # scale
    sy = int(R * 0.205)
    def P(px, py):
        return (cx + px * sx, cy + py * sy)
    TL, TR = P(-0.45, -1.0), P(0.45, -1.0)
    GL, GR = P(-1.0, -0.18), P(1.0, -0.18)
    M, K = P(0.0, -0.18), P(0.0, -0.62)
    B = P(0.0, 1.18)
    sil = [TL, TR, GR, B, GL]

    # outer glow
    base.alpha_composite(soft_glow(
        lambda d: d.polygon(sil, fill=(255, 70, 140, 200)), 26 * SS))
    base.alpha_composite(soft_glow(
        lambda d: d.polygon(sil, fill=(180, 60, 200, 160)), 12 * SS))

    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    facets = [
        ([TL, TR, K], N_GLOW),
        ([TL, K, M, GL], N_MAG),
        ([TR, GR, M, K], N_PURPLE),
        ([GL, M, B], N_RED),
        ([GR, B, M], (120, 32, 78)),
    ]
    for poly, col in facets:
        d.polygon(poly, fill=col)
    # facet edges (light)
    edges = [(TL,TR),(TR,GR),(GR,B),(B,GL),(GL,TL),(TL,K),(TR,K),(GL,M),(GR,M),(K,M),(M,B)]
    for a, b in edges:
        d.line([a, b], fill=(255, 200, 235, 220), width=max(1, SS))
    base.alpha_composite(layer)

    # top specular sparkle
    spk = P(-0.12, -0.78)
    sg = soft_glow(lambda dr: dr.ellipse([spk[0]-10*SS, spk[1]-10*SS, spk[0]+10*SS, spk[1]+10*SS],
                                         fill=(255,255,255,255)), 6*SS)
    base.alpha_composite(sg)
    dd = ImageDraw.Draw(base)
    L = 22*SS
    dd.line([spk[0]-L, spk[1], spk[0]+L, spk[1]], fill=(255,255,255,235), width=SS*2)
    dd.line([spk[0], spk[1]-L, spk[0], spk[1]+L], fill=(255,255,255,235), width=SS*2)
    return base


# ---------- wordmark ----------
def wordmark(base):
    text = "NEROSPACE"
    fsize = int(R * 0.118)
    font = ImageFont.truetype(FONT_BOLD, fsize)
    # letterspacing by drawing chars manually
    spacing = int(fsize * 0.10)
    widths = [ImageDraw.Draw(base).textlength(c, font=font) for c in text]
    total = sum(widths) + spacing * (len(text) - 1)
    x0 = (R - total) / 2
    y0 = int(R * 0.80)

    def draw_text(dr, fill):
        x = x0
        for c, w in zip(text, widths):
            dr.text((x, y0), c, font=font, fill=fill)
            x += w + spacing

    # glow
    gl = Image.new("RGBA", (R, R), (0,0,0,0))
    draw_text(ImageDraw.Draw(gl), (255, 80, 160, 255))
    base.alpha_composite(gl.filter(ImageFilter.GaussianBlur(10*SS)))
    base.alpha_composite(gl.filter(ImageFilter.GaussianBlur(4*SS)))
    # dark outline for contrast
    out = Image.new("RGBA", (R, R), (0,0,0,0))
    draw_text(ImageDraw.Draw(out), (20, 8, 30, 255))
    out = out.filter(ImageFilter.MaxFilter(2*SS+1))
    base.alpha_composite(out)
    # main fill (vertical gradient white->light purple via two passes)
    draw_text(ImageDraw.Draw(base), (245, 240, 255, 255))

    # tagline
    tf = ImageFont.truetype(FONT_BOLD, int(R*0.030))
    tag = "S P A C E   •   T E C H   •   E X P L O R A T I O N"
    tw = ImageDraw.Draw(base).textlength(tag, font=tf)
    ImageDraw.Draw(base).text(((R-tw)/2, y0 + fsize*1.06), tag, font=tf, fill=(200, 170, 220, 255))
    return base


def main():
    img = background()
    img = add_stars(img)
    img = planet(img)
    img = rocket(img)
    img = crystal(img)
    img = wordmark(img)
    final = img.convert("RGB").resize((FINAL, FINAL), Image.LANCZOS)
    p1 = os.path.join(OUT, "nerospace_logo.png")
    p2 = os.path.join(OUT, "nerospace_logo_400.png")
    final.save(p1)
    final.resize((400, 400), Image.LANCZOS).save(p2)
    print("wrote", os.path.relpath(p1, ROOT))
    print("wrote", os.path.relpath(p2, ROOT))


if __name__ == "__main__":
    main()
