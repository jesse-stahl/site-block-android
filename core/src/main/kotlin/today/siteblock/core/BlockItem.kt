package today.siteblock.core

import kotlinx.serialization.Serializable

@Serializable
enum class ItemSource { BUILTIN, PICKER, MANUAL_APP, MANUAL_SITE }

/** One row on the block list: a built-in bundle, a single app, or a single website. */
@Serializable
data class BlockItem(
    val id: String,
    val label: String,
    val packages: List<String> = emptyList(),
    val domains: List<String> = emptyList(),
    val source: ItemSource,
) {
    companion object {
        fun builtinId(catalogId: String) = "builtin:$catalogId"
        fun appId(packageId: String) = "app:$packageId"
        fun siteId(host: String) = "site:$host"
    }
}
