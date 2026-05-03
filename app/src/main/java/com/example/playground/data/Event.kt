package com.example.playground.data

data class Event(
    val id: String = "",
    val hostId: String = "",
    val sport: String = "",
    val title: String = "",
    val description: String? = null,
    val startTime: Long = 0L,
    val maxPlayers: Int = 0,
    val imageUri: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationLabel: String = "",
    val address: String? = null,
    val published: Boolean = true
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "hostId" to hostId,
        "sport" to sport,
        "title" to title,
        "description" to description,
        "startTime" to startTime,
        "maxPlayers" to maxPlayers,
        "imageUri" to imageUri,
        "latitude" to latitude,
        "longitude" to longitude,
        "locationLabel" to locationLabel,
        "address" to address,
        "published" to published
    )

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): Event = Event(
            id = id,
            hostId = data["hostId"] as? String ?: "",
            sport = data["sport"] as? String ?: "",
            title = data["title"] as? String ?: "",
            description = data["description"] as? String,
            startTime = (data["startTime"] as? Long) ?: 0L,
            maxPlayers = (data["maxPlayers"] as? Long)?.toInt() ?: 0,
            imageUri = data["imageUri"] as? String,
            latitude = (data["latitude"] as? Double) ?: 0.0,
            longitude = (data["longitude"] as? Double) ?: 0.0,
            locationLabel = data["locationLabel"] as? String ?: "",
            address = data["address"] as? String,
            published = (data["published"] as? Boolean) ?: true
        )
    }
}
