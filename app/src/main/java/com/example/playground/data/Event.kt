package com.example.playground.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["hostId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["hostId"])]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hostId: Long,
    val sport: String,
    val title: String,
    val description: String?,
    val startTime: Long, // Timestamp
    val maxPlayers: Int,
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String,
    val published: Boolean = true
)
