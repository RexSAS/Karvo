# MasterSki

Offline Android app for a ski instructor to log lessons and track hours.

- Log lessons (date, duration, student name, optional notes) with edit and swipe-to-delete with undo
- Monthly and lifetime hour totals
- One global hourly rate (Settings); earnings are hidden by default, revealed on demand, and hidden again each time the app is opened
- Time zone setting (default Europe/Rome) only decides what "today" is: the default date of a new lesson and the date in the export file name
- Export to CSV (Settings -> Export to CSV); export only, there is no import

The app has no network access (no INTERNET permission); all data stays on the phone.

## Build prerequisites

- JDK 17 at `~/.local/jdk17`
- Android SDK at `~/Android/Sdk`

```sh
export JAVA_HOME=$HOME/.local/jdk17
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH
```

Run tests: `./gradlew testDebugUnitTest`

Build the release APK: `./gradlew assembleRelease`
(output: `app/build/outputs/apk/release/app-release.apk`; without a keystore it is `app-release-unsigned.apk`, which cannot be installed).

## Release signing

Create a keystore once:

```sh
keytool -genkeypair -v -keystore ~/masterski-release.jks -alias masterski -keyalg RSA -keysize 2048 -validity 10000
```

Then create `keystore.properties` in the repo root:

```properties
storeFile=~/masterski-release.jks
storePassword=...
keyAlias=masterski
keyPassword=...
```

**Back up the keystore and its passwords somewhere safe. If you lose them, future updates cannot be installed over the existing app. Never commit the keystore or `keystore.properties` (both are git-ignored).**

## Installing on a phone

- Send `app-release.apk` to the phone and open it, allowing "Install unknown apps" for the app you opened it from; or
- With USB debugging: `adb install app/build/outputs/apk/release/app-release.apk`

A debug build has a different signature; uninstall it before installing the release build.

> **Warning: uninstalling the app deletes all lessons. Export a CSV first — it cannot be imported back.**

## Backup

Settings -> Export to CSV, then save the file somewhere safe (it opens in Sheets or Excel).
The CSV is a read-only copy for your records: the app cannot import it, so it cannot restore lessons after an uninstall.
