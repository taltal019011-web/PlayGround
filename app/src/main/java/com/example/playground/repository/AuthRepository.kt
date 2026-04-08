package com.example.playground.repository

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import com.example.playground.data.AppDatabase
import com.example.playground.data.User
import java.security.MessageDigest
import java.security.SecureRandom

class AuthRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    sealed class AuthResult {
        data class Success(val user: User) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    fun signUp(username: String, password: String): AuthResult {
        if (username.isBlank()) return AuthResult.Error("Username is required")
        if (password.isEmpty()) return AuthResult.Error("Password is required")
        if (userDao.findByUsername(username) != null) {
            return AuthResult.Error("Username is already taken")
        }

        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        val user = User(username = username, passwordHash = hash, salt = salt)
        val id = try {
            userDao.insertUser(user)
        } catch (_: SQLiteConstraintException) {
            return AuthResult.Error("Username is already taken")
        }
        val savedUser = user.copy(id = id)

        prefs.edit().putLong(KEY_USER_ID, id).apply()
        return AuthResult.Success(savedUser)
    }

    fun signIn(username: String, password: String): AuthResult {
        if (username.isBlank()) return AuthResult.Error("Username is required")
        if (password.isEmpty()) return AuthResult.Error("Password is required")

        val user = userDao.findByUsername(username)
            ?: return AuthResult.Error("Invalid username or password")

        val hash = hashPassword(password, user.salt)
        if (hash != user.passwordHash) {
            return AuthResult.Error("Invalid username or password")
        }

        prefs.edit().putLong(KEY_USER_ID, user.id).apply()
        return AuthResult.Success(user)
    }

    fun signOut() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    fun getCurrentUser(): User? {
        val userId = prefs.getLong(KEY_USER_ID, -1L)
        if (userId == -1L) return null
        return userDao.findById(userId)
    }

    companion object {
        private const val KEY_USER_ID = "signed_in_user_id"

        @Volatile
        private var instance: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(context).also { instance = it }
            }
        }

        fun generateSalt(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun hashPassword(password: String, salt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("$salt$password".toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
