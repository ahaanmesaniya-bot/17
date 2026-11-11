package com.r7studio.r7Playapp

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
 *  - Banner        : home screen, under the hero slider (loaded once, never reloaded by us)
 *  - Interstitial  : after every 2nd finished game (bundled games) or when an online game asks for it
 *  - Rewarded      : "watch video" button inside a game
 *
 * Every ad kind is kept pre-loaded in the background. After an ad is used (or fails) the next one is
 * requested automatically; failed loads retry with a growing delay (30 s, 1 m, 2 m, 4 m, 5 m max) and no
 * request is sent while the phone is offline. Ad failures never block the game.
 * Ad unit ids come from BuildConfig (test ids in debug, real ids in release - see app/build.gradle).
 */
class AdManager(private val activity: Activity, private val store: Store) {

    companion object {
        /** show an interstitial after this many finished games */
        const val GAMES_PER_INTERSTITIAL = 2
        private const val SHOW_DELAY_MS = 900L           // let the player see the win / game over screen first
        private const val RETRY_BASE_MS = 30_000L
        private const val RETRY_MAX_MS = 5 * 60_000L
        private const val AD_MAX_AGE_MS = 55 * 60_000L   // AdMob ads expire after 1 hour: never show an old one
        private const val MIN_INTERSTITIAL_GAP_MS = 60_000L  // at least this long between two interstitials
        private const val KICK_MIN_GAP_MS = 15_000L      // a user tap may force a new request, not more often than this
    }

    private val main = Handler(Looper.getMainLooper())
    private val started = AtomicBoolean(false)
    private val sdkInit = AtomicBoolean(false)
    private var destroyed = false

    @Volatile private var interstitial: InterstitialAd? = null
    @Volatile private var interstitialAt = 0L
    private var loadingInterstitial = false
    private var interstitialFails = 0
    private var interstitialReqAt = 0L
    private var lastInterstitialClosedAt = 0L

    @Volatile private var rewarded: RewardedAd? = null
    @Volatile private var rewardedAt = 0L
    private var loadingRewarded = false
    private var rewardedFails = 0
    private var rewardedReqAt = 0L

    private var fullscreenShowing = false
    private var finishedGames = 0
    private var lastEndAt = 0L

    private val retryInterstitial = Runnable { loadInterstitial() }
    private val retryRewarded = Runnable { loadRewarded() }
    private val retryBanner = Runnable { retryBannerNow() }

    /** true when a rewarded video is loaded, fresh and can be shown */
    val rewardedReady: Boolean
        get() = rewarded != null && SystemClock.elapsedRealtime() - rewardedAt <= AD_MAX_AGE_MS

    /** true once the home banner has really loaded (the slot stays hidden until then) */
    var bannerLoaded by mutableStateOf(false)
        private set
    private var banner: AdView? = null
    private var bannerFails = 0

    // ad state events for the online-games bridge: (kind, "loaded" | "failed")
    private val listeners = LinkedHashSet<(String, String) -> Unit>()
    fun addListener(l: (String, String) -> Unit) { listeners.add(l) }
    fun removeListener(l: (String, String) -> Unit) { listeners.remove(l) }
    private fun emit(kind: String, event: String) {
        for (l in listeners.toList()) {
            try { l(kind, event) } catch (e: Throwable) { }
        }
    }

    private fun backoff(fails: Int): Long =
        (RETRY_BASE_MS shl (fails - 1).coerceIn(0, 4)).coerceAtMost(RETRY_MAX_MS)

