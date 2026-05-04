package com.example.playground.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.playground.R
import com.example.playground.data.Event
import com.example.playground.data.Venue
import com.example.playground.data.Venues
import com.example.playground.repository.AuthRepository
import com.example.playground.repository.EventRepository
import com.example.playground.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var viewModel: MapViewModel

    private var googleMap: GoogleMap? = null
    private var userLocation: LatLng? = null

    private lateinit var searchInput: TextInputEditText

    private lateinit var chipAllSports: Chip
    private lateinit var chipBasketball: Chip
    private lateinit var chipFootball: Chip
    private lateinit var chipTennis: Chip
    private lateinit var chipVolleyball: Chip

    private lateinit var activeGamesCard: View
    private lateinit var activeGamesCountText: TextView
    private lateinit var closeActiveGamesButton: ImageButton
    private lateinit var toggleActiveButton: ImageButton
    private lateinit var activeContent: View
    private lateinit var nearby1: TextView
    private lateinit var nearby2: TextView
    private lateinit var nearby3: TextView

    private lateinit var detailsCard: View
    private lateinit var closeDetailsButton: ImageButton
    private lateinit var hostAvatarText: TextView
    private lateinit var hostNameText: TextView
    private lateinit var postedAgoText: TextView
    private lateinit var sportChipText: TextView
    private lateinit var joiningCountText: TextView
    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var locationText: TextView
    private lateinit var coordinatesText: TextView
    private lateinit var ratingValueText: TextView
    private lateinit var star1: TextView
    private lateinit var star2: TextView
    private lateinit var star3: TextView
    private lateinit var star4: TextView
    private lateinit var star5: TextView
    private lateinit var commentsHeaderText: TextView
    private lateinit var commentListContainer: LinearLayout
    private lateinit var commentInput: TextInputEditText
    private lateinit var addCommentButton: MaterialButton
    private lateinit var imComingButton: MaterialButton

    private lateinit var weatherContainer: View
    private lateinit var weatherEmojiText: TextView
    private lateinit var weatherDescText: TextView
    private lateinit var weatherDetailsText: TextView

    private lateinit var eventImageView: android.widget.ImageView
    private lateinit var progressIndicator: LinearProgressIndicator

    private var activeExpanded = true

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val DEFAULT_LOCATION = LatLng(32.0853, 34.7818)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val authRepository = AuthRepository.getInstance(requireContext())
        val eventRepository = EventRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(
            this,
            MapViewModel.Factory(authRepository, eventRepository)
        )[MapViewModel::class.java]

        bindViews(view)
        setupActions()
        observeViewModel()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun bindViews(view: View) {
        searchInput = view.findViewById(R.id.searchInput)

        chipAllSports = view.findViewById(R.id.chipAllSports)
        chipBasketball = view.findViewById(R.id.chipBasketball)
        chipFootball = view.findViewById(R.id.chipFootball)
        chipTennis = view.findViewById(R.id.chipTennis)
        chipVolleyball = view.findViewById(R.id.chipVolleyball)

        activeGamesCard = view.findViewById(R.id.activeGamesCard)
        activeGamesCountText = view.findViewById(R.id.activeGamesCountText)
        closeActiveGamesButton = view.findViewById(R.id.closeActiveGamesButton)
        toggleActiveButton = view.findViewById(R.id.toggleActiveButton)
        activeContent = view.findViewById(R.id.activeContent)
        nearby1 = view.findViewById(R.id.nearby1)
        nearby2 = view.findViewById(R.id.nearby2)
        nearby3 = view.findViewById(R.id.nearby3)

        detailsCard = view.findViewById(R.id.detailsCard)
        closeDetailsButton = view.findViewById(R.id.closeDetailsButton)
        hostAvatarText = view.findViewById(R.id.hostAvatarText)
        hostNameText = view.findViewById(R.id.hostNameText)
        postedAgoText = view.findViewById(R.id.postedAgoText)
        sportChipText = view.findViewById(R.id.sportChipText)
        joiningCountText = view.findViewById(R.id.joiningCountText)
        titleText = view.findViewById(R.id.titleText)
        descriptionText = view.findViewById(R.id.descriptionText)
        locationText = view.findViewById(R.id.locationText)
        coordinatesText = view.findViewById(R.id.coordinatesText)
        ratingValueText = view.findViewById(R.id.ratingValueText)
        star1 = view.findViewById(R.id.star1)
        star2 = view.findViewById(R.id.star2)
        star3 = view.findViewById(R.id.star3)
        star4 = view.findViewById(R.id.star4)
        star5 = view.findViewById(R.id.star5)
        commentsHeaderText = view.findViewById(R.id.commentsHeaderText)
        commentListContainer = view.findViewById(R.id.commentListContainer)
        commentInput = view.findViewById(R.id.commentInput)
        addCommentButton = view.findViewById(R.id.addCommentButton)
        imComingButton = view.findViewById(R.id.imComingButton)
        weatherContainer = view.findViewById(R.id.weatherContainer)
        weatherEmojiText = view.findViewById(R.id.weatherEmojiText)
        weatherDescText = view.findViewById(R.id.weatherDescText)
        weatherDetailsText = view.findViewById(R.id.weatherDetailsText)
        eventImageView = view.findViewById(R.id.eventImageView)
        progressIndicator = view.findViewById(R.id.progressIndicator)
    }

    private fun setupActions() {
        closeActiveGamesButton.setOnClickListener {
            activeGamesCard.visibility = View.GONE
        }

        toggleActiveButton.setOnClickListener {
            activeExpanded = !activeExpanded
            activeContent.visibility = if (activeExpanded) View.VISIBLE else View.GONE
            toggleActiveButton.rotation = if (activeExpanded) 180f else 0f
        }

        closeDetailsButton.setOnClickListener {
            closeDetails()
        }

        imComingButton.setOnClickListener {
            viewModel.joinOrUnjoinEvent()
        }

        addCommentButton.setOnClickListener {
            val text = commentInput.text.toString().trim()
            if (text.isNotBlank()) {
                viewModel.postComment(text)
                commentInput.setText("")
            }
        }

        star1.setOnClickListener { viewModel.rateEvent(1) }
        star2.setOnClickListener { viewModel.rateEvent(2) }
        star3.setOnClickListener { viewModel.rateEvent(3) }
        star4.setOnClickListener { viewModel.rateEvent(4) }
        star5.setOnClickListener { viewModel.rateEvent(5) }

        chipAllSports.setOnClickListener { setSport("All") }
        chipBasketball.setOnClickListener { setSport("Basketball") }
        chipFootball.setOnClickListener { setSport("Football") }
        chipTennis.setOnClickListener { setSport("Tennis") }
        chipVolleyball.setOnClickListener { setSport("Volleyball") }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.setSearchQuery(searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }

        updateChipSelection()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.filteredEvents.observe(viewLifecycleOwner) { events ->
            showMarkers(events)
            activeGamesCountText.text = "${events.size} games near you"
        }

        viewModel.eventDetails.observe(viewLifecycleOwner) { details ->
            if (details != null) renderDetails(details)
        }

        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            renderComments(comments)
        }

        viewModel.weather.observe(viewLifecycleOwner) { weather ->
            if (weather != null) {
                weatherEmojiText.text = weather.emoji
                weatherDescText.text = weather.description
                weatherDetailsText.text = "%.1f\u00B0C \u2022 Wind %.0f km/h".format(
                    weather.temperature, weather.windSpeed
                )
                weatherContainer.visibility = View.VISIBLE
            } else {
                weatherContainer.visibility = View.GONE
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 12.5f))

        map.setOnMarkerClickListener { marker ->
            val event = marker.tag as? Event
            if (event != null) {
                detailsCard.visibility = View.VISIBLE
                viewModel.selectEvent(event)
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(event.latitude, event.longitude),
                        13.5f
                    )
                )
            }
            true
        }

        map.setOnMapClickListener {
            closeDetails()
        }

        viewModel.loadEvents()
        enableMyLocation()
    }

    private fun setSport(sport: String) {
        viewModel.setSport(sport)
        updateChipSelection()
    }

    private fun updateChipSelection() {
        val sport = viewModel.selectedSport
        chipAllSports.isChecked = sport == "All"
        chipBasketball.isChecked = sport == "Basketball"
        chipFootball.isChecked = sport == "Football"
        chipTennis.isChecked = sport == "Tennis"
        chipVolleyball.isChecked = sport == "Volleyball"
    }

    private fun showMarkers(events: List<Event>) {
        val map = googleMap ?: return

        map.clear()

        for (event in events) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(event.latitude, event.longitude))
                    .title(event.title)
            )

            marker?.tag = event
        }
    }

    private fun renderDetails(details: MapViewModel.EventDetails) {
        val event = details.event
        val displayRating = details.userRating ?: details.averageRating.toInt().coerceAtLeast(1)

        hostAvatarText.text = getSportEmoji(event.sport)
        hostNameText.text = details.hostDisplayName
        postedAgoText.text = formatTimeAgo(event.startTime)
        sportChipText.text = event.sport
        joiningCountText.text = "${details.participantCount} / ${event.maxPlayers} joining"
        titleText.text = event.title
        descriptionText.text = event.description ?: ""
        locationText.text = "📍 ${event.locationLabel}"
        coordinatesText.text = "%.5f, %.5f".format(event.latitude, event.longitude)

        if (event.imageUri != null) {
            try {
                eventImageView.setImageURI(android.net.Uri.parse(event.imageUri))
                eventImageView.visibility = View.VISIBLE
            } catch (e: Exception) {
                eventImageView.visibility = View.GONE
            }
        } else {
            eventImageView.visibility = View.GONE
        }

        when {
            details.isJoined -> {
                imComingButton.text = "Cancel Join"
                imComingButton.isEnabled = true
                imComingButton.setBackgroundColor(0xFFE0E0E0.toInt())
                imComingButton.setTextColor(0xFF444444.toInt())
            }

            details.participantCount >= event.maxPlayers -> {
                imComingButton.text = "Game Full"
                imComingButton.isEnabled = false
                imComingButton.setBackgroundColor(0xFFCCCCCC.toInt())
                imComingButton.setTextColor(0xFF888888.toInt())
            }

            else -> {
                imComingButton.text = "I'm Coming!"
                imComingButton.isEnabled = true
                imComingButton.setBackgroundColor(0xFF2A5BD7.toInt())
                imComingButton.setTextColor(0xFFFFFFFF.toInt())
            }
        }

        updateRatingViews(displayRating)
    }

    private fun renderComments(comments: List<MapViewModel.CommentWithAuthor>) {
        commentsHeaderText.text = "Comments (${comments.size})"
        commentListContainer.removeAllViews()

        for (item in comments) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 14, 0, 0)
                }
            }

            val avatar = TextView(requireContext()).apply {
                text = "👤"
                textSize = 22f
                gravity = Gravity.CENTER
                setBackgroundColor(0xFFE8F0FE.toInt())
                layoutParams = LinearLayout.LayoutParams(44.dpToPx(), 44.dpToPx())
            }

            val bubble = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFFF5F5F5.toInt())
                setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(12.dpToPx(), 0, 0, 0)
                }
            }

            val authorView = TextView(requireContext()).apply {
                text = item.authorName
                textSize = 15f
                setTextColor(0xFF444444.toInt())
                setTypeface(null, Typeface.BOLD)
            }

            val bodyView = TextView(requireContext()).apply {
                text = item.comment.content
                textSize = 15f
                setTextColor(0xFF555555.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 5.dpToPx(), 0, 0)
                }
            }

            bubble.addView(authorView)
            bubble.addView(bodyView)
            row.addView(avatar)
            row.addView(bubble)
            commentListContainer.addView(row)
        }
    }

    private fun closeDetails() {
        detailsCard.visibility = View.GONE
        viewModel.clearSelectedEvent()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        val map = googleMap ?: return

        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            showNearbyVenues(DEFAULT_LOCATION)
            return
        }

        map.isMyLocationEnabled = true

        val client = LocationServices.getFusedLocationProviderClient(requireActivity())
        client.lastLocation.addOnSuccessListener { location ->
            val current = if (location != null) {
                LatLng(location.latitude, location.longitude)
            } else {
                DEFAULT_LOCATION
            }

            userLocation = current
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 14f))
            showNearbyVenues(current)
        }.addOnFailureListener {
            userLocation = DEFAULT_LOCATION
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 12.5f))
            showNearbyVenues(DEFAULT_LOCATION)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            userLocation = DEFAULT_LOCATION
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 12.5f))
            showNearbyVenues(DEFAULT_LOCATION)
        }
    }

    private fun showNearbyVenues(currentLocation: LatLng) {
        val nearest = getNearbyVenues(currentLocation).take(3)

        nearby1.text = formatVenueRow(nearest.getOrNull(0))
        nearby2.text = formatVenueRow(nearest.getOrNull(1))
        nearby3.text = formatVenueRow(nearest.getOrNull(2))
    }

    private fun formatVenueRow(item: Pair<Venue, Float>?): String {
        if (item == null) return "-"

        val venue = item.first
        val km = item.second / 1000

        return "${venue.emoji} ${venue.name} • %.1f km".format(km)
    }

    private fun getNearbyVenues(currentLocation: LatLng): List<Pair<Venue, Float>> {
        return Venues.all.map { venue ->
            val result = FloatArray(1)

            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                venue.latitude,
                venue.longitude,
                result
            )

            venue to result[0]
        }.sortedBy { it.second }
    }

    private fun updateRatingViews(rating: Int) {
        val stars = listOf(star1, star2, star3, star4, star5)

        stars.forEachIndexed { index, textView ->
            val selected = index < rating
            textView.text = if (selected) "★" else "☆"
            textView.alpha = if (selected) 1f else 0.45f
        }

        ratingValueText.text = "%.1f".format(rating.toFloat())
    }

    private fun getSportEmoji(sport: String): String {
        return when (sport.lowercase()) {
            "basketball" -> "🏀"
            "football" -> "⚽"
            "tennis" -> "🎾"
            "volleyball" -> "🏐"
            else -> "👤"
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

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
