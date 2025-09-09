package com.example.good2jog.model

data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind
)

data class Main(
    val temp: Double
)

data class Weather(
    val description: String
)

data class Wind(
    val speed: Double
)
