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
