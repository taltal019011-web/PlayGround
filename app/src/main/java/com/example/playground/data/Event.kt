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
    indices = [
        Index(value = ["hostId"]),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
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
        "firestoreId" to firestoreId,
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
        fun fromFirestoreMap(
            firestoreId: String,
            data: Map<String, Any?>,
            localHostId: Long
        ): Event = Event(
            firestoreId = firestoreId,
            hostId = localHostId,
            sport = data["sport"] as? String ?: "",
            title = data["title"] as? String ?: "",
            description = data["description"] as? String,
            startTime = readLong(data["startTime"]),
            maxPlayers = readLong(data["maxPlayers"]).toInt(),
            imageUri = data["imageUri"] as? String,
            latitude = readDouble(data["latitude"]),
            longitude = readDouble(data["longitude"]),
            locationLabel = data["locationLabel"] as? String ?: "",
            address = data["address"] as? String,
            published = data["published"] as? Boolean ?: true
        )

        private fun readLong(value: Any?): Long {
            return when (value) {
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                is Float -> value.toLong()
                else -> 0L
            }
        }

        private fun readDouble(value: Any?): Double {
            return when (value) {
                is Double -> value
                is Float -> value.toDouble()
                is Long -> value.toDouble()
                is Int -> value.toDouble()
                else -> 0.0
            }
        }
    }
}