package com.r7studio.r7Playapp

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** cubic-bezier(.22,1,.36,1) - the easing used all over the original design */
val R7Ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

class R7Colors(
    val bg: Color, val bg2: Color, val card: Color, val tx: Color, val mu: Color, val bd: Color,
    val p: Color, val hi: Color, val on: Color, val navOn: Color, val dark: Boolean
)

val DarkColors = R7Colors(
    bg = Color(0xFF060D13), bg2 = Color(0xFF0C1620), card = Color(0xFF122030),
    tx = Color(0xFFEEF8FF), mu = Color(0xFF8FB0C6), bd = Color(0x1AEEF8FF),
    p = Color(0xFF03A9F4), hi = Color(0xFFFF4D6D), on = Color(0xFF02202F),
    navOn = Color(0xFF03A9F4), dark = true
)

val LightColors = R7Colors(
    bg = Color(0xFFF0F9FE), bg2 = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
    tx = Color(0xFF08283A), mu = Color(0xFF5A7F94), bd = Color(0x1F036496),
    p = Color(0xFF03A9F4), hi = Color(0xFFFF4D6D), on = Color(0xFF02202F),
    navOn = Color(0xFF0288D1), dark = false
)

val LocalR7 = staticCompositionLocalOf { DarkColors }

class R7Font(val family: FontFamily, val weight: FontWeight)

/**
 * Poppins (body) and Fredoka (headings) are loaded from assets/fonts when present.
 * If a font file is missing the app falls back to the system font, so it never crashes.
 */
object R7Fonts {
    var regular = R7Font(FontFamily.SansSerif, FontWeight.Normal)
    var medium = R7Font(FontFamily.SansSerif, FontWeight.Medium)
    var semi = R7Font(FontFamily.SansSerif, FontWeight.SemiBold)
    var fredoka = R7Font(FontFamily.SansSerif, FontWeight.Bold)

    fun load(ctx: Context) {
        regular = file(ctx, "fonts/Poppins-Regular.ttf", FontWeight.Normal)
        medium = file(ctx, "fonts/Poppins-Medium.ttf", FontWeight.Medium)
        semi = file(ctx, "fonts/Poppins-SemiBold.ttf", FontWeight.SemiBold)
        fredoka = fredokaFont(ctx)
    }

    private fun file(ctx: Context, path: String, fallback: FontWeight): R7Font {
        return try {
            val tf = Typeface.createFromAsset(ctx.assets, path)
            R7Font(FontFamily(tf), FontWeight.Normal)
        } catch (e: Exception) {
            R7Font(FontFamily.SansSerif, fallback)
        }
    }

    private fun fredokaFont(ctx: Context): R7Font {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val tf = Typeface.Builder(ctx.assets, "fonts/Fredoka.ttf")
                    .setFontVariationSettings("'wght' 700")
                    .build()
                if (tf != null) return R7Font(FontFamily(tf), FontWeight.Normal)
            }
        } catch (e: Exception) {
        }
        return R7Font(FontFamily.SansSerif, FontWeight.Bold)
    }
}
