package com.r7studio.r7Playapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun R7App(store: Store) {
    val dark = when (store.theme) {
        "light" -> false
        "auto" -> isSystemInDarkTheme()
        else -> true
    }
    val c = if (dark) DarkColors else LightColors

    // first load + offline checks + live refresh (same timings as the HTML app)
    LaunchedEffect(Unit) {
        if (!store.isOnline()) store.offline = true
        store.load()
    }
    // coming back to the app (or turning the screen on) -> refresh the games list at once
    LaunchedEffect(store.paused) {
        if (!store.paused && store.isOnline()) store.sync()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            if (!store.paused && store.offline) store.chkNet(false)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            if (!store.paused && store.isOnline()) store.sync()
        }
    }

    CompositionLocalProvider(LocalR7 provides c) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(c.p.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width / 2f, -size.height * 0.1f),
                            radius = size.width * 0.9f
                        )
                    )
                }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .imePadding()
            ) {
                Header(store)
                Box(Modifier.weight(1f).fillMaxWidth()) { TabContent(store) }
                BottomNav(store)
            }
            GameLayer(store)
            LegalLayer(store)
            ConfirmClearLayer(store)
            OfflineOverlay(store)
            ToastLayer(store)
            BurstLayer(store)
            SplashOverlay()
        }
    }
}

@Composable
fun TabContent(store: Store) {
    val px = with(LocalDensity.current) { 44.dp.roundToPx() }
    AnimatedContent(
        targetState = store.tab,
        transitionSpec = {
            if (!store.anim) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val d = if (TAB_ORDER.indexOf(targetState) >= TAB_ORDER.indexOf(initialState)) 1 else -1
                (fadeIn(tween(450, easing = R7Ease)) +
                    slideInHorizontally(tween(500, easing = R7Ease)) { d * px }) togetherWith
                    fadeOut(tween(120))
            }
        },
        label = "tabs"
    ) { tab ->
        when (tab) {
            "home" -> HomeScreen(store)
            "explore" -> ExploreScreen(store)
            "top" -> TopScreen(store)
            "recent" -> RecentScreen(store)
            else -> SettingsScreen(store)
        }
    }
}

@Composable
fun GameLayer(store: Store) {
    val g = store.game
    val holder = remember { arrayOfNulls<Game>(1) }
    if (g != null) holder[0] = g
    AnimatedVisibility(
        visible = g != null,
        enter = if (store.anim) slideInHorizontally(tween(420, easing = R7Ease)) { it } else EnterTransition.None,
        exit = fadeOut(tween(120))
    ) {
        val cur = holder[0]
        if (cur != null) GamePlayer(store, cur)
    }
}

/** Black screen + R7 Studio logo for 2 seconds (medium size, same proportion as your logo file). */
@Composable
fun SplashOverlay() {
    var show by remember { mutableStateOf(true) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(450)) }
    LaunchedEffect(Unit) {
        delay(2000)
        show = false
    }
    AnimatedVisibility(
        visible = show,
        enter = EnterTransition.None,
        exit = fadeOut(tween(350))
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { } },
            contentAlignment = Alignment.Center
        ) {
            val side = if (maxWidth < maxHeight) maxWidth else maxHeight
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = "R7 Studio",
                modifier = Modifier
                    .width(side * 0.43f)
                    .aspectRatio(720f / 672f)
                    .graphicsLayer { alpha = a.value }
            )
        }
    }
}
