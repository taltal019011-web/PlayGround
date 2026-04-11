package com.example.playground.ui.myposts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.playground.R
import com.example.playground.auth.AuthManager
import com.example.playground.data.Event
import com.example.playground.repository.EventRepository

class MyPostsFragment : Fragment() {

    private lateinit var authManager: AuthManager
    private lateinit var eventRepository: EventRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_posts, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager(requireContext())
        eventRepository = EventRepository.getInstance(requireContext())
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val user = authManager.getCurrentUser()
        if (user != null) {
            val myEvents = eventRepository.getAllEvents().filter { it.hostId == user.id }
            if (myEvents.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
            } else {
                recyclerView.adapter = MyEventsAdapter(myEvents)
            }
        }
    }

    class MyEventsAdapter(private val items: List<Event>) :
        RecyclerView.Adapter<MyEventsAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val sportChip: TextView = v.findViewById(R.id.sportChip)
            val timeText: TextView = v.findViewById(R.id.timeText)
            val titleText: TextView = v.findViewById(R.id.titleText)
            val descriptionText: TextView = v.findViewById(R.id.descriptionText)
            val locationText: TextView = v.findViewById(R.id.locationText)
            val playersText: TextView = v.findViewById(R.id.playersText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_my_event, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val event = items[position]
            holder.sportChip.text = event.sport
            holder.timeText.text = formatTimeAgo(event.startTime)
            holder.titleText.text = event.title
            holder.descriptionText.text = event.description ?: ""
            holder.locationText.text = "📍 ${event.locationLabel}"
            holder.playersText.text = "👥 Max ${event.maxPlayers}"
        }

        override fun getItemCount() = items.size

        private fun formatTimeAgo(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            val minutes = diff / 60_000
            return when {
                minutes < 60 -> "${minutes}m ago"
                minutes < 1440 -> "${minutes / 60}h ago"
                else -> "${minutes / 1440}d ago"
            }
        }
    }
}