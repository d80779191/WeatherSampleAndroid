package com.weathersample.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object WeatherApiClient {
    private val client = OkHttpClient()

    suspend fun getWeather(latitude: Double, longitude: Double) = withContext(Dispatchers.IO) {
        val url = String.format(ApiConstants.WEATHER_API_URL, latitude, longitude)
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("WeatherApiClient", "Response failed: ${response.code}")
                    return@withContext
                }
                response.body?.string()?.let { bodyString ->
                    Log.e("WeatherApiClient", "Response JSON: $bodyString")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
