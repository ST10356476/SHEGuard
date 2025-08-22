package com.iiest10356476.sheguard.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import com.iiest10356476.sheguard.data.EmergencyContact

class ContactsPreferencesHelper(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveContacts(contacts: List<EmergencyContact>) {
        val jsonArray = JSONArray()

        contacts.forEach { contact ->
            val jsonObject = JSONObject().apply {
                put("id", contact.id)
                put("name", contact.name)
                put("phoneNumber", contact.phoneNumber)
                put("isSelected", contact.isSelected)
            }
            jsonArray.put(jsonObject)
        }

        sharedPreferences.edit()
            .putString(KEY_CONTACTS, jsonArray.toString())
            .apply()
    }

    fun getContacts(): List<EmergencyContact> {
        val contactsJson = sharedPreferences.getString(KEY_CONTACTS, null)
        val contacts = mutableListOf<EmergencyContact>()

        if (contactsJson != null) {
            try {
                val jsonArray = JSONArray(contactsJson)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val contact = EmergencyContact(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name"),
                        phoneNumber = jsonObject.getString("phoneNumber"),
                        isSelected = jsonObject.getBoolean("isSelected")
                    )
                    contacts.add(contact)
                }
            } catch (e: Exception) {
                // Handle JSON parsing error
                e.printStackTrace()
            }
        }

        return contacts
    }

    fun getSelectedContacts(): List<EmergencyContact> {
        return getContacts().filter { it.isSelected }
    }

    fun hasSelectedContacts(): Boolean {
        return getSelectedContacts().isNotEmpty()
    }

    fun addContact(contact: EmergencyContact) {
        val currentContacts = getContacts().toMutableList()
        currentContacts.add(contact)
        saveContacts(currentContacts)
    }

    fun updateContact(contact: EmergencyContact) {
        val contacts = getContacts().toMutableList()
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            contacts[index] = contact
            saveContacts(contacts)
        }
    }

    fun deleteContact(contactId: String) {
        val contacts = getContacts().filter { it.id != contactId }
        saveContacts(contacts)
    }

    companion object {
        private const val PREFS_NAME = "sheguard_contacts"
        private const val KEY_CONTACTS = "emergency_contacts"
    }
}