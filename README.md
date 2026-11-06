# New Journey

A calm, accessible meditation and mindfulness app for Android, built with Kotlin and Jetpack Compose.

## Status: Phase 4b, progress and reminders (version 0.5.0)

What works in this build:

- **Music library** (Explore): six categories from `catalog.json`; the Seven Chakras collection with all seven tracks. Favourites and recently played appear at the top.
- **Player**: play, pause, next, previous, seek. Keeps playing with the screen off, with the standard Android controls on the lock screen, in the notification shade and on headphones. Pauses for phone calls and when headphones are unplugged.
- **Meditate with a track**: choose a length (5 and 10 minutes free, longer is Premium). The track starts with its own fade-in and then repeats seamlessly; at the end it fades out and the bell rings. Counts as a session.
- **Continue listening** on Home picks up a track where you stopped. A **mini player** sits above the tabs while music is loaded.
- A track listened to all the way through also counts as a session.
- **Journal**: write entries with how you feel (and, after a session, how you felt before). Search, a list grouped by day, and a month calendar where each day's dot takes the colour of its mood. Edit and delete entries. Opens from the Journal tab, from the mood buttons on Home, and from "Write in your journal" after a session.
- **Journal privacy**: entries are kept in their own database, encrypted as a whole with SQLCipher (AES-256). Its key is sealed by Android Keystore, so the file is unreadable off this phone. Nothing is sent anywhere, and the app does not take part in Android backups.
- Everything from before: timer with bells and Rain/Forest, lock-screen controls, Today card, day and night themes.

- **Progress** (Profile › See your progress): total time, sessions, days meditated, longest run, the last seven days as bars with the daily goal, a month calendar of practice days, and 15 milestones. Everything counts up; a missed day never takes anything away, and runs are only mentioned when there is one to celebrate.
- **Daily goal** (Profile): 5, 10, 15, 20 or 30 minutes; Home's Today card follows it.
- **Milestones** reached by a session are shown on its finish screen.
- **Reminders** (Profile › Set reminders), all off until turned on: daily reminder (can skip days you already meditated), evening wind-down, Sunday summary (skipped in a week without sessions), and one kind note after three quiet days. Times move in 15-minute steps. Planned with AlarmManager windows (no exact-alarm permission, battery-friendly) and restored after a restart, update or time-zone change.

Not built yet: routines, accounts and online catalog, premium purchases, offline downloads and The Journey Within.

### Music: how tracks and loops work

- Tracks live in `core/catalog/src/main/assets/audio/` (AAC, 160 kbps). Each is the owner's master with one volume adjustment, so all tracks play at the same loudness (−19 LUFS). No compression or other processing.
- `core/catalog/src/main/assets/catalog/catalog.json` describes every category, sub-category and track. **Adding a track** = put the `.m4a` in `audio/` and add one entry under `"tracks"`. A unit test (`CatalogParserTest`) checks every entry points to a real file.
- `"loop"` gives the seamless-repeat points in milliseconds: `inMs` (after the fade-in), `outMs` (before the fade-out) and `crossfadeMs`. During a long session `LoopingTrackPlayer` starts a second copy at `inMs` shortly before `outMs` and crossfades the two, so playback never reaches a file edge. The points for the chakra tracks were chosen by measuring where loudness and sound colour match best. Leave `"loop"` out and a track simply repeats from the start.
- Later the same catalog moves online, so tracks can be added without an app update.

### Sounds

`feature/meditate/src/main/res/raw/` holds `bell.m4a`, `rain.m4a` and `forest.m4a`. Rain and Forest were made loop-ready: their last 3 seconds crossfade into the start, so the loop has no dip. To replace a sound, put a new `.m4a` file with the same name in that folder; no code changes are needed. For a looping sound, make sure its end flows into its start.

## Open and run it

1. Install the latest **Android Studio** (developer.android.com/studio).
2. In Android Studio choose **File › Open** and pick this folder. Let it finish "Gradle sync" (the first time downloads a few hundred MB).
3. Plug in your Android phone with USB debugging on, or create an emulator in **Device Manager**.
4. Press the green **Run** button.

If Android Studio offers to upgrade the Android Gradle Plugin or libraries, it's safe to accept; then run the app again to check.

## Project layout

```
app/                     Starts the app: MainActivity, theme choice, bottom tabs, navigation
core/
  model/                 Plain Kotlin rules and data: quotes, catalog, moods, timer limits, theme schedule, progress and milestones (unit-tested)
  designsystem/          Colours, fonts, shapes, the app theme and shared buttons and cards
  datastore/             Saves small settings on the phone (theme, timer choices, saved quotes)
  database/              On-device database: sessions, favourites, listening history (schemas/ keeps each version)
  catalog/               catalog.json and the music files; CatalogRepository reads them
  playback/              PlaybackService (library player + lock-screen controls), MusicPlayer, LoopingTrackPlayer
  journal/               Encrypted journal database (SQLCipher) and its Keystore-protected key
  notifications/         Reminder planning (ReminderScheduler) and the receivers that show them
feature/
  home/                  Home screen and its ViewModel
  explore/               Library, category and player screens
  meditate/              Timer screens; timer/ holds the timer logic and the background service
  journal/               Journal list, search, calendar and the writing screen
  profile/               Profile, progress, daily goal, reminders and appearance
gradle/libs.versions.toml  Every library version in one place
licenses/                Font licences (SIL Open Font License)
```

Rules that keep the code easy to change:

- A `feature` module never depends on another feature; shared code lives in `core`.
- Screens show a UI state from their ViewModel and send user actions back to it.
- All text lives in `res/values/strings.xml` files, so the app can be translated later.
- Free and premium limits live in `core/model/.../Meditation.kt` (`TimerLimits`).

## Changing things yourself

| To change | Edit |
| --- | --- |
| App name under the icon | `app/src/main/res/values/strings.xml` → `app_name` |
| Colours | `core/designsystem/.../theme/Color.kt` |
| Sample quotes | `core/model/.../Quote.kt` → `SampleQuotes` (moves online in Phase 3) |
| Free timer limit | `core/model/.../Meditation.kt` → `FREE_MAX_MINUTES` |
| App icon | `app/src/main/res/drawable/ic_launcher_foreground.xml` (placeholder) |

## Before publishing on Google Play

- Confirm the application ID `com.newjourney.app` in `app/build.gradle.kts`; it can't change after the first upload.
- Raise `targetSdk` to the level Google Play requires at that time.
- Replace the placeholder icon and sample quotes.

## Fonts

Fraunces and Nunito Sans, both under the SIL Open Font License 1.1 (see `licenses/`). The bundled files contain the Latin character set; add the extended sets when other languages are added.
