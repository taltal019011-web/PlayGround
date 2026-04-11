package com.example.playground.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playground.data.Event
import com.example.playground.repository.AuthRepository
import com.example.playground.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateEventViewModel(
    private val authRepository: AuthRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _createEventState = MutableStateFlow<CreateEventState>(CreateEventState.Idle)
    val createEventState: StateFlow<CreateEventState> = _createEventState.asStateFlow()

    fun createEvent(
        sport: String,
        title: String,
        description: String?,
        startTime: Long,
        maxPlayers: Int,
        latitude: Double,
        longitude: Double,
        locationLabel: String
    ) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _createEventState.value = CreateEventState.Error("User not logged in")
            return
        }

        _createEventState.value = CreateEventState.Loading

        val event = Event(
            hostId = currentUser.id,
            sport = sport,
            title = title,
            description = description,
            startTime = startTime,
            maxPlayers = maxPlayers,
            latitude = latitude,
            longitude = longitude,
            locationLabel = locationLabel
        )

        viewModelScope.launch {
            val result = eventRepository.createEvent(event)
            _createEventState.value = when (result) {
                is EventRepository.EventResult.Success -> CreateEventState.Success(result.eventId)
                is EventRepository.EventResult.Error -> CreateEventState.Error(result.message)
            }
        }
    }

    sealed class CreateEventState {
        object Idle : CreateEventState()
        object Loading : CreateEventState()
        data class Success(val eventId: Long) : CreateEventState()
        data class Error(val message: String) : CreateEventState()
    }
}
