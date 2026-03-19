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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayoutMediator
import com.weathersample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchLocationAndWeather()
            } else {
                showCitySelectionDialog()
            }
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
                1 -> getString(R.string.tab_weekly_report)
                else -> ""
            }
        }.attach()
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
                    viewModel.fetchWeather(location.latitude, location.longitude, getString(R.string.current_location))
                } else {
                    showCitySelectionDialog()
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
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
                viewModel.fetchWeather(
                    selectedCity.second.first,
                    selectedCity.second.second,
                    selectedCity.first
                )
            }
            .setCancelable(false)
            .show()
    }
}