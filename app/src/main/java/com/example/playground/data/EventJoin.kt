package com.example.playground.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "event_joins",
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
data class EventJoin(
    val eventId: Long,
    val userId: Long
) {
    fun toFirestoreMap(
        eventFirestoreId: String,
        userFirebaseUid: String
    ): Map<String, Any> = mapOf(
        "eventFirestoreId" to eventFirestoreId,
        "userFirebaseUid" to userFirebaseUid
    )
}
