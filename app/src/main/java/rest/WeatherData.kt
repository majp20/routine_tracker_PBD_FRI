package rest

import androidx.room.Entity

@Entity
data class WeatherData(
    val temperature: Double,
    val conditions: String,
)
