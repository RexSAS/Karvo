# MasterSki

Offline Android app for a ski instructor to log lessons and track hours.

- Log lessons (date, time, duration, hourly rate) with edit and swipe-to-delete with undo
- Monthly and lifetime hour totals
- Earnings are hidden by default and revealed on demand
- Time zone aware dates and times
- Backup and restore via CSV (Settings -> Export to CSV)

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

## Backup

Settings -> Export to CSV, then save the file somewhere safe (it opens in Sheets or Excel).
