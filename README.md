# BetterMaps — Maps Complimentor 🎉

> Your co-pilot for Google Maps. Correct turn? **"Good! Perfect! Amazing!"** Wrong turn? **Roast mode.** Auto-starts when you open Google Maps.

No API key. No entering your destination twice. Just navigate in Google Maps like normal — this app listens in the background and reacts.

## 🔊 Voice lines

**Correct turn (praise — random pick):**
- Good! / Perfect! / Amazing! / Nice!
- Great turn! / Smooth! / Well done! / Nailed it! / Excellent! / Brilliant!

**Wrong turn (roast — random pick, ⚠️ explicit):**
- Stupid bastard. / Shithead. / Dumbass.
- You idiot. / Moron move. / Wrong turn, dumbass.

**Arrival:** *You made it! Great driving!*

Don't want the roasts? Turn off **"Roast wrong turns"** in the app — praise keeps playing. All lines are editable in `FeedbackSpeaker.kt` (`PRAISE` / `ROAST` arrays).

## How it works

1. **Notification listener** (`NavNotificationListener.kt`) reads Google Maps turn-by-turn notifications: *"Turn left onto Main St"*, *"Rerouting…"*, *"You have arrived"*.
2. **Turn state machine** (`TurnDetector.kt`) tracks maneuvers:
   - New maneuver with no reroute in between → previous turn was **correct** → praise 🎉
   - `Rerouting / Make a U-turn / Wrong way` → **wrong turn** → roast 😅
   - Distance countdowns (*"in 500 m"* → *"in 200 m"*) are de-duplicated; highway cruising (`Continue straight`) is never praised.
3. **Auto-start** (`MapsAccessibilityService.kt` + `AutoStartReceiver.kt`): opening Google Maps warms the voice engine and resets trip state. Both listeners are system-managed, so they restart on reboot with no foreground service and near-zero RAM. (Android 10+ blocks apps from popping open over navigation, so BetterMaps runs silently instead of interrupting your drive.)
4. **Audio** (`FeedbackSpeaker.kt`): single shared on-device TTS engine, no audio files.

## Setup (2 minutes, one time only)

1. Install and open **BetterMaps**.
2. Tap **1. Enable notification access** → turn on *BetterMaps*.
3. Tap **2. Enable accessibility = auto-start** → turn on *BetterMaps*.
4. Tap **3. Allow background running** (battery exemption, recommended).
5. **Test: Amazing!** / **Test roast** to check volume.
6. After that, **just open Google Maps and navigate** — you never need to open this app again.

## Logo

The BetterMaps logo (black pin + colors) is used as the launcher icon (`mipmap-anydpi-v26/ic_launcher`), in-app header, splash screen, and notification silhouette — all as tiny vectors to save RAM.
To use your exact PNG instead: save it as `app/src/main/res/drawable-nodpi/logo.png`, then swap `@drawable/ic_logo_foreground` → `@drawable/logo` in the manifest + layout + themes.

## Low-memory design

- No foreground service, no location polling, no network calls.
- Static keyword sets + precompiled regex (no per-event allocations).
- Accessibility tree walk capped (depth 8, 40 nodes, throttled 1.5 s).
- Single lazy TTS engine on app context, stopped on `onTrimMemory`.
- Release builds shrink + minify; AppCompat only (no Material lib).
- `largeHeap="false"`.

## Build

Android Studio Ladybug+ → Sync → Run on a physical device.

```bash
./gradlew assembleDebug
```

Requires Android SDK 34, minSdk 26.

## Privacy

- Only reads **Google Maps navigation text** (`com.google.android.apps.maps`).
- Never collects keystrokes, passwords, contacts, or location history.
- All processing on-device.

## Customize

Edit `FeedbackSpeaker.kt` → `PRAISE` / `ROAST` arrays. Keep it road-friendly. 😄
