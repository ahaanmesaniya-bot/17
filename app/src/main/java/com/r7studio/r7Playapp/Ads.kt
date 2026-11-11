package com.r7studio.r7Playapp

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * All AdMob logic in one place.
 *  - Banner        : home screen, under the hero slider
 *  - Interstitial  : after every 2nd finished game (win OR game over)
 *  - Rewarded      : "watch video" button inside a game (Math Master revive)
 *
 * Ad unit ids come from BuildConfig (set in app/build.gradle: test ids until you paste real ones).
 */
class AdManager(private val activity: Activity, private val store: Store) {

    companion object {
        /** show an interstitial after this many finished games */
        const val GAMES_PER_INTERSTITIAL = 2
        private const val SHOW_DELAY_MS = 900L      // let the player see the win / game over screen first
        private const val RETRY_MS = 30_000L        // retry after a failed load
    }

    private val main = Handler(Looper.getMainLooper())
    private val started = AtomicBoolean(false)
    private val sdkInit = AtomicBoolean(false)
    private var destroyed = false

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var rewarded: RewardedAd? = null
    private var loadingRewarded = false
    private var fullscreenShowing = false
    private var finishedGames = 0

    /** true when a rewarded video is loaded and can be shown */
    @Volatile var rewardedReady = false
        private set

    /** true once the home banner has really loaded (the slot stays hidden until then) */
    var bannerLoaded by mutableStateOf(false)
        private set
    private var banner: AdView? = null

    // ------------------------------------------------------------------ start / consent

    fun start() {
        if (!started.compareAndSet(false, true)) return
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity, params,
            {
                // shows the GDPR / EEA consent form only when it is required
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    if (info.canRequestAds()) initSdk()
                }
            },
            { _ -> if (info.canRequestAds()) initSdk() }
        )
        // consent from an earlier launch may already allow ads
        if (info.canRequestAds()) initSdk()
    }

    private fun initSdk() {
        if (!sdkInit.compareAndSet(false, true)) return
        Thread {
            try {
                MobileAds.initialize(activity) { }
            } catch (e: Throwable) {
            }
            main.post {
                if (destroyed) return@post
                loadBanner()
                loadInterstitial()
                loadRewarded()
            }
        }.start()
    }

    // ------------------------------------------------------------------ banner

    private fun loadBanner() {
        if (banner != null) return
        try {
            val dm = activity.resources.displayMetrics
            val widthDp = (dm.widthPixels / dm.density).toInt().coerceIn(320, 600) - 24
            val v = AdView(activity)
            v.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
            v.adUnitId = BuildConfig.ADMOB_BANNER
            v.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    bannerLoaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // AdMob keeps retrying a banner by itself when auto-refresh is on in the console
                    main.postDelayed({ if (!destroyed && !bannerLoaded) banner?.loadAd(AdRequest.Builder().build()) }, RETRY_MS)
                }
            }
            banner = v
            v.loadAd(AdRequest.Builder().build())
        } catch (e: Throwable) {
        }
    }

    /** the one banner view, detached from any old parent so Compose can re-attach it */
    fun bannerView(): AdView? {
        val v = banner ?: return null
        (v.parent as? ViewGroup)?.removeView(v)
        return v
    }

    // ------------------------------------------------------------------ interstitial

    private fun loadInterstitial() {
        if (destroyed || interstitial != null || loadingInterstitial || !sdkInit.get()) return
        loadingInterstitial = true
        try {
            InterstitialAd.load(
                activity, BuildConfig.ADMOB_INTERSTITIAL, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        loadingInterstitial = false
                        interstitial = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingInterstitial = false
                        interstitial = null
                        main.postDelayed({ loadInterstitial() }, RETRY_MS)
                    }
                }
            )
        } catch (e: Throwable) {
            loadingInterstitial = false
        }
    }

    /** called (through AdBridge) every time a game is won or lost */
    fun onGameEnd() {
        finishedGames++
        if (finishedGames % GAMES_PER_INTERSTITIAL != 0) return
        main.postDelayed({ showInterstitial() }, SHOW_DELAY_MS)
    }

    private fun showInterstitial() {
        if (destroyed || fullscreenShowing) return
        if (store.game == null || store.paused) return          // player already left the game
        val ad = interstitial
        if (ad == null) {
            loadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                fullscreenShowing = false
                interstitial = null
                loadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                fullscreenShowing = false
                interstitial = null
                loadInterstitial()
            }
        }
        fullscreenShowing = true
        try {
            ad.show(activity)
        } catch (e: Throwable) {
            fullscreenShowing = false
            interstitial = null
            loadInterstitial()
        }
    }

    // ------------------------------------------------------------------ rewarded

    private fun loadRewarded() {
        if (destroyed || rewarded != null || loadingRewarded || !sdkInit.get()) return
        loadingRewarded = true
        try {
            RewardedAd.load(
                activity, BuildConfig.ADMOB_REWARDED, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        loadingRewarded = false
                        rewarded = ad
                        rewardedReady = true
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingRewarded = false
                        rewarded = null
                        rewardedReady = false
                        main.postDelayed({ loadRewarded() }, RETRY_MS)
                    }
                }
            )
        } catch (e: Throwable) {
            loadingRewarded = false
        }
    }

    /**
     * Shows the rewarded video. [result] is called once with true (reward earned) or false
     * (not ready / closed early). Must be called on the main thread.
     */
    fun showRewarded(result: (Boolean) -> Unit) {
        val ad = rewarded
        if (destroyed || fullscreenShowing || ad == null) {
            store.toast(store.t("adWait"))
            loadRewarded()
            result(false)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                fullscreenShowing = false
                rewarded = null
                rewardedReady = false
                loadRewarded()
                if (!earned) store.toast(store.t("adSkip"))
                result(earned)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                fullscreenShowing = false
                rewarded = null
                rewardedReady = false
                loadRewarded()
                store.toast(store.t("adWait"))
                result(false)
            }
        }
        fullscreenShowing = true
        try {
            ad.show(activity) { earned = true }
        } catch (e: Throwable) {
            fullscreenShowing = false
            rewarded = null
            rewardedReady = false
            loadRewarded()
            result(false)
        }
    }

    // ------------------------------------------------------------------ lifecycle

    fun resume() {
        banner?.resume()
    }

    fun pause() {
        banner?.pause()
    }

    fun destroy() {
        destroyed = true
        main.removeCallbacksAndMessages(null)
        try {
            banner?.destroy()
        } catch (e: Throwable) {
        }
        banner = null
        interstitial = null
        rewarded = null
    }
}

