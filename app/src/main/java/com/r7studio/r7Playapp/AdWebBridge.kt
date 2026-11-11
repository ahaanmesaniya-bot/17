package com.r7studio.r7Playapp

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/**
 * Secure ads channel for ONLINE games (pages on Config.AD_TRUSTED_HOSTS).
 *
 * The native object `R7AdsNative` is injected by WebView ONLY into frames whose origin is in the allowed
 * list, and every message is re-checked here: main frame only, https, trusted host. Iframes, other sites and
 * http pages get nothing. Games do not use it directly - they call the small `R7Ads` API from
 * assets/r7ads.js (injected automatically at document start, or included by the game itself).
 *
 * Messages (JSON strings):
 *   {id, op:"isOnline"}                         -> {id, online}
 *   {id, op:"isReady", kind}                    -> {id, ready}
 *   {id, op:"interstitial"}                     -> {id, status:"closed|unavailable|offline|too_soon|busy", message}
 *   {id, op:"rewarded"}                         -> {id, status:"rewarded|skipped|unavailable|offline|busy", message}
 *   {op:"subscribe"}                            -> later pushes {event:"loaded|failed", kind}
 */
class AdWebBridge(private val store: Store) {

    companion object {
        const val NAME = "R7AdsNative"
        const val LIB_ASSET = "r7ads.js"

        fun origins(): Set<String> = Config.AD_TRUSTED_HOSTS.map { "https://$it" }.toSet()
    }

    private var events: JavaScriptReplyProxy? = null
    private val listener: (String, String) -> Unit = { kind, event ->
        val p = events
        if (p != null) {
            try {
                p.postMessage(JSONObject().put("event", event).put("kind", kind).toString())
            } catch (e: Throwable) {
                events = null
            }
        }
    }

    /** false when this phone's WebView is too old for origin rules: then online games get no ads bridge */
    fun install(web: WebView): Boolean {
        if (origins().isEmpty()) return false
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return false
        WebViewCompat.addWebMessageListener(web, NAME, origins()) { _, message, sourceOrigin, isMainFrame, reply ->
            if (isMainFrame && originOk(sourceOrigin)) handle(message.data, reply)
        }
        store.ads?.addListener(listener)
        return true
    }

    /** puts the R7Ads JS library into the trusted pages before the game's own scripts run */
    fun injectLibrary(ctx: Context, web: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        try {
            val js = ctx.assets.open(LIB_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            WebViewCompat.addDocumentStartJavaScript(web, js, origins())
        } catch (e: Throwable) {
        }
    }

    fun detach() {
        store.ads?.removeListener(listener)
        events = null
    }

    private fun originOk(o: Uri): Boolean =
        o.scheme.equals("https", true) && o.host?.lowercase() in Config.AD_TRUSTED_HOSTS

    private fun send(reply: JavaScriptReplyProxy, id: String, body: JSONObject) {
        try {
            reply.postMessage(body.put("id", id).toString())
        } catch (e: Throwable) {
            // the page is gone: nothing to answer
        }
    }

    private fun messageFor(status: String): String = when (status) {
        "offline" -> store.t("chkNet")
        "unavailable", "busy", "too_soon" -> store.t("adNA")
        "skipped" -> store.t("adSkip")
        else -> ""
    }

    private fun handle(raw: String?, reply: JavaScriptReplyProxy) {
        val o = try { JSONObject(raw ?: return) } catch (e: Exception) { return }
        val id = o.optString("id").filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(40)
        val ads = store.ads
        when (o.optString("op")) {
            "isOnline" -> send(reply, id, JSONObject().put("online", store.isOnline()))
            "isReady" -> send(reply, id, JSONObject().put("ready", ads?.isReady(o.optString("kind")) == true))
            "subscribe" -> events = reply
            "interstitial" -> {
                if (ads == null) { send(reply, id, status("unavailable")); return }
                ads.showInterstitialNow { st -> send(reply, id, status(st)) }
            }
            "rewarded" -> {
                if (ads == null) { send(reply, id, status("unavailable")); return }
                ads.showRewardedEx { st -> send(reply, id, status(st)) }
            }
        }
    }

    private fun status(st: String) = JSONObject().put("status", st).put("message", messageFor(st))
}
