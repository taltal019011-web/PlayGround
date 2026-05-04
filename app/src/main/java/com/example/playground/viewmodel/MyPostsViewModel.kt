package com.example.playground.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.Event
import com.example.playground.repository.AuthRepository
import com.example.playground.repository.EventRepository
import kotlinx.coroutines.launch

class MyPostsViewModel(
    private val authRepository: AuthRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _myEvents = MutableLiveData<List<Event>>(emptyList())
    val myEvents: LiveData<List<Event>> = _myEvents

    private val _isEmpty = MutableLiveData(true)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResult = MutableLiveData<OperationResult?>()
    val operationResult: LiveData<OperationResult?> = _operationResult

    fun loadEvents() {
        val user = authRepository.getCurrentUser() ?: return
        _isLoading.value = true
        viewModelScope.launch {
            val all = eventRepository.getEventsByHost(user.id)
            _myEvents.value = all
            _isEmpty.value = all.isEmpty()
            _isLoading.value = false
        }
    }

    fun deleteEvent(event: Event) {
        val user = authRepository.getCurrentUser() ?: return
        when (val result = eventRepository.deleteEvent(user.id, event)) {
            is EventRepository.EventResult.Success -> {
                _operationResult.value = OperationResult.Success("Post deleted")
                loadLocalEvents()
            }
            is EventRepository.EventResult.Error -> {
                _operationResult.value = OperationResult.Error(result.message)
            }
        }
    }

    fun updateEvent(event: Event) {
        val user = authRepository.getCurrentUser() ?: return
        when (val result = eventRepository.updateEvent(user.id, event)) {
            is EventRepository.EventResult.Success -> {
                _operationResult.value = OperationResult.Success("Post updated")
                loadLocalEvents()
            }
            is EventRepository.EventResult.Error -> {
                _operationResult.value = OperationResult.Error(result.message)
            }
        }
    }

    private fun loadLocalEvents() {
        val user = authRepository.getCurrentUser() ?: return
        val mine = eventRepository.getLocalEventsByHost(user.id)
        _myEvents.value = mine
        _isEmpty.value = mine.isEmpty()
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val eventRepository: EventRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyPostsViewModel(authRepository, eventRepository) as T
        }
    }
}
