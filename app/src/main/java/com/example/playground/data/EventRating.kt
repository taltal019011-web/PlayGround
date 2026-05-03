package com.example.playground.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "event_ratings",
    primaryKeys = ["eventId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("userId")]
)
data class EventRating(
    val eventId: Long,
    val userId: Long,
    val stars: Int
) {
    fun toFirestoreMap(
        eventFirestoreId: String,
        userFirebaseUid: String
    ): Map<String, Any> = mapOf(
        "eventFirestoreId" to eventFirestoreId,
        "userFirebaseUid" to userFirebaseUid,
        "stars" to stars
    )
}
