package com.r7studio.r7Playapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------- toast

@Composable
fun BoxScope.ToastLayer(store: Store) {
    val c = LocalR7.current
    var show by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    LaunchedEffect(store.toastTick) {
        val m = store.toastMsg
        if (m != null && store.toastTick > 0) {
            msg = m
            show = true
            delay(1800)
            show = false
        }
    }
    AnimatedVisibility(
        visible = show,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp),
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { 20 },
        exit = fadeOut(tween(250))
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(c.tx)
                .padding(horizontal = 18.dp, vertical = 9.dp)
        ) { R7Text(msg, 13f, R7Fonts.regular, c.bg) }
    }
}

// ---------------------------------------------------------------- hearts burst

@Composable
fun BurstLayer(store: Store) {
    Box(Modifier.fillMaxSize()) {
        store.bursts.toList().forEach { b ->
            key(b.id) { BurstItem(store, b) }
        }
    }
}

@Composable
fun BurstItem(store: Store, b: Burst) {
    val p = remember { Animatable(0f) }
    val d = LocalDensity.current
    val hi = LocalR7.current.hi
    LaunchedEffect(b.id) {
        p.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        store.bursts.remove(b)
    }
    val half = with(d) { 6.5.dp.toPx() }
    val r = with(d) { 36.dp.toPx() }
    val lift = with(d) { 14.dp.toPx() }
    for (i in 0 until 7) {
        val a = Math.PI * 2 * i / 7
        val dx = (cos(a) * r).toFloat()
        val dy = (sin(a) * r).toFloat() - lift
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = hi,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (b.x + dx * p.value - half).roundToInt(),
                        (b.y + dy * p.value - half).roundToInt()
                    )
                }
                .size(13.dp)
                .graphicsLayer {
                    alpha = 1f - p.value
                    val sc = 0.6f + 0.7f * p.value
                    scaleX = sc
                    scaleY = sc
                }
        )
    }
}

// ---------------------------------------------------------------- offline dialog

@Composable
fun OfflineOverlay(store: Store) {
    val c = LocalR7.current
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = store.offline,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200))
    ) {
        val shakeX = remember { Animatable(0f) }
        val enter = remember { Animatable(0f) }
        val d = LocalDensity.current
        LaunchedEffect(store.shake) {
            if (store.shake > 0) {
                for (target in listOf(-9f, 9f, -9f, 9f, 0f)) {
                    shakeX.animateTo(target, tween(80))
                }
            }
        }
        LaunchedEffect(Unit) { enter.animateTo(1f, tween(600, easing = R7Ease)) }
        val inf = rememberInfiniteTransition(label = "off")
        val floatY by inf.animateFloat(
            initialValue = 0f, targetValue = -7f,
            animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
            label = "flt"
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val shape = RoundedCornerShape(28.dp)
            Column(
                Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = (1f - enter.value) * 70.dp.toPx()
                        alpha = enter.value
                        translationX = shakeX.value.dp.toPx()
                    }
                    .clip(shape)
                    .background(c.bg2)
                    .border(1.dp, c.bd, shape)
                    .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .graphicsLayer { translationY = with(d) { floatY.dp.toPx() } }
                        .size(148.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.no_internet),
                        contentDescription = null,
                        modifier = Modifier.size(116.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                R7Text(store.t("noNet"), 22f, R7Fonts.fredoka, c.tx, align = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                R7Text(store.t("noNetD"), 13f, R7Fonts.regular, c.mu, align = TextAlign.Center, lineHeight = 20f)
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .r7Click(store, 0.94f) { scope.launch { store.chkNet(true) } }
                        .clip(RoundedCornerShape(99.dp))
                        .background(ThumbGradient)
                        .padding(horizontal = 30.dp, vertical = 12.dp)
                ) { R7Text(store.t("retry"), 14f, R7Fonts.semi, c.on) }
            }
        }
    }
}

// ---------------------------------------------------------------- Privacy Policy / Terms

