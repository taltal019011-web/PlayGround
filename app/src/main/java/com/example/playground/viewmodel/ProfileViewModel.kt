package com.example.playground.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.playground.data.User
import com.example.playground.repository.AuthRepository

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isEditing = MutableLiveData(false)
    val isEditing: LiveData<Boolean> = _isEditing

    private val _profileSaved = MutableLiveData<Boolean>()
    val profileSaved: LiveData<Boolean> = _profileSaved

    fun loadUser() {
        _user.value = authRepository.getCurrentUser()
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun saveProfile(newName: String, profileImageUrl: String?) {
        val current = _user.value ?: return
        if (newName.isBlank()) return

        val updated = current.copy(
            displayName = newName,
            profileImageUrl = profileImageUrl ?: current.profileImageUrl
        )

        authRepository.updateUser(updated)
        _user.value = updated
        _isEditing.value = false
        _profileSaved.value = true
    }

    fun signOut() {
        authRepository.signOut()
    }

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(authRepository) as T
        }
    }
}