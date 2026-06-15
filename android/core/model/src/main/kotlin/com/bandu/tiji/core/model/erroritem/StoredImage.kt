package com.bandu.tiji.core.model.erroritem

data class StoredImage(
    val relativePath: String,
    val sha256Hex: String,
    val width: Int,
    val height: Int,
    val thumbnailRelativePath: String? = null,
)
