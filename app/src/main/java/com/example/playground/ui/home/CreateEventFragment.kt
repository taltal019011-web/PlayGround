package com.example.playground.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso

class CreateEventFragment : Fragment() {

    private lateinit var viewModel: CreateEventViewModel
    private var selectedVenue: Venue? = null
    private var venueAdapter: VenueAdapter? = null
    private var userLocation: Location? = null
    private var currentSportFilter: String? = null
    private var selectedImageUri: Uri? = null
    private var selectedImageView: ImageView? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2001
        private const val DEFAULT_LATITUDE = 32.0853
        private const val DEFAULT_LONGITUDE = 34.7818
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val permanentUri = copyImageToAppStorage(it) ?: it
                selectedImageUri = permanentUri
                Picasso.get()
                    .load(permanentUri)
                    .into(selectedImageView)
                selectedImageView?.visibility = View.VISIBLE
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) pickImageLauncher.launch("image/*")
            else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }

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

    private fun openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_create_event, container, false)
    }

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

        selectImageButton.setOnClickListener { openImagePicker() }

        venueAdapter = VenueAdapter(emptyList()) { venue ->
            selectedVenue = venue
            selectedLocationCard.visibility = View.VISIBLE
            selectedLocationName.text = venue.name
            selectedLocationCoords.text = buildSelectedLocationText(venue)
            venueAdapter?.setSelected(venue)
        }

        venueRecycler.layoutManager = LinearLayoutManager(requireContext())
        venueRecycler.adapter = venueAdapter
        venueRecycler.isNestedScrollingEnabled = false

        setDefaultLocation()
        refreshVenueList()
        requestUserLocation()

        sportGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentSportFilter = if (checkedIds.isNotEmpty()) {
                val chip = view.findViewById<Chip>(checkedIds.first())
                cleanSportName(chip.text.toString())
            } else null

            refreshVenueList()

            val currentSelection = selectedVenue
            if (currentSelection != null && !venueMatchesSport(currentSelection, currentSportFilter)) {
                selectedVenue = null
                selectedLocationCard.visibility = View.GONE
                venueAdapter?.setSelected(null)
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

            publishButton.isEnabled = false

            val rawSport = view.findViewById<Chip>(selectedChipId).text.toString()
            val sport = cleanSportName(rawSport)
            val title = titleEdit.text.toString().ifBlank { "$sport Game" }
            val description = descriptionEdit.text.toString()
            val maxPlayers = maxPlayersEdit.text.toString().toIntOrNull() ?: 10
            val venue = selectedVenue!!

            val createPost: (String?) -> Unit = { imageUrl ->
                viewModel.createEvent(
                    sport = sport,
                    title = title,
                    description = description,
                    startTime = System.currentTimeMillis() + 86400000,
                    maxPlayers = maxPlayers,
                    latitude = venue.latitude,
                    longitude = venue.longitude,
                    locationLabel = venue.name,
                    imageUri = imageUrl
                )

                publishButton.isEnabled = true
                Toast.makeText(context, "Event created!", Toast.LENGTH_SHORT).show()

                sportGroup.clearCheck()
                titleEdit.setText("")
                descriptionEdit.setText("")
                maxPlayersEdit.setText("")
                selectedVenue = null
                selectedImageUri = null
                selectedImageView?.visibility = View.GONE
                selectedLocationCard.visibility = View.GONE
                currentSportFilter = null
                venueAdapter?.setSelected(null)
                refreshVenueList()
            }

            val imageUri = selectedImageUri

            if (imageUri == null) {
                createPost(null)
            } else {
                val ref = com.google.firebase.storage.FirebaseStorage.getInstance()
                    .reference
                    .child("event_images/event_${System.currentTimeMillis()}.jpg")

                ref.putFile(imageUri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                        ref.downloadUrl
                    }
                    .addOnSuccessListener { downloadUri ->
                        createPost(downloadUri.toString())
                    }
                    .addOnFailureListener {
                        publishButton.isEnabled = true
                        Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun requestUserLocation() {
        val hasPermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(requireActivity())
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLocation = location
                refreshVenueList()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            requestUserLocation()
        }
    }

    private fun setDefaultLocation() {
        userLocation = Location("default").apply {
            latitude = DEFAULT_LATITUDE
            longitude = DEFAULT_LONGITUDE
        }
    }

    private fun refreshVenueList() {
        val filtered = Venues.all
            .filter { venue -> venueMatchesSport(venue, currentSportFilter) }
            .sortedBy { venue -> distanceInMeters(venue) }
        venueAdapter?.updateList(filtered, userLocation)
    }

    private fun venueMatchesSport(venue: Venue, sport: String?): Boolean {
        if (sport.isNullOrBlank()) return true
        return venue.sports.isEmpty() || venue.sports.any { it.equals(sport, ignoreCase = true) }
    }

    private fun distanceInMeters(venue: Venue): Float {
        val location = userLocation ?: return Float.MAX_VALUE
        val result = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            venue.latitude, venue.longitude, result
        )
        return result[0]
    }

    private fun buildSelectedLocationText(venue: Venue): String {
        val distanceText = formatDistance(distanceInMeters(venue))
        return "%.4f, %.4f · ${venue.area} · $distanceText".format(venue.latitude, venue.longitude)
    }

    private fun cleanSportName(value: String): String {
        return value
            .replace("🏀 ", "").replace("⚽ ", "")
            .replace("🎾 ", "").replace("🏐 ", "").replace("🏃 ", "").trim()
    }

    class VenueAdapter(
        private var items: List<Venue>,
        private val onSelect: (Venue) -> Unit
    ) : RecyclerView.Adapter<VenueAdapter.VH>() {

        private var selectedVenue: Venue? = null
        private var userLocation: Location? = null

        fun setSelected(venue: Venue?) {
            selectedVenue = venue
            notifyDataSetChanged()
        }

        fun updateList(newItems: List<Venue>, location: Location?) {
            items = newItems
            userLocation = location
            notifyDataSetChanged()
        }

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.venueEmoji)
            val name: TextView = v.findViewById(R.id.venueName)
            val area: TextView = v.findViewById(R.id.venueArea)
            val distance: TextView = v.findViewById(R.id.venueDistance)
            val check: TextView = v.findViewById(R.id.venueSelectedIndicator)
            val card: MaterialCardView = v as MaterialCardView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_venue, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val venue = items[position]
            holder.emoji.text = venue.emoji
            holder.name.text = venue.name
            holder.area.text = venue.area
            holder.distance.text = formatDistance(calculateDistance(venue))

            val isSelected = selectedVenue?.name == venue.name
            holder.check.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.card.strokeWidth = if (isSelected) 2 else 0
            holder.card.strokeColor = if (isSelected) 0xFF2A5BD7.toInt() else 0x00000000
            holder.card.setCardBackgroundColor(
                if (isSelected) 0xFFEEF2FF.toInt() else 0xFFFFFFFF.toInt()
            )
            holder.itemView.setOnClickListener { onSelect(venue) }
        }

        override fun getItemCount() = items.size

        private fun calculateDistance(venue: Venue): Float? {
            val location = userLocation ?: return null
            val result = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                venue.latitude, venue.longitude, result
            )
            return result[0]
        }
    }
}

private fun formatDistance(distanceMeters: Float?): String {
    if (distanceMeters == null || distanceMeters == Float.MAX_VALUE) return "Distance unavailable"
    return if (distanceMeters < 1000) "${distanceMeters.toInt()} m away"
    else "%.1f km away".format(distanceMeters / 1000)
}