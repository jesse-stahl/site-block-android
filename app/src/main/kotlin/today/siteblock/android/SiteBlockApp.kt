package today.siteblock.android

import android.app.Application
import android.content.Context
import today.siteblock.android.data.BlockListRepository
import today.siteblock.android.system.Notifier

class SiteBlockApp : Application() {
    lateinit var repository: BlockListRepository
        private set

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
        repository = BlockListRepository(this)
        repository.start()
    }
}

/** The process-wide repository. Main thread only (see BlockListRepository). */
val Context.repository: BlockListRepository
    get() = (applicationContext as SiteBlockApp).repository
