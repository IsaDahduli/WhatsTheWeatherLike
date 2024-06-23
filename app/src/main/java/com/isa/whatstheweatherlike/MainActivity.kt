package com.isa.whatstheweatherlike

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val API_KEY = "56ab3e249da84168291bf5fa9c7d6538"
    }

    private var tvAppName: TextView? = null
    private var tvAddress: TextView? = null
    private var tvUpdatedAt: TextView? = null
    private var tvStatus: TextView? = null
    private var tvTemp: TextView? = null
    private var tvTempMin: TextView? = null
    private var tvTempMax: TextView? = null
    private var tvHumidity: TextView? = null
    private var tvPressure: TextView? = null
    private var tvWind: TextView? = null
    private var weatherIcon: ImageView? = null

    private var tvDewPoint: TextView? = null
    private var tvVisibility: TextView? = null
    private var tvUVIndex: TextView? = null

    private var sunriseTime: Long = 0
    private var sunsetTime: Long = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        tvAppName = findViewById(R.id.tv_app_name)
        tvAddress = findViewById(R.id.address)
        tvUpdatedAt = findViewById(R.id.updated_at)
        tvStatus = findViewById(R.id.status)
        tvTemp = findViewById(R.id.temp)
        tvTempMin = findViewById(R.id.temp_min)
        tvTempMax = findViewById(R.id.temp_max)
        weatherIcon = findViewById(R.id.weather_icon)
        tvHumidity = findViewById(R.id.humidity)
        tvPressure = findViewById(R.id.preasure)
        tvWind = findViewById(R.id.wind)

        tvUVIndex = findViewById(R.id.uv_text)
        tvDewPoint = findViewById(R.id.dew_point_text)
        tvVisibility = findViewById(R.id.visibility_txt)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkLocationPermission()) {
            fetchLocationAndWeather()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndWeather() {
        fusedLocationClient.lastLocation
            .addOnCompleteListener { task: Task<Location> ->
                val location: Location? = task.result
                if (location != null) {
                    fetchWeatherData(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        thread {
            try {
                val response = URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$API_KEY&units=metric").readText()
                val responseForecast = URL("https://api.openweathermap.org/data/2.5/forecast?lat=$latitude&lon=$longitude&appid=$API_KEY&units=metric").readText()
                val responseUV = URL("https://api.openweathermap.org/data/2.5/uvi?lat=$latitude&lon=$longitude&appid=$API_KEY").readText()
                val jsonObj = JSONObject(response)
                val jsonResponse = JSONObject(responseForecast)
                val jsonUV = JSONObject(responseUV)

                val main = jsonObj.getJSONObject("main")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val wind = jsonObj.getJSONObject("wind")
                val sys = jsonObj.getJSONObject("sys")

                val temp = main.getDouble("temp")
                val tempMin = main.getString("temp_min")
                val tempMax = main.getString("temp_max")
                val pressure = main.getString("pressure")
                val humidity = main.getInt("humidity")
                val status = weather.getString("description")
                val windSpeed = wind.getString("speed")
                val visibility = jsonObj.getInt("visibility") / 1000  // Convert meters to kilometers
                val city = jsonObj.getString("name")
                val country = sys.getString("country")
                val updatedAt = jsonObj.getLong("dt")

                // Calculate dew point
                val dewPoint = calculateDewPoint(temp, humidity)

                val uvIndex = jsonUV.getString("value")

                sunriseTime = sys.getLong("sunrise")
                sunsetTime = sys.getLong("sunset")

                runOnUiThread {
                    tvAppName!!.text = getString(R.string.app_name)
                    tvAddress!!.text = "$city, $country"
                    tvUpdatedAt!!.text = "Updated at: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date(updatedAt * 1000))}"
                    tvTemp!!.text = "$temp°C"
                    tvTempMin!!.text = "Min Temp: $tempMin°C"
                    tvTempMax!!.text = "Max Temp: $tempMax°C"
                    tvStatus!!.text = status.capitalize()
                    tvPressure!!.text = "$pressure hPa"
                    tvHumidity!!.text = "$humidity %"
                    tvWind!!.text = "$windSpeed m/s"
                    tvUVIndex!!.text = uvIndex
                    tvDewPoint!!.text = String.format("%.2f°C", dewPoint)
                    tvVisibility!!.text = "${visibility} km"
                    weatherIcon!!.setImageResource(getWeatherIcon(status, updatedAt))

                    // Parse daily forecast and set it to RecyclerView
                    if (jsonResponse.has("list")) {
                        val forecastList = parseDailyForecast(jsonResponse)
                        val recyclerView: RecyclerView = findViewById(R.id.RvWeather)
                        recyclerView.adapter = WeatherForecastAdapter(forecastList)
                        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    } else {
                        Toast.makeText(this, "Error getting Forecast", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Error getting data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndWeather()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getWeatherIcon(status: String, timestamp: Long): Int {
        val isDayTime = timestamp in sunriseTime until sunsetTime
        return when (status.toLowerCase()) {
            "clear sky" -> if (isDayTime) R.drawable.ic_clear_sky_day else R.drawable.ic_clear_sky_night
            "few clouds" -> if (isDayTime) R.drawable.ic_few_clouds_day else R.drawable.ic_few_clouds_night
            "scattered clouds" -> if (isDayTime) R.drawable.ic_scattered_clouds else R.drawable.ic_scattered_clouds
            "broken clouds" -> if (isDayTime) R.drawable.ic_broken_clouds else R.drawable.ic_broken_clouds
            "shower rain" -> if (isDayTime) R.drawable.ic_shower_rain else R.drawable.ic_shower_rain
            "rain" -> if (isDayTime) R.drawable.ic_rain else R.drawable.ic_rain
            "thunderstorm" -> if (isDayTime) R.drawable.ic_thunderstorm else R.drawable.ic_thunderstorm
            "snow" -> if (isDayTime) R.drawable.ic_snow else R.drawable.ic_snow
            "mist" -> if (isDayTime) R.drawable.ic_mist else R.drawable.ic_mist
            else -> if (isDayTime) R.drawable.ic_few_clouds_day else R.drawable.ic_few_clouds_night
        }
    }

    private fun parseDailyForecast(jsonResponse: JSONObject): List<ForecastItem> {
        val forecastList = mutableListOf<ForecastItem>()

        val list = jsonResponse.getJSONArray("list")
        val dailyMap = mutableMapOf<String, MutableList<JSONObject>>()

        // Group forecast data by day
        for (i in 0 until list.length()) {
            val forecast = list.getJSONObject(i)
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date(forecast.getLong("dt") * 1000))
            if (!dailyMap.containsKey(date)) {
                dailyMap[date] = mutableListOf()
            }
            dailyMap[date]!!.add(forecast)
        }

        // Process each day's forecasts
        for ((date, forecasts) in dailyMap) {
            val dayForecast = forecasts[0]
            val temp = dayForecast.getJSONObject("main").getDouble("temp")
            val status = dayForecast.getJSONArray("weather").getJSONObject(0).getString("description")
            val day = java.text.SimpleDateFormat("EEE, dd MMM", java.util.Locale.ENGLISH).format(java.util.Date(dayForecast.getLong("dt") * 1000))

            forecastList.add(ForecastItem(day, temp, status))
        }

        return forecastList
    }

    private fun calculateDewPoint(temp: Double, humidity: Int): Double {
        val a = 17.625
        val b = 243.04
        val alpha = ((a * temp) / (b + temp)) + Math.log(humidity / 100.0)
        return (b * alpha) / (a - alpha)
    }

}
