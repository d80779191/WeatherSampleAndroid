package com.weathersample.data

object ApiConstants {
    const val WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode&timezone=Asia%%2FTaipei"
}
