# New Journey

A calm, accessible meditation and mindfulness app for Android, built with Kotlin and Jetpack Compose.

## Status: Phase 1 foundation (version 0.1.0)

What works in this build:

- **Day and Night themes** with four choices in Profile › Appearance: Follow my phone, Day, Night, or On a schedule (pick when night starts and when day returns). The choice is saved on the phone.
- **Five-tab navigation**: Home, Explore, Meditate, Journal, Profile, each with an icon and a text label.
- **Home screen**: greeting by time of day, daily quote (changes every day; tap Save to keep it), quick-start durations, Begin button, categories, today's progress card, mood check-in.
- **Free vs premium timer rule**: 5 and 10 minutes are free; 15 minutes and longer show a gold dot and a short, calm explanation.
- **Meditate**: shows the chosen duration with a breathing circle (4 s in, 6 s out).
- **Explore** lists the six categories; **Journal** opens with the mood tapped on Home.

Not built yet (next phases): the working timer with bells and sounds, music playback, journal entries, progress tracking, reminders, accounts, premium purchases and The Journey Within.

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
  datastore/             Saves small settings on the phone (theme, saved quotes)
feature/
  home/                  Home screen and its ViewModel
  explore/               Music library (starter)
  meditate/              Meditation timer (starter)
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
