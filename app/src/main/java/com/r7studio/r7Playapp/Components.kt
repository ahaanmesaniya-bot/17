package com.r7studio.r7Playapp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.min

/** Text with the app fonts (Poppins body, Fredoka headings). */
@Composable
fun R7Text(
    text: String,
    size: Float,
    font: R7Font = R7Fonts.regular,
    color: Color = LocalR7.current.tx,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    lineHeight: Float = 0f
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontFamily = font.family,
        fontWeight = font.weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        lineHeight = if (lineHeight > 0f) lineHeight.sp else TextUnit.Unspecified
    )
}

/** Tap with the press-scale feel + click sound + light vibration (like the HTML app). */
@Composable
fun Modifier.r7Click(store: Store, scale: Float = 0.94f, onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed && store.anim) scale else 1f,
        animationSpec = tween(220, easing = R7Ease),
        label = "press"
    )
    return this
        .graphicsLayer {
            scaleX = s
            scaleY = s
        }
        .clickable(interactionSource = src, indication = null) {
            store.tick()
            onClick()
        }
}

/** Slide-in + fade-in entry animation, played once per screen visit (staggered by index). */
@Composable
fun Appear(
    store: Store,
    key: String,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val fresh = remember(key) { store.seen.add(key) && store.anim }
    val a = remember(key) { Animatable(if (fresh) 0f else 1f) }
    LaunchedEffect(key) {
        if (a.value < 1f) {
            a.animateTo(1f, tween(650, delayMillis = min(index, 14) * 55, easing = R7Ease))
        }
    }
    val px = with(LocalDensity.current) { 36.dp.toPx() }
    Box(
        modifier.graphicsLayer {
            alpha = a.value
            translationX = (1f - a.value) * px
        }
    ) { content() }
}

/** Shimmer placeholder while the games list is loading. */
@Composable
fun Modifier.shimmer(): Modifier {
    val c = LocalR7.current
    val t = rememberInfiniteTransition(label = "sk")
    val x by t.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "skx"
    )
    val mid = lerp(c.card, c.p, 0.22f)
    return this.drawBehind {
        val w = size.width
        drawRect(
            Brush.horizontalGradient(
                colors = listOf(c.card, mid, c.card),
                startX = x * w - w,
                endX = x * w + w
            )
        )
    }
}

val ThumbGradient = Brush.linearGradient(listOf(Color(0xFF03A9F4), Color(0xFF29B9FA)))

@Composable
fun Thumb(g: Game, modifier: Modifier = Modifier, radius: Dp = 18.dp, letterSize: Float = 34f) {
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(ThumbGradient),
        contentAlignment = Alignment.Center
    ) {
        R7Text(g.title.take(1), letterSize, R7Fonts.fredoka, Color(0xFF02202F))
        if (g.logo.isNotEmpty()) {
            R7Image(url = g.logo, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun HeartBtn(store: Store, id: String, modifier: Modifier = Modifier) {
    val c = LocalR7.current
    val f = store.fav.contains(id)
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val sc = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(f) {
        if (first) {
            first = false
        } else if (store.anim) {
            sc.animateTo(1.4f, tween(150))
            sc.animateTo(1f, tween(150))
        }
    }
    Box(
        modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0x8C04101A))
            .onGloballyPositioned { coords = it }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                val added = store.toggleFav(id)
                val cc = coords
                if (added && store.anim && cc != null) {
                    val p = cc.positionInRoot()
                    store.burst(p.x + cc.size.width / 2f, p.y + cc.size.height / 2f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (f) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = "Favorite",
            tint = if (f) c.hi else Color.White,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    scaleX = sc.value
                    scaleY = sc.value
                }
        )
    }
}

@Composable
fun GameCard(store: Store, g: Game, index: Int, prefix: String, modifier: Modifier = Modifier) {
    val c = LocalR7.current
    Appear(store, "$prefix:${g.id}", index, modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .r7Click(store, 0.94f) { store.play(g) }
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                Thumb(g, Modifier.fillMaxSize())
                if (g.isNew) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(c.p)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) { R7Text("NEW", 10f, R7Fonts.semi, c.on) }
                }
                HeartBtn(store, g.id, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            R7Text(g.title, 12.5f, R7Fonts.semi, c.tx, Modifier.padding(top = 7.dp), maxLines = 1)
            R7Text(g.type, 11f, R7Fonts.regular, c.mu, maxLines = 1)
        }
    }
}

@Composable
fun SectionTitle(text: String, topPad: Dp = 22.dp) {
    R7Text(
        text, 17f, R7Fonts.semi, LocalR7.current.tx,
        Modifier.padding(start = 2.dp, top = topPad, end = 2.dp, bottom = 10.dp)
    )
}

@Composable
fun PlayPill(label: String?, modifier: Modifier = Modifier) {
    val c = LocalR7.current
    Row(
        modifier
            .clip(RoundedCornerShape(99.dp))
            .background(c.p)
            .padding(horizontal = if (label != null) 16.dp else 12.dp, vertical = if (label != null) 9.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = c.on,
            modifier = Modifier.size(14.dp)
        )
        if (label != null) R7Text(label, 13f, R7Fonts.semi, c.on, maxLines = 1)
    }
}

@Composable
fun Seg(store: Store, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val c = LocalR7.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .clip(shape)
            .background(c.bg)
            .border(1.dp, c.bd, shape)
            .padding(3.dp)
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            val bg by animateColorAsState(if (on) c.p else Color.Transparent, tween(350), label = "seg")
            Box(
                Modifier
                    .r7Click(store, 0.94f) { onSelect(value) }
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                R7Text(label, 12.5f, R7Fonts.regular, if (on) c.on else c.mu, maxLines = 1)
            }
        }
    }
}

@Composable
fun R7Switch(store: Store, on: Boolean, onToggle: () -> Unit) {
    val c = LocalR7.current
    val knobX by animateDpAsState(if (on) 23.dp else 3.dp, tween(300, easing = R7Ease), label = "knob")
    val bg by animateColorAsState(if (on) c.p else c.bd, tween(200), label = "swbg")
    Box(
        Modifier
            .r7Click(store, 1f) { onToggle() }
            .size(46.dp, 26.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
    ) {
        Box(
            Modifier
                .offset(x = knobX, y = 3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (on) c.on else Color.White)
        )
    }
}

@Composable
fun SettingRow(
    store: Store,
    index: Int,
    icon: ImageVector?,
    label: String,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = LocalR7.current
    val shape = RoundedCornerShape(18.dp)
    Appear(store, "settings:$index", index, Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        val base = if (onClick != null) Modifier.fillMaxWidth().r7Click(store, 0.94f) { onClick() } else Modifier.fillMaxWidth()
        Row(
            base
                .clip(shape)
                .background(c.card)
                .border(1.dp, c.bd, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = c.p, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                }
                Column {
                    R7Text(label, 14f, R7Fonts.regular, c.tx)
                    if (sub != null) R7Text(sub, 11.5f, R7Fonts.regular, c.mu)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
    }
}
