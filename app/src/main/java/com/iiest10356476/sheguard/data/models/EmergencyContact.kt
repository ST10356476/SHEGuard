package com.iiest10356476.sheguard.data

data class EmergencyContact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val isSelected: Boolean = false
) {
    // Format phone number for WhatsApp (remove spaces, dashes, etc.)
    fun getFormattedPhoneNumber(): String {
        return phoneNumber.replace(Regex("[^+\\d]"), "")
    }
}