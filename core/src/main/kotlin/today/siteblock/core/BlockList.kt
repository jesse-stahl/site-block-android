package today.siteblock.core

import kotlinx.serialization.Serializable

/**
 * Persisted blocker state. Never holds pending-gate state (that is transient by design).
 * All withX() and without() methods return a new list, or null if the change is invalid or a no-op.
 * Gating weaken actions is the caller's job (see GateController).
 */
@Serializable
data class BlockList(
    val items: List<BlockItem> = emptyList(),
    val pausedIds: List<String> = emptyList(),
    val blockingOn: Boolean = false,
    val settings: FrictionSettings = FrictionSettings(),
    val filterPause: FilterPause? = null,
    /** Apps that skip the web filter. Adding is a weaken action (gated); removing is instant. */
    val whitelist: List<WhitelistedApp> = WhitelistedApp.DEFAULTS,
) {
    fun item(id: String): BlockItem? = items.firstOrNull { it.id == id }

    fun isPaused(id: String): Boolean = id in pausedIds

    fun activeItems(): List<BlockItem> = items.filter { it.id !in pausedIds }

    fun coversDomain(host: String): Boolean = items.any { host in it.domains }

    fun coversPackage(packageId: String): Boolean = items.any { packageId in it.packages }

    fun withDomain(raw: String): BlockList? {
        val host = Domain.normalize(raw) ?: return null
        if (coversDomain(host)) return null
        val row = BlockItem(BlockItem.siteId(host), host, domains = listOf(host), source = ItemSource.MANUAL_SITE)
        return copy(items = items + row)
    }

    fun withApp(rawPackageId: String, label: String, source: ItemSource): BlockList? {
        val id = PackageId.normalize(rawPackageId) ?: return null
        if (!PackageId.isBlockable(id) || coversPackage(id)) return null
        val name = label.trim().ifEmpty { id }
        val row = BlockItem(BlockItem.appId(id), name, packages = listOf(id), source = source)
        return copy(items = items + row)
    }

    fun withBuiltin(entry: CatalogEntry): BlockList? {
        val id = BlockItem.builtinId(entry.id)
        if (item(id) != null) return null
        val row = BlockItem(id, entry.label, entry.packages, entry.domains, ItemSource.BUILTIN)
        return copy(items = items + row)
    }

    fun without(id: String): BlockList? {
        if (item(id) == null) return null
        return copy(items = items.filterNot { it.id == id }, pausedIds = pausedIds - id)
    }

    fun withPaused(id: String, paused: Boolean): BlockList? {
        if (item(id) == null || isPaused(id) == paused) return null
        return copy(pausedIds = if (paused) pausedIds + id else pausedIds - id)
    }

    fun isWhitelisted(packageId: String): Boolean = whitelist.any { it.packageId == packageId }

    fun withWhitelisted(rawPackageId: String, label: String): BlockList? {
        val id = PackageId.normalize(rawPackageId) ?: return null
        if (!WhitelistedApp.isAllowed(id) || isWhitelisted(id)) return null
        return copy(whitelist = whitelist + WhitelistedApp(id, label.trim().ifEmpty { id }))
    }

    fun withoutWhitelisted(packageId: String): BlockList? {
        if (!isWhitelisted(packageId)) return null
        return copy(whitelist = whitelist.filterNot { it.packageId == packageId })
    }

    /** Repair anything a hand-edited or older file might contain. */
    fun normalized(): BlockList {
        val uniqueItems = items.distinctBy { it.id }
        val ids = uniqueItems.map { it.id }.toSet()
        return copy(
            items = uniqueItems,
            pausedIds = pausedIds.distinct().filter { it in ids },
            settings = settings.clamped(),
            whitelist = whitelist.filter { WhitelistedApp.isAllowed(it.packageId) }.distinctBy { it.packageId },
        )
    }
}
