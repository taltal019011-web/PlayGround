package com.example.playground.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var authManager: AuthManager
    private lateinit var eventRepository: EventRepository
    private var googleMap: GoogleMap? = null

    private lateinit var searchInput: TextInputEditText
    private lateinit var chipGroup: ChipGroup

    private lateinit var activeGamesCard: View
    private lateinit var activeGamesCountText: TextView
    private lateinit var closeActiveGamesButton: ImageButton

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
    private lateinit var commentAuthorText: TextView
    private lateinit var commentBodyText: TextView
    private lateinit var commentAgoText: TextView

    private var allEvents: List<Event> = emptyList()
    private var selectedSport: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager(requireContext())
        eventRepository = EventRepository.getInstance(requireContext())

        val welcomeText = view.findViewById<TextView>(R.id.welcomeText)
        val currentUser = authManager.getCurrentUser()
        welcomeText.text = """Welcome, ${currentUser?.username ?: "User"}!"""

        searchInput = view.findViewById(R.id.searchInput)
        chipGroup = view.findViewById(R.id.chipGroup)

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
        commentAuthorText = view.findViewById(R.id.commentAuthorText)
        commentBodyText = view.findViewById(R.id.commentBodyText)
        commentAgoText = view.findViewById(R.id.commentAgoText)

        closeActiveGamesButton.setOnClickListener {
            activeGamesCard.visibility = View.GONE
        }

        closeDetailsButton.setOnClickListener {
            detailsCard.visibility = View.GONE
        }

        setupChips(view)

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilters()
                true
            } else {
                false
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupChips(view: View) {
        val chipAll = view.findViewById<Chip>(R.id.chipAllSports)
        val chipBasket = view.findViewById<Chip>(R.id.chipBasketball)
        val chipFoot = view.findViewById<Chip>(R.id.chipFootball)
        val chipTennis = view.findViewById<Chip>(R.id.chipTennis)
        val chipVolley = view.findViewById<Chip>(R.id.chipVolleyball)

        chipAll.setOnClickListener { selectedSport = "All"; applyFilters() }
        chipBasket.setOnClickListener { selectedSport = "Basketball"; applyFilters() }
        chipFoot.setOnClickListener { selectedSport = "Football"; applyFilters() }
        chipTennis.setOnClickListener { selectedSport = "Tennis"; applyFilters() }
        chipVolley.setOnClickListener { selectedSport = "Volleyball"; applyFilters() }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val telAviv = LatLng(32.0853, 34.7818)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(telAviv, 12.5f))

        map.setOnMarkerClickListener { marker ->
            val event = marker.tag as? Event
            if (event != null) {
                showDetailsCard(event)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(event.latitude, event.longitude), 13.5f))
            }
            true
        }

        map.setOnMapClickListener { detailsCard.visibility = View.GONE }

        refreshEvents()
    }

    private fun refreshEvents() {
        allEvents = eventRepository.getAllEvents()
        applyFilters()
    }

    private fun applyFilters() {
        val query = searchInput.text?.toString().orEmpty().trim().lowercase()
        val filtered = allEvents.filter { item ->
            val sportMatches = selectedSport == "All" || item.sport.equals(selectedSport, ignoreCase = true)
            val textMatches = query.isEmpty() || item.title.lowercase().contains(query) || item.sport.lowercase().contains(query)
            sportMatches && textMatches
        }
        showMarkers(filtered)
        updateActiveGamesCard(filtered)
    }

    private fun showMarkers(items: List<Event>) {
        val map = googleMap ?: return
        map.clear()
        for (item in items) {
            val marker = map.addMarker(MarkerOptions().position(LatLng(item.latitude, item.longitude)).title(item.title))
            marker?.tag = item
        }
    }

    private fun updateActiveGamesCard(items: List<Event>) {
        activeGamesCountText.text = """${items.size} games near you"""
    }

    private fun showDetailsCard(event: Event) {
        detailsCard.visibility = View.VISIBLE
        titleText.text = event.title
        descriptionText.text = event.description
        sportChipText.text = event.sport
        locationText.text = event.locationLabel
        joiningCountText.text = """0/${event.maxPlayers}"""
        coordinatesText.text = """${event.latitude}, ${event.longitude}"""
        
        // Host name and comments would require further DB queries or Relations
        hostNameText.text = """Host ID: ${event.hostId}""" 
    }
}
