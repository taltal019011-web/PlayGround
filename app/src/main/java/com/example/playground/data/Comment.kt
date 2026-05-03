package com.example.playground.data

data class Comment(
    val id: String = "",
    val eventId: String = "",
    val authorId: String = "",
    val content: String = "",
    val timestamp: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "eventId" to eventId,
        "authorId" to authorId,
        "content" to content,
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): Comment = Comment(
            id = id,
            eventId = data["eventId"] as? String ?: "",
            authorId = data["authorId"] as? String ?: "",
            content = data["content"] as? String ?: "",
            timestamp = (data["timestamp"] as? Long) ?: 0L
        )
    }
}
