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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager(requireContext())
        eventRepository = EventRepository.getInstance(requireContext())
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val user = authManager.getCurrentUser()
        if (user != null) {
            val allEvents = eventRepository.getAllEvents()
            val myEvents = allEvents.filter { it.hostId == user.id }
            recyclerView.adapter = MyEventsAdapter(myEvents)
        }
    }

    class MyEventsAdapter(private val items: List<Event>) : RecyclerView.Adapter<MyEventsAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val titleText: TextView = v.findViewById(android.R.id.text1)
            val sportText: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.titleText.text = items[position].title
            holder.sportText.text = items[position].sport
        }

        override fun getItemCount() = items.size
    }
}
