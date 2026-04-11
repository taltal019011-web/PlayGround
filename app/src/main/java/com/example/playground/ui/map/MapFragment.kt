package com.example.playground.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.playground.R
import com.example.playground.auth.AuthManager
import com.example.playground.data.Comment
import com.example.playground.data.Event
import com.example.playground.repository.EventRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var authManager: AuthManager
    private lateinit var eventRepository: EventRepository
    private var googleMap: GoogleMap? = null

    private lateinit var detailsCard: View
    private lateinit var closeDetailsButton: ImageButton
    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var commentInput: TextInputEditText
    private lateinit var addCommentButton: MaterialButton
    private lateinit var commentListText: TextView

    private var selectedEvent: Event? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager(requireContext())
        eventRepository = EventRepository.getInstance(requireContext())

        detailsCard = view.findViewById(R.id.detailsCard)
        closeDetailsButton = view.findViewById(R.id.closeDetailsButton)
        titleText = view.findViewById(R.id.titleText)
        descriptionText = view.findViewById(R.id.descriptionText)
        commentInput = view.findViewById(R.id.commentInput) // Assuming we add this to your layout
        addCommentButton = view.findViewById(R.id.addCommentButton) // Assuming we add this
        commentListText = view.findViewById(R.id.commentListText) // Assuming we add this

        closeDetailsButton.setOnClickListener { detailsCard.visibility = View.GONE }

        addCommentButton.setOnClickListener {
            val event = selectedEvent ?: return@setOnClickListener
            val user = authManager.getCurrentUser() ?: return@setOnClickListener
            val comment = commentInput.text.toString()
            
            if (comment.isNotBlank()) {
                eventRepository.postComment(event.id, user.id, comment)
                commentInput.setText("")
                refreshComments(event.id)
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        refreshEvents()
    }

    private fun refreshEvents() {
        val events = eventRepository.getAllEvents()
        googleMap?.clear()
        for (event in events) {
            val marker = googleMap?.addMarker(MarkerOptions().position(LatLng(event.latitude, event.longitude)).title(event.title))
            marker?.tag = event
        }
        googleMap?.setOnMarkerClickListener { marker ->
            selectedEvent = marker.tag as? Event
            selectedEvent?.let { showDetails(it) }
            true
        }
    }

    private fun showDetails(event: Event) {
        selectedEvent = event
        detailsCard.visibility = View.VISIBLE
        titleText.text = event.title
        descriptionText.text = event.description
        refreshComments(event.id)
    }

    private fun refreshComments(eventId: Long) {
        val comments = eventRepository.getCommentsForEvent(eventId)
        commentListText.text = comments.joinToString("
") { "${it.content} (${it.timestamp})" }
    }
}
