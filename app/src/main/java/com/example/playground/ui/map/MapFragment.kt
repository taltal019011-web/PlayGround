package com.example.playground.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.playground.R
import com.example.playground.auth.AuthManager
import com.example.playground.data.Event
import com.example.playground.repository.EventRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var authManager: AuthManager
    private lateinit var eventRepository: EventRepository
    private var googleMap: GoogleMap? = null

    // Search & filter
    private lateinit var searchInput: TextInputEditText
    private lateinit var chipAllSports: Chip
    private lateinit var chipBasketball: Chip
    private lateinit var chipFootball: Chip
    private lateinit var chipTennis: Chip
    private lateinit var chipVolleyball: Chip
    private var selectedSport: String = "All"

    // Active games card
    private lateinit var activeGamesCard: View
    private lateinit var activeGamesCountText: TextView
    private lateinit var closeActiveGamesButton: ImageButton

    // Details card
    private lateinit var detailsCard: View
    private lateinit var closeDetailsButton: ImageButton
    private lateinit var hostNameText: TextView
    private lateinit var postedAgoText: TextView
    private lateinit var sportChipText: TextView
    private lateinit var joiningCountText: TextView
    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var locationText: TextView
    private lateinit var coordinatesText: TextView
    private lateinit var commentsHeaderText: TextView
    private lateinit var commentListContainer: LinearLayout
    private lateinit var commentInput: TextInputEditText
    private lateinit var addCommentButton: MaterialButton
    private lateinit var imComingButton: MaterialButton

    private lateinit var eventImageView: android.widget.ImageView

    private var allEvents: List<Event> = emptyList()
    private var selectedEvent: Event? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager(requireContext())
        eventRepository = EventRepository.getInstance(requireContext())

        searchInput = view.findViewById(R.id.searchInput)
        chipAllSports = view.findViewById(R.id.chipAllSports)
        chipBasketball = view.findViewById(R.id.chipBasketball)
        chipFootball = view.findViewById(R.id.chipFootball)
        chipTennis = view.findViewById(R.id.chipTennis)
        chipVolleyball = view.findViewById(R.id.chipVolleyball)

        activeGamesCard = view.findViewById(R.id.activeGamesCard)
        activeGamesCountText = view.findViewById(R.id.activeGamesCountText)
        closeActiveGamesButton = view.findViewById(R.id.closeActiveGamesButton)

        detailsCard = view.findViewById(R.id.detailsCard)
        closeDetailsButton = view.findViewById(R.id.closeDetailsButton)
        hostNameText = view.findViewById(R.id.hostNameText)
        postedAgoText = view.findViewById(R.id.postedAgoText)
        sportChipText = view.findViewById(R.id.sportChipText)
        joiningCountText = view.findViewById(R.id.joiningCountText)
        titleText = view.findViewById(R.id.titleText)
        descriptionText = view.findViewById(R.id.descriptionText)
        locationText = view.findViewById(R.id.locationText)
        coordinatesText = view.findViewById(R.id.coordinatesText)
        commentsHeaderText = view.findViewById(R.id.commentsHeaderText)
        commentListContainer = view.findViewById(R.id.commentListContainer)
        commentInput = view.findViewById(R.id.commentInput)
        addCommentButton = view.findViewById(R.id.addCommentButton)
        imComingButton = view.findViewById(R.id.imComingButton)
        eventImageView = view.findViewById(R.id.eventImageView)

        closeActiveGamesButton.setOnClickListener { activeGamesCard.visibility = View.GONE }
        closeDetailsButton.setOnClickListener { detailsCard.visibility = View.GONE }

        imComingButton.setOnClickListener {
            // No join table yet — just update button state visually
            imComingButton.text = "Already Joined"
            imComingButton.isEnabled = false
        }

        addCommentButton.setOnClickListener {
            val event = selectedEvent ?: return@setOnClickListener
            val user = authManager.getCurrentUser() ?: return@setOnClickListener
            val text = commentInput.text.toString().trim()
            if (text.isNotBlank()) {
                eventRepository.postComment(event.id, user.id, text)
                commentInput.setText("")
                refreshComments(event.id)
            }
        }

        setupChips()

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilters(); true
            } else false
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupChips() {
        chipAllSports.setOnClickListener { selectedSport = "All"; updateChipSelection(); applyFilters() }
        chipBasketball.setOnClickListener { selectedSport = "Basketball"; updateChipSelection(); applyFilters() }
        chipFootball.setOnClickListener { selectedSport = "Football"; updateChipSelection(); applyFilters() }
        chipTennis.setOnClickListener { selectedSport = "Tennis"; updateChipSelection(); applyFilters() }
        chipVolleyball.setOnClickListener { selectedSport = "Volleyball"; updateChipSelection(); applyFilters() }
        updateChipSelection()
    }

    private fun updateChipSelection() {
        chipAllSports.isChecked = selectedSport == "All"
        chipBasketball.isChecked = selectedSport == "Basketball"
        chipFootball.isChecked = selectedSport == "Football"
        chipTennis.isChecked = selectedSport == "Tennis"
        chipVolleyball.isChecked = selectedSport == "Volleyball"
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val telAviv = LatLng(32.0853, 34.7818)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(telAviv, 12.5f))

        map.setOnMarkerClickListener { marker ->
            val event = marker.tag as? Event
            if (event != null) {
                showDetails(event)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(event.latitude, event.longitude), 13.5f))
            }
            true
        }

        map.setOnMapClickListener { detailsCard.visibility = View.GONE }

        allEvents = eventRepository.getAllEvents()
        showMarkers(allEvents)
        updateActiveGamesCard(allEvents)
    }

    private fun applyFilters() {
        val query = searchInput.text?.toString().orEmpty().trim().lowercase()
        val filtered = allEvents.filter { event ->
            val sportMatches = selectedSport == "All" || event.sport.equals(selectedSport, ignoreCase = true)
            val textMatches = query.isEmpty() ||
                    event.title.lowercase().contains(query) ||
                    event.sport.lowercase().contains(query) ||
                    event.locationLabel.lowercase().contains(query)
            sportMatches && textMatches
        }
        showMarkers(filtered)
        updateActiveGamesCard(filtered)
        val map = googleMap ?: return
        if (filtered.isNotEmpty()) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(filtered.first().latitude, filtered.first().longitude), 13f))
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(32.0853, 34.7818), 12.5f))
        }
    }

    private fun showMarkers(events: List<Event>) {
        val map = googleMap ?: return
        map.clear()
        detailsCard.visibility = View.GONE
        for (event in events) {
            val marker = map.addMarker(
                MarkerOptions().position(LatLng(event.latitude, event.longitude)).title(event.title)
            )
            marker?.tag = event
        }
    }

    private fun updateActiveGamesCard(events: List<Event>) {
        activeGamesCountText.text = "${events.size} games near you"
    }

    private fun showDetails(event: Event) {
        selectedEvent = event
        detailsCard.visibility = View.VISIBLE

        hostNameText.text = "Host #${event.hostId}"
        postedAgoText.text = formatEventTime(event.startTime)
        sportChipText.text = event.sport
        joiningCountText.text = "Max ${event.maxPlayers} players"
        titleText.text = event.title
        descriptionText.text = event.description ?: ""
        locationText.text = event.locationLabel
        coordinatesText.text = "%.3f, %.3f".format(event.latitude, event.longitude)

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

        imComingButton.text = "I'm Coming!"
        imComingButton.isEnabled = true

        refreshComments(event.id)
    }
    private fun refreshComments(eventId: Long) {
        val comments = eventRepository.getCommentsForEvent(eventId)
        commentsHeaderText.text = "Comments (${comments.size})"
        commentListContainer.removeAllViews()

        for (comment in comments) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 12, 0, 0) }
            }

            // Avatar
            val avatar = TextView(requireContext()).apply {
                text = "👤"
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(40.dpToPx(), 40.dpToPx())
                gravity = android.view.Gravity.CENTER
            }

            // Bubble
            val bubble = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFFF3F3F3.toInt())
                setPadding(12.dpToPx(), 10.dpToPx(), 12.dpToPx(), 10.dpToPx())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(10.dpToPx(), 0, 0, 0) }
            }

            val authorView = TextView(requireContext()).apply {
                text = "User #${comment.authorId}"
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val bodyView = TextView(requireContext()).apply {
                text = comment.content
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 4.dpToPx(), 0, 0) }
            }

            bubble.addView(authorView)
            bubble.addView(bodyView)
            row.addView(avatar)
            row.addView(bubble)
            commentListContainer.addView(row)
        }
    }


    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun formatEventTime(timestamp: Long): String {
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
