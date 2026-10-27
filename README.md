<p align="center">
  <img src="docs/assets/logo.svg" width="120" alt="Karvo logo">
</p>

<h1 align="center">Karvo</h1>

<p align="center">
  <b>The lesson log for ski instructors.</b><br>
  Log every lesson in seconds and always know your hours, your students and what you're owed.
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.karvo"><img src="https://img.shields.io/badge/Google%20Play-closed%20test-414141?logo=googleplay&logoColor=white" alt="Karvo on Google Play (closed test)"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/languages-Italiano%20%C2%B7%20English-1565C0" alt="Italiano and English">
  <img src="https://img.shields.io/badge/data-stays%20on%20your%20phone-1565C0" alt="Data stays on your phone">
</p>

<p align="center">
  <img src="docs/assets/screenshots/new-lesson.png" width="200" alt="Add a lesson">
  &nbsp;
  <img src="docs/assets/screenshots/overview.png" width="200" alt="Overview with statistics">
  &nbsp;
  <img src="docs/assets/screenshots/podium.png" width="200" alt="Top students podium">
  &nbsp;
  <img src="docs/assets/screenshots/settings.png" width="200" alt="Settings">
</p>

---

Ski instructors are paid by the hour, and at the end of the month the hours have to add up. **Karvo** lets you log each lesson the moment it ends — then check your month, your season and your pay at a glance. No account, no sign-up, nothing to set up.

## Why Karvo

- ⛷️ **Made for the slopes** — log a lesson in two taps: quick durations (1h, 1h 30m, 2h, 3h) and student names suggested from your past lessons.
- 📅 **Every hour accounted for** — lessons grouped by month with their totals, plus your lifetime hours.
- 📷 **Moments** — capture your day on the slopes and watch your season fill up a photo calendar. Tap any day for its recap: the moment, the lessons and the hours. Everything is also saved to a *Karvo* album in your gallery.
- 🔒 **Private by design** — your lessons never leave your phone. No account, no ads, no tracking.
- 🎁 **Free, all of it** — every feature for everyone: no Pro plan, no in-app purchases.
- 🇮🇹 **Italiano and English** — follows your phone's language, or pick one in Settings.

## Privacy

Karvo has no servers and no accounts. Your lessons are stored only on your phone, and the Google Play version doesn't even have internet access. If your phone's Google backup is on, Android includes Karvo's data in it, so your lessons come back when you set up a new phone. Photos and videos go to your phone's gallery and nowhere else. Fingerprint unlock is handled by Android: Karvo never sees your fingerprint.

## Roadmap

### 3.0 — Moments
- [x] **Google Play build** — updated by the Play Store, with no in-app updater and no internet access.
- [x] **Moments** — capture your day on the slopes: tap for a photo, hold for a video, up to 3 moments a day.
- [x] **Moments calendar and daily recap** — a photo calendar of your season; tap a day for its moments, lessons and hours.
- [x] **Full-screen viewer** — swipe through a day's moments, zoom photos, play videos with mute and a seek bar.
- [x] **Better photos** — full 4:3 sensor quality, Karvo's look on every photo, and the phone's HDR or Night mode where available.
- [x] **Restore moments** — after a new phone or a reinstall, pick your photos from the gallery's Karvo album to bring them back into the calendar.
- [x] **Share moments** — share a photo or video in one tap; photos go out in a Karvo design (Banner or Frame).
- [ ] **Mascot: Kevin** — a penguin to give Karvo a face: on the splash screen, empty pages and celebrations.

### 3.1 — Faster and more fun
- [ ] **Repeat a lesson** — long-press a lesson to log the same student and duration again in one tap.
- [ ] **Badges and achievements** — season milestones (50h, 100h, 250h…) with a small celebration.
- [ ] **Evening reminder** — an optional notification on days with nothing logged.
- [ ] **Search and filters** — find lessons by student and filter the list.

### 3.2 — Your season
- [ ] **Widget** — this month's hours on your home screen, with a quick **+** to add a lesson.
- [ ] **Season page** — tap a season to see every month in it, with hours, earnings, lessons and paid status.
- [ ] **Goals** — set a season target and follow your progress on the Overview.
- [ ] **Season Wrapped** — your season in one shareable card: hours, students, busiest day.

See the [changelog](CHANGELOG.md) for what's new in each version.

## Download

Karvo is on **[Google Play](https://play.google.com/store/apps/details?id=com.karvo)**, in a closed test for now: the page opens once you've joined the test as a tester. Everyone else can install the APK from the [latest GitHub release](https://github.com/RexSAS/Karvo/releases/latest): download **`Karvo-x.y.apk`**, open it and allow *Install unknown apps* when Android asks.

<details>
<summary><b>For developers</b></summary>

Requirements: JDK 17 and the Android SDK (platform 36).

```sh
./gradlew testGithubDebugUnitTest   # run the tests
./gradlew assembleGithubRelease     # APK for GitHub releases (with the in-app updater)
./gradlew bundlePlayRelease         # AAB for Google Play (no updater, no internet permission)
```

Release builds are signed with the key in `keystore.properties` (git-ignored). Every release bumps `versionCode` and `versionName`, is tagged `vX.Y.Z` and has exactly one `Karvo-X.Y.apk` asset, which the in-app updater downloads.

Kotlin · Jetpack Compose (Material 3) · Room · DataStore · Navigation Compose · JUnit + Robolectric

</details>
