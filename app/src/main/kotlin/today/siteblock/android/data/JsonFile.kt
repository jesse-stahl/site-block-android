package today.siteblock.android.data

import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** A small text file written atomically (temp + rename), so a crash mid-write never corrupts it. */
class JsonFile(private val file: File) {
    private val atomic = AtomicFile(file)

    /** Contents, or null if the file doesn't exist yet. */
    @Throws(IOException::class)
    fun read(): String? = try {
        atomic.readFully().toString(Charsets.UTF_8)
    } catch (e: FileNotFoundException) {
        null
    }

    @Throws(IOException::class)
    fun write(text: String) {
        val out = atomic.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(out)
        } catch (e: IOException) {
            atomic.failWrite(out)
            throw e
        }
    }

    /** Keep a copy of an unreadable file for diagnosis. */
    fun backupTo(name: String) {
        runCatching { file.copyTo(File(file.parentFile, name), overwrite = true) }
    }
}
