package com.r7studio.r7Playapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Handles fullscreen video / HTML5 fullscreen and window.open() inside a game. */
class GameChrome(
    private val container: FrameLayout,
    private val web: WebView,
    private val store: Store
) : WebChromeClient() {

    private var custom: View? = null
    private var callback: CustomViewCallback? = null

    override fun onShowCustomView(view: View?, cb: CustomViewCallback?) {
        if (view == null) return
        if (custom != null) {
            cb?.onCustomViewHidden()
            return
        }
        custom = view
        callback = cb
        container.addView(view, FrameLayout.LayoutParams(-1, -1))
        web.visibility = View.INVISIBLE
    }

    override fun onHideCustomView() {
        val v = custom ?: return
        container.removeView(v)
        custom = null
        web.visibility = View.VISIBLE
        callback?.onCustomViewHidden()
        callback = null
    }

    // Some phones show nothing (or crash) for video ads unless a poster is supplied
    override fun getDefaultVideoPoster(): Bitmap? =
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    // Games may not use the camera / microphone / location
    override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
        request?.deny()
    }

    fun hideIfShowing(): Boolean {
        if (custom == null) return false
        onHideCustomView()
        return true
    }

    // window.open(...) -> open in the phone's browser, never inside the app
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        val msg = resultMsg ?: return false
        val popup = WebView(container.context)
        popup.webViewClient = object : WebViewClient() {
            private var handled = false

            private fun handle(u: Uri?) {
                if (handled || u == null) return
                handled = true
                if (u.scheme == "https") store.openExternalUri(u)
                Handler(Looper.getMainLooper()).post { popup.destroy() }
            }

            override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                handle(r?.url)
                return true
            }

            override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                if (url != null && url != "about:blank") {
                    v?.stopLoading()
                    handle(Uri.parse(url))
                }
            }
        }
        val transport = msg.obj as WebView.WebViewTransport
        transport.webView = popup
        msg.sendToTarget()
        return true
    }
}

class GameHolder(val container: FrameLayout, val web: WebView, val chrome: GameChrome)

private fun isAllowedHost(store: Store, host: String?): Boolean {
    val h = host?.lowercase() ?: return false
    return store.allowedHosts().any { h == it || h.endsWith(".$it") }
}

@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
private fun buildGame(
    ctx: Context,
    store: Store,
    game: Game,
    onLoaded: () -> Unit,
    onFailed: () -> Unit
): GameHolder {
    val container = FrameLayout(ctx)
    container.setBackgroundColor(android.graphics.Color.BLACK)

    val web = WebView(ctx)
    web.setBackgroundColor(android.graphics.Color.WHITE)
    web.overScrollMode = View.OVER_SCROLL_NEVER

    val s = web.settings
    s.javaScriptEnabled = true
    s.domStorageEnabled = true
    s.mediaPlaybackRequiresUserGesture = false
    // Same as Chrome: pictures/video over http are allowed inside an https game (ads need this),
    // but scripts and iframes over http stay blocked.
    s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    s.setSupportMultipleWindows(true)
    s.javaScriptCanOpenWindowsAutomatically = true
    // no access to phone files / content providers
    s.allowFileAccess = false
    s.allowContentAccess = false
    s.allowFileAccessFromFileURLs = false
    s.allowUniversalAccessFromFileURLs = false
    s.setSupportZoom(false)
    s.builtInZoomControls = false
    s.textZoom = 100
    // (no addJavascriptInterface anywhere: games cannot call into the app)

    // Very old "Android System WebView" (common on old phones) cannot run modern games / ads.
    try {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val v = WebView.getCurrentWebViewPackage()?.versionName ?: ""
            val major = v.substringBefore('.').toIntOrNull()
            if (major != null && major < 90) store.toast(store.t("oldWeb"))
        }
    } catch (e: Exception) {
    }

    val cm = CookieManager.getInstance()
    cm.setAcceptCookie(true)
    cm.setAcceptThirdPartyCookies(web, true)

    val chrome = GameChrome(container, web, store)
    web.webChromeClient = chrome
    web.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val req = request ?: return true
            val u = req.url
            val scheme = u.scheme?.lowercase()
            if (!req.isForMainFrame) {
                // iframes inside the game: https (or blank) only
                return !(scheme == "https" || scheme == "about" || scheme == "blob" || scheme == "data")
            }
            return when (scheme) {
                "https" -> {
                    if (isAllowedHost(store, u.host)) {
                        false
                    } else {
                        if (req.hasGesture()) store.openExternalUri(u)
                        true
                    }
                }
                "about" -> false
                "http", "mailto", "tel", "sms", "market" -> {
                    if (req.hasGesture()) store.openExternalUri(u)
                    true
                }
                else -> true
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            if (!store.gameFail && url != null && url != "about:blank") store.gameFailCount = 0
            onLoaded()
        }

        private fun failed(view: WebView?) {
            if (store.gameFail) return
            store.gameFailCount += 1
            if (store.gameFailCount >= 4) {
                store.toast(store.t("noNet"))
                store.closeGame()
                return
            }
            store.gameNetFail()
            view?.stopLoading()
            view?.loadUrl("about:blank")
            onFailed()
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            if (request?.isForMainFrame == true) failed(view)
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) failed(view)
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            // Always refuse a bad certificate, but only treat it as "game failed" when it is the
            // game's own page. Ad / analytics / CDN files with a bad certificate (common on old
            // phones with outdated certificate lists) must NOT kill the whole game.
            handler?.cancel()
            val host = try { Uri.parse(error?.url ?: "").host } catch (e: Exception) { null }
            val own = try { Uri.parse(game.url.trim()).host } catch (e: Exception) { null }
            if (host != null && own != null && host.equals(own, ignoreCase = true)) failed(view)
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            store.toast(store.t("gameCrash"))
            store.closeGame()
            return true
        }
    }

    container.addView(web, FrameLayout.LayoutParams(-1, -1))
    store.gameView = web
    web.loadUrl(game.url.trim())
    return GameHolder(container, web, chrome)
}

