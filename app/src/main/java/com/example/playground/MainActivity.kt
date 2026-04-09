package com.example.playground

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.playground.auth.AuthManager
import com.example.playground.data.GamePost
import com.example.playground.data.MockGamePosts
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var authManager: AuthManager
    private var googleMap: GoogleMap? = null

    private lateinit var searchInput: TextInputEditText

    private lateinit var chipAllSports: Chip
    private lateinit var chipBasketball: Chip
    private lateinit var chipFootball: Chip
    private lateinit var chipTennis: Chip
    private lateinit var chipVolleyball: Chip

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

    private val gamePosts: List<GamePost> = MockGamePosts.items
    private var selectedSport: String = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authManager = AuthManager(this)

        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val signOutButton = findViewById<MaterialButton>(R.id.signOutButton)

        searchInput = findViewById(R.id.searchInput)

        chipAllSports = findViewById(R.id.chipAllSports)
        chipBasketball = findViewById(R.id.chipBasketball)
        chipFootball = findViewById(R.id.chipFootball)
        chipTennis = findViewById(R.id.chipTennis)
        chipVolleyball = findViewById(R.id.chipVolleyball)

        activeGamesCard = findViewById(R.id.activeGamesCard)
        activeGamesCountText = findViewById(R.id.activeGamesCountText)
        closeActiveGamesButton = findViewById(R.id.closeActiveGamesButton)

        detailsCard = findViewById(R.id.detailsCard)
        closeDetailsButton = findViewById(R.id.closeDetailsButton)
        hostNameText = findViewById(R.id.hostNameText)
        postedAgoText = findViewById(R.id.postedAgoText)
        sportChipText = findViewById(R.id.sportChipText)
        joiningCountText = findViewById(R.id.joiningCountText)
        titleText = findViewById(R.id.titleText)
        descriptionText = findViewById(R.id.descriptionText)
        locationText = findViewById(R.id.locationText)
        coordinatesText = findViewById(R.id.coordinatesText)
        commentAuthorText = findViewById(R.id.commentAuthorText)
        commentBodyText = findViewById(R.id.commentBodyText)
        commentAgoText = findViewById(R.id.commentAgoText)

        welcomeText.text = getString(R.string.welcome_message, currentUser.username)

        signOutButton.setOnClickListener {
            authManager.signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        closeActiveGamesButton.setOnClickListener {
            activeGamesCard.visibility = View.GONE
        }

        closeDetailsButton.setOnClickListener {
            detailsCard.visibility = View.GONE
        }

        setupChips()

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilters()
                true
            } else {
                false
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupChips() {
        chipAllSports.setOnClickListener {
            selectedSport = "All"
            updateChipSelection()
            applyFilters()
        }

        chipBasketball.setOnClickListener {
            selectedSport = "Basketball"
            updateChipSelection()
            applyFilters()
        }

        chipFootball.setOnClickListener {
            selectedSport = "Football"
            updateChipSelection()
            applyFilters()
        }

        chipTennis.setOnClickListener {
            selectedSport = "Tennis"
            updateChipSelection()
            applyFilters()
        }

        chipVolleyball.setOnClickListener {
            selectedSport = "Volleyball"
            updateChipSelection()
            applyFilters()
        }

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
            val post = marker.tag as? GamePost
            if (post != null) {
                showDetailsCard(post)
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(post.latitude, post.longitude),
                        13.5f
                    )
                )
            }
            true
        }

        map.setOnMapClickListener {
            detailsCard.visibility = View.GONE
        }

        showMarkers(gamePosts)
        updateActiveGamesCard(gamePosts)
    }

    private fun showMarkers(items: List<GamePost>) {
        val map = googleMap ?: return

        map.clear()
        detailsCard.visibility = View.GONE

        for (item in items) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(item.latitude, item.longitude))
                    .title(item.title)
            )
            marker?.tag = item
        }
    }

    private fun applyFilters() {
        val map = googleMap ?: return
        val query = searchInput.text?.toString().orEmpty().trim().lowercase()

        val filtered = gamePosts.filter { item ->
            val sportMatches =
                selectedSport == "All" || item.sport.equals(selectedSport, ignoreCase = true)

            val textMatches =
                query.isEmpty() ||
                        item.title.lowercase().contains(query) ||
                        item.sport.lowercase().contains(query) ||
                        item.location.lowercase().contains(query)

            sportMatches && textMatches
        }

        showMarkers(filtered)
        updateActiveGamesCard(filtered)

        if (filtered.isNotEmpty()) {
            val firstLocation = LatLng(filtered.first().latitude, filtered.first().longitude)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 13f))
        } else {
            val telAviv = LatLng(32.0853, 34.7818)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(telAviv, 12.5f))
        }
    }

    private fun updateActiveGamesCard(items: List<GamePost>) {
        activeGamesCountText.text = "${items.size} games near you"
    }

    private fun showDetailsCard(post: GamePost) {
        detailsCard.visibility = View.VISIBLE

        hostNameText.text = post.hostName
        postedAgoText.text = post.postedAgo
        sportChipText.text = post.sport
        joiningCountText.text = "${post.currentPlayers} joining"
        titleText.text = post.title
        descriptionText.text = post.description
        locationText.text = post.location
        coordinatesText.text = "${post.latitude}, ${post.longitude}"
        commentAuthorText.text = post.commentAuthor
        commentBodyText.text = post.commentText
        commentAgoText.text = post.commentAgo
    }
}