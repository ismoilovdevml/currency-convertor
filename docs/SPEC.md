# Currency Converter — Native App Spec (iOS + Android)

Offline-first currency converter. Two **native** apps (SwiftUI + Jetpack Compose) that
pixel-match the design mock and share the same data contract. Free, no API key, works
fully offline from first launch.

Source design: `design/currency-converter-app-design/project/Currency Converter v2.dc.html`
(read it for exact layout). Screenshot reference: `design/.../uploads/pasted-*.png`.

---

## 1. Shared data contract (`shared/`)

Both apps bundle a copy of these files (iOS: app bundle / asset; Android: `assets/`).

- **`shared/currencies.json`**
  ```json
  { "popular": ["USD","EUR","UZS", ...],
    "currencies": [ { "code": "AED", "name": "UAE Dirham", "cc": "ae" }, ... ] }
  ```
  166 fiat currencies. `cc` = 2-letter country code for the flag file (`shared/flags/<cc>.png`).
  `cc` may be `null` for supranational codes (XOF/XAF/XCD/XPF/XDR/ANG) → render a neutral
  circle with no flag.
- **`shared/seed-rates.json`**
  ```json
  { "base": "USD", "updated_utc": "Sat, 15 Aug 2026 00:02:31 +0000",
    "rates": { "USD": 1, "EUR": 0.864617, "UZS": 11913.10991, ... } }
  ```
  Rates are **per 1 USD**. This is the offline seed shipped in the binary.
- **`shared/flags/<cc>.png`** — 155 PNG flags (w160). Bundle all. Missing `cc` → neutral circle.

---

## 2. Conversion logic (identical to mock)

All rates are relative to USD base. To convert `amount` of `FROM` into `TO`:

```
result = amount / rate[FROM] * rate[TO]
```

- `rate[X]` = units of X per 1 USD (from current rates map).
- Editable side: user types into either FROM or TO; the other side is computed.
  - side == "from":  toValue   = amount / rate[from] * rate[to]
  - side == "to":    fromValue = amount / rate[to]   * rate[from]
- Rate line text: `1 <FROM> = <fmt(rate[to]/rate[from])> <TO>`.
- Number formatting (match mock `fmt`):
  - abs >= 1e12 → `/1e12` + "T" (2 dp)
  - abs >= 1e9  → `/1e9`  + "B" (2 dp)
  - abs >= 1e6  → grouped integer, 0 dp
  - else → grouped, min 2 dp, max (abs>=1 ? 2 : 4) dp
  - grouping = thousands separators, en-US style.
- Entry buffer: string; keys `1-9 . 0 ⌫`. Max 9 significant digits. `0`+digit replaces.
  Single `.` allowed. `⌫` deletes, min "0".

Unit tests MUST cover at least: `100 USD → UZS`, `1 EUR → USD`, swap symmetry,
and formatting thresholds (1e6, 1e9, sub-1 decimals).

---

## 3. Rates repository (offline-first)

State: `Live | Offline`. Toggle chip in header switches intent; actual data source:

1. On launch: load **LocalCache** if present, else **BundledSeed**. Convert immediately (no network).
2. If online mode AND network available: fetch latest, write to LocalCache, show "Just now".
3. On failure/offline: keep last data, show "Saved <N>h ago" (from cache timestamp) or "Offline".

**Remote API (free, no key):**
- Primary: `https://open.er-api.com/v6/latest/USD` → `{ result, time_last_update_utc, rates }`.
- Fallback: `https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json`
  → `{ date, usd: { "eur": 0.86, ... } }` (lowercase codes, multiply into USD base map).

LocalCache: persist `{ updated_utc, rates }` (iOS: JSON file in Application Support /
UserDefaults; Android: DataStore or file in filesDir).

`updatedLine`: online success → "Just now"; cache age h≥1 → "Saved Nh ago"; offline → "Offline".

---

## 4. UI spec (pixel-match `Currency Converter v2`)

Font: **Plus Jakarta Sans** (bundle the weights 400–800; iOS: add ttf; Android: `font/`).

### Themes (exact hex)
| token | light | dark |
|---|---|---|
| bg | #F0F4F1 | #141A18 |
| surface | #FFFFFF | #1D2523 |
| fg | #0F1F18 | #E4EBE7 |
| muted | #6C7C74 | #8C9C95 |
| line | #E2E8E3 | #2A3330 |
| key | #FFFFFF | #212A27 |
| sheet | #FFFFFF | #1A2220 |
| accent | #10A56B (both) | |
| accentInk | #FFFFFF | #07271B |
| accentSoft | #E4F3EC | #1E3A2E |
| accentText | #0E7A52 | #5FD3A2 |

