package com.weathersample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayoutMediator
import com.weathersample.databinding.ActivityMainBinding

import android.content.res.Configuration

/**
 * App 的主畫面進入點。
 * 負責管理 TabLayout 與 ViewPager2 的分頁邏輯，處理 GPS 定位權限、網路狀態檢查，
 * 以及動態更換 App 語系設定 (zh-TW / en) 等核心介面互動。
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastRefreshTime: Long = 0

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchLocationAndWeather()
            } else {
                loadFallbackCityOrShowDialog()
            }
        }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("app_language", "zh-TW") ?: "zh-TW"
        val locale = java.util.Locale.forLanguageTag(languageCode)
        java.util.Locale.setDefault(locale)
        
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        val appRes = newBase.applicationContext.resources
        val appConfig = Configuration(appRes.configuration)
        appConfig.setLocale(locale)
        appRes.updateConfiguration(appConfig, appRes.displayMetrics)

        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_current_city)
                1 -> getString(R.string.tab_selected_city)
                else -> ""
            }
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setActiveTab(position)
            }
        })

        viewModel.errorMessage.observe(this) { errMsg ->
            errMsg?.let {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.error_title))
                    .setMessage(it)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        viewModel.clearError()
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        binding.btnRefresh.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRefreshTime < 30_000) {
                android.widget.Toast.makeText(this, "Please wait 30 seconds before refreshing.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lastRefreshTime = currentTime
            if (isNetworkAvailable()) {
                checkLocationPermission()
                loadSavedSelectedCity()
            } else {
                showNoNetworkDialog()
            }
        }

        binding.btnLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.btnChangeCity.setOnClickListener {
            showCitySelectionDialogForSelectedCity()
        }

        loadSavedSelectedCity()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "繁體中文")
        val languageCodes = arrayOf("en", "zh-TW")
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentCode = prefs.getString("app_language", "zh-TW")
        val checkedItem = languageCodes.indexOf(currentCode).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(this)
            .setTitle("語言選擇 / Select Language")
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedCode = languageCodes[which]
                prefs.edit().putString("app_language", selectedCode).apply()
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadSavedSelectedCity() {
        val sharedPrefs = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val savedCityName = sharedPrefs.getString("selected_city_name", null)
        if (savedCityName != null) {
            val lat = sharedPrefs.getFloat("selected_city_lat", 0f).toDouble()
            val lon = sharedPrefs.getFloat("selected_city_lon", 0f).toDouble()
            viewModel.fetchSelectedCityWeather(lat, lon, savedCityName)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isNetworkAvailable()) {
            checkLocationPermission()
        } else {
            showNoNetworkDialog()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun showNoNetworkDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_no_internet_title))
            .setMessage(getString(R.string.dialog_no_internet_msg))
            .setPositiveButton(getString(R.string.dialog_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocationAndWeather()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchLocationAndWeather() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val geocoder = android.location.Geocoder(this@MainActivity, java.util.Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val city = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.adminArea
                            val title = if (city != null) {
                                getString(R.string.current_location_format, city)
                            } else {
                                getString(R.string.current_location)
                            }
                            withContext(Dispatchers.Main) {
                                viewModel.fetchWeather(location.latitude, location.longitude, title)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                val title = getString(R.string.current_location)
                                viewModel.fetchWeather(location.latitude, location.longitude, title)
                            }
                        }
                    }
                } else {
                    loadFallbackCityOrShowDialog()
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            loadFallbackCityOrShowDialog()
        }
    }

    private fun loadFallbackCityOrShowDialog() {
        val sharedPrefs = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val savedCityName = sharedPrefs.getString("current_city_fallback_name", null)
        if (savedCityName != null) {
            val lat = sharedPrefs.getFloat("current_city_fallback_lat", 0f).toDouble()
            val lon = sharedPrefs.getFloat("current_city_fallback_lon", 0f).toDouble()
            viewModel.fetchWeather(lat, lon, savedCityName)
        } else {
            showCitySelectionDialog()
        }
    }

    private fun showCitySelectionDialog() {
        val cities = arrayOf(
            Pair(getString(R.string.city_taipei), Pair(25.0330, 121.5654)),
            Pair(getString(R.string.city_new_taipei), Pair(25.0112, 121.4646)),
            Pair(getString(R.string.city_taoyuan), Pair(24.9936, 121.3010)),
            Pair(getString(R.string.city_taichung), Pair(24.1477, 120.6736)),
            Pair(getString(R.string.city_tainan), Pair(22.9999, 120.2269)),
            Pair(getString(R.string.city_kaohsiung), Pair(22.6273, 120.3014))
        )
        val cityNames = cities.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_select_city_title))
            .setItems(cityNames) { _, which ->
                val selectedCity = cities[which]
                val cityName = selectedCity.first
                val lat = selectedCity.second.first
                val lon = selectedCity.second.second

                val sharedPrefs = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .putString("current_city_fallback_name", cityName)
                    .putFloat("current_city_fallback_lat", lat.toFloat())
                    .putFloat("current_city_fallback_lon", lon.toFloat())
                    .apply()

                viewModel.fetchWeather(lat, lon, cityName)
            }
            .setCancelable(false)
            .show()
    }

    private fun showCitySelectionDialogForSelectedCity() {
        val cities = arrayOf(
            Pair(getString(R.string.city_taipei), Pair(25.0330, 121.5654)),
            Pair(getString(R.string.city_new_taipei), Pair(25.0112, 121.4646)),
            Pair(getString(R.string.city_taoyuan), Pair(24.9936, 121.3010)),
            Pair(getString(R.string.city_taichung), Pair(24.1477, 120.6736)),
            Pair(getString(R.string.city_tainan), Pair(22.9999, 120.2269)),
            Pair(getString(R.string.city_kaohsiung), Pair(22.6273, 120.3014))
        )
        val cityNames = cities.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_select_city_title))
            .setItems(cityNames) { _, which ->
                val selectedCity = cities[which]
                val cityName = selectedCity.first
                val lat = selectedCity.second.first
                val lon = selectedCity.second.second

                val sharedPrefs = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .putString("selected_city_name", cityName)
                    .putFloat("selected_city_lat", lat.toFloat())
                    .putFloat("selected_city_lon", lon.toFloat())
                    .apply()

                viewModel.fetchSelectedCityWeather(lat, lon, cityName)
            }
            .setCancelable(true)
            .show()
    }
}