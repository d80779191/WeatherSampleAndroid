package com.weathersample.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    @SerializedName("daily") val daily: DailyWeather?
)

data class CurrentWeather(
    @SerializedName("temperature") val temperature: Double,
    @SerializedName("weathercode") val weathercode: Int,
    @SerializedName("windspeed") val windspeed: Double
)

data class DailyWeather(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double>
)
