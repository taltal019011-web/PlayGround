package com.example.playground.repository

import android.content.Context
import com.example.playground.data.AppDatabase
import com.example.playground.data.Comment
import com.example.playground.data.Event
import com.google.firebase.firestore.FirebaseFirestore

class EventRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val eventDao = db.eventDao()
    private val commentDao = db.commentDao()
    private val userDao = db.userDao()

    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")
    private val commentsCollection = firestore.collection("comments")

    sealed class EventResult {
        data class Success(val eventId: Long) : EventResult()
        data class Error(val message: String) : EventResult()
    }

    fun createEvent(event: Event): EventResult {
        if (event.sport.isBlank()) return EventResult.Error("Sport is required")
        if (event.title.isBlank()) return EventResult.Error("Title is required")
        if (event.maxPlayers < 1) return EventResult.Error("Max players must be at least 1")
        if (event.startTime <= System.currentTimeMillis()) return EventResult.Error("Start time must be in the future")
        if (event.latitude == 0.0 && event.longitude == 0.0) return EventResult.Error("Valid location is required")

        val id = eventDao.insertEvent(event)
        syncEventToFirestore(event.copy(id = id))
        return EventResult.Success(id)
    }

    fun getAllEvents(): List<Event> = eventDao.getAllEvents()

    fun getEventById(id: Long): Event? = eventDao.findById(id)

    fun updateEvent(hostId: Long, event: Event): EventResult {
        if (event.hostId != hostId) return EventResult.Error("Unauthorized")

        if (event.sport.isBlank()) return EventResult.Error("Sport is required")
        if (event.title.isBlank()) return EventResult.Error("Title is required")
        if (event.maxPlayers < 1) return EventResult.Error("Max players must be at least 1")

        eventDao.updateEvent(event)
        syncEventToFirestore(event)
        return EventResult.Success(event.id)
    }

    fun deleteEvent(hostId: Long, event: Event): EventResult {
        if (event.hostId != hostId) return EventResult.Error("Unauthorized")
        eventDao.deleteEvent(event)
        eventsCollection.document(event.id.toString()).delete()
        return EventResult.Success(event.id)
    }

    fun postComment(eventId: Long, authorId: Long, content: String): EventResult {
        val comment = Comment(
            eventId = eventId,
            authorId = authorId,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        val id = commentDao.insertComment(comment)
        syncCommentToFirestore(comment.copy(id = id))
        return EventResult.Success(id)
    }

    fun getCommentsForEvent(eventId: Long): List<Comment> =
        commentDao.getCommentsByEvent(eventId)

    private fun syncEventToFirestore(event: Event) {
        val host = userDao.findById(event.hostId)
        val hostUid = host?.firebaseUid ?: ""
        eventsCollection.document(event.id.toString()).set(event.toFirestoreMap(hostUid))
    }

    private fun syncCommentToFirestore(comment: Comment) {
        val author = userDao.findById(comment.authorId)
        val authorUid = author?.firebaseUid ?: ""
        commentsCollection.document(comment.id.toString())
            .set(comment.toFirestoreMap(comment.eventId.toString(), authorUid))
    }

    companion object {
        @Volatile
        private var instance: EventRepository? = null

        fun getInstance(context: Context): EventRepository {
            return instance ?: synchronized(this) {
                instance ?: EventRepository(context).also { instance = it }
            }
        }
    }
}
