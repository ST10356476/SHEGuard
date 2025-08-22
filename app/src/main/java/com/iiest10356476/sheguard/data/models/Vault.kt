package com.iiest10356476.sheguard.data.models

data class Vault(
    val vaultId: String = "",
    val files: List<VaultFile> = emptyList(),
    val submitDate: Long = 0L,
    val uid: String = ""
)
