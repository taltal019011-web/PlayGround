package com.example.playground.auth

import org.junit.Assert.*
import org.junit.Test

class AuthManagerTest {

    @Test
    fun hashPassword_returnsDifferentHashForDifferentSalts() {
        val hash1 = AuthManager.hashPassword("password", "salt1")
        val hash2 = AuthManager.hashPassword("password", "salt2")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun hashPassword_returnsSameHashForSameInput() {
        val hash1 = AuthManager.hashPassword("password", "salt1")
        val hash2 = AuthManager.hashPassword("password", "salt1")
        assertEquals(hash1, hash2)
    }

    @Test
    fun hashPassword_returnsHexString() {
        val hash = AuthManager.hashPassword("test", "salt")
        assertTrue(hash.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun generateSalt_returnsNonEmptyString() {
        val salt = AuthManager.generateSalt()
        assertTrue(salt.isNotEmpty())
    }

    @Test
    fun generateSalt_returnsDifferentValues() {
        val salt1 = AuthManager.generateSalt()
        val salt2 = AuthManager.generateSalt()
        assertNotEquals(salt1, salt2)
    }
}
