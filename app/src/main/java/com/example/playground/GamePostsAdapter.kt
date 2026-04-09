package com.example.playground

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.playground.data.GamePost

class GamePostsAdapter(
    private var items: List<GamePost>
) : RecyclerView.Adapter<GamePostsAdapter.GamePostViewHolder>() {

    class GamePostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sportTextView: TextView = itemView.findViewById(R.id.sportTextView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val playersTextView: TextView = itemView.findViewById(R.id.playersTextView)

        fun bind(item: GamePost) {
            sportTextView.text = item.sport
            titleTextView.text = item.title
            dateTextView.text = item.date
            locationTextView.text = item.location
            playersTextView.text = "${item.currentPlayers}/${item.maxPlayers} players"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GamePostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_post, parent, false)
        return GamePostViewHolder(view)
    }

    override fun onBindViewHolder(holder: GamePostViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<GamePost>) {
        items = newItems
        notifyDataSetChanged()
    }
}