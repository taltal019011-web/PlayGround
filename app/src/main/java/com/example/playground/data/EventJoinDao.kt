package com.example.playground.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventJoinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(join: EventJoin)

    @Query("DELETE FROM event_joins WHERE eventId = :eventId AND userId = :userId")
    fun delete(eventId: Long, userId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM event_joins WHERE eventId = :eventId AND userId = :userId)")
    fun isJoined(eventId: Long, userId: Long): Boolean

    @Query("SELECT COUNT(*) FROM event_joins WHERE eventId = :eventId")
    fun getJoinCount(eventId: Long): Int

    @Query("DELETE FROM event_joins WHERE eventId = :eventId")
    fun deleteByEvent(eventId: Long)
}
