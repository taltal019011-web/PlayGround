package com.example.playground.repository

import android.content.Context
import com.example.playground.data.Comment
import com.example.playground.data.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class EventRepository(context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")
    private val commentsCollection = firestore.collection("comments")

    sealed class EventResult {
        data class Success(val eventId: String) : EventResult()
        data class Error(val message: String) : EventResult()
    }

    suspend fun createEvent(event: Event): EventResult {
        if (event.sport.isBlank()) return EventResult.Error("Sport is required")
        if (event.title.isBlank()) return EventResult.Error("Title is required")
        if (event.maxPlayers < 1) return EventResult.Error("Max players must be at least 1")
        if (event.startTime <= System.currentTimeMillis()) return EventResult.Error("Start time must be in the future")
        if (event.latitude == 0.0 && event.longitude == 0.0) return EventResult.Error("Valid location is required")

        return try {
            val docRef = eventsCollection.add(event.toMap()).await()
            EventResult.Success(docRef.id)
        } catch (e: Exception) {
            EventResult.Error(e.message ?: "Failed to create event")
        }
    }

    suspend fun getAllEvents(): List<Event> {
        return try {
            val snapshot = eventsCollection
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Event.fromMap(doc.id, it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getEventById(id: String): Event? {
        return try {
            val doc = eventsCollection.document(id).get().await()
            doc.data?.let { Event.fromMap(doc.id, it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateEvent(hostId: String, event: Event): EventResult {
        if (event.hostId != hostId) return EventResult.Error("Unauthorized")
        if (event.sport.isBlank()) return EventResult.Error("Sport is required")
        if (event.title.isBlank()) return EventResult.Error("Title is required")
        if (event.maxPlayers < 1) return EventResult.Error("Max players must be at least 1")

        return try {
            eventsCollection.document(event.id).update(event.toMap()).await()
            EventResult.Success(event.id)
        } catch (e: Exception) {
            EventResult.Error(e.message ?: "Failed to update event")
        }
    }

    suspend fun deleteEvent(hostId: String, event: Event): EventResult {
        if (event.hostId != hostId) return EventResult.Error("Unauthorized")

        return try {
            eventsCollection.document(event.id).delete().await()
            EventResult.Success(event.id)
        } catch (e: Exception) {
            EventResult.Error(e.message ?: "Failed to delete event")
        }
    }

    suspend fun postComment(eventId: String, authorId: String, content: String): EventResult {
        val comment = Comment(
            eventId = eventId,
            authorId = authorId,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        return try {
            val docRef = commentsCollection.add(comment.toMap()).await()
            EventResult.Success(docRef.id)
        } catch (e: Exception) {
            EventResult.Error(e.message ?: "Failed to post comment")
        }
    }

    suspend fun getCommentsForEvent(eventId: String): List<Comment> {
        return try {
            val snapshot = commentsCollection
                .whereEqualTo("eventId", eventId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Comment.fromMap(doc.id, it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
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
