package com.r7studio.r7Playapp

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

const val GITHUB_URL = "https://raw.githubusercontent.com/ahaanmesaniya-bot/r7-database/main/games.json"
const val PLAY_LINK = "https://play.google.com/store/apps/details?id=com.r7studio.r7Playapp"
const val TRENDING_ICON = "https://res.cloudinary.com/so9phvfy/image/upload/v1790938146/1000052451.png"

val TAB_ORDER = listOf("home", "explore", "top", "recent", "settings")

val TX: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "home" to "Home", "explore" to "Explore", "top" to "Top", "recent" to "Recent",
        "settings" to "Settings", "fav" to "Favorites", "cont" to "Continue playing",
        "nw" to "New games", "trend" to "Trending now", "rec" to "Recommended", "all" to "All games",
        "search" to "Search games", "theme" to "Theme", "color" to "Colour", "lang" to "Language",
        "sound" to "Sound", "vib" to "Vibration", "anim" to "Animations",
        "clrR" to "Clear recent games", "clrF" to "Clear favorites", "share" to "Share R7 Play",
        "rate" to "Rate this app", "feed" to "Send feedback", "priv" to "Privacy Policy",
        "terms" to "Terms of Use", "about" to "About", "switch" to "Switch game", "play" to "Play now",
        "seeall" to "See all", "load" to "Loading...", "noNet" to "No Internet Connection",
        "noNetD" to "Check your Wi-Fi or mobile data and try again.", "retry" to "Try again",
        "back" to "Back online",
        "badUrl" to "This game link is not secure, so it can't be opened.",
        "errBad" to "Game list is not readable right now", "errEmpty" to "No games are available right now",
        "errBadD" to "Please try again in a little while.", "errEmptyD" to "New games will appear here soon.",
        "cfmR" to "Clear recent games?", "cfmF" to "Clear favorites?",
        "cfmD" to "This can't be undone.", "cfmYes" to "Clear", "cfmNo" to "Cancel",
        "cleared" to "Cleared",
        "noWebView" to "This phone's web engine (Android System WebView) is missing or disabled. Please enable or update it in Play Store.",
        "gameCrash" to "The game ran out of memory and closed. Close other apps and try again.",
        "oldWeb" to "Update \"Android System WebView\" in Play Store so games and ads work properly."
    ),
    "hi" to mapOf(
        "home" to "होम", "explore" to "खोजें", "top" to "टॉप", "recent" to "हाल के",
        "settings" to "सेटिंग्स", "fav" to "पसंदीदा", "cont" to "खेलना जारी रखें",
        "nw" to "नए गेम", "trend" to "ट्रेंडिंग", "rec" to "आपके लिए", "all" to "सभी गेम",
        "search" to "गेम खोजें", "theme" to "थीम", "color" to "रंग", "lang" to "भाषा",
        "sound" to "आवाज़", "vib" to "वाइब्रेशन", "anim" to "एनिमेशन",
        "clrR" to "हाल के गेम हटाएं", "clrF" to "पसंदीदा हटाएं", "share" to "R7 Play शेयर करें",
        "rate" to "ऐप को रेट करें", "feed" to "फीडबैक भेजें", "priv" to "प्राइवेसी पॉलिसी",
        "terms" to "नियम और शर्तें", "about" to "हमारे बारे में", "switch" to "गेम बदलें", "play" to "अभी खेलें",
        "seeall" to "सभी देखें", "load" to "लोड हो रहा है...", "noNet" to "इंटरनेट कनेक्शन नहीं है",
        "noNetD" to "अपना वाई-फाई या मोबाइल डेटा जांचें और फिर कोशिश करें।",
        "retry" to "फिर से कोशिश करें", "back" to "फिर से ऑनलाइन",
        "badUrl" to "यह गेम लिंक सुरक्षित नहीं है, इसलिए खुल नहीं सकता।",
        "errBad" to "गेम की सूची अभी पढ़ी नहीं जा सकती", "errEmpty" to "अभी कोई गेम उपलब्ध नहीं है",
        "errBadD" to "कृपया थोड़ी देर बाद फिर कोशिश करें।", "errEmptyD" to "नए गेम जल्द ही यहाँ दिखेंगे।",
        "cfmR" to "हाल के गेम हटाएं?", "cfmF" to "पसंदीदा हटाएं?",
        "cfmD" to "इसे वापस नहीं लाया जा सकता।", "cfmYes" to "हटाएं", "cfmNo" to "रद्द करें",
        "cleared" to "हटा दिया",
        "noWebView" to "इस फ़ोन का वेब इंजन (Android System WebView) बंद या गायब है। कृपया उसे चालू या अपडेट करें।",
        "gameCrash" to "मेमोरी कम पड़ने से गेम बंद हो गया। दूसरे ऐप बंद करके फिर कोशिश करें।",
        "oldWeb" to "गेम और विज्ञापन सही चलें, इसके लिए Play Store में \"Android System WebView\" अपडेट करें।"
    )
)

