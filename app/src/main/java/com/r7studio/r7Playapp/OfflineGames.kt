package com.r7studio.r7Playapp

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Games that are packed INSIDE the app (assets/games) and run with no internet.
 * Everything else in games.json stays an online game (it needs internet and shows the No Internet dialog).
 *
 * A game from games.json becomes offline when its title / id / link contains one of the keys below.
 * The game page is served to the WebView from the APK itself (no file access, no network).
 */
object OfflineGames {
    const val HOST = "appassets.androidplatform.net"
    private const val BASE = "https://$HOST/games/"
    private const val ASSETS = "file:///android_asset/games/"

    class Local(
        val title: String,
        val type: String,
        val page: String,
        val keys: List<String>
    ) {
        val icon get() = ASSETS + page.removeSuffix(".html") + "_icon.webp"
        val banner get() = ASSETS + page.removeSuffix(".html") + "_banner.webp"
    }

    val all = listOf(
        Local("Block Puzzle", "Puzzle", "block_puzzle.html", listOf("blockpuzzle")),
        Local("Hill Adventure", "Racing", "hill_adventure.html", listOf("hilladventure")),
        Local("Nut Sort", "Puzzle", "nut_sort.html", listOf("nutsort", "nutbolt", "nutsandbolts", "nutandbolt", "boltnut", "boltsort")),
        Local("Math Master", "Brain", "math_master.html", listOf("mathmaster", "mathgame")),
        Local("Tap Away Arrows", "Puzzle", "tap_away_arrows.html", listOf("tapaway", "taparrow", "arrowpuzzle", "aeropuzzle", "arrowaway", "arrowsaway", "arrowescape", "tapaero"))
    )

    private fun norm(s: String) = s.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }

    fun find(title: String, id: String, url: String): Local? {
        val hay = listOf(norm(title), norm(id), norm(url.trim().substringAfterLast('/')))
        return all.firstOrNull { l -> l.keys.any { k -> hay.any { it.contains(k) } } }
    }

    /** Same game, but running from the APK with its own bundled icon + banner. */
    fun localize(g: Game): Game {
        val l = find(g.title, g.id, g.url) ?: return g
        return g.copy(url = BASE + l.page, logo = l.icon, hero = l.banner, openInNewTab = false)
    }

    fun isLocalUrl(url: String): Boolean = url.trim().startsWith(BASE, ignoreCase = true)

    /** Used only when the phone has no internet AND no saved games list yet (first start offline). */
    fun builtinGames(): List<Game> = all.map {
        Game(
            id = "builtin-" + it.page.removeSuffix(".html"),
            title = it.title,
            logo = it.icon,
            url = BASE + it.page,
            type = it.type,
            openInNewTab = false,
            featured = false,
            hero = it.banner,
            isNew = false,
            plays = 0
        )
    }

    /**
     * The offline games live inside the app, so they always stay in the list, even when the admin panel
     * deletes / hides them. If the admin still lists one, the admin's entry is used (featured, new, order ...).
     */
    fun ensureAll(list: List<Game>): List<Game> {
        val out = ArrayList(list)
        for (b in builtinGames()) {
            if (out.none { it.url.trim().equals(b.url, ignoreCase = true) }) out.add(b)
        }
        return out
    }

    /** Called from the WebView: gives the bundled game page for https://appassets.androidplatform.net/games/... */
    fun serve(ctx: Context, u: Uri?): WebResourceResponse? {
        if (u == null) return null
        if (!u.scheme.equals("https", true) || !u.host.equals(HOST, true)) return null
        val p = u.path ?: return null
        // anything else on our own fake host (favicon, fonts ...) must never go to the real network
        if (!p.startsWith("/games/")) return notFound()
        val name = p.removePrefix("/games/")
        // only the known game pages can be opened (no other file of the APK)
        if (all.none { it.page == name }) return notFound()
        return try {
            WebResourceResponse(
                "text/html", "utf-8", 200, "OK",
                mapOf("Cache-Control" to "no-cache"),
                ctx.assets.open("games/$name")
            )
        } catch (e: Exception) {
            notFound()
        }
    }

    private fun notFound() = WebResourceResponse(
        "text/plain", "utf-8", 404, "Not Found", emptyMap(), ByteArrayInputStream(ByteArray(0))
    )
}
