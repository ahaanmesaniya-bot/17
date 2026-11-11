package com.r7studio.r7Playapp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.composed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- helpers

@Composable
fun ScreenBox(content: @Composable (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val cols = if (maxWidth >= 1000.dp) 7 else if (maxWidth >= 700.dp) 5 else 3
        content(cols)
    }
}

private val PagePad = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 24.dp)

private fun Modifier.pageWidth(): Modifier = this.widthIn(max = 1100.dp).fillMaxWidth().fillMaxHeight()

fun LazyListScope.gameGrid(store: Store, list: List<Game>, cols: Int, prefix: String) {
    val rows = list.chunked(cols)
    itemsIndexed(rows) { r, row ->
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            row.forEachIndexed { i, g ->
                GameCard(store, g, r * cols + i, prefix, Modifier.weight(1f))
            }
            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun EmptyBox(title: String, text: String, button: String? = null, onButton: (() -> Unit)? = null) {
    val c = LocalR7.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        R7Text(title, 16f, R7Fonts.semi, c.tx, align = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        R7Text(text, 14f, R7Fonts.regular, c.mu, align = TextAlign.Center)
        if (button != null && onButton != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(c.p)
                    .clickableNoRipple(onButton)
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) { R7Text(button, 14f, R7Fonts.semi, c.on) }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

// ---------------------------------------------------------------- header + nav

@Composable
fun Header(store: Store) {
    val c = LocalR7.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(c.bg.copy(alpha = 0.82f))
            .drawLineBottom(c.bd)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.app_logo),
            contentDescription = "R7 Play",
            modifier = Modifier.size(44.dp)
        )
        R7Text("R7 Play", 22f, R7Fonts.fredoka, c.p, maxLines = 1)
    }
}

private fun Modifier.drawLineBottom(color: Color): Modifier = this.drawBehind {
    drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1.dp.toPx())
}

private fun Modifier.drawLineTop(color: Color): Modifier = this.drawBehind {
    drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx())
}

private fun navIcon(k: String): ImageVector = when (k) {
    "home" -> Icons.Filled.Home
    "explore" -> Icons.Filled.Explore
    "top" -> Icons.Filled.LocalFireDepartment
    "recent" -> Icons.Filled.History
    else -> Icons.Filled.Settings
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BottomNav(store: Store) {
    val c = LocalR7.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.bg2.copy(alpha = 0.92f))
            .drawLineTop(c.bd)
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
    ) {
        TAB_ORDER.forEach { k ->
            val on = store.tab == k
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (on) 1.12f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                ),
                label = "navscale"
            )
            val barW by animateDpAsState(if (on) 28.dp else 0.dp, tween(400, easing = R7Ease), label = "navbar")
            Box(
                Modifier
                    .weight(1f)
                    .height(62.dp)
                    .r7Click(store, 0.9f) { store.go(k) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .width(barW)
                        .height(3.dp)
                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(c.navOn)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        navIcon(k), null,
                        tint = if (on) c.navOn else c.mu,
                        modifier = Modifier.size(22.dp).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                    Spacer(Modifier.height(3.dp))
                    R7Text(store.t(k), 11f, if (on) R7Fonts.semi else R7Fonts.regular, if (on) c.navOn else c.mu, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- skeleton / empty

@Composable
fun SkeletonHome(cols: Int) {
    LazyColumn(Modifier.pageWidth(), contentPadding = PagePad, userScrollEnabled = false) {
        item { Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(22.dp)).shimmer()) }
        item {
            Box(Modifier.padding(top = 22.dp, bottom = 10.dp).width(130.dp).height(16.dp).clip(RoundedCornerShape(6.dp)).shimmer())
        }
        items(2) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(cols) {
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().aspectRatioSquare().clip(RoundedCornerShape(16.dp)).shimmer())
                        Box(Modifier.padding(top = 9.dp).fillMaxWidth(0.75f).height(10.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                        Box(Modifier.padding(top = 6.dp).fillMaxWidth(0.45f).height(8.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                    }
                }
            }
        }
    }
}

private fun Modifier.aspectRatioSquare(): Modifier = this.aspectRatio(1f)

@Composable
fun LoadFailed(store: Store) {
    val scope = rememberCoroutineScope()
    val e = store.dataError
    EmptyBox(
        if (e == "bad") store.t("errBad") else if (e == "empty") store.t("errEmpty") else "Can't load games",
        if (e == "bad") store.t("errBadD") else if (e == "empty") store.t("errEmptyD") else "Check your internet connection and try again.",
        store.t("retry")
    ) { scope.launch { store.reload() } }
}

// ---------------------------------------------------------------- HOME

@Composable
fun HomeScreen(store: Store) {
    ScreenBox { cols ->
        val g = store.games
        if (g.isEmpty()) {
            if (!store.loaded) SkeletonHome(cols) else LoadFailed(store)
        } else {
            val hero = remember(g) {
                var f = g.filter { it.featured && (it.hero.isNotEmpty() || it.logo.isNotEmpty()) }
                if (f.isEmpty()) f = g.filter { it.logo.isNotEmpty() }.take(5)
                if (f.isEmpty()) f = g.take(5)
                f
            }
            val fresh = remember(g) { g.filter { it.isNew } }
            LazyColumn(Modifier.pageWidth(), contentPadding = PagePad) {
                item { Hero(store, hero) }
                item { BannerSlot(store) }
                if (fresh.isNotEmpty()) {
                    item { SectionTitle(store.t("nw")) }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 6.dp)
                        ) {
                            itemsIndexed(fresh) { i, game ->
                                GameCard(store, game, i, "home:new", Modifier.width(112.dp))
                            }
                        }
                    }
                }
                item { SectionTitle(store.t("all")) }
                gameGrid(store, g, cols, "home:all")
            }
        }
    }
}

