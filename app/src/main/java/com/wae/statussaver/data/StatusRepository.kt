package com.wae.statussaver.data

import android.content.Context
import com.wae.statussaver.util.HashIndex
import com.wae.statussaver.util.StatusDiscovery

class StatusRepository(private val context: Context) {

    private val index = HashIndex(context)

    private val videoExtensions = setOf("mp4", "mkv", "3gp", "3gpp")

    fun list(): List<StatusItem> =
        StatusDiscovery.mediaFiles().map { file ->
            val fingerprint = HashIndex.of(file)
            StatusItem(
                file = file,
                isVideo = file.extension.lowercase() in videoExtensions,
                name = file.name,
                sizeBytes = file.length(),
                modified = file.lastModified(),
                fingerprint = fingerprint,
                saved = fingerprint != null && index.contains(fingerprint)
            )
        }.sortedByDescending { it.modified }

    fun dirsFound(): Int = StatusDiscovery.candidateDirs().size

    /** Returns true when the item is in the gallery afterwards. */
    fun save(item: StatusItem): Boolean {
        val fingerprint = item.fingerprint ?: return false
        if (index.contains(fingerprint)) return true
        if (!StatusDiscovery.saveToGallery(context, item.file)) return false
        index.add(fingerprint)
        return true
    }

    fun saveAll(items: List<StatusItem>): Int = items.count { save(it) }

    fun resetIndex() = index.clear()

    fun savedCount(): Int = index.size
}
