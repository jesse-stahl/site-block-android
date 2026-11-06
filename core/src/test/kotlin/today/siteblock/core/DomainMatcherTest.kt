package today.siteblock.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMatcherTest {
    private val matcher = DomainMatcher(listOf("x.com", "twimg.com"))

    @Test fun matchesExactAndAnySubdomain() {
        assertTrue(matcher.matches("x.com"))
        assertTrue(matcher.matches("api.x.com"))
        assertTrue(matcher.matches("pbs.twimg.com"))
    }

    @Test fun ignoresCaseAndTrailingDot() {
        assertTrue(matcher.matches("API.X.COM."))
    }

    @Test fun doesNotMatchLookalikes() {
        assertFalse(matcher.matches("notx.com"))
        assertFalse(matcher.matches("x.com.evil.net"))
        assertFalse(matcher.matches("com"))
        assertFalse(matcher.matches(""))
    }

    @Test fun emptyMatcherMatchesNothing() {
        val empty = DomainMatcher(emptyList())
        assertTrue(empty.isEmpty)
        assertFalse(empty.matches("x.com"))
    }
}
