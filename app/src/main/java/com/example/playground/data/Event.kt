package com.example.playground.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["hostId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["hostId"])]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hostId: Long,
    val sport: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val maxPlayers: Int,
    val imageUri: String? = null,
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String,
    val address: String? = null,
    val published: Boolean = true
) {
    fun toFirestoreMap(hostFirebaseUid: String): Map<String, Any?> = mapOf(
        "hostFirebaseUid" to hostFirebaseUid,
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
        fun fromFirestoreMap(data: Map<String, Any?>, localHostId: Long): Event = Event(
            hostId = localHostId,
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
