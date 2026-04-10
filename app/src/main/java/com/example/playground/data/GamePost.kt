package com.example.playground.data

data class GamePost(
    val id: String,
    val sport: String,
    val title: String,
    val date: String,
    val location: String,
    var currentPlayers: Int,
    val maxPlayers: Int,
    val latitude: Double,
    val longitude: Double,
    val hostName: String,
    val postedAgo: String,
    val description: String,
    val isPublished: Boolean = true,
    val isUpcoming: Boolean = true,
    var averageRating: Float = 4.5f,
    var ratingCount: Int = 1,
    var myRating: Int? = null,
    var joinedByMe: Boolean = false,
    val comments: MutableList<GameComment> = mutableListOf()
) {
    fun isActive(): Boolean {
        return isPublished && isUpcoming && currentPlayers < maxPlayers
    }

    fun remainingSpots(): Int {
        return (maxPlayers - currentPlayers).coerceAtLeast(0)
    }
}