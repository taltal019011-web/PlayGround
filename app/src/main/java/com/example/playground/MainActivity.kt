package com.example.playground

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.playground.auth.AuthManager
import com.example.playground.data.GameComment
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
    private var currentUsername: String = "Player"

    private lateinit var searchInput: TextInputEditText

    private lateinit var chipAllSports: Chip
    private lateinit var chipBasketball: Chip
    private lateinit var chipFootball: Chip
    private lateinit var chipTennis: Chip
    private lateinit var chipVolleyball: Chip

    private lateinit var activeGamesCard: View
    private lateinit var activeGamesCountText: TextView
    private lateinit var closeActiveGamesButton: ImageButton
    private lateinit var toggleActiveGamesButton: ImageButton
    private lateinit var activeGamesContent: View

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
    private lateinit var commentsTitleText: TextView
    private lateinit var commentAuthorText: TextView
    private lateinit var commentBodyText: TextView
    private lateinit var commentAgoText: TextView
    private lateinit var commentInput: TextInputEditText
    private lateinit var sendCommentButton: MaterialButton
    private lateinit var joinButton: MaterialButton
    private lateinit var ratingValueText: TextView

    private lateinit var star1: TextView
    private lateinit var star2: TextView
    private lateinit var star3: TextView
    private lateinit var star4: TextView
    private lateinit var star5: TextView

    private var selectedSport: String = "All"
    private var activeGamesExpanded = true
    private var selectedPostId: String? = null

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

        currentUsername = currentUser.username

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
        toggleActiveGamesButton = findViewById(R.id.toggleActiveGamesButton)
        activeGamesContent = findViewById(R.id.activeGamesContent)

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
        commentsTitleText = findViewById(R.id.commentsTitleText)
        commentAuthorText = findViewById(R.id.commentAuthorText)
        commentBodyText = findViewById(R.id.commentBodyText)
        commentAgoText = findViewById(R.id.commentAgoText)
        commentInput = findViewById(R.id.commentInput)
        sendCommentButton = findViewById(R.id.sendCommentButton)
        joinButton = findViewById(R.id.joinButton)
        ratingValueText = findViewById(R.id.ratingValueText)

        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        star4 = findViewById(R.id.star4)
        star5 = findViewById(R.id.star5)

        welcomeText.text = getString(R.string.welcome_message, currentUsername)

        signOutButton.setOnClickListener {
            authManager.signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        closeActiveGamesButton.setOnClickListener {
            activeGamesCard.visibility = View.GONE
        }

        toggleActiveGamesButton.setOnClickListener {
            activeGamesExpanded = !activeGamesExpanded
            activeGamesContent.visibility = if (activeGamesExpanded) View.VISIBLE else View.GONE
            toggleActiveGamesButton.rotation = if (activeGamesExpanded) 180f else 0f
        }

        closeDetailsButton.setOnClickListener {
            closeDetailsCard()
        }

        joinButton.setOnClickListener {
            joinSelectedGame()
        }

        sendCommentButton.setOnClickListener {
            sendComment()
        }

        setupRatingClicks()
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

    private fun setupRatingClicks() {
        star1.setOnClickListener { rateSelectedGame(1) }
        star2.setOnClickListener { rateSelectedGame(2) }
        star3.setOnClickListener { rateSelectedGame(3) }
        star4.setOnClickListener { rateSelectedGame(4) }
        star5.setOnClickListener { rateSelectedGame(5) }
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
                selectedPostId = post.id
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
            closeDetailsCard()
        }

        applyFilters()
    }

    private fun getFilteredPosts(): List<GamePost> {
        val query = searchInput.text?.toString().orEmpty().trim().lowercase()

        return MockGamePosts.getActiveGames().filter { item ->
            val sportMatches =
                selectedSport == "All" || item.sport.equals(selectedSport, ignoreCase = true)

            val textMatches =
                query.isEmpty() ||
                        item.title.lowercase().contains(query) ||
                        item.sport.lowercase().contains(query) ||
                        item.location.lowercase().contains(query)

            sportMatches && textMatches
        }
    }

    private fun showMarkers(items: List<GamePost>) {
        val map = googleMap ?: return

        map.clear()

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
        val filtered = getFilteredPosts()

        showMarkers(filtered)
        updateActiveGamesCard(filtered)

        val currentSelectedPost = selectedPostId?.let { id ->
            MockGamePosts.findById(id)?.takeIf { it.isActive() }
        }

        if (currentSelectedPost == null) {
            closeDetailsCard()
        } else {
            showDetailsCard(currentSelectedPost)
        }

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

    private fun joinSelectedGame() {
        val postId = selectedPostId ?: return
        val updatedPost = MockGamePosts.joinGame(postId) ?: return

        if (updatedPost.joinedByMe) {
            Toast.makeText(this, "You joined the game", Toast.LENGTH_SHORT).show()
        }

        applyFilters()
        closeDetailsCard()

        if (!updatedPost.isActive()) {
            Toast.makeText(this, "This game is now full", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rateSelectedGame(stars: Int) {
        val postId = selectedPostId ?: return
        val updatedPost = MockGamePosts.rateGame(postId, stars) ?: return
        showDetailsCard(updatedPost)
        Toast.makeText(this, "You rated this court $stars stars", Toast.LENGTH_SHORT).show()
    }

    private fun sendComment() {
        val postId = selectedPostId ?: return
        val text = commentInput.text?.toString().orEmpty()

        if (text.isBlank()) {
            Toast.makeText(this, "Write a comment first", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedPost = MockGamePosts.addComment(
            id = postId,
            author = currentUsername,
            text = text
        ) ?: return

        commentInput.setText("")
        showDetailsCard(updatedPost)
        Toast.makeText(this, "Comment sent", Toast.LENGTH_SHORT).show()
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

        val latestComment = post.comments.firstOrNull()
        bindLatestComment(latestComment, post.comments.size)

        ratingValueText.text = String.format("%.1f", post.averageRating)
        renderStars(post.myRating ?: post.averageRating.toInt().coerceIn(1, 5))

        if (post.isActive()) {
            joinButton.isEnabled = true
            joinButton.text = if (post.joinedByMe) "Already Joined" else "I'm Coming!"
            joinButton.alpha = if (post.joinedByMe) 0.7f else 1f
            joinButton.isClickable = !post.joinedByMe
        } else {
            joinButton.isEnabled = false
            joinButton.text = "Game Full"
            joinButton.alpha = 0.7f
        }
    }

    private fun bindLatestComment(comment: GameComment?, count: Int) {
        commentsTitleText.text = "Comments ($count)"

        if (comment == null) {
            commentAuthorText.text = "No comments yet"
            commentBodyText.text = "Be the first to comment"
            commentAgoText.text = ""
            return
        }

        commentAuthorText.text = comment.author
        commentBodyText.text = comment.text
        commentAgoText.text = comment.postedAgo
    }

    private fun renderStars(selectedStars: Int) {
        val stars = listOf(star1, star2, star3, star4, star5)

        stars.forEachIndexed { index, textView ->
            val filled = index < selectedStars
            textView.text = if (filled) "★" else "☆"
            textView.alpha = if (filled) 1f else 0.45f
        }
    }

    private fun closeDetailsCard() {
        detailsCard.visibility = View.GONE
        selectedPostId = null
    }
}