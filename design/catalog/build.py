"""Assemble src/*.txt into assignments.tsv (+ things.tsv, rubrics.tsv) and check coverage.

Source row: thing|level|seasons|weather|time|en|ru  (empty field = any)
seasons: w sp su au   weather: sun rain wet snow frost fog wind   time: day dusk dark
Russian uses {masc|fem} where the correspondent's gender shows.
"""
import os
import re
import sys
from collections import Counter

RUBRICS = {  # file -> (id prefix, en, ru)
    "chronicle": ("ch", "Chronicle", "Хроника"),
    "fauna": ("fa", "City fauna", "Городская фауна"),
    "weather": ("we", "Weather & sky", "Погода и небо"),
    "green": ("gr", "Greenery", "Зелень"),
    "transport": ("tr", "On the move", "Транспорт"),
    "letters": ("le", "Signs & letters", "Вывески и знаки"),
    "self": ("se", "Correspondent's own", "Свой кадр"),
    "texture": ("tx", "Textures", "Фактуры"),
    "finds": ("fi", "Finds", "Находки"),
    "shapes": ("sh", "Colour & shape", "Цвет и форма"),
}

# "Or…" fallback when the exact thing isn't there; empty = a free frame for the rubric.
OR = {
    "door": ("any door", "любая дверь"), "window": ("any window", "любое окно"),
    "entrance": ("any entrance", "любой подъезд"), "stairs": ("any steps", "любые ступеньки"),
    "bench": ("any bench", "любая скамейка"), "fence": ("any fence", "любой забор"),
    "gate": ("any gate or arch", "любые ворота или арка"), "balcony": ("any balcony", "любой балкон"),
    "roof": ("any roof", "любая крыша"), "drainpipe": ("any pipe", "любая труба"),
    "hatch": ("any manhole or grate", "любой люк или решётка"), "curb": ("any curb", "любой бордюр"),
    "mailbox": ("any letterbox", "любой почтовый ящик"), "bin": ("any bin", "любая урна"),
    "playground": ("any playground", "любая детская площадка"), "wall": ("any wall", "любая стена"),
    "yard": ("any courtyard", "любой двор"), "kiosk": ("any shop", "любой магазин"),
    "pigeon": ("any bird", "любая птица"), "sparrow": ("any bird", "любая птица"),
    "crow": ("any bird", "любая птица"), "bird": ("any bird", "любая птица"),
    "cat": ("any cat", "любой кот"), "dog": ("any dog", "любая собака"),
    "bug": ("any small creature", "любая мелкая живность"), "animal": ("any animal", "любое животное"),
    "sky": ("the sky as it is", "небо как есть"), "cloud": ("the sky as it is", "небо как есть"),
    "sun": ("any light", "любой свет"), "shadow": ("any shadow", "любая тень"),
    "puddle": ("anything wet", "что угодно мокрое"), "rain": ("anything wet", "что угодно мокрое"),
    "snow": ("anything white", "что угодно белое"), "ice": ("anything cold", "что угодно холодное"),
    "fog": ("anything far away", "что угодно вдали"), "wind": ("anything that moves", "что угодно движущееся"),
    "light": ("any lamp", "любой фонарь"), "reflection": ("any reflection", "любое отражение"),
    "tree": ("any tree", "любое дерево"), "branch": ("any branch", "любая ветка"),
    "leaf": ("any leaf", "любой лист"), "grass": ("any green", "любая зелень"),
    "flower": ("any plant", "любое растение"), "bush": ("any bush", "любой куст"),
    "moss": ("any plant", "любое растение"),
    "berry": ("anything a plant dropped", "что угодно, что уронило растение"),
    "plant": ("any plant", "любое растение"),
    "car": ("any car", "любая машина"), "bus": ("any stop or bus", "любая остановка или автобус"),
    "sign": ("any road sign", "любой дорожный знак"), "tlight": ("any traffic light", "любой светофор"),
    "road": ("any road", "любая дорога"), "bike": ("anything on wheels", "что угодно на колёсах"),
    "number": ("any number", "любое число"), "letter": ("any word", "любое слово"),
    "signboard": ("any sign", "любая вывеска"), "notice": ("any notice", "любое объявление"),
    "graffiti": ("any drawing", "любой рисунок"), "arrow": ("any arrow", "любая стрелка"),
    "symbol": ("any sign or symbol", "любой знак или символ"),
    "shoes": ("your shoes", "твои ботинки"), "hand": ("your hand", "твоя рука"),
    "myshadow": ("your reflection", "твоё отражение"), "pocket": ("your pocket", "твой карман"),
    "brick": ("any wall", "любая стена"), "rust": ("anything metal", "что угодно железное"),
    "paint": ("anything painted", "что угодно крашеное"),
    "asphalt": ("the ground under your feet", "земля под ногами"),
    "tile": ("the ground under your feet", "земля под ногами"),
    "wood": ("anything wooden", "что угодно деревянное"), "metal": ("anything metal", "что угодно железное"),
    "stone": ("any stone", "любой камень"), "fabric": ("anything soft", "что угодно мягкое"),
    "glass": ("any glass", "любое стекло"), "wire": ("any wires", "любые провода"),
    "pattern": ("anything that repeats", "что угодно повторяющееся"),
    "ground": ("anything on the ground", "что угодно на земле"),
    "left": ("anything left outside", "что угодно, оставленное на улице"),
    "circle": ("any round thing", "что угодно круглое"), "square": ("any square thing", "что угодно квадратное"),
    "triangle": ("any sharp corner", "любой острый угол"), "arch": ("any curve", "любой изгиб"),
    "line": ("any line", "любая линия"),
}
for c in "red orange yellow green blue purple pink brown white black grey gold silver colors".split():
    OR[c] = ("anything bright", "что угодно яркое")
