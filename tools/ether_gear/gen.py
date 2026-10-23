# Genera la textura del Ether Gear: python3 gen.py tex.png glow.png preview.png (spec.py = el modelo)
"""Textura del Ether Gear (x2) + capa brillante + vista previa con un rasterizador simple."""
import math, random, sys
from PIL import Image, ImageDraw
sys.path.insert(0, '.')
from spec import PARTS

K = 2
TW = 64
img = Image.new("RGBA", (TW * K, TW * K), (0, 0, 0, 0))
glow = Image.new("RGBA", (TW * K, TW * K), (0, 0, 0, 0))
rnd = random.Random(7)

BLACK = (18, 13, 26, 255); BLACK_D = (10, 7, 15, 255); BLACK_L = (34, 25, 48, 255)
MARK = (178, 104, 255, 255); MARK_L = (226, 186, 255, 255); MARK_D = (104, 48, 178, 255)
HAIR = (232, 228, 240, 255); HAIR_L = (255, 255, 255, 255); HAIR_D = (192, 186, 208, 255); HAIR_O = (140, 132, 160, 255)
EYE = (206, 124, 255, 255); EYE_L = (250, 222, 255, 255); EYE_D = (120, 52, 196, 255)
TEETH = (236, 220, 255, 255); MOUTH = (36, 8, 52, 255)
CLEAR = (0, 0, 0, 0)

