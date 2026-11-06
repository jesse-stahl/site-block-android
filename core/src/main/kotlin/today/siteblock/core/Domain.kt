package today.siteblock.core

/** Parsing and normalization of user-entered website domains. */
object Domain {

    /**
     * Normalize raw user input into a canonical bare host, or null if it is not a plausible
     * domain. Strips scheme, path/query/fragment, userinfo, port, one leading "www." and a
     * trailing dot, then lowercases and trims. Path is cut BEFORE userinfo so "x.com/@user"
     * stays "x.com". Example: "https://www.Facebook.com/feed" -> "facebook.com".
     */
    fun normalize(raw: String): String? {
        var host = raw.trim().lowercase()
        if (host.isEmpty()) return null
        val scheme = host.indexOf("://")
        if (scheme >= 0) host = host.substring(scheme + 3)
        for (sep in charArrayOf('/', '?', '#')) {
            val i = host.indexOf(sep)
            if (i >= 0) host = host.substring(0, i)
        }
        val at = host.lastIndexOf('@')
        if (at >= 0) host = host.substring(at + 1)
        val colon = host.indexOf(':')
        if (colon >= 0) host = host.substring(0, colon)
        host = host.removePrefix("www.").removeSuffix(".")
        return if (isValidHost(host)) host else null
    }

    /**
     * Valid if: at most 253 chars, at least two dot-separated labels, each label 1–63 chars of
     * [a-z0-9-] not starting/ending with '-', and an alphabetic TLD of 2+ chars.
     */
    fun isValidHost(host: String): Boolean {
        if (host.length > 253) return false
        val labels = host.split('.')
        if (labels.size < 2) return false
        for (label in labels) {
            if (label.length !in 1..63) return false
            if (label.startsWith('-') || label.endsWith('-')) return false
            if (!label.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }) return false
        }
        val tld = labels.last()
        return tld.length >= 2 && tld.all { it in 'a'..'z' }
    }
}
