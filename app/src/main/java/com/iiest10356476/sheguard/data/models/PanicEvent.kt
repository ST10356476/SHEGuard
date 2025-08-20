package com.iiest10356476.sheguard.data.models

data class PanicEvent(
    val panicEventId: String = "",
    val recordUrl: String = "",
    val eventDate: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val uid: String = ""
)
