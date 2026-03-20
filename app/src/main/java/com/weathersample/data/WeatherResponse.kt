package com.weathersample.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    @SerializedName("daily") val daily: DailyWeather?
)

data class CurrentWeather(
    @SerializedName("time") val time: String?,
    @SerializedName("temperature") val temperature: Double,
    @SerializedName("weathercode") val weathercode: Int,
    @SerializedName("windspeed") val windspeed: Double,
    @SerializedName("winddirection") val winddirection: Double?,
    @SerializedName("is_day") val isDay: Int?
)

data class DailyWeather(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerializedName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>?,
    @SerializedName("weathercode") val weathercode: List<Int>?
)