    fun isReady(kind: String): Boolean = when (kind) {
        "interstitial" -> freshInterstitial() != null
        "rewarded" -> rewardedReady
        "banner" -> bannerLoaded
        else -> false
    }

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
                    bannerFails = 0
                    bannerLoaded = true
                    emit("banner", "loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    bannerFails++
                    emit("banner", "failed")
                    main.removeCallbacks(retryBanner)
                    main.postDelayed(retryBanner, backoff(bannerFails))
                }
            }
            banner = v
            if (store.isOnline()) v.loadAd(AdRequest.Builder().build())
            else main.postDelayed(retryBanner, RETRY_BASE_MS)
        } catch (e: Throwable) {
        }
    }

    private fun retryBannerNow() {
        if (destroyed || bannerLoaded) return
        if (!store.isOnline()) {
            main.postDelayed(retryBanner, RETRY_BASE_MS)   // offline: only a local check, no ad request
            return
        }
        try { banner?.loadAd(AdRequest.Builder().build()) } catch (e: Throwable) { }
    }

    /** the one banner view, detached from any old parent so Compose can re-attach it */
    fun bannerView(): AdView? {
        val v = banner ?: return null
        (v.parent as? ViewGroup)?.removeView(v)
        return v
    }

    // ------------------------------------------------------------------ interstitial

    private fun freshInterstitial(): InterstitialAd? {
        val ad = interstitial ?: return null
        if (SystemClock.elapsedRealtime() - interstitialAt > AD_MAX_AGE_MS) {
            interstitial = null          // expired: throw it away and load a new one
            loadInterstitial()
            return null
        }
        return ad
    }

    private fun loadInterstitial() {
        if (destroyed || interstitial != null || loadingInterstitial || !sdkInit.get()) return
        main.removeCallbacks(retryInterstitial)
        if (!store.isOnline()) {
            main.postDelayed(retryInterstitial, RETRY_BASE_MS)   // offline: no request, check again later
            return
        }
        loadingInterstitial = true
        interstitialReqAt = SystemClock.elapsedRealtime()
        try {
            InterstitialAd.load(
                activity, BuildConfig.ADMOB_INTERSTITIAL, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        loadingInterstitial = false
                        interstitialFails = 0
                        interstitial = ad
                        interstitialAt = SystemClock.elapsedRealtime()
                        emit("interstitial", "loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingInterstitial = false
                        interstitial = null
                        interstitialFails++
                        emit("interstitial", "failed")
                        main.postDelayed(retryInterstitial, backoff(interstitialFails))
                    }
                }
            )
        } catch (e: Throwable) {
            loadingInterstitial = false
        }
    }

    /** the user asked for an ad again after a failure: try a new request now (not more often than every 15 s) */
    private fun kickInterstitial() {
        if (loadingInterstitial || interstitial != null) return
        if (SystemClock.elapsedRealtime() - interstitialReqAt < KICK_MIN_GAP_MS) return
        main.removeCallbacks(retryInterstitial)
        loadInterstitial()
    }

    /** called (through AdBridge) every time a bundled game is won or lost */
    fun onGameEnd() {
        // a game can report its end twice (own call + watcher): count it once
        val now = SystemClock.elapsedRealtime()
        if (now - lastEndAt < 2500) return
        lastEndAt = now
        finishedGames++
        if (finishedGames % GAMES_PER_INTERSTITIAL != 0) return
        main.postDelayed({
            if (!destroyed && store.game != null && !store.paused) showInterstitialNow { }
        }, SHOW_DELAY_MS)
    }

    /**
     * Shows the interstitial if one is ready. [result] is called once, on the main thread:
     * "closed" | "unavailable" | "offline" | "too_soon" | "busy".
     */
    fun showInterstitialNow(result: (String) -> Unit) {
        if (destroyed || fullscreenShowing) { result("busy"); return }
        val now = SystemClock.elapsedRealtime()
        if (lastInterstitialClosedAt != 0L && now - lastInterstitialClosedAt < MIN_INTERSTITIAL_GAP_MS) {
            result("too_soon"); return
        }
        val ad = freshInterstitial()
        if (ad == null) {
            if (!store.isOnline()) { result("offline"); return }
            kickInterstitial()
            result("unavailable")
            return
        }
        var done = false
        fun finish(s: String) { if (!done) { done = true; result(s) } }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                fullscreenShowing = false
                interstitial = null
                lastInterstitialClosedAt = SystemClock.elapsedRealtime()
                loadInterstitial()
                finish("closed")
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                fullscreenShowing = false
                interstitial = null
                loadInterstitial()
                finish("unavailable")
            }
        }
        fullscreenShowing = true
        try {
            ad.show(activity)
        } catch (e: Throwable) {
            fullscreenShowing = false
            interstitial = null
            loadInterstitial()
            finish("unavailable")
        }
    }

    // ------------------------------------------------------------------ rewarded

    private fun loadRewarded() {
        if (destroyed || rewarded != null || loadingRewarded || !sdkInit.get()) return
        main.removeCallbacks(retryRewarded)
        if (!store.isOnline()) {
            main.postDelayed(retryRewarded, RETRY_BASE_MS)   // offline: no request, check again later
            return
        }
        loadingRewarded = true
        rewardedReqAt = SystemClock.elapsedRealtime()
        try {
            RewardedAd.load(
                activity, BuildConfig.ADMOB_REWARDED, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        loadingRewarded = false
                        rewardedFails = 0
                        rewarded = ad
                        rewardedAt = SystemClock.elapsedRealtime()
                        emit("rewarded", "loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingRewarded = false
                        rewarded = null
                        rewardedFails++
                        emit("rewarded", "failed")
                        main.postDelayed(retryRewarded, backoff(rewardedFails))
                    }
                }
            )
        } catch (e: Throwable) {
            loadingRewarded = false
        }
    }

    /** the user tapped "watch video" while nothing was ready: check again now (not more often than every 15 s) */
    private fun kickRewarded() {
        if (loadingRewarded) return
        if (rewarded != null && rewardedReady) return
        rewarded = null                               // expired ad, if any
        if (SystemClock.elapsedRealtime() - rewardedReqAt < KICK_MIN_GAP_MS) return
        main.removeCallbacks(retryRewarded)
        loadRewarded()
    }

    /**
     * Shows the rewarded video. [result] is called exactly once, on the main thread:
     * "rewarded" (Google's reward callback fired) | "skipped" (closed early) | "offline" | "unavailable" | "busy".
     * The reward must only be given for "rewarded".
     */
    fun showRewardedEx(result: (String) -> Unit) {
        if (destroyed || fullscreenShowing) { result("busy"); return }
        val ad = if (rewardedReady) rewarded else null
        if (ad == null) {
            if (!store.isOnline()) { result("offline"); return }
            kickRewarded()
            result("unavailable")
            return
        }
        var earned = false
        var done = false
        fun finish(s: String) { if (!done) { done = true; result(s) } }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                fullscreenShowing = false
                rewarded = null
                loadRewarded()
                finish(if (earned) "rewarded" else "skipped")
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                fullscreenShowing = false
                rewarded = null
                loadRewarded()
                finish("unavailable")
            }
        }
        fullscreenShowing = true
        try {
            ad.show(activity) { earned = true }
        } catch (e: Throwable) {
            fullscreenShowing = false
            rewarded = null
            loadRewarded()
            finish("unavailable")
        }
    }

    /** Old callback style used by the bundled games (through AdBridge): shows the right message itself. */
    fun showRewarded(result: (Boolean) -> Unit) {
        showRewardedEx { status ->
            when (status) {
                "offline" -> store.toast(store.t("chkNet"))
                "unavailable" -> store.toast(store.t("adNA"))
                "skipped" -> store.toast(store.t("adSkip"))
            }
            result(status == "rewarded")
        }
    }

    // ------------------------------------------------------------------ consent options (EEA / UK)

    fun privacyOptionsRequired(): Boolean = try {
        UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    } catch (e: Throwable) {
        false
    }

    fun showPrivacyOptions() {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
        } catch (e: Throwable) {
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
        listeners.clear()
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

    private fun isBundledPage(): Boolean = OfflineGames.adsAllowed(web.url ?: "")

    @JavascriptInterface
    fun gameEnd() {
        main.post { if (isBundledPage()) store.ads?.onGameEnd() }
    }

    @JavascriptInterface
    fun hasRewarded(): Boolean = store.ads?.rewardedReady == true

    /** lets a bundled game choose between the "no ad" and the "check internet" message */
    @JavascriptInterface
    fun isOnline(): Boolean = store.isOnline()

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
    if (c.contains('in') || c.contains('on') || c.contains('show')) return el.style.display !== 'none';
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
