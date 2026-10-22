"""Draws the README logo, the launcher icon (a district paper with a halftone photo and the editor's stamp on a telegraph form), as docs/logo.png.

python tools/icon.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
S = 1024
K = 4  # supersampling

FORM = (0xED, 0xE2, 0xC4)
PAPER = (0xF7, 0xF3, 0xEA)
PHOTO = (0xE4, 0xDC, 0xCA)
INK = (0x22, 0x1F, 0x1B)
RED = (0xAE, 0x2F, 0x27)


def main():
    n = S * K
    u = n / 108
    im = Image.new("RGBA", (n, n), FORM + (255,))
    d = ImageDraw.Draw(im)

    def rect(x0, y0, x1, y1, fill):
        d.rectangle([x0 * u, y0 * u, x1 * u, y1 * u], fill=fill)

    def ring(cx, cy, r, w, fill):
        d.ellipse([(cx - r) * u, (cy - r) * u, (cx + r) * u, (cy + r) * u], outline=fill, width=round(w * u))

    rect(36, 30, 76, 82, (0xC9, 0xBE, 0xA2))  # the page's shadow
    rect(34, 28, 74, 80, PAPER)
    rect(38, 32, 70, 37, INK)
    rect(38, 39, 70, 40, INK)
    rect(38, 41.5, 70, 42.1, INK)
    rect(38, 45, 70, 65, PHOTO)
    # The same halftone dots as the launcher vector.
    for row in range(5):
        for col in range(6):
            x = 40.5 + col * 4.6 + (2.3 if row % 2 else 0)
            y = 48 + row * 3.8
            if x > 67:
                continue
            r = 0.6 + row * 0.18 + (0.25 if (col + row) % 3 == 0 else 0)
            d.ellipse([(x) * u, (y - r) * u, (x + 2 * r) * u, (y + r) * u], fill=INK)
    for x0, y0, x1 in ((38, 68, 52), (38, 71, 52), (38, 74, 49), (56, 68, 70)):
        rect(x0, y0, x1, y0 + 1.4, INK)
    ring(74, 73, 9, 2, RED)
    ring(74, 73, 6, 1, RED)
    rect(70.5, 72.2, 77.5, 73.8, RED)

    out = im.resize((S, S), Image.LANCZOS)
    (ROOT / "docs").mkdir(exist_ok=True)
    out.save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()
