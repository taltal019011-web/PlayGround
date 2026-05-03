package com.example.playground.data

data class User(
    val id: String = "",
    val firebaseUid: String = "",
    val email: String = "",
    val displayName: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "firebaseUid" to firebaseUid,
        "email" to email,
        "displayName" to displayName
    )

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): User = User(
            id = id,
            firebaseUid = data["firebaseUid"] as? String ?: "",
            email = data["email"] as? String ?: "",
            displayName = data["displayName"] as? String ?: ""
        )
    }
}