def faces(u, v, w, h, d):
    """Regiones (en unidades) de cada cara de una caja: nombre -> (u0, v0, ancho, alto)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}

def px(x, y, c, im=None):
    im = im or img
    if 0 <= x < TW * K and 0 <= y < TW * K: im.putpixel((int(x), int(y)), c)

def fill(region, fn):
    """fn(i, j, W, H) -> color (i, j en píxeles dentro de la cara)."""
    u0, v0, w, h = region
    W, H = int(w * K), int(h * K)
    for j in range(H):
        for i in range(W):
            c = fn(i, j, W, H)
            if c is not None: px(u0 * K + i, v0 * K + j, c)

def black_skin(i, j, W, H):
    n = rnd.random()
    return BLACK_L if n < 0.06 else BLACK_D if n < 0.16 else BLACK

def box_all(u, v, w, h, d, fn, which=None):
    for name, r in faces(u, v, w, h, d).items():
        if which is None or name in which: fill(r, fn)

def line(region, pts, c=MARK, glow_too=True, width=1):
    """Polilínea pixelada dentro de una cara (pts en píxeles relativos)."""
    u0, v0, w, h = region
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for k in range(n + 1):
            t = k / n
            x = round(x0 + (x1 - x0) * t); y = round(y0 + (y1 - y0) * t)
            for dx in range(width):
                if 0 <= x + dx < w * K and 0 <= y < h * K:
                    px(u0 * K + x + dx, v0 * K + y, c)
                    if glow_too: px(u0 * K + x + dx, v0 * K + y, (c[0], c[1], c[2], 255), glow)

def dot(region, x, y, c=MARK_L):
    u0, v0, w, h = region
    px(u0 * K + x, v0 * K + y, c); px(u0 * K + x, v0 * K + y, c, glow)

# ---------------------------------------------------------------- cuerpo negro con marcas de éter (discretas)
SOFT = (84, 44, 138, 255); SOFT_L = (150, 92, 220, 255)
B = faces(0, 14, 6, 6, 4)
box_all(0, 14, 6, 6, 4, black_skin)
f = B["front"]
for (x, y) in ((6, 3), (5, 4), (7, 4), (6, 5)): dot(f, x, y, SOFT_L)      # rombito que brilla un poco
line(f, [(5, 5), (4, 7), (4, 9), (3, 11)], SOFT, False)
line(f, [(7, 5), (8, 7), (8, 9), (9, 11)], SOFT, False)
f = B["back"]
line(f, [(6, 1), (6, 4), (5, 7), (6, 10)], SOFT, False)
for (u, v) in ((20, 14), (28, 14)):
    box_all(u, v, 2, 6, 2, black_skin)
    F = faces(u, v, 2, 6, 2)
    line(F["front"], [(2, 1), (1, 4), (2, 7)], SOFT, False)
    for name in ("front", "back", "right", "left"):
        line(F[name], [(0, 9), (3, 9)], SOFT, False)
for (u, v) in ((36, 14), (44, 14)):
    box_all(u, v, 2, 4, 2, black_skin)
    F = faces(u, v, 2, 4, 2)
    for name in ("front", "back", "right", "left"):
        line(F[name], [(0, 6), (3, 6)], SOFT, False)

# ---------------------------------------------------------------- cabeza: negra, ojos morados y sonrisa
H = faces(0, 0, 8, 7, 7)
box_all(0, 0, 8, 7, 7, black_skin)
f = H["front"]  # 16 x 14 px
# Ojos rasgados que miran hacia el centro (como los de la referencia), pero morados y brillantes
eye_l = [(2, 6), (3, 5), (4, 5), (5, 5), (6, 6), (6, 7), (5, 8), (4, 8), (3, 7)]
def eye(cx, mirror):
    pts = [(2, 0), (3, 0), (4, 0), (1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (0, 2), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2),
           (1, 3), (2, 3), (3, 3), (4, 3), (2, 4), (3, 4)]
    for (x, y) in pts:
        xx = cx + (5 - x if mirror else x); yy = 5 + y
        c = EYE_D if y >= 3 or (x in (0, 5)) else EYE
        dot(f, xx, yy, c)
    # brillo
    hx = cx + (5 - 2 if mirror else 2)
    dot(f, hx, 6, EYE_L); dot(f, hx + (-1 if mirror else 1), 6, EYE_L)
    # cejita inclinada (le da la cara traviesa)
    for k in range(4):
        x = cx + (5 - k - 1 if mirror else k + 1)
        px(f[0] * K + x, f[1] * K + 4 - (k // 2 if not mirror else (3 - k) // 2) , BLACK_L)
eye(1, False); eye(9, True)
# Sonrisita con dientecitos
mouth = [(5, 11), (6, 12), (7, 12), (8, 12), (9, 12), (10, 11)]
for x, y in mouth: px(f[0] * K + x, f[1] * K + y, MOUTH)
for x in range(6, 10):
    px(f[0] * K + x, f[1] * K + 11, TEETH if x % 2 == 0 else (200, 176, 236, 255))
    px(f[0] * K + x, f[1] * K + 11, (190, 150, 240, 120), glow)
px(f[0] * K + 4, f[1] * K + 10, BLACK_L); px(f[0] * K + 11, f[1] * K + 10, BLACK_L)

# ---------------------------------------------------------------- pelo morado
def hair_fn(i, j, W, H):
    # mechones verticales con brillo arriba
    strand = (i * 7 + (i // 3) * 5) % 9
    c = HAIR_L if strand == 0 else HAIR_D if strand in (4, 5) else HAIR
    return c
box_all(30, 0, 9, 3, 8, hair_fn, ("top",))
# Lados del pelo: mechones que caen en punta (con huecos), así no parece un gorro
def fringe(i, j, W, H):
    k = i % 6
    depth = (2, 4, 6, 5, 3, 1)[k]
    if j > depth: return CLEAR
    return HAIR_O if j == depth else HAIR_D if k in (0, 5) else HAIR
for name in ("front", "back", "right", "left"):
    fill(faces(30, 0, 9, 3, 8)[name], fringe)
fill(faces(30, 0, 9, 3, 8)["bottom"], lambda i, j, W, H: HAIR_D)
# Flequillo: puntas hacia abajo, con huecos para que se vean los ojos
def bangs_front(i, j, W, H):
    # 16 x 6 px: triángulos invertidos de 4 px de ancho
    k = i % 4
    depth = {0: 1, 1: 3, 2: 2, 3: 0}[k] if (i // 4) % 2 == 0 else {0: 0, 1: 2, 2: 3, 3: 1}[k]
    if j >= depth + 1: return CLEAR
    return HAIR_O if j == depth else HAIR_L if j == 0 else HAIR
BG = faces(20, 26, 8, 3, 1)
fill(BG["front"], bangs_front); fill(BG["back"], bangs_front)
fill(BG["top"], lambda i, j, W, H: HAIR)
fill(BG["bottom"], lambda i, j, W, H: CLEAR)
fill(BG["right"], lambda i, j, W, H: HAIR if j < 3 else CLEAR)
fill(BG["left"], lambda i, j, W, H: HAIR if j < 3 else CLEAR)

# Cuernos: negros con la punta morada
box_all(52, 14, 2, 3, 2, lambda i, j, W, H: MARK_D if j < 2 else BLACK_L if j < 3 else BLACK)
F = faces(52, 14, 2, 3, 2)
fill(F["top"], lambda i, j, W, H: MARK)

# Alas de murciélago (planas): huesos finos, dedos que salen de la articulación y membrana con borde festoneado
def bez(p0, p1, c, n=8):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c[0] + t * t * p1[0],
             (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c[1] + t * t * p1[1]) for t in (i / n for i in range(n + 1))]

def inside(poly, x, y):
    n = len(poly); ins = False
    for i in range(n):
        x0, y0 = poly[i]; x1, y1 = poly[(i + 1) % n]
        if (y0 > y) != (y1 > y) and x < (x1 - x0) * (y - y0) / (y1 - y0) + x0: ins = not ins
    return ins

def bres(p0, p1):
    x0, y0 = map(int, map(round, p0)); x1, y1 = map(int, map(round, p1))
    dx, dy = abs(x1 - x0), -abs(y1 - y0); sx = 1 if x0 < x1 else -1; sy = 1 if y0 < y1 else -1; err = dx + dy
    out = []
    while True:
        out.append((x0, y0))
        if x0 == x1 and y0 == y1: return out
        e2 = 2 * err
        if e2 >= dy: err += dy; x0 += sx
        if e2 <= dx: err += dx; y0 += sy

TONES = [(50, 20, 90, 255), (72, 32, 124, 255), (98, 50, 162, 255), (124, 70, 196, 255)]
BONE = (14, 9, 22, 255); BONE_L = (44, 30, 62, 255); EDGE = (36, 12, 64, 255)

def wing_part(u, v, w, h, origin, outline, bones, tips):
    W, H = w * K, h * K
    pix = {}
    maxd = max(((x - origin[0]) ** 2 + (y - origin[1]) ** 2) ** 0.5 for x in range(W) for y in range(H))
    for y in range(H):
        for x in range(W):
            if inside(outline, x + 0.5, y + 0.5):
                d = ((x - origin[0]) ** 2 + (y - origin[1]) ** 2) ** 0.5 / maxd
                pix[(x, y)] = TONES[min(3, int(d * 4.2))]
    for (x, y) in list(pix):
        if any((x + dx, y + dy) not in pix for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))): pix[(x, y)] = EDGE
    for (p0, p1, top) in bones:
        pts = bres(p0, p1)
        for (x, y) in pts:
            if 0 <= x < W and 0 <= y < H: pix[(x, y)] = BONE
        if top:  # brillo bajo el hueso principal
            for (x, y) in pts:
                if 0 <= x < W and 0 <= y + 1 < H and pix.get((x, y + 1)) not in (None, BONE): pix[(x, y + 1)] = BONE_L
    for (x, y) in tips: pix[(x, y)] = MARK
    for face_u in (u, u + w):
        for (x, y), c in pix.items():
            if 0 <= x < W and 0 <= y < H:
                img.putpixel((face_u * K + x, v * K + y), c)
                if c == MARK: glow.putpixel((face_u * K + x, v * K + y), c)

# Tramo de dentro (10 x 14 px): de la espalda (izquierda) a la articulación (arriba a la derecha)
inner = [(0, 2), (10, 0)] + bez((10, 0), (10, 14), (10, 7))[1:] + bez((10, 14), (0, 9), (6.5, 8.5))[1:]
wing_part(0, 37, 5, 7, (10, 0), inner, [((0, 2), (9, 0), True), ((9, 0), (9, 13), False)], [])
# Tramo de fuera (14 x 16 px): tres dedos desde la articulación (arriba a la izquierda) y una garrita en la punta
J = (0, 0)
ends = [(13, 7), (8, 14), (2, 15)]
outer = [(0, 0), (14, 0), (14, 1), (14, 7.5)]
prev = (14, 7.5)
for e in ends[1:] + [(0, 12)]:
    mid = ((prev[0] + e[0]) / 2, (prev[1] + e[1]) / 2)
    c = (mid[0] + (J[0] - mid[0]) * 0.32, mid[1] + (J[1] - mid[1]) * 0.32)
    outer += bez(prev, (e[0] + 0.5, e[1] + 0.5), c)[1:]
    prev = (e[0] + 0.5, e[1] + 0.5)
wing_part(12, 37, 7, 8, J, outer,
          [((0, 0), (13, 0), True)] + [((0, 1), e, False) for e in ends],
          [(13, 0)] + ends)

# Aureola: piezas moradas brillantes
box_all(26, 37, 2, 1, 1, lambda i, j, W, H: MARK_L if j == 0 else MARK)
u0, v0 = 26 * K, 37 * K
for y in range(2 * K):
    for x in range(6 * K):
        c = img.getpixel((u0 + x, v0 + y))
        if c[3]: glow.putpixel((u0 + x, v0 + y), c)

# Cola negra con una línea morada y punta de flecha morada
box_all(50, 26, 1, 1, 4, lambda i, j, W, H: BLACK)
for n in ("top", "right", "left"):
    fill(faces(50, 26, 1, 1, 4)[n], lambda i, j, W, H: MARK_D if (i + j) % 3 == 0 else None)
def arrow(i, j, W, H):
    # 6 x 6: rombo/flecha
    cx, cy = (W - 1) / 2, (H - 1) / 2
    if abs(i - cx) + abs(j - cy) > 3.2: return CLEAR
    return MARK_L if abs(i - cx) + abs(j - cy) < 1.2 else MARK
F = faces(50, 31, 1, 3, 3)
fill(F["right"], arrow); fill(F["left"], arrow)
for n in ("front", "back", "top", "bottom"): fill(F[n], lambda i, j, W, H: CLEAR)
for n in ("right", "left"):
    u0, v0, w, h = F[n]
    for j in range(h * K):
        for i in range(w * K):
            c = img.getpixel((u0 * K + i, v0 * K + j))
            if c[3]: glow.putpixel((u0 * K + i, v0 * K + j), c)

img.save(sys.argv[1]); glow.save(sys.argv[2])

# ---------------------------------------------------------------- vista previa
def rot_matrix(rx, ry, rz):
    # Igual que ModelPart: rotationZYX(z, y, x) -> v' = Rz * Ry * Rx * v
    cx, sx = math.cos(rx), math.sin(rx); cy, sy = math.cos(ry), math.sin(ry); cz, sz = math.cos(rz), math.sin(rz)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    def mm(a, b): return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mm(mm(Rz, Ry), Rx)

def apply(M, v): return [sum(M[i][k] * v[k] for k in range(3)) for i in range(3)]

parts = {p[0]: p for p in PARTS}
def world(name, v):
    _, parent, pivot, rot, _ = parts[name]
    M = rot_matrix(*[math.radians(a) for a in rot])
    w = apply(M, v); w = [w[i] + pivot[i] for i in range(3)]
    return world(parent, w) if parent else w

def render(yaw_deg, size=360, scale=11):
    out = Image.new("RGBA", (size, size), (40, 36, 52, 255))
    zbuf = [[1e9] * size for _ in range(size)]
    yaw = math.radians(yaw_deg)
    tex = img.convert("RGBA").load()
    for name, parent, pivot, rot, boxes in PARTS:
        for bx in boxes:
            (u, v, x, y, z, w, h, d) = bx[:8]; mirror = len(bx) > 8 and bx[8]
            F = faces(u, v, w, h, d)
            # muestrear cada cara a resolución de textura x2
            def face_pts(face):
                u0, v0, fw, fh = F[face]
                W, Hh = int(round(fw * K)), int(round(fh * K))
                if W == 0 or Hh == 0: return
                for j in range(Hh * 3):
                    for i in range(W * 3):
                        a = (i + 0.5) / (W * 3); b = (j + 0.5) / (Hh * 3)
                        tu = int(u0 * K + ((1 - a) if mirror else a) * W); tv = int(v0 * K + b * Hh)
                        if face == "front": p = (x + a * w, y + b * h, z)
                        elif face == "back": p = (x + w - a * w, y + b * h, z + d)
                        elif face == "right": p = (x, y + b * h, z + d - a * d)
                        elif face == "left": p = (x + w, y + b * h, z + a * d)
                        elif face == "top": p = (x + a * w, y, z + d - b * d)
                        else: p = (x + a * w, y + h, z + b * d)
                        yield p, (tu, tv)
            for face in F:
                for p, (tu, tv) in face_pts(face):
                    c = tex[min(tu, TW * K - 1), min(tv, TW * K - 1)]
                    if c[3] < 128: continue
                    wv = world(name, list(p))
                    # girar todo el modelo (vista)
                    X = wv[0] * math.cos(yaw) - wv[2] * math.sin(yaw)
                    Z = wv[0] * math.sin(yaw) + wv[2] * math.cos(yaw)
                    sx = int(size / 2 + X * scale); sy = int(wv[1] * scale + 10 - 0 * scale)
                    sy = int((wv[1] + 6) * scale)
                    shade = {"front": 1.0, "back": 0.6, "right": 0.8, "left": 0.8, "top": 1.0, "bottom": 0.5}[face]
                    col = tuple(int(c[k] * shade) for k in range(3)) + (255,)
                    for dx in range(-2, 3):
                        for dy in range(-2, 3):
                            xx, yy = sx + dx, sy + dy
                            if 0 <= xx < size and 0 <= yy < size and Z < zbuf[yy][xx]:
                                zbuf[yy][xx] = Z; out.putpixel((xx, yy), col)
    return out

views = [render(0), render(35), render(90), render(180)]
render(0, 520, 18).save(sys.argv[3].replace('.png', '_big.png'))
sheet = Image.new("RGBA", (360 * 4, 360))
for i, vimg in enumerate(views): sheet.paste(vimg, (360 * i, 0))
sheet.save(sys.argv[3])
