package com.example.playground.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.playground.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthManager(context: Context) {

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

            val user = saveFirestoreUser(firebaseUser)
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

            val user = saveFirestoreUser(firebaseUser)
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

    suspend fun getCurrentUser(): User? {
        val uid = prefs.getString(KEY_FIREBASE_UID, null) ?: return null
        return try {
            val snapshot = usersCollection
                .whereEqualTo("firebaseUid", uid)
                .get()
                .await()
            val doc = snapshot.documents.firstOrNull() ?: return null
            User.fromMap(doc.id, doc.data ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUser(user: User) {
        usersCollection.document(user.id).update(user.toMap()).await()
    }

    private suspend fun saveFirestoreUser(firebaseUser: FirebaseUser): User {
        val snapshot = usersCollection
            .whereEqualTo("firebaseUid", firebaseUser.uid)
            .get()
            .await()

        val existingDoc = snapshot.documents.firstOrNull()
        if (existingDoc != null) {
            return User.fromMap(existingDoc.id, existingDoc.data ?: emptyMap())
        }

        val user = User(
            firebaseUid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: ""
        )
        val docRef = usersCollection.add(user.toMap()).await()
        return user.copy(id = docRef.id)
    }

    companion object {
        private const val KEY_FIREBASE_UID = "signed_in_firebase_uid"
    }
}
