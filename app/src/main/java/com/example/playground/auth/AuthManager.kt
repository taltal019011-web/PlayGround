package com.example.playground.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.playground.data.AppDatabase
import com.example.playground.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthManager(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    sealed class AuthResult {
        data class Success(val user: User) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    suspend fun signUp(email: String, password: String): AuthResult {
        if (email.isBlank()) return AuthResult.Error("Email is required")
        if (password.isEmpty()) return AuthResult.Error("Password is required")

        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return AuthResult.Error("Sign up failed")

            val user = saveLocalUser(firebaseUser)
            prefs.edit().putString(KEY_FIREBASE_UID, firebaseUser.uid).apply()
            firebaseAuth.signOut()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        if (email.isBlank()) return AuthResult.Error("Email is required")
        if (password.isEmpty()) return AuthResult.Error("Password is required")

        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return AuthResult.Error("Sign in failed")

            val user = saveLocalUser(firebaseUser)
            prefs.edit().putString(KEY_FIREBASE_UID, firebaseUser.uid).apply()
            firebaseAuth.signOut()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    fun signOut() {
        prefs.edit().remove(KEY_FIREBASE_UID).apply()
    }

    fun getCurrentUser(): User? {
        val uid = prefs.getString(KEY_FIREBASE_UID, null) ?: return null
        return userDao.findByFirebaseUid(uid)
    }

    fun updateUser(user: User) {
        userDao.updateUser(user)
        syncUserToFirestore(user)
    }

    private suspend fun saveLocalUser(firebaseUser: FirebaseUser): User {
        val existing = userDao.findByFirebaseUid(firebaseUser.uid)
        if (existing != null) {
            syncUserToFirestore(existing)
            return existing
        }

        val user = User(
            firebaseUid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: ""
        )
        val id = userDao.insertUser(user)
        val saved = user.copy(id = id)
        syncUserToFirestore(saved)
        return saved
    }

    private fun syncUserToFirestore(user: User) {
        usersCollection.document(user.firebaseUid).set(user.toFirestoreMap())
            .addOnSuccessListener {
                android.util.Log.d("AuthManager", "User synced to Firestore: ${user.firebaseUid}")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuthManager", "Failed to sync user to Firestore", e)
            }
    }

    companion object {
        private const val KEY_FIREBASE_UID = "signed_in_firebase_uid"
    }
}
