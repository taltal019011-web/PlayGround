package com.example.playground.ui.myposts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.playground.R
import com.example.playground.auth.AuthManager
import com.example.playground.data.Event
import com.example.playground.repository.EventRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import android.app.AlertDialog

class MyPostsFragment : Fragment() {

    private lateinit var authManager: AuthManager
    private lateinit var eventRepository: EventRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_posts, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager(requireContext())
        eventRepository = EventRepository.getInstance(requireContext())
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadEvents()
    }

    private fun loadEvents() {
        val user = authManager.getCurrentUser() ?: return
        val myEvents = eventRepository.getAllEvents().filter { it.hostId == user.id }
        if (myEvents.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            recyclerView.adapter = MyEventsAdapter(
                items = myEvents.toMutableList(),
                onEdit = { event -> showEditDialog(event) },
                onDelete = { event -> showDeleteDialog(event) }
            )
        }
    }

    private fun showDeleteDialog(event: Event) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete \"${event.title}\"?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                val user = authManager.getCurrentUser() ?: return@setPositiveButton
                when (val result = eventRepository.deleteEvent(user.id, event)) {
                    is EventRepository.EventResult.Success -> {
                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                        loadEvents()
                    }
                    is EventRepository.EventResult.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showEditDialog(event: Event) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_event, null)

        val titleEdit = dialogView.findViewById<TextInputEditText>(R.id.editTitle)
        val descriptionEdit = dialogView.findViewById<TextInputEditText>(R.id.editDescription)
        val maxPlayersEdit = dialogView.findViewById<TextInputEditText>(R.id.editMaxPlayers)

        titleEdit.setText(event.title)
        descriptionEdit.setText(event.description ?: "")
        maxPlayersEdit.setText(event.maxPlayers.toString())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Post")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val user = authManager.getCurrentUser() ?: return@setPositiveButton
                val updatedEvent = event.copy(
                    title = titleEdit.text.toString().ifBlank { event.title },
                    description = descriptionEdit.text.toString(),
                    maxPlayers = maxPlayersEdit.text.toString().toIntOrNull() ?: event.maxPlayers
                )
                when (val result = eventRepository.updateEvent(user.id, updatedEvent)) {
                    is EventRepository.EventResult.Success -> {
                        Toast.makeText(context, "Post updated", Toast.LENGTH_SHORT).show()
                        loadEvents()
                    }
                    is EventRepository.EventResult.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    class MyEventsAdapter(
        private val items: MutableList<Event>,
        private val onEdit: (Event) -> Unit,
        private val onDelete: (Event) -> Unit
    ) : RecyclerView.Adapter<MyEventsAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val sportChip: TextView = v.findViewById(R.id.sportChip)
            val timeText: TextView = v.findViewById(R.id.timeText)
            val titleText: TextView = v.findViewById(R.id.titleText)
            val descriptionText: TextView = v.findViewById(R.id.descriptionText)
            val locationText: TextView = v.findViewById(R.id.locationText)
            val playersText: TextView = v.findViewById(R.id.playersText)
            val editButton: MaterialButton = v.findViewById(R.id.editButton)
            val deleteButton: MaterialButton = v.findViewById(R.id.deleteButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_my_event, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val event = items[position]
            holder.sportChip.text = event.sport
            holder.timeText.text = formatTimeAgo(event.startTime)
            holder.titleText.text = event.title
            holder.descriptionText.text = event.description ?: ""
            holder.locationText.text = "📍 ${event.locationLabel}"
            holder.playersText.text = "👥 Max ${event.maxPlayers}"
            holder.editButton.setOnClickListener { onEdit(event) }
            holder.deleteButton.setOnClickListener { onDelete(event) }
        }

        override fun getItemCount() = items.size

        private fun formatTimeAgo(timestamp: Long): String {
            val diff = timestamp - System.currentTimeMillis()
            val minutes = diff / 60_000
            return when {
                diff < 0 -> "Past event"
                minutes < 60 -> "In ${minutes}m"
                minutes < 1440 -> "In ${minutes / 60}h"
                else -> "In ${minutes / 1440}d"
            }
        }
    }
}