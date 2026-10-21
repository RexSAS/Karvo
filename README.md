<p align="center">
  <img src="docs/assets/logo.svg" width="120" alt="MasterSki logo">
</p>

<h1 align="center">MasterSki</h1>

<p align="center">
  <b>Keep track of every ski lesson you teach — hours, students and pay, right on your phone.</b>
</p>

<p align="center">
  <a href="https://github.com/RexSAS/MasterSki/releases/latest"><img src="https://img.shields.io/github/v/release/RexSAS/MasterSki?label=download&color=1565C0" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin + Jetpack Compose">
  <img src="https://img.shields.io/badge/works-offline-1565C0" alt="Works offline">
</p>

---

Ski instructors are paid by the hour, and at the end of the month the hours have to add up. **MasterSki** is a small, fast app to log each lesson the moment it ends and check your monthly hours against what the ski school pays you.

<p align="center">
  <img src="docs/assets/screenshots/new-lesson.png" width="240" alt="Add a lesson">
  &nbsp;
  <img src="docs/assets/screenshots/overview.png" width="240" alt="Overview of hours">
  &nbsp;
  <img src="docs/assets/screenshots/settings.png" width="240" alt="Settings">
</p>

## Features

- **Log a lesson in seconds** — date, duration (quick buttons for 1h, 1h 30m, 2h, 3h), student name with suggestions from previous lessons, and optional notes. Group lessons? Just write it in the name, e.g. *Group Rossi*.
- **Monthly and lifetime hours** — the Overview shows your total hours and a row for every month; tap a month to see its lessons.
- **Private earnings** — set your hourly rate and MasterSki calculates what you earned. Amounts stay hidden as `**** €` until you tap the eye, and hide again when you leave the screen.
- **Statistics with charts** — below the months, the Overview shows lessons this month, average lesson length, your number of students, a 6-month hours chart, your top students and your busiest weekdays. Any money there stays hidden until you tap the eye.
- **Alpine Night theme** — a dark navy and gold look, on by default. Prefer the old look? Pick *Classic* in Settings.
- **Italiano / English** — the app follows your phone's language, or pick one in Settings.
- **Edit or undo** — tap a lesson to fix it, swipe it away to delete, and tap *Undo* if that was a mistake.
- **Export to CSV** — save all your lessons as a spreadsheet to keep a backup or send to the ski school.
- **Your time zone** — "today" follows the time zone you pick (default *Europe/Rome*).
- **Fully offline and private** — no account, no internet permission. Everything stays on your phone.

## Install

1. On your Android phone, open the [latest release](https://github.com/RexSAS/MasterSki/releases/latest) and download **`MasterSki-x.y.apk`**.
2. Open the file and allow *Install unknown apps* when Android asks.
3. Open **MasterSki**, set your hourly rate in *Settings*, and log your first lesson with **+**.

> [!WARNING]
> Uninstalling the app deletes all lessons. Use **Settings → Export to CSV** regularly as a backup — the CSV is a copy for your records and cannot be imported back.

## Build from source

Requirements: JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).

```sh
export JAVA_HOME=$HOME/.local/jdk17
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH

./gradlew testDebugUnitTest   # run the tests
./gradlew installDebug        # install a debug build on a connected phone
./gradlew assembleRelease     # build app/build/outputs/apk/release/app-release.apk
```

### Release signing

Create a keystore once:

```sh
keytool -genkeypair -v -keystore ~/masterski-release.jks -alias masterski -keyalg RSA -keysize 2048 -validity 10000
```

Then add `keystore.properties` in the repository root:

```properties
storeFile=~/masterski-release.jks
storePassword=...
keyAlias=masterski
keyPassword=...
```

Without this file `assembleRelease` produces an unsigned APK that cannot be installed.

> [!CAUTION]
> Back up the keystore and its password. If you lose them, updates can no longer be installed over the existing app. Never commit the keystore or `keystore.properties` (both are git-ignored).

A debug build is signed differently from the release build: uninstall it (export your lessons first!) before installing the release.

## Tech

Kotlin · Jetpack Compose (Material 3) · Room · DataStore · Navigation Compose · MVVM · JUnit + Robolectric

Design and implementation notes live in [`docs/superpowers/`](docs/superpowers/).
