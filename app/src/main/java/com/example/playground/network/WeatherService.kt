package com.example.playground.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather
)

data class CurrentWeather(
    val temperature: Double,
    @SerializedName("windspeed") val windSpeed: Double,
    @SerializedName("weathercode") val weatherCode: Int
)

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): WeatherResponse
}

object WeatherService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: WeatherApi = retrofit.create(WeatherApi::class.java)

    fun weatherDescription(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snowfall"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    fun weatherEmoji(code: Int): String = when (code) {
        0 -> "\u2600\uFE0F"
        1, 2 -> "\u26C5"
        3 -> "\u2601\uFE0F"
        45, 48 -> "\uD83C\uDF2B\uFE0F"
        51, 53, 55 -> "\uD83C\uDF26\uFE0F"
        61, 63, 65, 80, 81, 82 -> "\uD83C\uDF27\uFE0F"
        66, 67 -> "\u2744\uFE0F\uD83C\uDF27\uFE0F"
        71, 73, 75, 77, 85, 86 -> "\u2744\uFE0F"
        95, 96, 99 -> "\u26A1"
        else -> "\uD83C\uDF24\uFE0F"
    }
}
