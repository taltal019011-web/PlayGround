package com.example.playground.repository

import android.content.Context
import com.example.playground.auth.AuthManager
import com.example.playground.data.User

class AuthRepository(context: Context) {

    private val authManager = AuthManager(context)

    suspend fun getCurrentUser(): User? = authManager.getCurrentUser()

    suspend fun updateUser(user: User) = authManager.updateUser(user)

    companion object {
        @Volatile
        private var instance: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(context).also { instance = it }
            }
        }
    }
}
