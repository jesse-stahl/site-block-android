package today.siteblock.core

import kotlinx.serialization.json.Json

/** JSON encoding of the persisted BlockList. Decode always returns a normalized list. */
object BlockListCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(list: BlockList): String = json.encodeToString(BlockList.serializer(), list)

    /** @throws IllegalArgumentException (SerializationException) if [text] is not a valid block list. */
    fun decode(text: String): BlockList = json.decodeFromString(BlockList.serializer(), text).normalized()
}