/**
 * Home-screen banner slot. Shows nothing until the banner has loaded, so there is never an empty gap.
 */
@Composable
fun BannerSlot(store: Store) {
    val ads = store.ads ?: return
    if (!ads.bannerLoaded) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { _ -> ads.bannerView() ?: android.view.View(ctx) },
            modifier = Modifier
        )
    }
}

/**
 * The bridge the BUNDLED HTML games use to talk to the ads. It is only added to games that are packed
 * inside the app (they never load anything from the network), and every call re-checks the page url.
 *
 *   R7Ads.gameEnd()          game was won or lost        (counted for the interstitial)
 *   R7Ads.hasRewarded()      true if a rewarded video is ready
 *   R7Ads.showRewarded(tag)  plays the video; answer arrives in window.r7AdResult(tag, true/false)
 */
class AdBridge(private val store: Store, private val web: WebView) {
    private val main = Handler(Looper.getMainLooper())

    private fun isBundledPage(): Boolean = OfflineGames.isLocalUrl(web.url ?: "")

    @JavascriptInterface
    fun gameEnd() {
        main.post { if (isBundledPage()) store.ads?.onGameEnd() }
    }

    @JavascriptInterface
    fun hasRewarded(): Boolean = store.ads?.rewardedReady == true

    @JavascriptInterface
    fun showRewarded(tag: String) {
        val safe = tag.filter { it.isLetterOrDigit() || it == '_' }.take(24)
        main.post {
            if (!isBundledPage()) return@post
            val ads = store.ads
            if (ads == null) reply(safe, false) else ads.showRewarded { ok -> reply(safe, ok) }
        }
    }

    private fun reply(tag: String, ok: Boolean) {
        try {
            web.evaluateJavascript("window.r7AdResult&&window.r7AdResult('$tag',$ok)", null)
        } catch (e: Throwable) {
        }
    }
}

/**
 * Small script injected into the bundled games after they load. It watches each game's own
 * "win" / "game over" screen and calls R7Ads.gameEnd() once when one appears.
 * (No change inside the big game files is needed for the interstitial.)
 */
const val GAME_END_HOOK_JS = """
(function(){
  if (window.__r7adHook || !window.R7Ads) return;
  window.__r7adHook = 1;
  var ids = ['over','ovOver','ovWin','winM','ov'], last = 0;
  function shown(el){
    var c = el.classList;
    if (c.contains('in') || c.contains('on') || c.contains('show')) return true;
    return el.id === 'ov' && el.style.display === 'flex';
  }
  ids.forEach(function(id){
    var el = document.getElementById(id);
    if (!el) return;
    var was = shown(el);
    new MutationObserver(function(){
      var now = shown(el);
      if (now && !was) {
        var t = Date.now();
        if (t - last > 3000) { last = t; try { R7Ads.gameEnd(); } catch (e) {} }
      }
      was = now;
    }).observe(el, {attributes: true, attributeFilter: ['class', 'style']});
  });
})();
"""
