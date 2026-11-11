# Online games (GitHub Pages) -> R7 Play ads

Works only inside R7 Play, only for pages on a host listed in `Config.AD_TRUSTED_HOSTS` (https, main frame - never iframes).
The app injects `r7ads.js` automatically. For testing in a normal browser also add `<script src="r7ads.js"></script>`
(copy `app/src/main/assets/r7ads.js` next to the game). In a browser every call just answers "unavailable".

```js
R7Ads.showInterstitial();                       // e.g. after a level / game over (app keeps a 60 s gap)

R7Ads.showRewarded({
  rewardType: "life", amount: 1,
  onReward: function (result) { if (result.success) { /* give reward ONCE */ } },
  onUnavailable: function (message) { /* show message inside the game */ }
});

R7Ads.isOnline(function (online) { if (!online) { /* internet message */ } });
R7Ads.isReady("rewarded", function (ready) { /* show / hide the watch-video button */ });
R7Ads.on("loaded", function (e) { /* e.kind: rewarded | interstitial | banner */ });
```
- Banner stays native (Home screen). Offline games use the older built-in bridge and are unchanged.
- Old WebView (without origin rules) -> online game gets no bridge; calls answer "unavailable".
