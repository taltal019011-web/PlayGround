package com.example.playground.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.playground.R
import com.example.playground.data.Venue
import com.example.playground.data.Venues
import com.example.playground.repository.AuthRepository
import com.example.playground.repository.EventRepository
import com.example.playground.viewmodel.CreateEventViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import android.content.Intent

class CreateEventFragment : Fragment() {

    private lateinit var viewModel: CreateEventViewModel
    private var selectedVenue: Venue? = null
    private var selectedImageUri: Uri? = null
    private var selectedImageView: ImageView? = null
    private var venueAdapter: VenueAdapter? = null

    // ---- Image picker launcher ----
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
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val permanentUri = copyImageToAppStorage(it) ?: it
                selectedImageUri = permanentUri
                selectedImageView?.setImageURI(permanentUri)
                selectedImageView?.visibility = View.VISIBLE
            }
        }

    // ---- Permission request launcher ----
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission denied — cannot access images",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_create_event, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val authRepository = AuthRepository.getInstance(requireContext())
        val eventRepository = EventRepository.getInstance(requireContext())
        viewModel = CreateEventViewModel(authRepository, eventRepository)

        val sportGroup = view.findViewById<ChipGroup>(R.id.sportChipGroup)
        val titleEdit = view.findViewById<TextInputEditText>(R.id.titleEditText)
        val descriptionEdit = view.findViewById<TextInputEditText>(R.id.descriptionEditText)
        val maxPlayersEdit = view.findViewById<TextInputEditText>(R.id.maxPlayersEditText)
        val publishButton = view.findViewById<MaterialButton>(R.id.publishButton)
        val venueRecycler = view.findViewById<RecyclerView>(R.id.venueRecyclerView)
        val selectedLocationCard = view.findViewById<MaterialCardView>(R.id.selectedLocationCard)
        val selectedLocationName = view.findViewById<TextView>(R.id.selectedLocationName)
        val selectedLocationCoords = view.findViewById<TextView>(R.id.selectedLocationCoords)
        val clearLocationButton = view.findViewById<TextView>(R.id.clearLocationButton)
        val selectImageButton = view.findViewById<MaterialButton>(R.id.selectImageButton)
        selectedImageView = view.findViewById(R.id.selectedImageView)

        // ---- Image button with runtime permission ----
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        // Setup venue list
        venueAdapter = VenueAdapter(Venues.all) { venue ->
            selectedVenue = venue
            selectedLocationCard.visibility = View.VISIBLE
            selectedLocationName.text = venue.name
            selectedLocationCoords.text =
                "%.4f, %.4f · ${venue.area}".format(venue.latitude, venue.longitude)
            venueAdapter?.setSelected(venue)
        }

        venueRecycler.layoutManager = LinearLayoutManager(requireContext())
        venueRecycler.adapter = venueAdapter
        venueRecycler.isNestedScrollingEnabled = false

        sportGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = view.findViewById<Chip>(checkedIds.first())
                val sport = chip.text.toString()
                    .replace("🏀 ", "").replace("⚽ ", "")
                    .replace("🎾 ", "").replace("🏐 ", "").replace("🏃 ", "")
                val filtered = Venues.all.filter { v ->
                    v.sports.isEmpty() || v.sports.any { it.equals(sport, ignoreCase = true) }
                }
                venueAdapter?.updateList(filtered)
                if (selectedVenue != null && filtered.none { it.name == selectedVenue!!.name }) {
                    selectedVenue = null
                    selectedLocationCard.visibility = View.GONE
                    venueAdapter?.setSelected(null)
                }
            }
        }

        clearLocationButton.setOnClickListener {
            selectedVenue = null
            selectedLocationCard.visibility = View.GONE
            venueAdapter?.setSelected(null)
        }

        publishButton.setOnClickListener {
            val selectedChipId = sportGroup.checkedChipId
            if (selectedChipId == View.NO_ID) {
                Toast.makeText(context, "Please select a sport", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedVenue == null) {
                Toast.makeText(context, "Please select a location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rawSport = view.findViewById<Chip>(selectedChipId).text.toString()
            val sport = rawSport.replace("🏀 ", "").replace("⚽ ", "")
                .replace("🎾 ", "").replace("🏐 ", "").replace("🏃 ", "")
            val title = titleEdit.text.toString().ifBlank { "$sport Game" }
            val description = descriptionEdit.text.toString()
            val maxPlayers = maxPlayersEdit.text.toString().toIntOrNull() ?: 10
            val venue = selectedVenue!!

            viewModel.createEvent(
                sport = sport,
                title = title,
                description = description,
                startTime = System.currentTimeMillis() + 86400000,
                maxPlayers = maxPlayers,
                latitude = venue.latitude,
                longitude = venue.longitude,
                locationLabel = venue.name,
                imageUri = selectedImageUri?.toString()  // ADD THIS
            )

            Toast.makeText(context, "Event created!", Toast.LENGTH_SHORT).show()

            // Reset form
            sportGroup.clearCheck()
            titleEdit.setText("")
            descriptionEdit.setText("")
            maxPlayersEdit.setText("")
            selectedVenue = null
            selectedImageUri = null
            selectedImageView?.visibility = View.GONE
            selectedLocationCard.visibility = View.GONE
            venueAdapter?.updateList(Venues.all)
            venueAdapter?.setSelected(null)
        }
    }

    // ---- Permission helper ----
    private fun openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ — use the Photo Picker directly, no permission needed
            pickImageLauncher.launch("image/*")
            return
        }

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission)
                    == PackageManager.PERMISSION_GRANTED -> pickImageLauncher.launch("image/*")
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(requireContext(), "Permission needed to select images", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> requestPermissionLauncher.launch(permission)
        }
    }
    // ---- Adapter ----

    class VenueAdapter(
        private var items: List<Venue>,
        private val onSelect: (Venue) -> Unit
    ) : RecyclerView.Adapter<VenueAdapter.VH>() {

        private var selectedVenue: Venue? = null

        fun setSelected(venue: Venue?) {
            selectedVenue = venue
            notifyDataSetChanged()
        }

        fun updateList(newItems: List<Venue>) {
            items = newItems
            notifyDataSetChanged()
        }

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.venueEmoji)
            val name: TextView = v.findViewById(R.id.venueName)
            val area: TextView = v.findViewById(R.id.venueArea)
            val check: TextView = v.findViewById(R.id.venueSelectedIndicator)
            val card: MaterialCardView = v as MaterialCardView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_venue, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val venue = items[position]
            holder.emoji.text = venue.emoji
            holder.name.text = venue.name
            holder.area.text = venue.area

            val isSelected = selectedVenue?.name == venue.name
            holder.check.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.card.strokeWidth = if (isSelected) 2 else 0
            holder.card.strokeColor = if (isSelected) 0xFF2A5BD7.toInt() else 0
            holder.card.setCardBackgroundColor(
                if (isSelected) 0xFFEEF2FF.toInt() else 0xFFFFFFFF.toInt()
            )

            holder.itemView.setOnClickListener { onSelect(venue) }
        }

        override fun getItemCount() = items.size
    }
}