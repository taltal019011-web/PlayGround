package com.example.playground.data

data class Venue(
    val name: String,
    val area: String,
    val emoji: String,
    val latitude: Double,
    val longitude: Double,
    val sports: List<String> = emptyList() // empty = all sports
)

object Venues {
    val all = listOf(
        Venue("Gordon Beach Basketball Court", "Tel Aviv - Gordon Beach", "🏀", 32.0864, 34.7681, listOf("Basketball")),
        Venue("Frishman Beach Volleyball", "Tel Aviv - Frishman Beach", "🏐", 32.0842, 34.7672, listOf("Volleyball")),
        Venue("Tel Aviv Tennis Center", "Tel Aviv - Ramat Aviv", "🎾", 32.1030, 34.7868, listOf("Tennis")),
        Venue("Bloomfield Stadium Area", "Jaffa", "⚽", 32.0480, 34.7580, listOf("Football")),
        Venue("Yarkon Park Football Pitch", "Tel Aviv - Yarkon", "⚽", 32.1040, 34.7980, listOf("Football")),
        Venue("Rabin Square Court", "Tel Aviv - Center", "🏀", 32.0810, 34.7810, listOf("Basketball")),
        Venue("Hayarkon Park Multi-Court", "Tel Aviv - Yarkon", "🏃", 32.1020, 34.7950),
        Venue("Jaffa Port Area Court", "Jaffa", "🏃", 32.0530, 34.7520),
        Venue("Shlomo Group Arena surroundings", "Tel Aviv - Yad Eliyahu", "🏀", 32.0657, 34.7789, listOf("Basketball")),
        Venue("HaPoel Tennis Club", "Tel Aviv - South", "🎾", 32.0600, 34.7700, listOf("Tennis")),
        Venue("Bavli Park Court", "Tel Aviv - Bavli", "🏃", 32.1100, 34.8020),
        Venue("Rishon LeZion Sports Park", "Rishon LeZion", "🏃", 31.9730, 34.8050),
        Venue("Herzliya Beach Court", "Herzliya", "🏐", 32.1650, 34.7960, listOf("Volleyball")),
        Venue("Ramat Gan National Park", "Ramat Gan", "⚽", 32.0800, 34.8200, listOf("Football")),
        Venue("Holon Sports Center", "Holon", "🏀", 32.0100, 34.7750, listOf("Basketball"))
    )
}