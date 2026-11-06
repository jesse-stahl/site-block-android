package today.siteblock.core

import kotlinx.serialization.Serializable

/** Lengths of the weaken gate, in minutes. Use [of] or [clamped] to keep values in range. */
@Serializable
data class FrictionSettings(
    val waitMinutes: Int = MIN_WAIT,
    val confirmMinutes: Int = MIN_CONFIRM,
) {
    val waitMillis: Long get() = waitMinutes * 60_000L
    val confirmMillis: Long get() = confirmMinutes * 60_000L

    fun clamped(): FrictionSettings = of(waitMinutes, confirmMinutes)

    companion object {
        const val MIN_WAIT = 5
        const val MAX_WAIT = 120
        const val MIN_CONFIRM = 2
        const val MAX_CONFIRM = 30

        fun of(waitMinutes: Int, confirmMinutes: Int) = FrictionSettings(
            waitMinutes.coerceIn(MIN_WAIT, MAX_WAIT),
            confirmMinutes.coerceIn(MIN_CONFIRM, MAX_CONFIRM),
        )
    }
}
