package com.example.playground.repository

import android.content.Context
import com.example.playground.data.AppDatabase
import com.example.playground.data.Comment
import com.example.playground.data.Event
import com.example.playground.data.EventJoin
import com.example.playground.data.EventRating
import com.example.playground.data.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class EventRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val eventDao = db.eventDao()
    private val commentDao = db.commentDao()
    private val userDao = db.userDao()
    private val eventJoinDao = db.eventJoinDao()
    private val eventRatingDao = db.eventRatingDao()

    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")
    private val commentsCollection = firestore.collection("comments")
    private val joinsCollection = firestore.collection("event_joins")
    private val ratingsCollection = firestore.collection("event_ratings")

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

        val firestoreId = event.firestoreId.ifBlank {
            eventsCollection.document().id
        }

        val eventWithFirestoreId = event.copy(firestoreId = firestoreId)
        val localId = eventDao.insertEvent(eventWithFirestoreId)
        val savedEvent = eventWithFirestoreId.copy(id = localId)

        syncEventToFirestore(savedEvent)

        return EventResult.Success(localId)
    }

    suspend fun getAllEvents(): List<Event> {
        return try {
            val snapshot = eventsCollection
                .whereEqualTo("published", true)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()

            val events = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val hostUid = data["hostFirebaseUid"] as? String ?: return@mapNotNull null
                val localHost = resolveOrCreateUser(hostUid, data)

                Event.fromFirestoreMap(
                    firestoreId = doc.id,
                    data = data,
                    localHostId = localHost.id
                )
            }

            eventDao.deleteAll()
            events.forEach { event ->
                eventDao.insertEvent(event)
            }

            eventDao.getAllEvents()
        } catch (e: Exception) {
            eventDao.getAllEvents()
        }
    }

    fun getEventById(id: Long): Event? {
        return eventDao.findById(id)
    }

    fun updateEvent(hostId: Long, event: Event): EventResult {
        if (event.hostId != hostId) return EventResult.Error("Unauthorized")
        if (event.sport.isBlank()) return EventResult.Error("Sport is required")
        if (event.title.isBlank()) return EventResult.Error("Title is required")
        if (event.maxPlayers < 1) return EventResult.Error("Max players must be at least 1")

        val firestoreId = event.firestoreId.ifBlank {
            eventsCollection.document().id
        }

        val updatedEvent = event.copy(firestoreId = firestoreId)
        eventDao.updateEvent(updatedEvent)
        syncEventToFirestore(updatedEvent)

        return EventResult.Success(updatedEvent.id)
    }

    fun deleteEvent(hostId: Long, event: Event): EventResult {
        if (event.hostId != hostId) return EventResult.Error("Unauthorized")

        eventDao.deleteEvent(event)

        if (event.firestoreId.isNotBlank()) {
            eventsCollection.document(event.firestoreId).delete()
        }

        return EventResult.Success(event.id)
    }

    fun postComment(eventId: Long, authorId: Long, content: String): EventResult {
        if (content.isBlank()) return EventResult.Error("Comment cannot be empty")

        val event = eventDao.findById(eventId)
            ?: return EventResult.Error("Event not found")

        val comment = Comment(
            eventId = eventId,
            authorId = authorId,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        val localCommentId = commentDao.insertComment(comment)
        val savedComment = comment.copy(id = localCommentId)

        syncCommentToFirestore(savedComment, event)

        return EventResult.Success(localCommentId)
    }

    suspend fun getCommentsForEvent(eventId: Long): List<Comment> {
        val event = eventDao.findById(eventId)
            ?: return commentDao.getCommentsByEvent(eventId)

        if (event.firestoreId.isBlank()) {
            return commentDao.getCommentsByEvent(eventId)
        }

        return try {
            val snapshot = commentsCollection
                .whereEqualTo("eventFirestoreId", event.firestoreId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val comments = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val authorUid = data["authorFirebaseUid"] as? String ?: return@mapNotNull null
                val localAuthor = resolveOrCreateUser(authorUid, data)

                Comment.fromFirestoreMap(
                    data = data,
                    localEventId = eventId,
                    localAuthorId = localAuthor.id
                )
            }

            commentDao.deleteByEvent(eventId)
            comments.forEach { comment ->
                commentDao.insertComment(comment)
            }

            commentDao.getCommentsByEvent(eventId)
        } catch (e: Exception) {
            commentDao.getCommentsByEvent(eventId)
        }
    }

    fun joinEvent(eventId: Long, userId: Long) {
        val event = eventDao.findById(eventId) ?: return
        val user = userDao.findById(userId) ?: return

        val join = EventJoin(eventId = eventId, userId = userId)
        eventJoinDao.insert(join)

        val eventFirestoreId = event.firestoreId.ifBlank { return }
        val userUid = user.firebaseUid
        val firestoreKey = "${userUid}_$eventFirestoreId"

        joinsCollection.document(firestoreKey)
            .set(join.toFirestoreMap(eventFirestoreId, userUid))
    }

    fun unjoinEvent(eventId: Long, userId: Long) {
        eventJoinDao.delete(eventId, userId)

        val event = eventDao.findById(eventId) ?: return
        val user = userDao.findById(userId) ?: return

        val eventFirestoreId = event.firestoreId.ifBlank { return }
        val firestoreKey = "${user.firebaseUid}_$eventFirestoreId"

        joinsCollection.document(firestoreKey).delete()
    }

    fun isJoined(eventId: Long, userId: Long): Boolean {
        return eventJoinDao.isJoined(eventId, userId)
    }

    fun getJoinCount(eventId: Long): Int {
        return eventJoinDao.getJoinCount(eventId)
    }

    fun rateEvent(eventId: Long, userId: Long, stars: Int) {
        val event = eventDao.findById(eventId) ?: return
        val user = userDao.findById(userId) ?: return

        val safeStars = stars.coerceIn(1, 5)
        val rating = EventRating(eventId = eventId, userId = userId, stars = safeStars)

        eventRatingDao.insert(rating)

        val eventFirestoreId = event.firestoreId.ifBlank { return }
        val firestoreKey = "${user.firebaseUid}_$eventFirestoreId"

        ratingsCollection.document(firestoreKey)
            .set(rating.toFirestoreMap(eventFirestoreId, user.firebaseUid))
    }

    fun getUserRating(eventId: Long, userId: Long): Int? {
        return eventRatingDao.getRating(eventId, userId)
    }

    fun getAverageRating(eventId: Long): Float {
        return eventRatingDao.getAverageRating(eventId) ?: 0f
    }

    private fun syncEventToFirestore(event: Event) {
        val host = userDao.findById(event.hostId)
        val hostUid = host?.firebaseUid ?: return

        val data = event.toFirestoreMap(hostUid).toMutableMap()
        data["email"] = host.email
        data["displayName"] = host.displayName

        eventsCollection.document(event.firestoreId).set(data)
    }

    private fun syncCommentToFirestore(comment: Comment, event: Event) {
        val author = userDao.findById(comment.authorId) ?: return
        val eventFirestoreId = event.firestoreId.ifBlank { return }

        val data = comment.toFirestoreMap(
            eventFirestoreId = eventFirestoreId,
            authorFirebaseUid = author.firebaseUid
        ).toMutableMap()

        data["email"] = author.email
        data["displayName"] = author.displayName

        commentsCollection.document().set(data)
    }

    private fun resolveOrCreateUser(firebaseUid: String, data: Map<String, Any?>): User {
        val existing = userDao.findByFirebaseUid(firebaseUid)
        if (existing != null) return existing

        val user = User(
            firebaseUid = firebaseUid,
            email = data["email"] as? String ?: "",
            displayName = data["displayName"] as? String ?: ""
        )

        val localId = userDao.insertUser(user)
        return user.copy(id = localId)
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