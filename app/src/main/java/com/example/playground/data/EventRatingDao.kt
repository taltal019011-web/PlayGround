package com.example.playground.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventRatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rating: EventRating)

    @Query("SELECT stars FROM event_ratings WHERE eventId = :eventId AND userId = :userId LIMIT 1")
    fun getRating(eventId: Long, userId: Long): Int?

    @Query("SELECT AVG(stars) FROM event_ratings WHERE eventId = :eventId")
    fun getAverageRating(eventId: Long): Float?

    @Query("DELETE FROM event_ratings WHERE eventId = :eventId")
    fun deleteByEvent(eventId: Long)
}
