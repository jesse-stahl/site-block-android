package today.siteblock.core

/** Big-endian (network order) 16-bit helpers. */
internal object Bytes {
    fun u16(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    fun put16(b: ByteArray, off: Int, value: Int) {
        b[off] = (value ushr 8).toByte()
        b[off + 1] = value.toByte()
    }
}
