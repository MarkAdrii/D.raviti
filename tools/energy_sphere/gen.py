"""Texturas de la esfera de energía (técnicas de área): python3 gen.py <carpeta de texturas>
- energy_swirl.png: franjas finas y curvadas en blanco sobre negro (se suman, el negro no se ve), repetible a lo ancho.
- energy_white.png: blanco liso (borde brillante)."""
import math, random, sys, os
from PIL import Image, ImageFilter
OUT = sys.argv[1]
W, H = 256, 128
rnd = random.Random(3)
img = Image.new("L", (W, H), 0)
px = img.load()
def add(x, y, v):
    x %= W
    if 0 <= y < H: px[x, y] = min(255, px[x, y] + int(v))
for k in range(46):
    y0 = rnd.uniform(8, H - 8)
    length = rnd.uniform(40, 150)
    x0 = rnd.uniform(0, W)
    amp = rnd.uniform(2, 9)
    freq = rnd.uniform(0.6, 1.6)
    bright = rnd.uniform(70, 200)
    thick = rnd.choice([1, 1, 1, 2])
    for s in range(int(length * 2)):
        t = s / (length * 2)
        x = x0 + t * length
        y = y0 + amp * math.sin(t * math.pi * freq + k) - t * rnd.uniform(-0.4, 0.4)
        fade = math.sin(t * math.pi) ** 1.4
        for dy in range(thick):
            add(int(x), int(y) + dy, bright * fade)
img = img.filter(ImageFilter.GaussianBlur(0.8))
# más brillo en el centro de la banda (el "ecuador" se ve más denso, como en la referencia)
for y in range(H):
    k = 0.55 + 0.45 * math.sin(math.pi * y / (H - 1))
    for x in range(W):
        px[x, y] = int(px[x, y] * k)
rgb = Image.merge("RGBA", (img, img, img, Image.new("L", (W, H), 255)))
rgb.save(os.path.join(OUT, "energy_swirl.png"))
Image.new("RGBA", (16, 16), (255, 255, 255, 255)).save(os.path.join(OUT, "energy_white.png"))
print("ok")

# ---------------------------------------------------------------- variaciones (una por tipo de técnica)
def save_gray(im, name):
    g = im.convert("L")
    Image.merge("RGBA", (g, g, g, Image.new("L", g.size, 255))).save(os.path.join(OUT, name))

def draw_line(px, w, h, x0, y0, x1, y1, v, thick=1):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for i in range(n + 1):
        t = i / n
        x = int(x0 + (x1 - x0) * t) % w; y = int(y0 + (y1 - y0) * t)
        for d in range(thick):
            if 0 <= y + d < h: px[x, y + d] = min(255, px[x, y + d] + v)

# Grietas / rayos (impactos): ramas que se parten
im = Image.new("L", (256, 128), 0); px = im.load(); rnd = random.Random(11)
def branch(x, y, ang, length, v, depth):
    if depth > 4 or length < 4: return
    for _ in range(int(length / 3)):
        ang += rnd.uniform(-0.6, 0.6)
        nx, ny = x + math.cos(ang) * 3, y + math.sin(ang) * 3
        draw_line(px, 256, 128, x, y, nx, ny, int(v), 1 if depth else 2)
        x, y = nx, ny
        if rnd.random() < 0.18: branch(x, y, ang + rnd.choice([-1, 1]) * rnd.uniform(0.5, 1.1), length * 0.55, v * 0.75, depth + 1)
for k in range(14):
    branch(rnd.uniform(0, 256), rnd.uniform(10, 118), rnd.uniform(0, 6.28), rnd.uniform(40, 90), 230, 0)
im = im.filter(ImageFilter.GaussianBlur(0.6)); save_gray(im, "energy_cracks.png")

# Bandas de presión (Peso Infernal): franjas horizontales de grosor variable que bajan
im = Image.new("L", (64, 128), 0); px = im.load()
y = 0.0
while y < 128:
    th = rnd.uniform(1, 4); v = rnd.uniform(90, 230)
    for yy in range(int(y), min(128, int(y + th))):
        for x in range(64):
            px[x, yy] = int(v * (0.75 + 0.25 * math.sin(x / 64 * math.pi * 2 * 2 + yy)))
    y += th + rnd.uniform(4, 12)
im = im.filter(ImageFilter.GaussianBlur(0.7)); save_gray(im, "energy_bands.png")

# Espiral (Onda Gravitatoria): rayas en diagonal en el mapa UV = espiral en la esfera
im = Image.new("L", (256, 128), 0); px = im.load()
for x in range(256):
    for y in range(128):
        d = (x * 2 + y * 3) % 64
        v = max(0, 1 - abs(d - 32) / 7.0)
        w2 = 0
        px[x, y] = int(255 * min(1, v * 0.9 + w2))
im = im.filter(ImageFilter.GaussianBlur(0.8)); save_gray(im, "energy_spiral.png")

# Celdas (Antigravedad y Cañón): bordes de celdas como una burbuja / cáusticas
W2, H2 = 256, 128
pts = [(rnd.uniform(0, W2), rnd.uniform(0, H2)) for _ in range(60)]
im = Image.new("L", (W2, H2), 0); px = im.load()
for x in range(W2):
    for y in range(H2):
        ds = sorted(min((x - a) ** 2, (x - a + W2) ** 2, (x - a - W2) ** 2) + (y - b) ** 2 for a, b in pts)
        e = math.sqrt(ds[1]) - math.sqrt(ds[0])
        px[x, y] = int(255 * max(0, 1 - e / 2.2) ** 1.5)
save_gray(im.filter(ImageFilter.GaussianBlur(0.5)), "energy_cells.png")

# Hexágonos (Campo Gravitatorio): Voronoi de una red triangular = celdas hexagonales (repetible a lo ancho)
im = Image.new("L", (256, 128), 0); px = im.load()
cols, sp = 16, 256 / 16
rowh = sp * math.sqrt(3) / 2
centers = []
for j in range(-1, int(128 / rowh) + 2):
    for i in range(-1, cols + 1):
        centers.append((i * sp + (sp / 2 if j % 2 else 0), j * rowh))
for X in range(256):
    for Y in range(128):
        ds = sorted((X - a) ** 2 + (Y - b) ** 2 for a, b in centers)
        e = math.sqrt(ds[1]) - math.sqrt(ds[0])
        px[X, Y] = int(255 * max(0, 1 - e / 1.6) ** 1.4)
save_gray(im.filter(ImageFilter.GaussianBlur(0.4)), "energy_hex.png")

# Vacío con estrellas (Cielo Negro): puntitos y nebulosa suave
im = Image.new("L", (256, 128), 0); px = im.load()
for _ in range(900):
    x, y = rnd.randrange(256), rnd.randrange(128)
    px[x, y] = min(255, px[x, y] + rnd.choice([60, 90, 140, 255]))
neb = Image.new("L", (256, 128), 0); npx = neb.load()
for x in range(256):
    for y in range(128):
        npx[x, y] = int(40 * (0.5 + 0.5 * math.sin(x * 0.07 + math.sin(y * 0.11) * 2) * math.sin(y * 0.09 + x * 0.02)))
neb = neb.filter(ImageFilter.GaussianBlur(3))
from PIL import ImageChops
save_gray(ImageChops.add(im.filter(ImageFilter.GaussianBlur(0.3)), neb), "energy_stars.png")
print("variaciones ok")
