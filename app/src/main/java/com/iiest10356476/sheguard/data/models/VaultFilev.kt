package com.iiest10356476.sheguard.data.models

data class VaultFile(
    val url: String = "",
    val type: FileType = FileType.PHOTO
)
enum class FileType { PHOTO, VIDEO, AUDIO }