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

            if (events.isNotEmpty()) {
                eventDao.deleteAll()
                events.forEach { eventDao.insertEvent(it) }
            }

            eventDao.getAllEvents()
        } catch (e: Exception) {
            eventDao.getAllEvents()
        }
    }

    fun getEventById(id: Long): Event? = eventDao.findById(id)

    suspend fun getEventsByHost(hostId: Long): List<Event> {
        getAllEvents()
        return eventDao.getEventsByHost(hostId)
    }

    fun getLocalEventsByHost(hostId: Long): List<Event> {
        return eventDao.getEventsByHost(hostId)
    }

    fun updateEvent(hostId: Long, event: Event): EventResult {
        if (event.hostId != hostId) return EventResult.Error("Unauthorized")

        if (event.sport.isBlank()) return EventResult.Error("Sport is required")
        if (event.title.isBlank()) return EventResult.Error("Title is required")
        if (event.maxPlayers < 1) return EventResult.Error("Max players must be at least 1")

        eventDao.updateEvent(event)
        syncEventUpdate(event)
        return EventResult.Success(event.id)
    }

    fun deleteEvent(hostId: Long, event: Event): EventResult {
        if (event.hostId != hostId) return EventResult.Error("Unauthorized")
        eventDao.deleteEvent(event)
        deleteEventFromFirestore(event)
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
            .addOnFailureListener { e ->
                android.util.Log.e("EventRepository", "Failed to sync event to Firestore", e)
            }
    }

    private fun syncEventUpdate(event: Event) {
        val host = userDao.findById(event.hostId)
        val hostUid = host?.firebaseUid ?: ""
        val data = event.toFirestoreMap(hostUid)

        eventsCollection
            .whereEqualTo("hostFirebaseUid", hostUid)
            .whereEqualTo("startTime", event.startTime)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    snapshot.documents.forEach { it.reference.set(data) }
                } else {
                    eventsCollection.document(event.id.toString()).set(data)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("EventRepository", "Failed to sync event update to Firestore", e)
            }
    }

    private fun deleteEventFromFirestore(event: Event) {
        val host = userDao.findById(event.hostId)
        val hostUid = host?.firebaseUid ?: ""

        eventsCollection
            .whereEqualTo("hostFirebaseUid", hostUid)
            .whereEqualTo("startTime", event.startTime)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { it.reference.delete() }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("EventRepository", "Failed to delete event from Firestore", e)
            }
    }

    private fun syncCommentToFirestore(comment: Comment) {
        val author = userDao.findById(comment.authorId)
        val authorUid = author?.firebaseUid ?: ""
        commentsCollection.document(comment.id.toString())
            .set(comment.toFirestoreMap(comment.eventId.toString(), authorUid))
            .addOnFailureListener { e ->
                android.util.Log.e("EventRepository", "Failed to sync comment to Firestore", e)
            }
    }

    fun joinEvent(eventId: Long, userId: Long) {
        val join = EventJoin(eventId = eventId, userId = userId)
        eventJoinDao.insert(join)
        syncJoinToFirestore(eventId, userId)
    }

    fun unjoinEvent(eventId: Long, userId: Long) {
        eventJoinDao.delete(eventId, userId)
        val user = userDao.findById(userId)
        val event = eventDao.findById(eventId)
        val host = event?.let { userDao.findById(it.hostId) }
        val hostUid = host?.firebaseUid ?: ""
        val userUid = user?.firebaseUid ?: ""

        if (event != null) {
            val firestoreKey = "${userUid}_${hostUid}_${event.startTime}"
            joinsCollection.document(firestoreKey).delete()
        }
    }

    fun isJoined(eventId: Long, userId: Long): Boolean =
        eventJoinDao.isJoined(eventId, userId)

    fun getJoinCount(eventId: Long): Int =
        eventJoinDao.getJoinCount(eventId)

    suspend fun fetchJoinsForEvent(eventId: Long) {
        val event = eventDao.findById(eventId) ?: return
        val host = userDao.findById(event.hostId)
        val hostUid = host?.firebaseUid ?: return

        try {
            val snapshot = joinsCollection
                .whereEqualTo("hostFirebaseUid", hostUid)
                .whereEqualTo("startTime", event.startTime)
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
        syncRatingToFirestore(eventId, userId, stars)
    }

    fun getUserRating(eventId: Long, userId: Long): Int? =
        eventRatingDao.getRating(eventId, userId)

    fun getAverageRating(eventId: Long): Float =
        eventRatingDao.getAverageRating(eventId) ?: 0f

    suspend fun fetchRatingsForEvent(eventId: Long) {
        val event = eventDao.findById(eventId) ?: return
        val host = userDao.findById(event.hostId)
        val hostUid = host?.firebaseUid ?: return

        try {
            val snapshot = ratingsCollection
                .whereEqualTo("hostFirebaseUid", hostUid)
                .whereEqualTo("startTime", event.startTime)
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

    private fun syncJoinToFirestore(eventId: Long, userId: Long) {
        val user = userDao.findById(userId)
        val event = eventDao.findById(eventId)
        val host = event?.let { userDao.findById(it.hostId) }
        val userUid = user?.firebaseUid ?: ""
        val hostUid = host?.firebaseUid ?: ""
        val startTime = event?.startTime ?: 0L

        val firestoreKey = "${userUid}_${hostUid}_${startTime}"
        val data = mapOf(
            "userFirebaseUid" to userUid,
            "hostFirebaseUid" to hostUid,
            "startTime" to startTime
        )
        joinsCollection.document(firestoreKey).set(data)
    }

    private fun syncRatingToFirestore(eventId: Long, userId: Long, stars: Int) {
        val user = userDao.findById(userId)
        val event = eventDao.findById(eventId)
        val host = event?.let { userDao.findById(it.hostId) }
        val userUid = user?.firebaseUid ?: ""
        val hostUid = host?.firebaseUid ?: ""
        val startTime = event?.startTime ?: 0L

        val firestoreKey = "${userUid}_${hostUid}_${startTime}"
        val data = mapOf(
            "userFirebaseUid" to userUid,
            "hostFirebaseUid" to hostUid,
            "startTime" to startTime,
            "stars" to stars
        )
        ratingsCollection.document(firestoreKey).set(data)
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
