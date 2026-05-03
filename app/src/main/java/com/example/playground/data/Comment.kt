package com.example.playground.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comments",
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
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("authorId")]
)
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val authorId: Long,
    val content: String,
    val timestamp: Long
) {
    fun toFirestoreMap(
        eventFirestoreId: String,
        authorFirebaseUid: String
    ): Map<String, Any> = mapOf(
        "eventFirestoreId" to eventFirestoreId,
        "authorFirebaseUid" to authorFirebaseUid,
        "content" to content,
        "timestamp" to timestamp
    )

    companion object {
        fun fromFirestoreMap(
            data: Map<String, Any?>,
            localEventId: Long,
            localAuthorId: Long
        ): Comment = Comment(
            eventId = localEventId,
            authorId = localAuthorId,
            content = data["content"] as? String ?: "",
            timestamp = readLong(data["timestamp"])
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
    }
}