@Composable
fun LegalLayer(store: Store) {
    val which = store.legal ?: return
    val c = LocalR7.current
    BackHandler(enabled = true) { store.legal = null }
    val title = if (which == "privacy") store.t("priv") else store.t("terms")
    val body = if (which == "privacy") privacyText() else termsText()
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .r7Click(store, 0.9f) { store.legal = null },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.tx, modifier = Modifier.size(24.dp))
            }
            R7Text(title, 20f, R7Fonts.fredoka, c.p, Modifier.padding(start = 6.dp), maxLines = 1)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            body.forEach { (head, text) ->
                if (head.isNotEmpty()) R7Text(head, 15f, R7Fonts.semi, c.tx, Modifier.padding(top = 8.dp))
                R7Text(text, 13.5f, R7Fonts.regular, c.mu, lineHeight = 21f)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun privacyText(): List<Pair<String, String>> = listOf(
    "" to "Last updated: ${Config.LAST_UPDATED}",
    "About" to "R7 Play (\"the app\") is a free game hub published by ${Config.DEVELOPER}. This policy explains what information the app uses and how it is handled.",
    "Information we collect" to "R7 Play does not require an account and does not ask for your name, e-mail address, phone number, contacts, photos, microphone, camera or location. The app itself collects no personal data and has no ads or analytics of its own.",
    "Stored on your device" to "The app saves your settings (theme, language, sound, animations), your favorite games, your recently played games, play counts and the last downloaded games list on your device only. This data is not sent to us. You can remove it any time with the Clear options in Settings or by uninstalling the app.",
    "Internet use" to "The app downloads the list of games from a server hosted on GitHub and loads game pictures from image servers. Like any internet request, these services can see technical information such as your IP address and device type. We do not control these services; their own privacy policies apply.",
    "Games" to "The games are web games: they run on the web and open inside the app in an embedded web view from their https web address. Links that leave the game open in your phone's browser. Some games may show ads from third-party ad networks, which may use cookies or an advertising ID under their own privacy policies. A game may use cookies or local storage to save your progress, and games made by other developers may collect information under their own policies. Please do not enter personal information inside games.",
    "Children" to "R7 Play is a general audience app and is not directed at children under 13. We do not knowingly collect personal information from children.",
    "Security" to "Data is downloaded over the internet using standard connections. No method of electronic storage is 100% secure, but we do not store your personal data on our servers.",
    "Changes" to "We may update this policy from time to time. The latest version is always available inside the app under Settings > Privacy Policy.",
    "Contact" to "Questions about this policy: ${Config.SUPPORT_EMAIL}"
)

private fun termsText(): List<Pair<String, String>> = listOf(
    "" to "Last updated: ${Config.LAST_UPDATED}",
    "Acceptance" to "By using R7 Play you agree to these terms. If you do not agree, please do not use the app.",
    "Use of the app" to "R7 Play is provided free of charge for personal, non-commercial entertainment. You agree not to misuse the app, try to break or reverse engineer it, or use it for anything unlawful.",
    "Game content" to "Games and pictures shown in the app belong to their respective owners. All rights remain with the owners.",
    "Availability" to "Games depend on an internet connection and third-party servers. We may add, change or remove games or features at any time without notice and cannot promise that the app will always be available or error free.",
    "No warranty" to "The app is provided \"as is\" without warranties of any kind. To the maximum extent permitted by law, ${Config.DEVELOPER} is not liable for any loss or damage arising from use of the app.",
    "Changes" to "We may update these terms. Continued use of the app after a change means you accept the new terms.",
    "Contact" to "${Config.SUPPORT_EMAIL}"
)

/** "Clear recent games / favorites?" confirmation. Nothing is deleted until the user taps Clear. */
@Composable
fun ConfirmClearLayer(store: Store) {
    val c = LocalR7.current
    val which = store.confirmClear
    if (which.isEmpty()) return
    BackHandler(enabled = true) { store.confirmClear = "" }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .pointerInput(Unit) { detectTapGestures { store.confirmClear = "" } }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(c.bg2)
                .border(1.dp, c.bd, shape)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            R7Text(store.t(if (which == "rec") "cfmR" else "cfmF"), 18f, R7Fonts.semi, c.tx, align = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            R7Text(store.t("cfmD"), 13f, R7Fonts.regular, c.mu, align = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .r7Click(store, 0.94f) { store.confirmClear = "" }
                        .clip(RoundedCornerShape(99.dp))
                        .background(c.card)
                        .border(1.dp, c.bd, RoundedCornerShape(99.dp))
                        .padding(horizontal = 22.dp, vertical = 11.dp)
                ) { R7Text(store.t("cfmNo"), 14f, R7Fonts.semi, c.tx) }
                Box(
                    Modifier
                        .r7Click(store, 0.94f) {
                            val w = store.confirmClear
                            store.confirmClear = ""
                            if (w.isNotEmpty()) store.clearList(w)
                        }
                        .clip(RoundedCornerShape(99.dp))
                        .background(ThumbGradient)
                        .padding(horizontal = 22.dp, vertical = 11.dp)
                ) { R7Text(store.t("cfmYes"), 14f, R7Fonts.semi, c.on) }
            }
        }
    }
}
