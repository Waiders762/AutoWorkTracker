# AutoWorkTracker

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
[![Download](https://img.shields.io/github/v/release/Waiders762/AutoWorkTracker?label=Download%20APK&color=1D9E75)](https://github.com/Waiders762/AutoWorkTracker/releases/latest)

AutoWorkTracker is an Android app for shift workers. Put a pin on your workplace, leave the building when the shift ends, and the day is written down for you. The month screen shows hours, how many short and long shifts you worked, and pay in the currency you choose.

**Location must stay on.** The GPS switch in the notification shade is required. The geofence uses that signal to notice when you enter work and when you leave, then it writes the shift. If location is off, the day is not recorded.

**[Download the APK](https://github.com/Waiders762/AutoWorkTracker/releases/latest)** · Android 8.0 or newer · arm64 phones · version 1.3.0

The published package is a debug build, signed with the debug key. Install it only if you trust this repository. Allow installation from unknown sources, then open the file on the phone.

Package: `com.autoworktracker`. The debug build targets `arm64-v8a`.

## Default shift rules

| Rule | Exit window | Hours |
| --- | --- | --- |
| Short shift | 13:40–15:30 | 7.5 h |
| Long shift | 17:40–19:30 | 11 h |
| Long-shift preset | same evening window | 11.5 h |

A visit shorter than 15 minutes is ignored. Hours can be written as `11.5` or `11,5` and are stored without rounding. The quick picks are `7.5 h`, `11 h`, and `11.5 h`.

If the evening overwrite option is on, a short shift already saved for that day is replaced by the long-shift hours. Both `11` and `11.5` count toward the long-shift badge, so a day of either length is not filed under “Other”.

The alarm backup on each rule is off by default. Hours are written when you leave the geofence. If you turn the backup on, it uses a fresh GPS fix and does not close the shift when accuracy is worse than 50 meters.

## Geofence

Keep location enabled in the notification shade for the whole shift. One geofence covers entry, a 15-minute stay, and exit. Android reports those events through `GeofencingClient`. On the home screen, distance is a fresh GPS fix: while accuracy is worse than 80 meters, the card shows the workplace radius instead of a guessed distance.

## Backup

Every saved change is copied to public storage, and those files stay after the app is uninstalled:

- `Documents/AutoWorkTracker/Отработанные_часы.txt` — readable report
- `Documents/AutoWorkTracker/autowork_backup.json` — shifts, workplace, rules, rate, currency, and language

Room database `autowork.db` keeps one shift per day. If the database is empty and a backup is already in Documents, the history is restored on launch. You can also pick the `.json` or `.txt` file yourself.

## Languages and currency

The settings header has a compact language switch: `RU`, `UA`, `CZ`, `EN`. Dates, hour words, reports, and the interface follow that choice:

| Language | Shift line | Month total |
| --- | --- | --- |
| RU | `2 ноября: 11 часов` | `часов за ноябрь` |
| UK | `2 листопада: 11 годин` | `годин за листопад` |
| CS | `2. listopadu: 11 hodin` | `hodin za listopad` |
| EN | `November 2: 11 hours` | `hours in November` |

Pay can be shown in `Kč`, `€`, `$`, `₴`, or `zł`. The hourly rate field uses the selected symbol.

## Build an APK

You need JDK 17 or newer and the Android SDK. In PowerShell:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug
```

Or open this folder in Android Studio. The IDE creates `local.properties` with `sdk.dir`.

The debug package is written to:

`app/build/outputs/apk/debug/app-debug.apk`

Toolchain: Gradle 9.6.0, Android Gradle Plugin 9.5.0, Kotlin 2.4.21. Project paths may contain Cyrillic characters; `gradle.properties` already sets `android.overridePathCheck=true`.

Unit tests for hours, rules, and the long-shift match live in `app/src/test`:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.autoworktracker.domain.HoursAndRulesTest
```

## Project layout

```
app/src/main/java/com/autoworktracker/
├── AutoWorkTrackerApp.kt
├── MainActivity.kt
├── di/                 dependency graph
├── domain/             shifts, rules, hours, money, dates
├── data/local/         Room
├── data/preferences/   DataStore
├── data/backup/        Documents text and JSON
├── location/           geofence, alarms, boot
├── notification/
└── ui/                 dashboard, settings, map
```
