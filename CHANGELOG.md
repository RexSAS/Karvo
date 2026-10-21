# Changelog

All notable changes to MasterSki are listed here.

## [Unreleased]

### Added
- **Animated splash screen**: the mountains rise and a gold sun appears while the app starts (Android 12+; older versions show the logo still).
- **Premium Alpine Night look**: a starry night sky with a soft aurora and snow-lit ridges behind every screen; the season total on a frosted-glass panel with the headline numbers tucked under it; solid deep-navy tiles everywhere else; a gold skier in the Overview header and a gold add button.
- **Swipe between pages**: swipe left and right to move between Lessons, Overview and Settings (swiping a lesson row still deletes it). Earnings hide and a typed rate is saved as soon as you swipe away.
- **Lessons grouped by month** in tiles, with each month's total hours in its header and the lesson count under the title.

### Changed
- Screen titles are left-aligned with a subtitle (today's date on Overview). Classic keeps the plain Material look with the same layout.

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

[1.2]: https://github.com/RexSAS/MasterSki/releases/tag/v1.2.0
[1.1]: https://github.com/RexSAS/MasterSki/releases/tag/v1.1.0
[1.0]: https://github.com/RexSAS/MasterSki/releases/tag/v1.0.0
