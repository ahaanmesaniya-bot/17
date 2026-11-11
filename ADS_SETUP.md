# AdMob setup (R7 Play)

IDs live in ONE place: `app/build.gradle` (top, "AdMob IDs").
- Debug build: always Google TEST ids.
- Release build: ONLY your REAL ids (App, Banner, Interstitial, Rewarded). If any real id is empty / placeholder / test id / wrong format, the release build FAILS on purpose (no silent test ads in a release).
- SDK: Google Mobile Ads SDK 25.5.0 (needs minSdk 24+, Kotlin 2.1+).
- New ad units can take up to ~1 hour before real ads are served ("no fill" until then is normal).
- Never click your own real ads (use a debug build / test device for testing).

How ads behave
- Banner: Home screen, loaded once. Interstitial: after every 2nd finished game (min 60 s apart). Rewarded: on demand.
- Every ad type is pre-loaded; after use the next one loads automatically. Failed load -> retry after 30 s, 1 m, 2 m, 4 m, 5 m (max).
- No ad request is sent while the phone is offline. Ads older than 55 min are discarded and reloaded.
- A user tap on "watch video" when nothing is ready triggers a fresh request (at most every 15 s) and shows:
  "Ad अभी उपलब्ध नहीं है। कृपया थोड़ी देर बाद कोशिश करें।"  (offline: "कृपया अपना इंटरनेट कनेक्शन चेक करें।")
- Reward is given only after Google's reward callback.

Constants to tune: `AdManager.GAMES_PER_INTERSTITIAL`, `MIN_INTERSTITIAL_GAP_MS`, `RETRY_*` in `Ads.kt`.

Before Play Store upload: Play Console -> App content -> Ads = "Yes"; Advertising ID = Yes; Data safety form; Privacy Policy link.
