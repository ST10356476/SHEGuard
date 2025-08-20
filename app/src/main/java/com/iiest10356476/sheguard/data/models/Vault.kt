package com.iiest10356476.sheguard.data.models

data class Vault(
    val vaultId: String = "",
    val photos: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    val audios: List<String> = emptyList(),
    val submitDate: Long = System.currentTimeMillis(),
    val uid: String = ""
)
