package today.siteblock.core

/** A built-in bundle: one tap blocks the app(s) and their websites together. */
data class CatalogEntry(
    val id: String,
    val label: String,
    val packages: List<String>,
    val domains: List<String>,
)

object Catalog {
    val entries: List<CatalogEntry> = listOf(
        CatalogEntry("x", "X (Twitter)", listOf("com.twitter.android"), listOf("x.com", "twitter.com", "t.co", "twimg.com")),
        CatalogEntry("instagram", "Instagram", listOf("com.instagram.android"), listOf("instagram.com", "cdninstagram.com")),
        CatalogEntry("threads", "Threads", listOf("com.instagram.barcelona"), listOf("threads.net", "threads.com")),
        CatalogEntry("facebook", "Facebook", listOf("com.facebook.katana", "com.facebook.lite"), listOf("facebook.com", "fb.com", "fb.watch")),
        CatalogEntry("tiktok", "TikTok", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"), listOf("tiktok.com", "tiktokcdn.com", "tiktokv.com")),
        CatalogEntry("reddit", "Reddit", listOf("com.reddit.frontpage"), listOf("reddit.com", "redd.it", "redditmedia.com", "redditstatic.com")),
        CatalogEntry("youtube", "YouTube", listOf("com.google.android.youtube"), listOf("youtube.com", "youtu.be", "ytimg.com", "youtube-nocookie.com")),
        CatalogEntry("snapchat", "Snapchat", listOf("com.snapchat.android"), listOf("snapchat.com", "sc-cdn.net")),
    )

    fun byId(id: String): CatalogEntry? = entries.firstOrNull { it.id == id }
}
