package com.example.playground.auth

import org.junit.Assert.*
import org.junit.Test

class AuthManagerTest {

    @Test
    fun authResult_success_holdsUser() {
        val user = com.example.playground.data.User(
            id = 1L,
            firebaseUid = "test-uid",
            email = "test@example.com",
            displayName = "Test User"
        )
        val result = AuthManager.AuthResult.Success(user)
        assertEquals(user, result.user)
    }

    @Test
    fun authResult_error_holdsMessage() {
        val result = AuthManager.AuthResult.Error("Something went wrong")
        assertEquals("Something went wrong", result.message)
    }
}
