package com.wae.statussaver.data

import java.io.File

data class StatusItem(
    val file: File,
    val isVideo: Boolean,
    val name: String,
    val sizeBytes: Long,
    val modified: Long,
    val fingerprint: String?,
    val saved: Boolean
)
