package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageIdTest {
    @Test fun acceptsRealIdsAndTrims() {
        assertEquals("com.twitter.android", PackageId.normalize(" com.twitter.android "))
        assertEquals("com.zhiliaoapp.musically", PackageId.normalize("com.zhiliaoapp.musically"))
        assertEquals("com.instagram.barcelona", PackageId.normalize("com.instagram.barcelona"))
    }

    @Test fun keepsCaseBecauseAndroidIdsAreCaseSensitive() {
        assertEquals("com.Example.App", PackageId.normalize("com.Example.App"))
    }

    @Test fun rejectsMalformed() {
        val bad = listOf("", "twitter", "com..twitter", "1com.app", "com.app!", "com. app", ".com.app", "com.app.", "com.1app")
        for (input in bad) assertNull("expected null for '$input'", PackageId.normalize(input))
    }

    @Test fun selfAndSystemAppsAreNeverBlockable() {
        assertFalse(PackageId.isBlockable(PackageId.SELF))
        assertFalse(PackageId.isBlockable("com.android.settings"))
        assertFalse(PackageId.isBlockable("com.sec.android.app.launcher"))
        assertTrue(PackageId.isBlockable("com.twitter.android"))
    }
}
