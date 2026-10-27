# Changelog

All notable changes to Karvo are listed here. Versions 1.0–1.3 were released as **MasterSki**.

## [3.0] — unreleased

### Added
- **Moments**: tap the new camera button on *Lessons* to capture your day on the slopes: tap the shutter for a photo, or hold it for a video of up to 10 seconds. Each day keeps up to 3 moments; when the day is full, you pick the one to replace. Moments are saved to a **Karvo** album in your gallery. Every photo gets Karvo's look: lifted shadows, a soft roll-off on bright snow and sky, and richer colours that leave skin natural. On phones that share their own camera processing with apps, a button turns it on: **HDR** (on by default) or, where only that is offered, **Night** (off by default, for dusk and evening).
- **Moments calendar and daily recap**: the *Moments* page is a calendar where each day shows its moment, and days with lessons get a dot. Tap a day to see its moments, its lessons and its hours. Videos play muted on loop right in the recap. Tap a moment to open it full screen: swipe through the day, pinch or double-tap photos to zoom, and play videos with sound, a mute button and a seek bar.
- **Share moments**: tap *Share* on a moment to send it to any app. Photos first pick a Karvo design, shown large over the blurred photo: **Frame** (9:16, story-sized: the photo in a tilted Karvo frame over a blurred copy of itself) or **Banner** (the whole photo with KARVO and the date along the bottom). Videos are shared as they are.
- **Instagram Stories**: in the share picker, **Story** opens Instagram's story editor with your moment already in place, full screen in either design.
- **Share from the day page**: every moment in a day's recap has a Share button, and tapping a photo grows it into the full-screen viewer, Share button and all.
- **Swipe between days**: in a day's recap, swipe to slide in the next or previous day, just like the photos.
- **Privacy policy**: *Settings → Privacy policy* opens it, in English and Italian. In short: everything stays on your phone.
- **Google Play build**: a separate build for Google Play that's updated by the Play Store, so it has no in-app updater and no internet access at all.

### Changed
- Karvo now targets Android 16 (API 36), as Google Play requires.

### Upgrading
- Install over Karvo 2.1 as usual: your lessons are kept.

## [2.1] — 2026-10-21

### Added
- **Paid months**: open a month and tick *Paid by the school* when you get paid (you pick the payment date). Months show a gold ✓ *Paid* or an amber *Partly paid* badge, and the Overview shows what's still **to be paid**. A lesson added to a month that's already paid is flagged, and the month becomes *Partly paid*.
- **Import from CSV**: *Settings → Import from CSV* adds lessons from a Karvo or MasterSki export — handy to bring your MasterSki lessons over or move to a new phone. Lessons already in Karvo are skipped, you see a preview first, and your current lessons are backed up before anything is added.
- **Seasons**: the Overview shows this season's hours and earnings, compared with last season at the same date, plus a folded list of every season. Pick the month your season starts in Settings (November by default).
- **Fingerprint unlock**: showing earnings asks for your fingerprint, face or phone PIN. On by default; turn it off in Settings.

### Changed
- CSV exports and backups have a new last column, `paid_on` (the date the school paid that lesson, empty if unpaid). Older files still import and restore; their lessons come in as unpaid.

### Upgrading
- Install over Karvo 2.0 as usual (the in-app update works): your lessons are kept and start as unpaid.

## [2.0] — 2026-10-21

### Changed
- **MasterSki is now Karvo.** New name everywhere: the app, the launcher icon label, the update dialog, the exported CSV file name (`karvo-lessons-YYYY-MM-DD.csv`) and the GitHub repository (`RexSAS/Karvo`).

### Upgrading
- Karvo is installed as a **new app** (package `com.karvo`), so MasterSki can't update into it and Karvo starts empty. Install `Karvo-2.0.apk`, then uninstall MasterSki. From now on Karvo offers its own updates inside the app.

## [1.3] — 2026-10-21

### Added
- **Animated splash screen**: the mountains rise and a gold sun appears while the app starts (Android 12+; older versions show the logo still).
- **Premium Alpine Night look**: a starry night sky with snow-lit ridges behind every screen; your total hours on a frosted-glass panel with the headline numbers tucked under it; solid deep-navy tiles everywhere else; the gold MasterSki emblem in the Overview header and a solid gold add button.
- **Swipe between pages**: swipe left and right to move between Lessons, Overview and Settings (swiping a lesson row still deletes it). Earnings hide and a typed rate is saved as soon as you swipe away.
- **Lessons grouped by month** in tiles, with each month's total hours in its header and the lesson count under the title.

### Changed
- Screen titles are left-aligned with a subtitle (today's date on Overview). Classic keeps the plain Material look with the same layout.

### Upgrading
- On 1.2 the app offers 1.3 by itself when you open it. On 1.0 or 1.1, install it by hand once — lessons and settings are kept.

## [1.2] — 2026-10-21

### Added
- **Automatic updates**: when the app starts, it checks GitHub for a newer release and asks whether to update. It backs up your lessons, downloads the new version and opens Android's installer. The first time, Android asks you to allow MasterSki to install apps.
- **Check for updates** in Settings, showing the installed version.
- **Backups**: up to 3 backups of your lessons are kept privately inside the app. One is made automatically before every update, and you can make one any time with *Back up now* (*Settings → Backups*).
- **Restore a backup**: two "accept the risk" steps, then an optional backup of your current lessons so the restore itself can be undone.

### Changed
- MasterSki now uses the internet **only** to check for and download updates from GitHub. Your lessons never leave the phone.

### Upgrading
- Install 1.2 by hand once (over 1.1 or 1.0 — lessons are kept). From 1.2 on, new versions are offered inside the app.

## [1.1] — 2026-10-21

### Added
- **Statistics on the Overview**: lessons this month, average lesson length, number of students, a 6-month hours chart (current month in gold) and your busiest weekdays.
- **Top students podium**: gold, silver and bronze medals for your top 3 students, plus places 4 and 5.
- **Alpine Night theme**: a premium dark navy and gold look, on by default. Choose *Classic* in Settings for the previous look.
- **Italiano / English**: the app follows your phone's language, or pick one in *Settings → Language*.
- **Delete confirmation**: swiping a lesson away now asks "Delete this lesson?" first. *Undo* is still there.

### Changed
- Statistics sit above the month list, which now has its own *Months* header.
- *Export to CSV* is now a settings row with an icon, so it no longer looks like a *Save* button.
- Month names are always capitalised (for example "Ottobre 2026").

### Privacy
- All amounts in the statistics stay hidden as `**** €` until you tap the eye, just like the rest of the app. Screen readers never announce hidden amounts.

### Upgrading
- Install 1.1 over 1.0: your lessons and settings are kept. No data migration is needed.

## [1.0] — 2026-10-21

First release: log lessons (date, duration, student, notes), monthly and lifetime hours, private earnings from an hourly rate, time zone setting, CSV export, fully offline.

[2.1]: https://github.com/RexSAS/Karvo/releases/tag/v2.1.0
[2.0]: https://github.com/RexSAS/Karvo/releases/tag/v2.0.0
[1.3]: https://github.com/RexSAS/Karvo/releases/tag/v1.3.0
[1.2]: https://github.com/RexSAS/Karvo/releases/tag/v1.2.0
[1.1]: https://github.com/RexSAS/Karvo/releases/tag/v1.1.0
[1.0]: https://github.com/RexSAS/Karvo/releases/tag/v1.0.0
