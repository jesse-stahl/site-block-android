package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    @Test fun stripsSchemePathWwwAndCase() {
        assertEquals("facebook.com", Domain.normalize("https://www.Facebook.com/feed"))
        assertEquals("news.yc.com", Domain.normalize("  News.YC.com "))
    }

    @Test fun stripsPortUserinfoAndTrailingDot() {
        assertEquals("x.com", Domain.normalize("x.com:443"))
        assertEquals("x.com", Domain.normalize("user:pw@x.com"))
        assertEquals("x.com", Domain.normalize("x.com."))
    }

    @Test fun atSignInPathIsNotTreatedAsUserinfo() {
        assertEquals("x.com", Domain.normalize("https://x.com/@elonmusk"))
    }

    @Test fun keepsOtherSubdomains() {
        assertEquals("mobile.twitter.com", Domain.normalize("mobile.twitter.com"))
    }

    @Test fun rejectsInvalidInput() {
        val bad = listOf("", "   ", "not a domain", "localhost", "x.c", "-bad.com", "bad-.com", "x.123", "a..com", "exa_mple.com")
        for (input in bad) assertNull("expected null for '$input'", Domain.normalize(input))
    }

    @Test fun isValidHostChecksLabelLengths() {
        assertTrue(Domain.isValidHost("t.co"))
        assertFalse(Domain.isValidHost("a".repeat(64) + ".com"))
    }
}
