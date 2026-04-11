package com.example.playground.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EventDao {
    @Insert
    fun insertEvent(event: Event): Long

    @Update
    fun updateEvent(event: Event)

    @Delete
    fun deleteEvent(event: Event)

    @Query("SELECT * FROM events WHERE id = :id")
    fun findById(id: Long): Event?

    @Query("SELECT * FROM events ORDER BY startTime ASC")
    fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events WHERE hostId = :hostId ORDER BY startTime ASC")
    fun getEventsByHost(hostId: Long): List<Event>
}
