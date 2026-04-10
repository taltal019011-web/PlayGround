package com.example.playground.data

data class GamePost(
    val id: String,
    val sport: String,
    val title: String,
    val date: String,
    val location: String,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val latitude: Double,
    val longitude: Double,
    val hostName: String,
    val postedAgo: String,
    val description: String,
    val commentAuthor: String,
    val commentText: String,
    val commentAgo: String
)