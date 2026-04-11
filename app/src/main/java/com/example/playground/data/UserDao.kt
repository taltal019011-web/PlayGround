package com.example.playground.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert
    fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    fun findByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun findById(id: Long): User?

    @androidx.room.Update
    fun updateUser(user: User)
}