@Composable
fun Hero(store: Store, list: List<Game>) {
    val c = LocalR7.current
    val shape = RoundedCornerShape(22.dp)
    val pager = rememberPagerState(pageCount = { list.size })

    LaunchedEffect(list.size) {
        if (list.size > 1) {
            while (true) {
                delay(4500)
                if (store.game == null) {
                    pager.animateScrollToPage((pager.currentPage + 1) % list.size, animationSpec = tween(800, easing = R7Ease))
                }
            }
        }
    }

    val inf = rememberInfiniteTransition(label = "kb")
    val kb by inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "kbs"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(shape)
            .background(c.card)
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val g = list[page]
            val img = if (g.hero.isNotEmpty()) g.hero else g.logo
            Box(
                Modifier
                    .fillMaxSize()
                    .background(c.p)
                    .r7Click(store, 1f) { store.play(g) }
            ) {
                if (img.isNotEmpty()) {
                    R7Image(
                        url = img,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val s = if (store.anim) kb else 1f
                            scaleX = s
                            scaleY = s
                        }
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.30f to Color.Transparent,
                            1f to Color(0xE6030E18)
                        )
                    )
                )
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        R7Text(g.title, 22f, R7Fonts.fredoka, Color.White, maxLines = 2, lineHeight = 24f)
                        R7Text(g.type, 11f, R7Fonts.regular, Color.White.copy(alpha = 0.8f), maxLines = 1)
                    }
                    PlayPill(store.t("play"))
                }
            }
        }
        if (list.size > 1) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                list.indices.forEach { i ->
                    val on = pager.currentPage == i
                    val w by animateDpAsState(if (on) 18.dp else 6.dp, tween(300), label = "dot")
                    Box(
                        Modifier
                            .width(w)
                            .height(6.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (on) c.p else Color.White.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- EXPLORE

private fun catIcon(name: String): ImageVector = when (name) {
    "All" -> Icons.Filled.Layers
    "Trending" -> Icons.Filled.LocalFireDepartment
    "App" -> Icons.Filled.PhoneAndroid
    "Action" -> Icons.Filled.Bolt
    "Puzzle" -> Icons.Filled.Extension
    "Racing" -> Icons.Filled.DirectionsCar
    "Fighting" -> Icons.Filled.SportsMma
    "Arcade" -> Icons.Filled.Pets
    "Adventure" -> Icons.Filled.Terrain
    "Shooting" -> Icons.Filled.GpsFixed
    "Brain" -> Icons.Filled.Psychology
    "Kids" -> Icons.Filled.ChildCare
    "Girls" -> Icons.Filled.Face
    "Sports" -> Icons.Filled.SportsSoccer
    "Simulation" -> Icons.Filled.ShoppingCart
    else -> Icons.Filled.SportsEsports
}

@Composable
fun SearchBox(store: Store) {
    val c = LocalR7.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
            .border(if (focused) 2.dp else 1.dp, if (focused) c.p else c.bd, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = c.mu, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (store.query.isEmpty()) R7Text(store.t("search"), 14f, R7Fonts.regular, c.mu)
                BasicTextField(
                    value = store.query,
                    onValueChange = { store.query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.tx, fontSize = 14.sp, fontFamily = R7Fonts.regular.family),
                    cursorBrush = SolidColor(c.p),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                )
            }
        }
    }
}

@Composable
fun ExploreScreen(store: Store) {
    val c = LocalR7.current
    val hi = store.lang == "hi"
    ScreenBox { cols ->
        val q = store.query.trim().lowercase()
        val g = store.games
        val state = rememberLazyListState()
        LaunchedEffect(store.cat) { state.scrollToItem(0) }

        fun label(n: String): String = when (n) {
            "All" -> if (hi) "सभी गेम" else "All games"
            "Trending" -> if (hi) "ट्रेंडिंग" else "Trending"
            else -> n
        }

        LazyColumn(Modifier.pageWidth(), state = state, contentPadding = PagePad) {
            item { SearchBox(store) }
            if (store.cat.isEmpty() && q.isEmpty()) {
                val names = store.cats.map { it.name }
                val extra = g.map { it.type }.distinct().filter { !names.contains(it) }
                val all = listOf("Trending") + names + extra
                item { Spacer(Modifier.height(12.dp)) }
                val rows = all.chunked(cols)
                itemsIndexed(rows) { r, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEachIndexed { i, name ->
                            val iconUrl = if (name == "Trending") TRENDING_ICON else store.cats.firstOrNull { it.name == name }?.icon ?: ""
                            val count = if (name == "All") g.size else g.count { it.type == name }
                            val sub = if (name == "Trending") (if (hi) "सबसे ज़्यादा खेले" else "Most played") else "$count " + (if (hi) "गेम" else "games")
                            Appear(store, "explore:cat:$name", r * cols + i, Modifier.weight(1f)) {
                                Column(
                                    Modifier.fillMaxWidth().r7Click(store, 0.94f) { store.cat = name }
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(c.card)
                                            .border(1.dp, c.bd, RoundedCornerShape(18.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(catIcon(name), null, tint = c.p, modifier = Modifier.size(46.dp))
                                        if (iconUrl.isNotEmpty()) {
                                            R7Image(url = iconUrl, modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                    R7Text(label(name), 12.5f, R7Fonts.semi, c.tx, Modifier.padding(top = 7.dp), maxLines = 1)
                                    R7Text(sub, 11f, R7Fonts.regular, c.mu, maxLines = 1)
                                }
                            }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            } else {
                if (store.cat.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                Modifier
                                    .r7Click(store, 0.94f) { store.cat = "" }
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(c.card)
                                    .border(1.dp, c.bd, RoundedCornerShape(99.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = c.mu, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                R7Text(if (hi) "वापस" else "Back", 13f, R7Fonts.medium, c.mu)
                            }
                            R7Text(label(store.cat), 17f, R7Fonts.fredoka, c.tx, Modifier.padding(start = 6.dp), maxLines = 1)
                        }
                    }
                }
                val cat = store.cat
                var list = g.filter {
                    (cat.isEmpty() || cat == "All" || cat == "Trending" || it.type == cat) &&
                        it.title.lowercase().contains(q)
                }
                if (cat == "Trending") list = list.sortedByDescending { store.playCount(it) }
                item { Spacer(Modifier.height(10.dp)) }
                if (list.isEmpty()) {
                    item { EmptyBox("No games found", "Try another word or category.") }
                } else {
                    gameGrid(store, list, cols, "explore:g")
                }
            }
        }
    }
}

// ---------------------------------------------------------------- TOP

@Composable
fun TopScreen(store: Store) {
    val c = LocalR7.current
    ScreenBox { _ ->
        val top = remember(store.games, store.plays.toMap()) {
            store.games.sortedByDescending { store.playCount(it) }.take(20)
        }
        LazyColumn(Modifier.pageWidth(), contentPadding = PagePad) {
            item { SectionTitle(store.t("top"), 6.dp) }
            itemsIndexed(top) { i, g ->
                val shape = RoundedCornerShape(18.dp)
                Appear(store, "top:${g.id}", i, Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .r7Click(store, 0.94f) { store.play(g) }
                            .clip(shape)
                            .background(c.card)
                            .border(1.dp, c.bd, shape)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        R7Text(
                            "${i + 1}", 22f, R7Fonts.fredoka,
                            if (i < 3) c.navOn else c.mu,
                            Modifier.width(30.dp), align = TextAlign.Center
                        )
                        Spacer(Modifier.width(12.dp))
                        Thumb(g, Modifier.size(56.dp), radius = 12.dp, letterSize = 20f)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            R7Text(g.title, 12.5f, R7Fonts.semi, c.tx, maxLines = 1)
                            R7Text(g.type + " · " + store.playCount(g) + " plays", 11f, R7Fonts.regular, c.mu, maxLines = 1)
                        }
                        Spacer(Modifier.width(8.dp))
                        PlayPill(null)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- RECENT / FAVORITES

@Composable
fun RecentScreen(store: Store) {
    ScreenBox { cols ->
        val ids = if (store.rseg == "r") store.rec.toList() else store.fav.toList()
        val list = ids.mapNotNull { id -> store.games.firstOrNull { it.id == id } }
        LazyColumn(Modifier.pageWidth(), contentPadding = PagePad) {
            item {
                Box(Modifier.padding(bottom = 14.dp)) {
                    Seg(
                        store,
                        listOf("r" to store.t("recent"), "f" to store.t("fav")),
                        store.rseg
                    ) { store.rseg = it }
                }
            }
            if (list.isEmpty()) {
                item {
                    EmptyBox(
                        if (store.rseg == "r") "No games played yet" else "No favorites yet",
                        if (store.rseg == "r") "Games you play will show up here." else "Tap the heart on a game to save it here.",
                        store.t("explore")
                    ) { store.go("explore") }
                }
            } else {
                gameGrid(store, list, cols, "recent:" + store.rseg)
            }
        }
    }
}

// ---------------------------------------------------------------- SETTINGS

@Composable
fun SettingsScreen(store: Store) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
    ScreenBox { _ ->
        LazyColumn(Modifier.pageWidth(), contentPadding = PagePad) {
            item { SectionTitle(store.t("settings"), 6.dp) }
            item {
                SettingRow(store, 0, Icons.Filled.BrightnessMedium, store.t("theme"), trailing = {
                    Seg(store, listOf("light" to "Light", "dark" to "Dark", "auto" to "Auto"), store.theme) {
                        store.theme = it
                        store.save()
                    }
                })
            }
            item {
                SettingRow(store, 1, Icons.Filled.Language, store.t("lang"), trailing = {
                    Seg(store, listOf("en" to "English", "hi" to "हिन्दी"), store.lang) {
                        store.lang = it
                        store.save()
                    }
                })
            }
            item {
                SettingRow(store, 2, Icons.Filled.VolumeUp, store.t("sound"), trailing = {
                    R7Switch(store, store.sound) {
                        store.sound = !store.sound
                        store.save()
                        if (store.sound) store.sfx()
                    }
                })
            }
            item {
                SettingRow(store, 3, Icons.Filled.AutoAwesome, store.t("anim"), trailing = {
                    R7Switch(store, store.anim) {
                        store.anim = !store.anim
                        store.save()
                    }
                })
            }
            item { SettingRow(store, 4, Icons.Filled.History, store.t("clrR"), onClick = { store.confirmClear = "rec" }) }
            item { SettingRow(store, 5, Icons.Filled.FavoriteBorder, store.t("clrF"), onClick = { store.confirmClear = "fav" }) }
            item { SettingRow(store, 6, Icons.Filled.Share, store.t("share"), onClick = { store.shareApp() }) }
            item { SettingRow(store, 7, Icons.Filled.Star, store.t("rate"), onClick = { store.rateApp() }) }
            item { SettingRow(store, 8, Icons.Filled.Email, store.t("feed"), onClick = { store.sendFeedback() }) }
            item { SettingRow(store, 9, Icons.Filled.Shield, store.t("priv"), onClick = { store.openLegal("privacy") }) }
            item { SettingRow(store, 10, Icons.Filled.Description, store.t("terms"), onClick = { store.openLegal("terms") }) }
            item { SettingRow(store, 11, Icons.Filled.Info, store.t("about"), onClick = { store.openAbout() }) }
            item { SettingRow(store, 12, Icons.Filled.Info, "R7 Studio", sub = "R7 Play v$version") }
        }
    }
}
