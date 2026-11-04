# New Journey

A calm, accessible meditation and mindfulness app for Android, built with Kotlin and Jetpack Compose.

## Status: Phase 2, meditation timer (version 0.2.0)

What works in this build:

- **Meditation timer**: start, pause, resume and end, with a large countdown and the breathing circle (4 s in, 6 s out; still when the phone's "Remove animations" setting is on).
- **Bells and background sound**: optional bell at the start and end; Silence, Rain or Forest, fading in and out gently. Choices are remembered.
- **Screen off**: the timer, sound and end bell keep going with the phone locked. The lock screen shows the time left with Pause/Resume and End.
- **Two taps from Home**: pick a length, tap Begin; it uses your saved bells and sound.
- **Ending early** asks once, kindly. A minute or more is still saved.
- **Finish screen**: "Well done", minutes meditated, Done, or Write in your journal.
- **Saved sessions** on the phone (Room database). Home's Today card shows minutes against a 10-minute goal and which days you meditated this week.
- **Free vs premium**: 5 and 10 minutes are free; 15 minutes and longer show a gold dot and a calm explanation.
- **Day and Night themes** with Follow my phone, Day, Night, or On a schedule (Profile › Appearance).

Not built yet: routines, the music library and player, journal entries, reminders, accounts, premium purchases and The Journey Within.

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
  model/                 Plain Kotlin rules and data: quotes, categories, moods, timer limits, theme schedule
  designsystem/          Colours, fonts, shapes, the app theme and shared buttons and cards
  datastore/             Saves small settings on the phone (theme, timer choices, saved quotes)
  database/              On-device database of meditation sessions (schemas/ keeps each version)
feature/
  home/                  Home screen and its ViewModel
  explore/               Music library (starter)
  meditate/              Timer screens; timer/ holds the timer logic and the background service
  journal/               Journal (starter)
  profile/               Profile and Appearance settings
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
