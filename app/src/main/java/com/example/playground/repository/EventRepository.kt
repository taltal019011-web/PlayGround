package com.example.playground.repository

import android.content.Context
import com.example.playground.data.AppDatabase
import com.example.playground.data.Comment
import com.example.playground.data.Event
import com.example.playground.data.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

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

    suspend fun getAllEvents(): List<Event> {
        return try {
            val snapshot = eventsCollection
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()

            val events = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val hostUid = data["hostFirebaseUid"] as? String ?: return@mapNotNull null
                val localHost = resolveOrCreateUser(hostUid, data)
                Event.fromFirestoreMap(data, localHost.id)
            }

            eventDao.deleteAll()
            events.forEach { eventDao.insertEvent(it) }

            eventDao.getAllEvents()
        } catch (e: Exception) {
            eventDao.getAllEvents()
        }
    }

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

    suspend fun getCommentsForEvent(eventId: Long): List<Comment> {
        val event = eventDao.findById(eventId) ?: return commentDao.getCommentsByEvent(eventId)
        val host = userDao.findById(event.hostId)
        val hostUid = host?.firebaseUid ?: return commentDao.getCommentsByEvent(eventId)

        return try {
            val firestoreEventId = findFirestoreEventId(event, hostUid)
                ?: return commentDao.getCommentsByEvent(eventId)

            val snapshot = commentsCollection
                .whereEqualTo("eventFirestoreId", firestoreEventId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val comments = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val authorUid = data["authorFirebaseUid"] as? String ?: return@mapNotNull null
                val localAuthor = resolveOrCreateUser(authorUid, data)
                Comment.fromFirestoreMap(data, eventId, localAuthor.id)
            }

            commentDao.deleteByEvent(eventId)
            comments.forEach { commentDao.insertComment(it) }

            commentDao.getCommentsByEvent(eventId)
        } catch (e: Exception) {
            commentDao.getCommentsByEvent(eventId)
        }
    }

    private suspend fun findFirestoreEventId(event: Event, hostUid: String): String? {
        val snapshot = eventsCollection
            .whereEqualTo("hostFirebaseUid", hostUid)
            .whereEqualTo("title", event.title)
            .whereEqualTo("startTime", event.startTime)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.id
    }

    private fun resolveOrCreateUser(firebaseUid: String, data: Map<String, Any?>): User {
        val existing = userDao.findByFirebaseUid(firebaseUid)
        if (existing != null) return existing

        val user = User(
            firebaseUid = firebaseUid,
            email = data["email"] as? String ?: "",
            displayName = data["displayName"] as? String ?: ""
        )
        val id = userDao.insertUser(user)
        return user.copy(id = id)
    }

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
