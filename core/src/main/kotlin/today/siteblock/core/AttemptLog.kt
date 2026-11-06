package today.siteblock.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Today's blocked-open counts per package. [day] is an ISO date string ("2026-11-06"). */
@Serializable
data class AttemptLog(
    val day: String = "",
    val counts: Map<String, Int> = emptyMap(),
) {
    fun countFor(key: String, today: String): Int = if (day == today) counts[key] ?: 0 else 0

    fun recorded(key: String, today: String): AttemptLog =
        if (day != today) AttemptLog(today, mapOf(key to 1))
        else copy(counts = counts + (key to (countFor(key, today) + 1)))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(log: AttemptLog): String = json.encodeToString(serializer(), log)

        /** Never throws: stats are best-effort, so unreadable text yields an empty log. */
        fun decode(text: String): AttemptLog =
            runCatching { json.decodeFromString(serializer(), text) }.getOrDefault(AttemptLog())
    }
}

/** Drops repeat hits for the same key within [windowMillis] (one app open fires many events). Not thread-safe: use from one thread. */
class AttemptCoalescer(private val windowMillis: Long = 3_000L) {
    private val lastCounted = HashMap<String, Long>()

    fun shouldCount(key: String, now: Long): Boolean {
        val previous = lastCounted[key]
        if (previous != null && now - previous < windowMillis) return false
        lastCounted[key] = now
        return true
    }
}