### Layout (top → bottom), device frame ~412×892
1. **Header** (h58): title "Converter" (20px, weight 800, letter-spacing -0.5).
   Right: Offline/Live pill (accentSoft bg, accentText, 6px accent dot, label "Offline"/"Live",
   h34, radius 999) + theme toggle button (34×34 round, surface bg, "☾"/"☀").
2. **Converter card** (surface, radius 28, padding 20, soft shadow):
   - FROM row: round flag 34, code (15px/800), name (11px/600 muted, ellipsis), "▼".
     Below, right-aligned big value (weight 800, tabular-nums, letter-spacing -1.6,
     dynamic font size via `fit()`), blinking caret when this side active.
   - Divider row: line — round **swap** button (46, accent bg, accentInk "⇅", shadow,
     active: rotate 180 + scale .94) — line. Padding 14 vertical.
   - TO row: same as FROM but value uses accentText color.
3. **Rate row** (padding 14/4): left `rateLine` (12px/700 muted, tabular), right `updatedLine`
   (11px/600 muted).
4. **Keypad**: grid 3 cols × 4 rows, gap 8, max-height ~326.
   Keys `1 2 3 / 4 5 6 / 7 8 9 / . 0 ⌫`. Each: key bg, radius 18, 21px/600 tabular,
   active state → accent bg + accentInk text.
5. **Currency sheet** (overlay): scrim rgba(8,20,15,.5) fade-in; sheet bottom, height 82%,
   radius 28 top, slide-up anim. Grabber bar. Title "Convert from"/"Convert to" + close ✕.
   Search input (h46, radius 14, "Search currency or country"). Scroll list: each row =
   flag 34 + code(14/800) + name(11/500 muted) + right rate `fmt(baseRate/c.rate) baseCode`
   + green ✓ check if selected. Tapping a row picks it; if it clashes with the other side,
   the two swap. Search filters by code or name (case-insensitive).

### `fit()` dynamic value font size
`fontSize = max(20, min(46, floor(300 / (0.62 * max(len,1)))))` where len = char count of the
formatted string. Apply to both FROM/TO big values.

### Interactions
- Tap FROM/TO code row → open sheet for that side.
- Tap FROM/TO value → make that side active (caret), reset entry to stripped current value.
- Swap button → swap from/to codes (keep entry/side).
- Offline/Live chip → toggle mode (and trigger/skip refresh).
- Theme button → toggle dark/light.
- Default state: entry "100", side "from", from "USD", to "UZS".

---

## 5. Tech + acceptance

### iOS (`ios/`)
- SwiftUI, min iOS 17, MVVM (`ConverterViewModel` = `@Observable`/`ObservableObject`).
- Layers: `ConverterEngine` (pure math + fmt, unit-tested), `RatesRepository`
  (seed/cache/remote), `Theme`, SwiftUI views. `URLSession async`.
- Project generation: use **XcodeGen** (`brew install xcodegen`) with a `project.yml`,
  or Swift-generated `.xcodeproj`. Bundle `shared/*.json` + `flags/` + font.
- Build & run: `xcodebuild -scheme CurrencyConverter -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build`,
  then `xcrun simctl install booted <app>` + `launch`. Screenshot via `xcrun simctl io booted screenshot`.

### Android (`android/`)
- Kotlin + Jetpack Compose (Material3), min SDK 26, target 35, JDK 21.
- Layers mirror iOS: `ConverterEngine`, `RatesRepository`, `ConverterViewModel`,
  Compose UI, DataStore/file cache. Ktor/`HttpURLConnection`.
- Gradle project + wrapper (`gradle wrapper --gradle-version 8.9`). SDK at `~/Library/Android/sdk`.
  `assets/` gets `shared/*.json` + `flags/`. Fonts in `res/font/`.
- Build & run: `./gradlew assembleDebug`, `adb install -r app-debug.apk`, launch,
  `adb exec-out screencap -p > shot.png`. AVD: `currency_pixel` (fallback `Medium_Phone_API_36.0`).

### Acceptance (must prove with real output)
1. App builds with **no errors**.
2. Runs on simulator/emulator; screenshot shows the converter matching the mock (light + dark).
3. Airplane-mode / no-network launch still converts (offline works) — seed loads.
4. `100 USD → UZS` shows the correct value from seed (`100 * 11913.10991` grouped).
5. Unit tests for `ConverterEngine` pass.
6. Currency sheet opens, search filters, selecting changes the rate.

**YAGNI:** no login, no history charts, no push, no ads. Just the converter.
