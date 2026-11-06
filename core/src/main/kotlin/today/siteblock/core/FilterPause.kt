package today.siteblock.core

import kotlinx.serialization.Serializable

/**
 * A pause of the web filter only (app blocking continues). [untilMonotonic] is a monotonic-clock
 * deadline valid only while [bootCount] matches the current boot; null = until resumed.
 */
@Serializable
data class FilterPause(
    val untilMonotonic: Long? = null,
    val bootCount: Int = 0,
)

enum class PauseDuration(val label: String, val millis: Long?) {
    MIN_15("15 minutes", 15 * 60_000L),
    HOUR_1("1 hour", 60 * 60_000L),
    HOUR_4("4 hours", 4 * 60 * 60_000L),
    DAY_1("1 day", 24 * 60 * 60_000L),
    UNTIL_RESUMED("Until I turn it back on", null),
}
