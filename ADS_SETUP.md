# AdMob setup (R7 Play)

All IDs live in ONE place: `app/build.gradle` (top, "AdMob IDs").
- Debug build: always Google TEST ids.
- Release build: TEST ids until all 4 real ids are pasted, then real ids automatically.

Paste (from AdMob console -> Apps -> R7PLAY):
  ADMOB_REAL_APP_ID       ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
  ADMOB_REAL_BANNER       ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz
  ADMOB_REAL_INTERSTITIAL ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz
  ADMOB_REAL_REWARDED     ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz

Where ads show
- Banner: Home screen, under the hero slider (hidden until it loads).
- Interstitial: after every 2nd finished game (win OR game over). Change AdManager.GAMES_PER_INTERSTITIAL.
- Rewarded: Math Master -> Game Over -> revive button (uses an Undo item if you have one, else plays a video).

Before Play Store upload: Play Console -> App content -> Ads = "Yes, contains ads"; Advertising ID = Yes;
Data safety form; keep your Privacy Policy link. Never click your own real ads.