data class Game(
    val id: String,
    val title: String,
    val logo: String,
    val url: String,
    val type: String,
    val openInNewTab: Boolean,
    val featured: Boolean,
    val hero: String,
    val isNew: Boolean,
    val plays: Int
)

data class Cat(val name: String, val icon: String)

class Burst(val id: Int, val x: Float, val y: Float)

private fun JSONObject.str(vararg keys: String): String {
    for (k in keys) {
        val v = opt(k)
        if (v != null && v != JSONObject.NULL) {
            val s = v.toString()
            if (s.isNotEmpty() && s != "0" && s != "false") return s
        }
    }
    return ""
}

private fun JSONObject.truthy(k: String): Boolean {
    val v = opt(k) ?: return false
    return when (v) {
        is Boolean -> v
        is Number -> v.toDouble() != 0.0
        is String -> v.isNotEmpty() && v != "false" && v != "0"
        else -> false
    }
}

/** Only secure https links from the admin panel are accepted; anything else counts as "not set". */
private fun cleanLink(v: String): String {
    val t = v.trim()
    return if (t.startsWith("https://", ignoreCase = true) && t.length > 8) t else ""
}

private fun parseGames(arr: JSONArray): List<Game> {
    val out = ArrayList<Game>()
    for (n in 0 until arr.length()) {
        val o = arr.optJSONObject(n) ?: continue
        // Admin panel switch: a game that is switched OFF ("hidden": true or "live": false) is not shown
        if (o.truthy("hidden")) continue
        if (o.has("live") && !o.truthy("live")) continue
        val title = o.str("title").ifEmpty { "Untitled" }
        val id = o.str("id").ifEmpty { title }
        var plays = o.optDouble("plays", 0.0)
        if (plays.isNaN()) plays = 0.0
        out.add(
            Game(
                id = id,
                title = title,
                logo = o.str("logo", "image", "thumb"),
                url = o.str("url", "link"),
                type = o.str("type", "category").ifEmpty { "Game" },
                openInNewTab = o.truthy("openInNewTab"),
                featured = o.truthy("featured"),
                hero = o.str("hero", "banner"),
                isNew = o.truthy("new"),
                plays = plays.toInt()
            )
        )
    }
    // games packed inside the app run offline with their own icon / banner
    return OfflineGames.ensureAll(out.map { OfflineGames.localize(it) })
}

