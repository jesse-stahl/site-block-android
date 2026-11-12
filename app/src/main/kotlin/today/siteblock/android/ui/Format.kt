package today.siteblock.android.ui

import java.util.Locale
import today.siteblock.core.BlockItem
import today.siteblock.core.BlockList
import today.siteblock.core.PauseDuration
import today.siteblock.core.PendingWeaken

object Format {
    /** 65_000 -> "1:05" (rounds up so the countdown never shows 0:00 early). */
    fun mmss(ms: Long): String {
        val seconds = (ms + 999) / 1000
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
    }

    /** 5_400_000 -> "1h 30m"; under an hour -> "12m"; under a minute -> "1m". */
    fun duration(ms: Long): String {
        val minutes = maxOf(1L, (ms + 59_999) / 60_000)
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    }

    fun describe(pending: PendingWeaken, list: BlockList): String = when (pending) {
        PendingWeaken.TurnOff -> "Turn blocking off"
        is PendingWeaken.Remove -> "Remove ${list.item(pending.itemId)?.label ?: "this item"} from your block list"
        is PendingWeaken.PauseFilter ->
            if (pending.duration == PauseDuration.UNTIL_RESUMED) "Pause the web filter until you turn it back on"
            else "Pause the web filter for ${pending.duration.label}"
        is PendingWeaken.Whitelist -> "Let ${pending.label} skip the web filter"
    }

    fun summary(item: BlockItem): String {
        val parts = mutableListOf<String>()
        if (item.packages.isNotEmpty()) parts += "App: " + item.packages.joinToString(", ")
        if (item.domains.isNotEmpty()) parts += "Sites: " + item.domains.joinToString(", ")
        return parts.joinToString(" · ")
    }
}
