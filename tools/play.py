"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/*.jpg (1080 px wide renders of the demo store, see tools/shots.py) and writes
the 512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen is cut short under its caption.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
FONT = ROOT / "app/src/main/res/font"

PAPER = (0xEC, 0xE6, 0xD8)
DOTS = (0xD9, 0xD0, 0xBB)
INK = (0x22, 0x1F, 0x1B)
RED = (0xAE, 0x2F, 0x27)

# The order they appear in the store, with the caption over each: an ink line and a red one.
PAGES = [
    ("2-telegram", ("The editor wires", "one thing to shoot"), ("Редактор шлёт", "телеграмму")),
    ("3-proof", ("Printed as", "a newspaper halftone"), ("Кадр печатается", "газетным растром")),
    ("5-issue", ("On Sunday,", "the week is an issue"), ("В воскресенье", "выходит номер")),
    ("1-desk", ("Ten rubrics,", "1271 assignments"), ("Десять рубрик,", "1271 задание")),
    ("4-filed", ("One line under", "every frame"), ("Одна строка", "под каждым кадром")),
    ("7-cases", ("Investigations", "for weeks of walks"), ("Расследования", "на недели прогулок")),
    ("6-archive", ("Every issue,", "bound by year"), ("Все номера", "в подшивке")),
    ("9-card", ("From trainee", "to chief of the district"), ("От стажёра", "до главного по району")),
]


def font(name, size):
    return ImageFont.truetype(str(FONT / name), size)


def newsprint(w, h):
    """The app's paper with a faint halftone screen fading in towards the bottom."""
    g = Image.new("RGB", (w, h), PAPER)
    d = ImageDraw.Draw(g)
    step = 18
    for row, y in enumerate(range(0, h + step, step)):
        r = 1.0 + 3.2 * (y / h) ** 1.5
        for x in range(-step, w + step, step):
            cx = x + (step / 2 if row % 2 else 0)
            d.ellipse((cx - r, y - r, cx + r, y + r), fill=DOTS)
    return g.convert("RGBA")


def lay(g, card, x, y):
    """A screen on the paper: a soft shadow and a thin ink rule round it."""
    sh = Image.new("RGBA", (card.width + 80, card.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rectangle((40, 50, 40 + card.width, 50 + card.height), fill=(60, 45, 30, 110))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(18)), (x - 40, y - 40))
    g.alpha_composite(card.convert("RGBA"), (x, y))
    ImageDraw.Draw(g).rectangle((x - 3, y - 3, x + card.width + 2, y + card.height + 2), outline=INK, width=3)


def screenshot(src, caption):
    W, H = 1080, 1920
    g = newsprint(W, H)
    d = ImageDraw.Draw(g)
    d.rectangle((0, 0, W, 330), fill=PAPER)
    d.rectangle((60, 40, W - 60, 46), fill=INK)
    title = font("pt_serif_bold.ttf", 84)
    d.text((W / 2, 135), caption[0], font=title, fill=INK, anchor="mm")
    d.text((W / 2, 235), caption[1], font=title, fill=RED, anchor="mm")
    d.rectangle((60, 318, W - 60, 321), fill=INK)
    im = Image.open(src).convert("RGB")
    w = 820
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 380
    # The screen runs off the bottom edge, like a page pulled from the press.
    lay(g, im.crop((0, 0, w, H - top + 20)), (W - w) // 2, top)
    return g.convert("RGB")


def feature(lang):
    W, H = 1024, 500
    g = newsprint(W, H)
    ImageDraw.Draw(g).rectangle((0, 0, 500, H), fill=PAPER)
    issue = Image.open(SHOTS / lang / "5-issue.jpg").convert("RGB")
    # The masthead, the headline and the front-page photo.
    issue = issue.crop((25, 150, 1055, 1060))
    issue = issue.resize((round(issue.width * 420 / issue.height), 420), Image.LANCZOS)
    lay(g, issue, W - issue.width - 40, 40)
    d = ImageDraw.Draw(g)
    name, lines = ("Stringer", ["One thing to shoot", "on every walk.", "On Sunday, an issue."]) if lang == "en" else \
        ("Внештатник", ["Одна вещь, которую снять", "на каждой прогулке.", "В воскресенье — номер."])
    # A masthead between two rules, like the issue beside it.
    d.rectangle((48, 110, 480, 116), fill=INK)
    size = 64
    while font("ruslan.ttf", size).getlength(name) > 430:
        size -= 2
    d.text((48, 135), name, font=font("ruslan.ttf", size), fill=INK)
    d.rectangle((48, 232, 480, 234), fill=INK)
    sub = font("pt_serif_bold.ttf", 30)
    for i, l in enumerate(lines):
        d.text((48, 258 + i * 40), l, font=sub, fill=RED if i == 2 else INK)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def icon():
    """The README logo is already a full square on the telegraph form, so Play's mask finds no corners."""
    Image.open(ROOT / "docs/logo.png").convert("RGB").resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.jpg", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
