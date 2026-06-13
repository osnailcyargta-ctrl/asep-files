package com.asepfiles.app

import java.io.File

data class FileItem(
    val file: File,
    val isSelected: Boolean = false
) {
    val name get() = file.name
    val isDirectory get() = file.isDirectory
    val size get() = if (file.isDirectory) file.listFiles()?.size ?: 0 else file.length().toInt()
    val extension get() = file.extension.lowercase()

    fun formattedSize(): String {
        if (isDirectory) return "${size} items"
        val bytes = file.length()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024f * 1024f))} MB"
            else -> "${String.format("%.1f", bytes / (1024f * 1024f * 1024f))} GB"
        }
    }

    fun mimeType(): String = when (extension) {
        "jpg","jpeg","png","gif","webp","bmp" -> "image"
        "mp4","mkv","avi","mov","webm" -> "video"
        "mp3","aac","ogg","wav","flac" -> "audio"
        "pdf" -> "pdf"
        "zip","rar","7z","tar","gz" -> "archive"
        "apk" -> "apk"
        "kt","java","py","js","ts","html","css","xml","json","txt","md","sh","c","cpp","cs" -> "code"
        else -> "file"
    }
}