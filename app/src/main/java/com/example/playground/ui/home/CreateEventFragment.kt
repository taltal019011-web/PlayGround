package com.example.playground.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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

class CreateEventFragment : Fragment() {

    private lateinit var viewModel: CreateEventViewModel
    private var selectedVenue: Venue? = null
    private var venueAdapter: VenueAdapter? = null

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

        // Setup venue list
        venueAdapter = VenueAdapter(Venues.all) { venue ->
            selectedVenue = venue
            selectedLocationCard.visibility = View.VISIBLE
            selectedLocationName.text = venue.name
            selectedLocationCoords.text = "%.4f, %.4f · ${venue.area}".format(venue.latitude, venue.longitude)
            venueAdapter?.setSelected(venue)
        }

        venueRecycler.layoutManager = LinearLayoutManager(requireContext())
        venueRecycler.adapter = venueAdapter
        venueRecycler.isNestedScrollingEnabled = false

        // Filter venues when sport chip changes
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
                // Clear selection if selected venue doesn't match new sport
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
                locationLabel = venue.name
            )

            Toast.makeText(context, "Event created!", Toast.LENGTH_SHORT).show()

            // Reset form
            sportGroup.clearCheck()
            titleEdit.setText("")
            descriptionEdit.setText("")
            maxPlayersEdit.setText("")
            selectedVenue = null
            selectedLocationCard.visibility = View.GONE
            venueAdapter?.updateList(Venues.all)
            venueAdapter?.setSelected(null)
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
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_venue, parent, false)
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