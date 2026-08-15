<div align="center">

# Currency Converter

**Offline-first currency converter — native iOS & Android.**
166 currencies · real exchange rates · works offline · free, no API key, no ads.

[![Download APK](https://img.shields.io/github/v/release/ismoilovdevml/currency-convertor?label=Download%20APK&logo=android&logoColor=white&color=10A56B&style=for-the-badge)](https://github.com/ismoilovdevml/currency-convertor/releases/latest/download/CurrencyConverter.apk)

[![CI](https://github.com/ismoilovdevml/currency-convertor/actions/workflows/ci.yml/badge.svg)](https://github.com/ismoilovdevml/currency-convertor/actions/workflows/ci.yml)
![Platforms](https://img.shields.io/badge/platform-Android%20%7C%20iOS-10A56B)
[![License: MIT](https://img.shields.io/badge/License-MIT-informational.svg)](LICENSE)

</div>

---

## Download

**Android** — [download the latest APK](https://github.com/ismoilovdevml/currency-convertor/releases/latest/download/CurrencyConverter.apk) and sideload it. Every tagged release ships an installable, signed APK.
**iOS** — build from source (below).

## Screenshots

|  | Light | Dark | Currency picker |
|---|---|---|---|
| **iOS** | <img src="docs/screenshots/ios-light.png" width="220" alt="iOS light"/> | <img src="docs/screenshots/ios-dark.png" width="220" alt="iOS dark"/> | <img src="docs/screenshots/ios-sheet.png" width="220" alt="iOS picker"/> |
| **Android** | <img src="docs/screenshots/android-light.png" width="220" alt="Android light"/> | <img src="docs/screenshots/android-dark.png" width="220" alt="Android dark"/> | <img src="docs/screenshots/android-sheet.png" width="220" alt="Android picker"/> |

## Features

- Works fully offline — a rates snapshot ships inside the app, so conversion works on first launch with no network.
- Real cross-rate math through a USD base: `amount ÷ rate[from] × rate[to]`.
- 166 currencies, each with a circular flag; type into either side.
- Automatic online/offline from the real network state, with a "last updated" timestamp.
- Favourites, search, and light/dark theme — all remembered across launches.
- 29 UI languages, following the device language (RTL for Arabic/Persian). Currency names are localized by the OS.
- Long-press ⌫ to clear the whole amount.

## How it works

Both apps share one data contract (`shared/`) and the same layers:

```
UI (SwiftUI / Compose) → ViewModel → ConverterEngine (pure math)
                                   → RatesRepository: bundled seed → cache → free API
```

Rates are stored relative to USD. Offline is the default path; the network only refreshes the cache.

## Build

**Android** — JDK 21 + Android SDK

```bash
cd android && ./gradlew assembleDebug
```

**iOS** — Xcode 16+ and [XcodeGen](https://github.com/yonaskolb/XcodeGen)

```bash
cd ios && xcodegen generate && xcodebuild -scheme CurrencyConverter build
```

## Structure

```
shared/   currencies.json · seed-rates.json · flags/ · i18n/
android/  Kotlin · Jetpack Compose
ios/      Swift · SwiftUI
.github/  CI + Release (tag → signed APK)
```

## License

MIT © 2026 ismoilovdevml
