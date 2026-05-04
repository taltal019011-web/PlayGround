package com.example.playground.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["firebaseUid"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firebaseUid: String,
    val email: String,
    val displayName: String = "",
    val profileImageUrl: String? = null
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "firebaseUid" to firebaseUid,
        "email" to email,
        "displayName" to displayName,
        "profileImageUrl" to profileImageUrl
    )

    companion object {
        fun fromFirestoreMap(data: Map<String, Any?>): User = User(
            firebaseUid = data["firebaseUid"] as? String ?: "",
            email = data["email"] as? String ?: "",
            displayName = data["displayName"] as? String ?: "",
            profileImageUrl = data["profileImageUrl"] as? String
        )
    }
}