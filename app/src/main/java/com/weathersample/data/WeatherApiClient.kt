package com.weathersample.data

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 負責處理所有的外部網路與 API 請求。
 * 使用 OkHttp 實作網路傳輸層，設定了超時（Timeout）限制，並以 Gson 解析 Open-Meteo 回傳的 JSON 資料。
 */
object WeatherApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        
    private val gson = Gson()

    /**
     * 發起非同步的網路查詢，並在失敗時拋出具有明確中文說明的錯誤例外 (Exception)。
     */
    suspend fun getWeather(latitude: Double, longitude: Double): WeatherResponse? = withContext(Dispatchers.IO) {
        val url = String.format(ApiConstants.WEATHER_API_URL, latitude, longitude)
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("WeatherApiClient", "Response failed: ${response.code}")
                if (response.code in 400..499) {
                    throw Exception("Client Error (HTTP ${response.code}). 請確認請求參數。")
                } else if (response.code in 500..599) {
                    throw Exception("Server Error (HTTP ${response.code}). 伺服器目前發生問題。")
                } else {
                    throw Exception("API Error (HTTP ${response.code}).")
                }
            }
            response.body?.string()?.let { bodyString ->
                Log.e("WeatherApiClient", "Response JSON: $bodyString")
                return@withContext gson.fromJson(bodyString, WeatherResponse::class.java)
            }
        }
        return@withContext null
    }
}
