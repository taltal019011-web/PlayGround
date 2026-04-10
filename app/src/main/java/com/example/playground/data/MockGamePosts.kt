package com.example.playground.data

object MockGamePosts {

    private val items = mutableListOf(
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
            averageRating = 4.5f,
            ratingCount = 8,
            comments = mutableListOf(
                GameComment(
                    author = "Sarah Levi",
                    text = "On my way! Be there in 10 mins",
                    postedAgo = "15m ago"
                )
            )
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
            averageRating = 4.1f,
            ratingCount = 5,
            comments = mutableListOf(
                GameComment(
                    author = "Noam Avni",
                    text = "I am bringing one more player with me",
                    postedAgo = "12m ago"
                )
            )
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
            averageRating = 4.7f,
            ratingCount = 3,
            comments = mutableListOf(
                GameComment(
                    author = "Ronit Shalev",
                    text = "Sounds good, I can come after work",
                    postedAgo = "8m ago"
                )
            )
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
            averageRating = 4.3f,
            ratingCount = 6,
            comments = mutableListOf(
                GameComment(
                    author = "Gal Mor",
                    text = "Saving this one, maybe joining later",
                    postedAgo = "25m ago"
                )
            )
        )
    )

    fun getPublishedUpcomingGames(): List<GamePost> {
        return items.filter { it.isPublished && it.isUpcoming }
    }

    fun getActiveGames(): List<GamePost> {
        return items.filter { it.isActive() }
    }

    fun findById(id: String): GamePost? {
        return items.find { it.id == id }
    }

    fun joinGame(id: String): GamePost? {
        val post = findById(id) ?: return null

        if (!post.isActive()) return post
        if (post.joinedByMe) return post

        post.currentPlayers += 1
        post.joinedByMe = true
        return post
    }

    fun rateGame(id: String, stars: Int): GamePost? {
        val post = findById(id) ?: return null
        val safeStars = stars.coerceIn(1, 5)

        val previousRating = post.myRating
        if (previousRating == null) {
            val total = (post.averageRating * post.ratingCount) + safeStars
            post.ratingCount += 1
            post.averageRating = total / post.ratingCount
        } else {
            val total = (post.averageRating * post.ratingCount) - previousRating + safeStars
            post.averageRating = total / post.ratingCount
        }

        post.myRating = safeStars
        return post
    }

    fun addComment(id: String, author: String, text: String): GamePost? {
        val post = findById(id) ?: return null
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return post

        post.comments.add(
            0,
            GameComment(
                author = author,
                text = trimmedText,
                postedAgo = "now"
            )
        )

        return post
    }
}