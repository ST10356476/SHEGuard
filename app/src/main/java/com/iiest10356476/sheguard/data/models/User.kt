package com.iiest10356476.sheguard.data.models

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val dateOfBirth: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)