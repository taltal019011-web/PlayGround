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

    fun joinEvent(eventId: Long, userId: Long) {
        val join = EventJoin(eventId = eventId, userId = userId)
        eventJoinDao.insert(join)
        syncJoinToFirestore(join)
    }

    fun unjoinEvent(eventId: Long, userId: Long) {
        eventJoinDao.delete(eventId, userId)
        val user = userDao.findById(userId)
        val event = eventDao.findById(eventId)
        val host = event?.let { userDao.findById(it.hostId) }
        val firestoreKey = "${user?.firebaseUid}_${event?.id}"
        joinsCollection.document(firestoreKey).delete()
    }

    fun isJoined(eventId: Long, userId: Long): Boolean =
        eventJoinDao.isJoined(eventId, userId)

    fun getJoinCount(eventId: Long): Int =
        eventJoinDao.getJoinCount(eventId)

    suspend fun fetchJoinsForEvent(eventId: Long) {
        try {
            val snapshot = joinsCollection
                .whereEqualTo("eventFirestoreId", eventId.toString())
                .get()
                .await()

            eventJoinDao.deleteByEvent(eventId)
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val userUid = data["userFirebaseUid"] as? String ?: continue
                val localUser = resolveOrCreateUser(userUid, data)
                eventJoinDao.insert(EventJoin(eventId = eventId, userId = localUser.id))
            }
        } catch (_: Exception) {
        }
    }

    fun rateEvent(eventId: Long, userId: Long, stars: Int) {
        val rating = EventRating(eventId = eventId, userId = userId, stars = stars)
        eventRatingDao.insert(rating)
        syncRatingToFirestore(rating)
    }

    fun getUserRating(eventId: Long, userId: Long): Int? =
        eventRatingDao.getRating(eventId, userId)

    fun getAverageRating(eventId: Long): Float =
        eventRatingDao.getAverageRating(eventId) ?: 0f

    suspend fun fetchRatingsForEvent(eventId: Long) {
        try {
            val snapshot = ratingsCollection
                .whereEqualTo("eventFirestoreId", eventId.toString())
                .get()
                .await()

            eventRatingDao.deleteByEvent(eventId)
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val userUid = data["userFirebaseUid"] as? String ?: continue
                val stars = (data["stars"] as? Long)?.toInt() ?: continue
                val localUser = resolveOrCreateUser(userUid, data)
                eventRatingDao.insert(EventRating(eventId = eventId, userId = localUser.id, stars = stars))
            }
        } catch (_: Exception) {
        }
    }

    private fun syncJoinToFirestore(join: EventJoin) {
        val user = userDao.findById(join.userId)
        val event = eventDao.findById(join.eventId)
        val host = event?.let { userDao.findById(it.hostId) }
        val userUid = user?.firebaseUid ?: ""
        val firestoreKey = "${userUid}_${join.eventId}"
        joinsCollection.document(firestoreKey)
            .set(join.toFirestoreMap(join.eventId.toString(), userUid))
    }

    private fun syncRatingToFirestore(rating: EventRating) {
        val user = userDao.findById(rating.userId)
        val userUid = user?.firebaseUid ?: ""
        val firestoreKey = "${userUid}_${rating.eventId}"
        ratingsCollection.document(firestoreKey)
            .set(rating.toFirestoreMap(rating.eventId.toString(), userUid))
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