@Composable
fun GamePlayer(store: Store, game: Game) {
    val holderRef = remember(game.id) { arrayOfNulls<GameHolder>(1) }
    var loading by remember(game.id) { mutableStateOf(true) }

    // never keep the loading screen forever
    LaunchedEffect(game.id, loading) {
        if (loading) {
            delay(12000)
            loading = false
        }
    }

    // internet dropped while the game was still opening -> same No Internet dialog
    LaunchedEffect(game.id, loading) {
        while (loading) {
            delay(1500)
            if (!store.isOnline() && !store.gameFail) {
                store.gameNetFail()
                holderRef[0]?.web?.let {
                    it.stopLoading()
                    it.loadUrl("about:blank")
                }
            }
        }
    }

    // net is back after a failed load -> reload the game automatically
    LaunchedEffect(store.offline, store.gameFail) {
        if (!store.offline && store.gameFail) {
            delay(500)
            val h = holderRef[0]
            if (h != null) {
                store.gameFail = false
                loading = true
                h.web.loadUrl(game.url.trim())
            }
        }
    }

    DisposableEffect(game.id) {
        onDispose {
            val h = holderRef[0]
            if (h != null) {
                try {
                    h.chrome.hideIfShowing()
                    (h.web.parent as? ViewGroup)?.removeView(h.web)
                    h.web.stopLoading()
                    h.web.webViewClient = WebViewClient()
                    h.web.webChromeClient = null
                    h.web.loadUrl("about:blank")
                    h.web.onPause()
                    h.web.removeAllViews()
                    h.web.destroy()
                    h.container.removeAllViews()
                } catch (e: Exception) {
                }
                holderRef[0] = null
            }
            store.gameView = null
        }
    }

    BackHandler(enabled = true) {
        val h = holderRef[0]
        if (h == null || !h.chrome.hideIfShowing()) store.closeGame()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                try {
                    val h = buildGame(ctx, store, game, onLoaded = { loading = false }, onFailed = { })
                    holderRef[0] = h
                    h.container
                } catch (e: Throwable) {
                    // phone has no / a disabled WebView: do not crash, tell the user
                    store.toast(store.t("noWebView"))
                    store.closeGame()
                    FrameLayout(ctx)
                }
            }
        )

        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(300))
        ) {
            LoadingOverlay(store, game)
        }

        FloatingHome(store, visible = !loading)
    }
}

@Composable
fun LoadingOverlay(store: Store, game: Game) {
    val c = LocalR7.current
    val t = rememberInfiniteTransition(label = "ld")
    val orb by t.animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb"
    )
    val ring by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "ring"
    )
    val bar by t.animateFloat(
        initialValue = -48f, targetValue = 144f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "bar"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                if (store.anim) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .graphicsLayer {
                                val sc = 0.92f + 0.53f * ring
                                scaleX = sc
                                scaleY = sc
                                alpha = 0.9f * (1f - ring)
                            }
                            .border(2.dp, c.p, RoundedCornerShape(30.dp))
                    )
                }
                Box(
                    Modifier
                        .size(84.dp)
                        .graphicsLayer {
                            val sc = if (store.anim) orb else 1f
                            scaleX = sc
                            scaleY = sc
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF14212B)),
                    contentAlignment = Alignment.Center
                ) {
                    if (game.logo.isNotEmpty()) {
                        // the game's own icon (same image used on the home screen)
                        R7Image(url = game.logo, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.SportsEsports, null, tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(c.bd)
            ) {
                Box(
                    Modifier
                        .offset(x = bar.dp)
                        .width(48.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(c.p)
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
            R7Text(store.t("load"), 13f, R7Fonts.regular, c.mu)
        }
    }
}

/** Draggable R7 logo button that takes you back to the app from inside a game. */
@Composable
fun FloatingHome(store: Store, visible: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val sizePx = with(density) { 66.dp.toPx() }
        val maxX = (with(density) { maxWidth.toPx() } - sizePx).coerceAtLeast(0f)
        val maxY = (with(density) { maxHeight.toPx() } - sizePx).coerceAtLeast(0f)
        val startX = with(density) { 12.dp.toPx() }
        val startY = with(density) { 10.dp.toPx() }

        var pos by remember {
            val saved = store.gbPos()
            mutableStateOf(if (saved != null) Offset(saved.first, saved.second) else Offset(startX, startY))
        }
        val ox = pos.x.coerceIn(0f, maxX)
        val oy = pos.y.coerceIn(0f, maxY)

        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "Home",
            modifier = Modifier
                .offset { IntOffset(ox.roundToInt(), oy.roundToInt()) }
                .size(66.dp)
                .alpha(if (visible) 1f else 0f)
                .pointerInput(visible) {
                    if (visible) {
                        detectTapGestures(onTap = {
                            store.tick()
                            store.closeGame()
                        })
                    }
                }
                .pointerInput(visible) {
                    if (visible) {
                        detectDragGestures(
                            onDragEnd = { store.saveGb(pos.x.coerceIn(0f, maxX), pos.y.coerceIn(0f, maxY)) }
                        ) { change, drag ->
                            change.consume()
                            pos = Offset(
                                (pos.x + drag.x).coerceIn(0f, maxX),
                                (pos.y + drag.y).coerceIn(0f, maxY)
                            )
                        }
                    }
                }
        )
    }
}
