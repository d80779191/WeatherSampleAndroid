package com.weathersample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weathersample.data.WeatherApiClient
import android.content.res.Configuration
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

data class WeatherDetails(
    val temperature: String,
    val windSpeed: String,
    val description: String,
    val pop: String
)

data class DailyForecast(
    val formattedDate: String,
    val weatherDescription: String,
    val temperatureMax: Float,
    val temperatureMin: Float,
    val temperatureRange: String
)

/**
 * 處理天氣應用程式所有業務邏輯與狀態管理的 ViewModel。
 * 負責向 WeatherApiClient 請求資料，並透過 LiveData 將狀態（包含 UI 顯示資料、讀取進度、錯誤訊息）綁定到畫面上。
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _text1 = MutableLiveData("TextView 1")
    val text1: LiveData<String> = _text1

    private var activeTab = 0
    private val _activeTabLiveData = MutableLiveData<Int>(0)
    val activeTabLiveData: LiveData<Int> = _activeTabLiveData
    private var currentCityStr: String = ""
    private var selectedCityStr: String = ""
    private var currentTempStr: String = ""
    private var selectedTempStr: String = ""
    private var currentDescStr: String = ""
    private var selectedDescStr: String = ""
    private var currentColorRes: Int = R.color.sky_blue
    private var selectedColorRes: Int = R.color.sky_blue

    private val _cityName = MutableLiveData<String>()
    val cityName: LiveData<String> = _cityName

    private val _temperature = MutableLiveData<String>()
    val temperature: LiveData<String> = _temperature

    private val _weatherDesc = MutableLiveData<String>()
    val weatherDesc: LiveData<String> = _weatherDesc

    private val _headerColorRes = MutableLiveData<Int>(R.color.sky_blue)
    val headerColorRes: LiveData<Int> = _headerColorRes

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }

    fun setActiveTab(position: Int) {
        activeTab = position
        _activeTabLiveData.value = position
        updateCityNameDisplay()
    }

    private fun updateCityNameDisplay() {
        _cityName.value = if (activeTab == 0) currentCityStr else selectedCityStr
        _temperature.value = if (activeTab == 0) currentTempStr else selectedTempStr
        _weatherDesc.value = if (activeTab == 0) currentDescStr else selectedDescStr
        _headerColorRes.value = if (activeTab == 0) currentColorRes else selectedColorRes
    }

    private fun getWeatherDescription(code: Int): String {
        val app = getApplication<Application>()
        return when (code) {
            0 -> app.getString(R.string.weather_clear)
            1, 2, 3 -> app.getString(R.string.weather_cloudy)
            45, 48 -> app.getString(R.string.weather_foggy)
            51, 53, 55, 56, 57 -> app.getString(R.string.weather_drizzle)
            61, 63, 65, 66, 67, 80, 81, 82 -> app.getString(R.string.weather_rainy)
            71, 73, 75, 77, 85, 86 -> app.getString(R.string.weather_snowy)
            95, 96, 99 -> app.getString(R.string.weather_thunderstorm)
            else -> app.getString(R.string.weather_unknown)
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(dateString) ?: return dateString
            val currentLocale = Locale.getDefault()
            if (currentLocale.language == "zh") {
                SimpleDateFormat("MM月dd日", currentLocale).format(date)
            } else {
                SimpleDateFormat("MM/dd", currentLocale).format(date)
            }
        } catch (e: Exception) {
            dateString
        }
    }

    private fun getHeaderColorRes(code: Int): Int {
        return when (code) {
            0 -> R.color.sky_blue
            else -> R.color.cloudy_gray
        }
    }

    private val _currentWeatherDetails = MutableLiveData<WeatherDetails>()
    val currentWeatherDetails: LiveData<WeatherDetails> = _currentWeatherDetails

    private val _currentWeeklyForecast = MutableLiveData<List<DailyForecast>>()
    val currentWeeklyForecast: LiveData<List<DailyForecast>> = _currentWeeklyForecast

    private val _selectedWeatherDetails = MutableLiveData<WeatherDetails>()
    val selectedWeatherDetails: LiveData<WeatherDetails> = _selectedWeatherDetails

    private val _selectedWeeklyForecast = MutableLiveData<List<DailyForecast>>()
    val selectedWeeklyForecast: LiveData<List<DailyForecast>> = _selectedWeeklyForecast

    fun fetchWeather(latitude: Double, longitude: Double, cityName: String? = null) {
        cityName?.let {
            currentCityStr = it
            updateCityNameDisplay()
        }
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = WeatherApiClient.getWeather(latitude, longitude)
                response?.let { weatherResp ->
                    weatherResp.currentWeather?.let { weather ->
                        currentTempStr = "${weather.temperature}°C"
                        currentDescStr = getWeatherDescription(weather.weathercode)
                        currentColorRes = getHeaderColorRes(weather.weathercode)
                        updateCityNameDisplay()

                        val app = getApplication<Application>()
                        val popValue = weatherResp.daily?.precipitationProbabilityMax?.getOrNull(0) ?: 0
                        val details = WeatherDetails(
                            temperature = app.getString(R.string.label_temperature, "${weather.temperature}°C"),
                            windSpeed = app.getString(R.string.label_wind_speed, "${weather.windspeed}"),
                            description = app.getString(R.string.label_weather_desc, currentDescStr),
                            pop = app.getString(R.string.label_pop, popValue)
                        )
                        _currentWeatherDetails.postValue(details)
                    }

                    weatherResp.daily?.let { daily ->
                        val weekly = daily.time.mapIndexed { index, dateStr ->
                            val max = daily.temperatureMax[index]
                            val min = daily.temperatureMin[index]
                            val code = daily.weathercode?.getOrNull(index) ?: 0
                            val desc = getWeatherDescription(code)
                            val formattedDate = formatDate(dateStr)
                            DailyForecast(formattedDate, desc, max.toFloat(), min.toFloat(), "$max°C\n$min°C")
                        }
                        _currentWeeklyForecast.postValue(weekly)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("取得資料發生錯誤 (Timeout/Error): ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun fetchSelectedCityWeather(latitude: Double, longitude: Double, cityName: String? = null) {
        cityName?.let {
            selectedCityStr = it
            updateCityNameDisplay()
        }
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = WeatherApiClient.getWeather(latitude, longitude)
                response?.let { weatherResp ->
                    weatherResp.currentWeather?.let { weather ->
                        selectedTempStr = "${weather.temperature}°C"
                        selectedDescStr = getWeatherDescription(weather.weathercode)
                        selectedColorRes = getHeaderColorRes(weather.weathercode)
                        updateCityNameDisplay()

                        val app = getApplication<Application>()
                        val popValue = weatherResp.daily?.precipitationProbabilityMax?.getOrNull(0) ?: 0
                        val details = WeatherDetails(
                            temperature = app.getString(R.string.label_temperature, "${weather.temperature}°C"),
                            windSpeed = app.getString(R.string.label_wind_speed, "${weather.windspeed}"),
                            description = app.getString(R.string.label_weather_desc, selectedDescStr),
                            pop = app.getString(R.string.label_pop, popValue)
                        )
                        _selectedWeatherDetails.postValue(details)
                    }

                    weatherResp.daily?.let { daily ->
                        val weekly = daily.time.mapIndexed { index, dateStr ->
                            val max = daily.temperatureMax[index]
                            val min = daily.temperatureMin[index]
                            val code = daily.weathercode?.getOrNull(index) ?: 0
                            val desc = getWeatherDescription(code)
                            val formattedDate = formatDate(dateStr)
                            DailyForecast(formattedDate, desc, max.toFloat(), min.toFloat(), "$max°C\n$min°C")
                        }
                        _selectedWeeklyForecast.postValue(weekly)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("取得資料發生錯誤 (Timeout/Error): ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
