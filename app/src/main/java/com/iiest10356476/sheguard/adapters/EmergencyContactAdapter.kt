package com.iiest10356476.sheguard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.EmergencyContact

class EmergencyContactAdapter(
    private var contacts: MutableList<EmergencyContact>,
    private val onContactSelectionChanged: (EmergencyContact, Boolean) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.contact_name)
        val phoneTextView: TextView = view.findViewById(R.id.contact_phone)
        val selectionCheckBox: CheckBox = view.findViewById(R.id.contact_checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]

        holder.nameTextView.text = contact.name
        holder.phoneTextView.text = contact.phoneNumber
        holder.selectionCheckBox.isChecked = contact.isSelected

        holder.selectionCheckBox.setOnCheckedChangeListener { _, isChecked ->
            onContactSelectionChanged(contact, isChecked)
        }

        holder.itemView.setOnClickListener {
            holder.selectionCheckBox.isChecked = !holder.selectionCheckBox.isChecked
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateContacts(newContacts: List<EmergencyContact>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }

    fun addContact(contact: EmergencyContact) {
        contacts.add(contact)
        notifyItemInserted(contacts.size - 1)
    }
}