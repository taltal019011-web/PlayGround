package com.example.playground.ui.myposts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.playground.R
import com.example.playground.data.Event
import com.example.playground.repository.AuthRepository
import com.example.playground.repository.EventRepository
import com.example.playground.viewmodel.MyPostsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import android.app.AlertDialog
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts

class MyPostsFragment : Fragment() {

    private lateinit var viewModel: MyPostsViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressIndicator: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_posts, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val authRepository = AuthRepository.getInstance(requireContext())
        val eventRepository = EventRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(
            this,
            MyPostsViewModel.Factory(authRepository, eventRepository)
        )[MyPostsViewModel::class.java]

        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        observeViewModel()
        viewModel.loadEvents()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            if (loading) {
                recyclerView.visibility = View.GONE
                emptyText.visibility = View.GONE
            } else {
                val empty = viewModel.isEmpty.value ?: true
                recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
                emptyText.visibility = if (empty) View.VISIBLE else View.GONE
            }
        }

        viewModel.myEvents.observe(viewLifecycleOwner) { events ->
            recyclerView.adapter = MyEventsAdapter(
                items = events.toMutableList(),
                onEdit = { event -> showEditDialog(event) },
                onDelete = { event -> showDeleteDialog(event) }
            )
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { empty ->
            if (viewModel.isLoading.value == true) return@observe
            recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
            emptyText.visibility = if (empty) View.VISIBLE else View.GONE
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is MyPostsViewModel.OperationResult.Success -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearOperationResult()
                }
                is MyPostsViewModel.OperationResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearOperationResult()
                }
                null -> {}
            }
        }
    }

    private fun showDeleteDialog(event: Event) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete \"${event.title}\"?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteEvent(event)
            }
            .show()
    }

    private var editImageUri: Uri? = null

    private val editPickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val permanentUri = copyImageToAppStorage(it) ?: it
                editImageUri = permanentUri
                editImageView?.setImageURI(permanentUri)
                editImageView?.visibility = View.VISIBLE
            }
        }

    private var editImageView: ImageView? = null

    private fun copyImageToAppStorage(uri: Uri): Uri? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(requireContext().filesDir, "img_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { output -> inputStream.copyTo(output) }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    private fun showEditDialog(event: Event) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_event, null)

        val titleEdit = dialogView.findViewById<TextInputEditText>(R.id.editTitle)
        val descriptionEdit = dialogView.findViewById<TextInputEditText>(R.id.editDescription)
        val maxPlayersEdit = dialogView.findViewById<TextInputEditText>(R.id.editMaxPlayers)
        val selectImageButton = dialogView.findViewById<MaterialButton>(R.id.selectImageButton)
        editImageView = dialogView.findViewById(R.id.selectedImageView)

        titleEdit.setText(event.title)
        descriptionEdit.setText(event.description ?: "")
        maxPlayersEdit.setText(event.maxPlayers.toString())

        editImageUri = event.imageUri?.let { Uri.parse(it) }
        editImageUri?.let {
            try {
                editImageView?.setImageURI(it)
                editImageView?.visibility = View.VISIBLE
            } catch (e: Exception) {
                editImageView?.visibility = View.GONE
            }
        }

        selectImageButton.setOnClickListener {
            editPickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val updatedEvent = event.copy(
                    title = titleEdit.text.toString().ifBlank { event.title },
                    description = descriptionEdit.text.toString(),
                    maxPlayers = maxPlayersEdit.text.toString().toIntOrNull() ?: event.maxPlayers,
                    imageUri = editImageUri?.toString() ?: event.imageUri
                )
                viewModel.updateEvent(updatedEvent)
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
