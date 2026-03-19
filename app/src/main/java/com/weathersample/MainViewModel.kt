package com.weathersample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weathersample.data.WeatherApiClient
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _text1 = MutableLiveData("TextView 1")
    val text1: LiveData<String> = _text1

    private val _text2 = MutableLiveData("TextView 2")
    val text2: LiveData<String> = _text2

    private val _text3 = MutableLiveData("TextView 3")
    val text3: LiveData<String> = _text3

    private val _currentCityText = MutableLiveData(application.getString(R.string.loading_current_weather))
    val currentCityText: LiveData<String> = _currentCityText

    private val _selectedCityText = MutableLiveData(application.getString(R.string.loading_weekly_weather))
    val selectedCityText: LiveData<String> = _selectedCityText

    fun fetchWeather(latitude: Double, longitude: Double, cityName: String? = null) {
        viewModelScope.launch {
            WeatherApiClient.getWeather(latitude, longitude)
        }
    }
}