private suspend fun httpGet(url: String, timeoutMs: Int): String? = withContext(Dispatchers.IO) {
    try {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.useCaches = false
            conn.setRequestProperty("Cache-Control", "no-cache")
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}

private suspend fun httpPing(url: String, timeoutMs: Int): Boolean = withContext(Dispatchers.IO) {
    try {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "HEAD"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.useCaches = false
            conn.responseCode
            true
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        false
    }
}

/** All app state: settings, favorites, recent, games list, current screen. */
class Store(context: Context) {
    private val ctx: Context = context.applicationContext
    private val prefs = ctx.getSharedPreferences("r7play", Context.MODE_PRIVATE)

    // settings
    var theme by mutableStateOf("dark")
    var lang by mutableStateOf("en")
    var sound by mutableStateOf(true)
    var vib by mutableStateOf(true)
    var anim by mutableStateOf(true)
    val fav = mutableStateListOf<String>()
    val rec = mutableStateListOf<String>()
    val plays = mutableStateMapOf<String, Int>()

    // data
    var games by mutableStateOf(emptyList<Game>())
    var cats by mutableStateOf(emptyList<Cat>())
    var loaded by mutableStateOf(false)

    // ui
    var tab by mutableStateOf("home")
    var rseg by mutableStateOf("r")
    var cat by mutableStateOf("")
    var query by mutableStateOf("")
    var game by mutableStateOf<Game?>(null)
    var offline by mutableStateOf(false)
    /** "", "bad" (games.json unreadable) or "empty" (no games in it) */
    var dataError by mutableStateOf("")
    /** which list is waiting for a "clear?" confirmation: "", "rec" or "fav" */
    var confirmClear by mutableStateOf("")
    /** set when the open game failed to load because of the network; game reloads when net returns */
    var gameFail by mutableStateOf(false)
    var gameFailCount = 0
    var shake by mutableIntStateOf(0)
    var toastMsg by mutableStateOf<String?>(null)
    var toastTick by mutableIntStateOf(0)
    val bursts = mutableStateListOf<Burst>()

    // game player hooks
    var gameView: WebView? = null
    @Volatile var paused = false
    /** Links set from the admin panel (games.json: privacyUrl / termsUrl / aboutUrl). Empty = not set. */
    var privacyUrl by mutableStateOf("")
    var termsUrl by mutableStateOf("")
    var aboutUrl by mutableStateOf("")

    val seen = HashSet<String>()
    private var lastRaw = ""
    private var loading = false
    private var burstId = 0

    private var pool: SoundPool? = null
    private var clickId = 0
    private var poolReady = false

    init {
        try {
            val s = JSONObject(prefs.getString("r7s", "{}") ?: "{}")
            theme = s.optString("theme", "dark")
            lang = s.optString("lang", "en")
            sound = s.optBoolean("sound", true)
            vib = s.optBoolean("vib", true)
            anim = s.optBoolean("anim", true)
            s.optJSONArray("fav")?.let { a -> for (i in 0 until a.length()) fav.add(a.getString(i)) }
            s.optJSONArray("rec")?.let { a -> for (i in 0 until a.length()) rec.add(a.getString(i)) }
            s.optJSONObject("plays")?.let { p ->
                val it = p.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    plays[k] = p.optInt(k)
                }
            }
        } catch (e: Exception) {
        }
        val raw = prefs.getString("r7raw", null)
        if (raw != null && applyRaw(raw)) {
            lastRaw = raw
            loaded = true
        }
        initSound()
    }

    // ---------- translations ----------
    fun t(k: String): String = (TX[lang] ?: TX["en"]!!)[k] ?: k

    // ---------- saving ----------
    fun save() {
        try {
            val s = JSONObject()
            s.put("theme", theme)
            s.put("lang", lang)
            s.put("sound", sound)
            s.put("vib", vib)
            s.put("anim", anim)
            s.put("fav", JSONArray(fav.toList()))
            s.put("rec", JSONArray(rec.toList()))
            val p = JSONObject()
            for ((k, v) in plays) p.put(k, v)
            s.put("plays", p)
            prefs.edit().putString("r7s", s.toString()).apply()
        } catch (e: Exception) {
        }
    }

    fun gbPos(): Pair<Float, Float>? {
        if (!prefs.contains("gbx")) return null
        return Pair(prefs.getFloat("gbx", 0f), prefs.getFloat("gby", 0f))
    }

    fun saveGb(x: Float, y: Float) {
        prefs.edit().putFloat("gbx", x).putFloat("gby", y).apply()
    }

    // ---------- feedback: sound + vibration ----------
    private fun initSound() {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val p = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
            p.setOnLoadCompleteListener { _, _, status -> poolReady = status == 0 }
            clickId = p.load(ctx, R.raw.click, 1)
            pool = p
        } catch (e: Exception) {
        }
    }

    fun sfx() {
        if (!sound || !poolReady) return
        try {
            pool?.play(clickId, 0.7f, 0.7f, 1, 0, 1f)
        } catch (e: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    fun vibrate(ms: Long) {
        if (!vib) return
        try {
            val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                v.vibrate(ms)
            }
        } catch (e: Exception) {
        }
    }

    fun tick() {
        sfx()
        vibrate(10)
    }

    fun toast(m: String) {
        toastMsg = m
        toastTick++
    }

    // ---------- navigation ----------
    fun go(k: String) {
        if (k == "explore" && tab == "explore") cat = ""
        seen.removeAll { it.startsWith("$k:") }
        tab = k
    }

    fun playCount(g: Game): Int = g.plays + (plays[g.id] ?: 0)

    // ---------- actions ----------
    fun play(g: Game) {
        vibrate(25)
        rec.remove(g.id)
        rec.add(0, g.id)
        while (rec.size > 20) rec.removeAt(rec.size - 1)
        plays[g.id] = (plays[g.id] ?: 0) + 1
        save()
        if (g.url.isBlank()) return
        if (!g.url.trim().startsWith("https://", ignoreCase = true)) {
            toast(t("badUrl"))
            return
        }
        if (g.openInNewTab) {
            openExternal(g.url.trim())
            return
        }
        gameFail = false
        gameFailCount = 0
        game = g
    }

    /** Hosts a game page may navigate to inside the app: games.json host + hosts of all game links. */
    fun allowedHosts(): Set<String> {
        val out = HashSet<String>()
        try { URL(GITHUB_URL).host?.let { out.add(it.lowercase()) } } catch (e: Exception) { }
        for (g in games) {
            try {
                val u = URL(g.url.trim())
                if (u.protocol.equals("https", true)) u.host?.let { out.add(it.lowercase()) }
            } catch (e: Exception) { }
        }
        return out
    }

    /** the game page could not load: show the same No Internet dialog (game reloads when net is back) */
    fun gameNetFail() {
        if (gameFail) return
        gameFail = true
        offline = true
    }

    fun closeGame() {
        gameFail = false
        gameFailCount = 0
        game = null
        // back on the home screen the offline games are still playable, so no blocking dialog there
        if (games.isNotEmpty()) offline = false
    }

    /** returns true if the game was added to favorites */
    fun toggleFav(id: String): Boolean {
        vibrate(25)
        val added = !fav.contains(id)
        if (added) fav.add(id) else fav.remove(id)
        save()
        toast(if (added) "Added to Favorites" else "Removed from Favorites")
        return added
    }

    fun burst(x: Float, y: Float) {
        bursts.add(Burst(burstId++, x, y))
    }

    fun clearList(which: String) {
        if (which == "rec") rec.clear() else fav.clear()
        save()
        toast(t("cleared"))
    }

    /** Privacy Policy / Terms: if the admin set a link, open it in the browser, else show the built-in text screen. */
    fun openLegal(which: String) {
        val link = if (which == "privacy") privacyUrl else termsUrl
        openExternal(link.ifEmpty { if (which == "privacy") Config.PRIVACY_URL else Config.TERMS_URL })
    }

    fun openAbout() {
        openExternal(aboutUrl.ifEmpty { Config.ABOUT_URL })
    }

    fun openExternal(url: String) {
        openExternalUri(Uri.parse(url))
    }

    fun openExternalUri(uri: Uri) {
        try {
            val i = Intent(Intent.ACTION_VIEW, uri)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (e: Exception) {
        }
    }

    fun shareApp() {
        try {
            val send = Intent(Intent.ACTION_SEND)
            send.type = "text/plain"
            send.putExtra(Intent.EXTRA_SUBJECT, "R7 Play")
            send.putExtra(Intent.EXTRA_TEXT, "Play free games on R7 Play\n$PLAY_LINK")
            val chooser = Intent.createChooser(send, "R7 Play")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(chooser)
        } catch (e: Exception) {
        }
    }

    fun rateApp() {
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.r7studio.r7Playapp"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (e: Exception) {
            openExternal(PLAY_LINK)
        }
    }

    fun sendFeedback() {
        openExternalUri(Uri.parse("mailto:" + Config.SUPPORT_EMAIL + "?subject=R7%20Play%20feedback"))
    }

    // ---------- games.json ----------
    private fun applyRaw(raw: String): Boolean {
        return try {
            val text = raw.trim()
            val arr: JSONArray
            var catArr: JSONArray? = null
            var pUrl = ""
            var tUrl = ""
            var aUrl = ""
            if (text.startsWith("[")) {
                arr = JSONArray(text)
            } else {
                val o = JSONObject(text)
                arr = o.optJSONArray("items") ?: JSONArray()
                catArr = o.optJSONArray("categories")
                pUrl = cleanLink(o.optString("privacyUrl", ""))
                tUrl = cleanLink(o.optString("termsUrl", ""))
                aUrl = cleanLink(o.optString("aboutUrl", ""))
            }
            privacyUrl = pUrl
            termsUrl = tUrl
            aboutUrl = aUrl
            val list = parseGames(arr)
            if (list.isEmpty()) {
                // valid file but every game is switched off / none exist: show the "no games" message
                games = emptyList()
                cats = emptyList()
                dataError = "empty"
                return true
            }
            val cl = ArrayList<Cat>()
            if (catArr != null) {
                for (i in 0 until catArr.length()) {
                    val c = catArr.optJSONObject(i) ?: continue
                    val name = c.str("name")
                    if (name.isNotEmpty()) cl.add(Cat(name, c.str("icon")))
                }
            }
            games = list
            cats = cl
            dataError = ""
            true
        } catch (e: Exception) {
            if (games.isEmpty()) dataError = "bad"
            false
        }
    }

    fun isOnline(): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(n) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    private suspend fun netOK(): Boolean {
        if (!isOnline()) return false
        return httpPing(GITHUB_URL + "?t=" + System.currentTimeMillis(), 6000)
    }

    suspend fun load() {
        if (loading) return
        loading = true
        try {
            // No internet and nothing saved yet (very first start): show the offline games built into
            // the app AT ONCE, no waiting for a network timeout. They are replaced by the real list
            // as soon as the internet is back (sync() does that).
            val online = isOnline()
            if (!online && games.isEmpty()) {
                games = OfflineGames.builtinGames()
                dataError = ""
                loaded = true
            }
            val raw = if (online) {
                httpGet(GITHUB_URL + "?t=" + System.currentTimeMillis(), if (games.isEmpty()) 2500 else 8000)
            } else null
            if (raw == null) {
                dataError = ""
                // first start without internet and no saved list: show the offline games built into the app
                if (games.isEmpty()) games = OfflineGames.builtinGames()
            }
            if (raw != null && applyRaw(raw)) {
                lastRaw = raw
                prefs.edit().putString("r7raw", raw).apply()
            } else {
                chkNet(false)
            }
        } finally {
            loading = false
            loaded = true
        }
    }

    suspend fun reload() {
        loaded = false
        load()
    }

    /** silent refresh while the app is open (every 3 sec, and instantly when the app is opened again) */
    suspend fun sync() {
        if (games.isEmpty()) {
            load()
            return
        }
        val raw = httpGet(GITHUB_URL + "?t=" + System.currentTimeMillis(), 8000) ?: return
        if (raw == lastRaw) return
        if (applyRaw(raw)) {
            lastRaw = raw
            prefs.edit().putString("r7raw", raw).apply()
        }
    }

    suspend fun chkNet(manual: Boolean) {
        val ok = netOK()
        if (ok) {
            if (offline) {
                offline = false
                toast(t("back"))
            }
            if (games.isEmpty() && !loading) {
                loaded = false
                load()
            }
        } else {
            // the No Internet dialog is only for online games (or when there is nothing to show at all);
            // games packed inside the app work without internet
            val g = game
            val needsNet = games.isEmpty() || (g != null && !OfflineGames.isLocalUrl(g.url))
            if (needsNet || offline) {
                offline = true
                if (manual) shake++
            }
        }
    }
}
