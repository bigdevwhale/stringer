# Stringer · Внештатник

Every walk is an assignment. The editor of a small district paper sends a telegram before you go out: one **ordinary** thing to shoot — a door, a pigeon, a puddle, a shadow — with a twist (from knee height, the shabbiest one, the sky in it). Shoot it with the phone's own camera, caption it in one line, and the frame is printed as a newspaper halftone. On Sunday the week's frames come out as an issue, laid out by rubric.

- **1271 assignments** in ten rubrics, English and Russian: three years of daily walks without a repeat. Seasons, weather you report and the evening light decide what can come.
- **The «or»**: every task carries a fallback («or any door»), so a walk never fails.
- **Flash**: at dusk the editor asks for the evening light before it goes.
- **Investigations** that take weeks: the district's alphabet, the digits 0–9, twelve doors, ten cats.
- **Press card** with ranks from trainee to chief of the district, a stamp for every issue, the bound volume of all issues.
- Two papers: the district weekly (plain twists) or the big-city daily (trickier ones mixed in).

No account, no internet, no camera permission: photos are taken by the phone's camera app and stay on the phone.

Part of [Stillcraft](https://bigdevwhale.github.io/stillcraft/).

## Build

```
./gradlew assembleDebug            # app.stringer.dev
./gradlew assembleDebug -Pdemo     # app.stringer.demo, four weeks of a filled newsroom
./gradlew testDebugUnitTest        # includes a three-year no-repeat simulation
./gradlew testDebugUnitTest -Prender --tests '*RenderArtScreens*'   # screen renders to app/build/art
```

The assignment catalogue lives in `design/catalog/src/<rubric>.txt` (`thing|level|seasons|weather|time|en|ru`); `python design/catalog/build.py` checks it and writes `app/src/main/assets/assignments.tsv` and `things.tsv`.

Fonts: Ruslan Display, PT Serif, PT Mono, Manrope, Caveat (SIL Open Font License). Demo photos: Unsplash via Lorem Picsum, see [docs/demo-credits.txt](docs/demo-credits.txt); they ship only in the demo build.
