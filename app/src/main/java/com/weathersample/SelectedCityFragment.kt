package com.weathersample

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weathersample.databinding.FragmentSelectedCityBinding

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import android.graphics.Color
import com.github.mikephil.charting.components.XAxis

/**
 * 負責顯示「選擇城市 / 第二分頁」的視圖控制器。
 * 使用者自選城市後會觸發 MainActivity.showCitySelectionDialogForSelectedCity() 更新資料，
 * 並在此片段中透過 ViewModel 的 selectedWeeklyForecast 動態渲染天氣資料與折線圖。
 */
class SelectedCityFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentSelectedCityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectedCityBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupChart()

        viewModel.selectedWeeklyForecast.observe(viewLifecycleOwner) { forecast ->
            if (forecast.isNotEmpty()) {
                val entriesHigh = ArrayList<Entry>()
                val entriesLow = ArrayList<Entry>()
                forecast.forEachIndexed { index, day ->
                    entriesHigh.add(Entry(index.toFloat(), day.temperatureMax))
                    entriesLow.add(Entry(index.toFloat(), day.temperatureMin))
                }

                val dataSetHigh = LineDataSet(entriesHigh, "High Temp")
                dataSetHigh.color = Color.RED
                dataSetHigh.valueTextSize = 12f
                dataSetHigh.valueTextColor = Color.RED
                dataSetHigh.setCircleColor(Color.RED)
                dataSetHigh.lineWidth = 2f
                dataSetHigh.circleRadius = 4f
                dataSetHigh.setDrawValues(true)

                val dataSetLow = LineDataSet(entriesLow, "Low Temp")
                dataSetLow.color = Color.BLUE
                dataSetLow.valueTextSize = 12f
                dataSetLow.valueTextColor = Color.BLUE
                dataSetLow.setCircleColor(Color.BLUE)
                dataSetLow.lineWidth = 2f
                dataSetLow.circleRadius = 4f
                dataSetLow.setDrawValues(true)

                val lineData = LineData(dataSetHigh, dataSetLow)
                binding.lineChart.data = lineData
                binding.lineChart.invalidate()
            }
        }
    }

    private fun setupChart() {
        val chart = binding.lineChart
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawLabels(false)
        xAxis.setDrawAxisLine(false)

        val yAxis = chart.axisLeft
        yAxis.setDrawGridLines(true)
        yAxis.setDrawLabels(false)
        yAxis.setDrawAxisLine(false)
        
        chart.setViewPortOffsets(40f, 40f, 40f, 40f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        checkAndShowCitySelection()
    }

    private fun checkAndShowCitySelection() {
        val sharedPrefs = requireContext().getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val savedCityName = sharedPrefs.getString("selected_city_name", null)

        if (savedCityName == null) {
            showCitySelectionDialog()
        }
    }

    private fun showCitySelectionDialog() {
        val context = requireContext()
        val cities = arrayOf(
            Pair(getString(R.string.city_taipei), Pair(25.0330, 121.5654)),
            Pair(getString(R.string.city_new_taipei), Pair(25.0112, 121.4646)),
            Pair(getString(R.string.city_taoyuan), Pair(24.9936, 121.3010)),
            Pair(getString(R.string.city_taichung), Pair(24.1477, 120.6736)),
            Pair(getString(R.string.city_tainan), Pair(22.9999, 120.2269)),
            Pair(getString(R.string.city_kaohsiung), Pair(22.6273, 120.3014))
        )
        val cityNames = cities.map { it.first }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle(getString(R.string.dialog_select_city_title))
            .setItems(cityNames) { _, which ->
                val selectedCity = cities[which]
                val cityName = selectedCity.first
                val lat = selectedCity.second.first
                val lon = selectedCity.second.second

                val sharedPrefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .putString("selected_city_name", cityName)
                    .putFloat("selected_city_lat", lat.toFloat())
                    .putFloat("selected_city_lon", lon.toFloat())
                    .apply()

                viewModel.fetchSelectedCityWeather(lat, lon, cityName)
            }
            .setCancelable(false)
            .show()
    }
}
