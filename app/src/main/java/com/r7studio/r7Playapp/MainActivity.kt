package com.r7studio.r7Playapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Pure native Compose UI. The phone can be held in portrait or landscape (it rotates both ways),
 * and the status bar / navigation bar stay hidden (full screen).
 */
class MainActivity : ComponentActivity() {

    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        store = Store(this)
        R7Fonts.load(this)
        hideBars()
        setContent { R7App(store) }
    }

    private fun hideBars() {
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    override fun onResume() {
        super.onResume()
        store.paused = false
        hideBars()
        store.gameView?.onResume()
    }

    override fun onPause() {
        store.paused = true
        store.gameView?.onPause()
        try { android.webkit.CookieManager.getInstance().flush() } catch (e: Throwable) { }
        super.onPause()
    }
}
