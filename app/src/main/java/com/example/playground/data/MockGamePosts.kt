package com.example.playground.data

object MockGamePosts {

    val items = listOf(
        GamePost(
            id = "1",
            sport = "Basketball",
            title = "Looking for 2 more players for 3v3",
            date = "Today • 20:00",
            location = "Gordon Beach Basketball Court",
            currentPlayers = 3,
            maxPlayers = 5,
            latitude = 32.0836,
            longitude = 34.7704,
            hostName = "David Cohen",
            postedAgo = "30m ago",
            description = "Good lighting, great court!",
            commentAuthor = "Sarah Levi",
            commentText = "On my way! Be there in 10 mins",
            commentAgo = "15m ago"
        ),
        GamePost(
            id = "2",
            sport = "Football",
            title = "Friendly match in the park",
            date = "Tomorrow • 18:30",
            location = "Yarkon Park",
            currentPlayers = 8,
            maxPlayers = 10,
            latitude = 32.0986,
            longitude = 34.8133,
            hostName = "Omer Ben David",
            postedAgo = "45m ago",
            description = "Bring water and dark shirts.",
            commentAuthor = "Noam Avni",
            commentText = "I am bringing one more player with me",
            commentAgo = "12m ago"
        ),
        GamePost(
            id = "3",
            sport = "Tennis",
            title = "Looking for a doubles partner",
            date = "Friday • 17:00",
            location = "Tel Aviv Tennis Center",
            currentPlayers = 2,
            maxPlayers = 4,
            latitude = 32.1030,
            longitude = 34.7868,
            hostName = "Maya Levi",
            postedAgo = "20m ago",
            description = "Intermediate level, casual game.",
            commentAuthor = "Ronit Shalev",
            commentText = "Sounds good, I can come after work",
            commentAgo = "8m ago"
        ),
        GamePost(
            id = "4",
            sport = "Volleyball",
            title = "Beach volleyball at sunset",
            date = "Saturday • 19:15",
            location = "Frishman Beach",
            currentPlayers = 5,
            maxPlayers = 8,
            latitude = 32.0806,
            longitude = 34.7691,
            hostName = "Neta Bar",
            postedAgo = "1h ago",
            description = "Fun game near the beach, all levels welcome.",
            commentAuthor = "Gal Mor",
            commentText = "Saving this one, maybe joining later",
            commentAgo = "25m ago"
        )
    )
}