for c in "texture count frame angle mood way".split():
    OR[c] = ("", "")

SEASONS = {"w", "sp", "su", "au"}
WEATHER = {"sun", "rain", "wet", "snow", "frost", "fog", "wind"}
TIME = {"day", "dusk", "dark"}

here = os.path.dirname(os.path.abspath(__file__))
rows, errs = [], []
for name, (pre, _, _) in RUBRICS.items():
    n = 0
    for ln, line in enumerate(open(os.path.join(here, "src", name + ".txt"), encoding="utf-8"), 1):
        line = line.rstrip("\n")
        if not line or line.startswith("#"):
            continue
        p = [x.strip() for x in re.split(r"\|(?![^{]*})", line)]  # keep {masc|fem} intact
        if len(p) != 7:
            errs.append(f"{name}:{ln} fields={len(p)}")
            continue
        thing, lvl, ss, wx, tm, en, ru = p
        if thing not in OR:
            errs.append(f"{name}:{ln} unknown thing {thing}")
        if lvl not in ("1", "2"):
            errs.append(f"{name}:{ln} level {lvl}")
        if set(ss.split()) - SEASONS:
            errs.append(f"{name}:{ln} seasons {ss}")
        if wx and wx not in WEATHER:
            errs.append(f"{name}:{ln} weather {wx}")
        if tm and tm not in TIME:
            errs.append(f"{name}:{ln} time {tm}")
        n += 1
        rows.append(dict(id=f"{pre}{n:03d}", rubric=name, thing=thing, level=lvl,
                         seasons=",".join(ss.split()), weather=wx, time=tm, en=en, ru=ru))

for lang in ("ru", "en"):
    for k, c in Counter(r[lang].lower() for r in rows).items():
        if c > 1:
            errs.append(f"duplicate {lang}: {k}")
if errs:
    print("\n".join(errs))
    sys.exit(1)

with open(os.path.join(here, "assignments.tsv"), "w", encoding="utf-8", newline="\n") as f:
    f.write("id\trubric\tthing\tlevel\tseasons\tweather\ttime\ttask_en\ttask_ru\n")
    for r in rows:
        f.write("\t".join(r[k] for k in ("id", "rubric", "thing", "level", "seasons", "weather", "time", "en", "ru")) + "\n")
with open(os.path.join(here, "things.tsv"), "w", encoding="utf-8", newline="\n") as f:
    f.write("thing\tor_en\tor_ru\n")
    for k in sorted(OR):
        f.write(f"{k}\t{OR[k][0]}\t{OR[k][1]}\n")
with open(os.path.join(here, "rubrics.tsv"), "w", encoding="utf-8", newline="\n") as f:
    f.write("rubric\tprefix\tname_en\tname_ru\n")
    for k, (pre, ren, rru) in RUBRICS.items():
        f.write(f"{k}\t{pre}\t{ren}\t{rru}\n")

# The app reads the same two files from its assets.
assets = os.path.join(here, "..", "..", "app", "src", "main", "assets")
if os.path.isdir(assets):
    for name in ("assignments.tsv", "things.tsv"):
        with open(os.path.join(here, name), encoding="utf-8") as src, open(os.path.join(assets, name), "w", encoding="utf-8", newline="\n") as dst:
            dst.write(src.read())

# Coverage: what an ordinary day (no special weather, daytime) can draw from, per season, vs 3 years of days.
print("total", len(rows))
print(" ".join(f"{k}={v}" for k, v in Counter(r["rubric"] for r in rows).items()))
plain = [r for r in rows if not r["weather"] and r["time"] in ("", "day")]
for s, days in (("w", 90), ("sp", 92), ("su", 92), ("au", 91)):
    pool = [r for r in plain if not r["seasons"] or s in r["seasons"].split(",")]
    print(f"{s}: plain-day pool {len(pool)}, days over 3 years {days * 3}")
print("level2", sum(r["level"] == "2" for r in rows),
      "weather-bound", sum(bool(r["weather"]) for r in rows),
      "evening/dark", sum(r["time"] in ("dusk", "dark") for r in rows))
