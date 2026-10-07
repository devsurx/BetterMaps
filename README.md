# BetterMaps — Maps Complimentor 🎉

> Your cheerful co-pilot for Google Maps. Hear **"Nice! Perfect! Good!"** on every correct turn — and a gentle **"Oops, wrong turn"** when you miss one.

No API key. No entering your destination twice. Just navigate in Google Maps like normal — this app listens in the background and cheers you on.

## How it works

1. **Notification listener** (`NavNotificationListener.kt`) reads Google Maps turn-by-turn notifications (`com.google.android.apps.maps`): *"Turn left onto Main St"*, *"Rerouting…"*, *"You have arrived"*.
2. **Turn state machine** (`TurnDetector.kt`) tracks maneuvers:
   - New maneuver arrives with no reroute in between → previous turn was **correct** → praise 🎉
   - `Rerouting / Make a U-turn / Wrong way` appears → **wrong turn** → gentle callout 😅
   - Distance countdowns (*"in 500 m"* → *"in 200 m"* → *"now"*) are de-duplicated so it doesn't spam.
   - Straight highway cruising (`Continue straight / Head north`) is intentionally *not* praised — only real turns.
3. **Accessibility fallback** (`MapsAccessibilityService.kt`) reads the on-screen nav card for devices where notification text is truncated.
4. **Audio feedback** (`FeedbackSpeaker.kt`) speaks via on-device TTS — no audio files needed, works offline:
   - Correct: *Nice! / Perfect! / Good! / Great turn! / Smooth! / Well done! / Nailed it!* (random)
   - Wrong: *Oops, wrong turn. / Missed it, rerouting. / Not that way.*
   - Arrival: *You made it! Great driving!*

## Setup (2 minutes)

1. Install the app (`Maps Complimentor`) and open it.
2. Tap **1. Enable notification access** → turn on *Maps Complimentor*.
3. Tap **2. Enable accessibility** → turn on *Maps Complimentor* (fallback, recommended).
4. Tap **Test: Nice!** / **Test: Oops** to check volume.
5. Open Google Maps and start navigation. That's it.

## Project structure

```
settings.gradle / build.gradle          Gradle setup (AGP 8.5.2, Kotlin 1.9.24, SDK 34)
app/src/main/AndroidManifest.xml        Permissions + 2 background services
app/src/main/java/com/mapscomplimentor/
  MainActivity.kt                       Setup UI + permission checks + test buttons
  NavNotificationListener.kt            Primary: parses Maps notifications
  MapsAccessibilityService.kt           Fallback: parses on-screen nav card
  TurnDetector.kt                       Pure-Kotlin turn right/wrong logic (testable)
  TurnEngine.kt                         De-dup + anti-spam + wiring to speaker
  FeedbackSpeaker.kt                    TTS "Nice! Perfect! Good!" / "Oops"
app/src/main/res/                       Layout, strings, accessibility config
```

## Build

Open in **Android Studio Ladybug+** → Sync Gradle → Run on a physical device (notification access doesn't work well on emulators).

```bash
./gradlew assembleDebug
```

Requires: Android SDK 34, minSdk 26 (Android 8.0+).

## Privacy

- Only reads **Google Maps navigation text** (`com.google.android.apps.maps`).
- Never collects keystrokes, passwords, contacts, or location history.
- All processing is on-device. No network calls, no analytics.

## Customize

Edit `FeedbackSpeaker.kt` → `praiseLines` / `wrongLines` to add your own phrases, or swap TTS for `MediaPlayer` + MP3s in `res/raw/`.

---
Built for safer, happier driving. ⭐ Star it if it made you smile on a correct turn.
