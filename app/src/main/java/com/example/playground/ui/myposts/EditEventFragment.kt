package com.example.playground.ui.myposts

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
import com.example.playground.auth.AuthManager
import com.example.playground.data.Event
import com.example.playground.data.Venue
import com.example.playground.data.Venues
import com.example.playground.repository.EventRepository
import com.example.playground.ui.home.CreateEventFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import android.content.Intent
class EditEventFragment : Fragment() {

    companion object {
        private const val ARG_EVENT_ID = "event_id"

        fun newInstance(eventId: Long): EditEventFragment {
            return EditEventFragment().apply {
                arguments = Bundle().apply { putLong(ARG_EVENT_ID, eventId) }
            }
        }
    }

    private lateinit var authManager: AuthManager
    private lateinit var eventRepository: EventRepository
    private lateinit var event: Event

    private var selectedVenue: Venue? = null
    private var selectedImageUri: Uri? = null
    private var selectedImageView: ImageView? = null

    private var venueAdapter: CreateEventFragment.VenueAdapter? = null

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
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) pickImageLauncher.launch("image/*")
            else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_edit_event, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager(requireContext())
        eventRepository = EventRepository.getInstance(requireContext())

        val eventId = arguments?.getLong(ARG_EVENT_ID) ?: return
        event = eventRepository.getEventById(eventId) ?: return

        // Views
        val sportGroup = view.findViewById<ChipGroup>(R.id.sportChipGroup)
        val titleEdit = view.findViewById<TextInputEditText>(R.id.editTitle)
        val descriptionEdit = view.findViewById<TextInputEditText>(R.id.editDescription)
        val maxPlayersEdit = view.findViewById<TextInputEditText>(R.id.editMaxPlayers)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveButton)
        val closeButton = view.findViewById<TextView>(R.id.closeButton)
        val venueRecycler = view.findViewById<RecyclerView>(R.id.venueRecyclerView)
        val selectedLocationCard = view.findViewById<MaterialCardView>(R.id.selectedLocationCard)
        val selectedLocationName = view.findViewById<TextView>(R.id.selectedLocationName)
        val selectedLocationCoords = view.findViewById<TextView>(R.id.selectedLocationCoords)
        val clearLocationButton = view.findViewById<TextView>(R.id.clearLocationButton)
        val selectImageButton = view.findViewById<MaterialButton>(R.id.selectImageButton)
        selectedImageView = view.findViewById(R.id.selectedImageView)

        // Load existing image if present
        event.imageUri?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                selectedImageUri = uri
                selectedImageView?.setImageURI(uri)
                selectedImageView?.visibility = View.VISIBLE
            } catch (e: Exception) {
                // URI no longer valid, ignore
            }
        }

        // Pre-fill fields
        titleEdit.setText(event.title)
        descriptionEdit.setText(event.description ?: "")
        maxPlayersEdit.setText(event.maxPlayers.toString())

        // Pre-select sport chip
        val chipMap = mapOf(
            "Basketball" to R.id.chipBasketball,
            "Football" to R.id.chipFootball,
            "Tennis" to R.id.chipTennis,
            "Volleyball" to R.id.chipVolleyball,
            "Other" to R.id.chipOther
        )
        chipMap[event.sport]?.let { sportGroup.check(it) }

        // Pre-select venue
        val matchingVenue = Venues.all.find { it.name == event.locationLabel }
        if (matchingVenue != null) {
            selectedVenue = matchingVenue
            selectedLocationCard.visibility = View.VISIBLE
            selectedLocationName.text = matchingVenue.name
            selectedLocationCoords.text =
                "%.4f, %.4f · ${matchingVenue.area}".format(matchingVenue.latitude, matchingVenue.longitude)
        }

        // Venue adapter
        venueAdapter = CreateEventFragment.VenueAdapter(Venues.all) { venue ->
            selectedVenue = venue
            selectedLocationCard.visibility = View.VISIBLE
            selectedLocationName.text = venue.name
            selectedLocationCoords.text =
                "%.4f, %.4f · ${venue.area}".format(venue.latitude, venue.longitude)
            venueAdapter?.setSelected(venue)
        }
        venueAdapter?.setSelected(matchingVenue)
        venueRecycler.layoutManager = LinearLayoutManager(requireContext())
        venueRecycler.adapter = venueAdapter
        venueRecycler.isNestedScrollingEnabled = false

        // Filter venues by sport
        sportGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = view.findViewById<Chip>(checkedIds.first())
                val sport = chip.text.toString()
                    .replace("🏀 ", "").replace("⚽ ", "")
                    .replace("🎾 ", "").replace("🏐 ", "").replace("🏃 ", "")
                val filtered = Venues.all.filter { v ->
                    v.sports.isEmpty() || v.sports.any { it.equals(sport, ignoreCase = true) }
                }
                venueAdapter?.updateList(filtered, null)
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

        selectImageButton.setOnClickListener { openImagePicker() }

        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        saveButton.setOnClickListener {
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
            val sport = rawSport
                .replace("🏀 ", "").replace("⚽ ", "")
                .replace("🎾 ", "").replace("🏐 ", "").replace("🏃 ", "")
            val venue = selectedVenue!!
            val user = authManager.getCurrentUser() ?: return@setOnClickListener

            val updatedEvent = event.copy(
                sport = sport,
                title = titleEdit.text.toString().ifBlank { event.title },
                description = descriptionEdit.text.toString(),
                maxPlayers = maxPlayersEdit.text.toString().toIntOrNull() ?: event.maxPlayers,
                latitude = venue.latitude,
                longitude = venue.longitude,
                locationLabel = venue.name,
                imageUri = selectedImageUri?.toString() ?: event.imageUri  // ADD THIS
            )

            when (val result = eventRepository.updateEvent(user.id, updatedEvent)) {
                is EventRepository.EventResult.Success -> {
                    Toast.makeText(context, "Post updated!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                is EventRepository.EventResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
}