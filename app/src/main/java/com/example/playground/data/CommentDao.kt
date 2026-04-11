package com.example.playground.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CommentDao {
    @Insert
    fun insertComment(comment: Comment): Long

    @Query("SELECT * FROM comments WHERE eventId = :eventId ORDER BY timestamp DESC")
    fun getCommentsByEvent(eventId: Long): List<Comment>
}
