package com.wae.statussaver.util

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/**
 * Append-only fingerprint store: "<length>:<sha256 of first 64 KiB>".
 * Stable across the copy/rename churn WhatsApp does, unlike the filename.
 */
class HashIndex(context: Context) {

    private val file = File(context.filesDir, "saved.index")
    private val lock = Any()
    private val known = HashSet<String>(2048)

    init {
        runCatching {
            if (file.exists()) {
                file.forEachLine { line ->
                    val t = line.trim()
                    if (t.isNotEmpty()) known += t
                }
            }
        }
    }

    val size: Int get() = synchronized(lock) { known.size }

    fun contains(fingerprint: String): Boolean = synchronized(lock) { fingerprint in known }

    fun add(fingerprint: String) {
        synchronized(lock) {
            if (!known.add(fingerprint)) return
            runCatching {
                file.parentFile?.mkdirs()
                file.appendText(fingerprint + "\n")
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            known.clear()
            runCatching { file.writeText("") }
        }
    }

    companion object {
        private const val SAMPLE_BYTES = 64 * 1024

        fun of(file: File): String? {
            if (!file.isFile || file.length() == 0L) return null
            val digest = MessageDigest.getInstance("SHA-256")
            try {
                RandomAccessFile(file, "r").use { raf ->
                    val len = minOf(SAMPLE_BYTES.toLong(), raf.length()).toInt()
                    if (len <= 0) return null
                    val buffer = ByteArray(len)
                    raf.readFully(buffer)
                    digest.update(buffer)
                }
            } catch (t: Throwable) {
                return null
            }
            val hex = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            return "${file.length()}:$hex"
        }
    }
}
