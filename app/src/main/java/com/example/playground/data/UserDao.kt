package com.example.playground.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE firebaseUid = :uid LIMIT 1")
    fun findByFirebaseUid(uid: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun findById(id: Long): User?

    @androidx.room.Update
    fun updateUser(user: User)
}
