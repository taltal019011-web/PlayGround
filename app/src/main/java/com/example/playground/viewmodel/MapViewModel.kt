package com.example.playground.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.Comment
import com.example.playground.data.Event
import com.example.playground.data.User
import com.example.playground.network.WeatherService
import com.example.playground.repository.AuthRepository
import com.example.playground.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel(
    private val authRepository: AuthRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _allEvents = MutableLiveData<List<Event>>(emptyList())

    private val _filteredEvents = MutableLiveData<List<Event>>(emptyList())
    val filteredEvents: LiveData<List<Event>> = _filteredEvents

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedEvent = MutableLiveData<Event?>(null)
    val selectedEvent: LiveData<Event?> = _selectedEvent

    private val _eventDetails = MutableLiveData<EventDetails?>(null)
    val eventDetails: LiveData<EventDetails?> = _eventDetails

    private val _comments = MutableLiveData<List<CommentWithAuthor>>(emptyList())
    val comments: LiveData<List<CommentWithAuthor>> = _comments

    private val _weather = MutableLiveData<WeatherInfo?>(null)
    val weather: LiveData<WeatherInfo?> = _weather

    var selectedSport: String = "All"
        private set

    var searchQuery: String = ""
        private set

    fun loadEvents() {
        _isLoading.value = true
        viewModelScope.launch {
            _allEvents.value = eventRepository.getAllEvents()
            applyFilters()
            _isLoading.value = false
        }
    }

    fun setSport(sport: String) {
        selectedSport = sport
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }

    private fun applyFilters() {
        val events = _allEvents.value.orEmpty()
        val query = searchQuery.trim().lowercase()

        _filteredEvents.value = events.filter { event ->
            val hasCoordinates = event.latitude != 0.0 && event.longitude != 0.0
            val sportMatches = selectedSport == "All" || event.sport.equals(selectedSport, ignoreCase = true)
            val textMatches = query.isEmpty() ||
                    event.title.lowercase().contains(query) ||
                    event.sport.lowercase().contains(query) ||
                    event.locationLabel.lowercase().contains(query)
            hasCoordinates && sportMatches && textMatches
        }
    }

    fun selectEvent(event: Event) {
        _selectedEvent.value = event
        _weather.value = null
        refreshEventDetails(event)
        viewModelScope.launch {
            eventRepository.fetchJoinsForEvent(event.id)
            eventRepository.fetchRatingsForEvent(event.id)
            refreshComments(event.id)
            if (_selectedEvent.value?.id == event.id) {
                refreshEventDetails(event)
            }
        }
        fetchWeather(event.latitude, event.longitude)
    }

    fun clearSelectedEvent() {
        _selectedEvent.value = null
        _eventDetails.value = null
        _comments.value = emptyList()
        _weather.value = null
    }

    private fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    WeatherService.api.getWeather(latitude, longitude)
                }
                val cw = response.currentWeather
                _weather.value = WeatherInfo(
                    temperature = cw.temperature,
                    windSpeed = cw.windSpeed,
                    description = WeatherService.weatherDescription(cw.weatherCode),
                    emoji = WeatherService.weatherEmoji(cw.weatherCode)
                )
            } catch (_: Exception) {
                _weather.value = null
            }
        }
    }

    fun joinOrUnjoinEvent() {
        val event = _selectedEvent.value ?: return
        val currentUser = authRepository.getCurrentUser() ?: return

        if (eventRepository.isJoined(event.id, currentUser.id)) {
            eventRepository.unjoinEvent(event.id, currentUser.id)
        } else {
            val joinCount = eventRepository.getJoinCount(event.id)
            if (joinCount >= event.maxPlayers) {
                refreshEventDetails(event)
                return
            }
            eventRepository.joinEvent(event.id, currentUser.id)
        }
        refreshEventDetails(event)
    }

    fun rateEvent(stars: Int) {
        val event = _selectedEvent.value ?: return
        val currentUser = authRepository.getCurrentUser() ?: return
        eventRepository.rateEvent(event.id, currentUser.id, stars.coerceIn(1, 5))
        refreshEventDetails(event)
    }

    fun postComment(text: String) {
        val event = _selectedEvent.value ?: return
        val user = authRepository.getCurrentUser() ?: return
        if (text.isBlank()) return

        eventRepository.postComment(event.id, user.id, text)
        viewModelScope.launch {
            refreshComments(event.id)
        }
    }

    fun getCurrentUser(): User? = authRepository.getCurrentUser()

    private fun refreshEventDetails(event: Event) {
        val currentUser = authRepository.getCurrentUser()
        val participantCount = eventRepository.getJoinCount(event.id)
        val isJoined = currentUser != null && eventRepository.isJoined(event.id, currentUser.id)
        val userRating = currentUser?.let { eventRepository.getUserRating(event.id, it.id) }
        val avgRating = eventRepository.getAverageRating(event.id)

        _eventDetails.value = EventDetails(
            event = event,
            participantCount = participantCount,
            isJoined = isJoined,
            userRating = userRating,
            averageRating = avgRating,
            hostDisplayName = resolveHostName(event, currentUser)
        )
    }

    private fun resolveHostName(event: Event, currentUser: User?): String {
        return if (currentUser != null && currentUser.id == event.hostId) {
            currentUser.displayName.ifEmpty { currentUser.email }
        } else {
            "Host #${event.hostId}"
        }
    }

    private suspend fun refreshComments(eventId: Long) {
        val rawComments = eventRepository.getCommentsForEvent(eventId)
        val currentUser = authRepository.getCurrentUser()

        _comments.value = rawComments.map { comment ->
            val authorName = if (currentUser != null && currentUser.id == comment.authorId) {
                currentUser.displayName.ifEmpty { currentUser.email }
            } else {
                "User ${comment.authorId}"
            }
            CommentWithAuthor(comment, authorName)
        }
    }

    data class EventDetails(
        val event: Event,
        val participantCount: Int,
        val isJoined: Boolean,
        val userRating: Int?,
        val averageRating: Float,
        val hostDisplayName: String
    )

    data class CommentWithAuthor(
        val comment: Comment,
        val authorName: String
    )

    data class WeatherInfo(
        val temperature: Double,
        val windSpeed: Double,
        val description: String,
        val emoji: String
    )

    class Factory(
        private val authRepository: AuthRepository,
        private val eventRepository: EventRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MapViewModel(authRepository, eventRepository) as T
        }
    }
}
