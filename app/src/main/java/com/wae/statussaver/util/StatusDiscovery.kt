package com.wae.statussaver.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream

/**
 * On Android 11+ WhatsApp writes to Android/media/<pkg>/WhatsApp/Media/.Statuses,
 * which is readable with NO permission. The legacy /sdcard/WhatsApp/Media/.Statuses
 * layout needs All Files Access on Android 11+. Both are probed.
 */
object StatusDiscovery {

    private const val TAG = "WaeDiscovery"
    private const val SETTLE_MS = 700L
    private const val RELATIVE_IMAGES = "Pictures/WhatsApp Statuses"
    private const val RELATIVE_VIDEO = "Movies/WhatsApp Statuses"

    private val PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")
    private val MEDIA_EXTENSIONS =
        setOf("jpg", "jpeg", "png", "webp", "gif", "mp4", "mkv", "3gp", "3gpp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "3gp", "3gpp")

    fun candidateDirs(): List<File> {
        val external = Environment.getExternalStorageDirectory()
        val out = ArrayList<File>(6)
        for (pkg in PACKAGES) {
            out += File(external, "Android/media/$pkg/WhatsApp/Media/.Statuses")
            out += File(external, "Android/media/$pkg/Media/.Statuses")
        }
        out += File(external, "WhatsApp/Media/.Statuses")
        out += File(external, "WhatsApp Business/Media/.Statuses")
        return out.distinct().filter { it.isDirectory }
    }

    fun legacyOnly(): Boolean {
        val external = Environment.getExternalStorageDirectory()
        val legacy = File(external, "WhatsApp/Media/.Statuses").isDirectory ||
            File(external, "WhatsApp Business/Media/.Statuses").isDirectory
        if (!legacy) return false
        val media = PACKAGES.any {
            File(external, "Android/media/$it/WhatsApp/Media/.Statuses").isDirectory ||
                File(external, "Android/media/$it/Media/.Statuses").isDirectory
        }
        return !media
    }

    fun mediaFiles(): List<File> = candidateDirs().flatMap { dir ->
        dir.listFiles { f ->
            f.isFile && !f.name.startsWith(".") && f.extension.lowercase() in MEDIA_EXTENSIONS
        }?.toList().orEmpty()
    }

    /** One full reconcile pass. Returns how many files were newly saved. */
    fun reconcile(context: Context, index: HashIndex): Int {
        val now = System.currentTimeMillis()
        var saved = 0
        for (source in mediaFiles()) {
            if (now - source.lastModified() < SETTLE_MS) continue
            val fingerprint = HashIndex.of(source) ?: continue
            if (index.contains(fingerprint)) continue
            if (saveToGallery(context, source)) {
                index.add(fingerprint)
                saved++
            }
        }
        return saved
    }

    /** Two-phase IS_PENDING insert; a failed copy is rolled back so no orphan row. */
    fun saveToGallery(context: Context, source: File): Boolean {
        val isVideo = source.extension.lowercase() in VIDEO_EXTENSIONS
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val relative = if (isVideo) RELATIVE_VIDEO else RELATIVE_IMAGES

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName(source.name))
            put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        var target: Uri? = null
        return try {
            target = context.contentResolver.insert(collection, values) ?: return false
            val out = context.contentResolver.openOutputStream(target)
                ?: throw IllegalStateException("null output stream for $target")
            out.use { sink -> FileInputStream(source).use { it.copyTo(sink, DEFAULT_BUFFER_SIZE) } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.update(
                    target,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null
                )
            }
            true
        } catch (t: Throwable) {
            Log.w(TAG, "copy failed for ${source.name}", t)
            runCatching { target?.let { context.contentResolver.delete(it, null, null) } }
            false
        }
    }

    private fun displayName(original: String): String {
        val ext = original.substringAfterLast('.', "jpg").lowercase()
        val stem = original.substringBeforeLast('.', "status")
            .filter { it.isLetterOrDigit() || it == '_' }
            .take(32)
            .ifEmpty { "status" }
        return "WA_${System.currentTimeMillis()}_$stem.$ext"
    }
}
