package com.isa.whatstheweatherlike

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
    private var tvAddress:android.widget.TextView? = null
    private var tvUpdatedAt:android.widget.TextView? = null
    private var tvStatus:android.widget.TextView? = null
    private var tvTemp:android.widget.TextView? = null
    private var tvTempMin:android.widget.TextView? = null
    private var tvTempMax:android.widget.TextView? = null
    private var tvHumidity:android.widget.TextView? = null
    private var tvPressure:android.widget.TextView? = null
    private var tvWind:android.widget.TextView? = null
    private var weatherIcon: ImageView? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAppName = findViewById(R.id.tv_app_name);
        tvAddress = findViewById(R.id.address);
        tvUpdatedAt = findViewById(R.id.updated_at);
        tvStatus = findViewById(R.id.status);
        tvTemp = findViewById(R.id.temp);
        tvTempMin = findViewById(R.id.temp_min);
        tvTempMax = findViewById(R.id.temp_max);
        weatherIcon = findViewById(R.id.weather_icon);
        tvHumidity = findViewById(R.id.humidity)
        tvPressure = findViewById(R.id.preasure)
        tvWind = findViewById(R.id.wind)

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
                //val responseDaily = URL("https://api.openweathermap.org/data/2.5/forecast/daily?lat=$latitude&lon=$longitude&appid=$API_KEY").readText()
                val jsonObj = JSONObject(response)
                //val jsonResponse = JSONObject(responseDaily)

                val main = jsonObj.getJSONObject("main")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val wind = jsonObj.getJSONObject("wind")
                val sys = jsonObj.getJSONObject("sys")

                val temp = main.getString("temp")
                val tempMin = main.getString("temp_min")
                val tempMax = main.getString("temp_max")
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")
                val status = weather.getString("description")
                val windSpeed = wind.getString("speed")
                val city = jsonObj.getString("name")
                val country = sys.getString("country")
                val updatedAt = jsonObj.getLong("dt")


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
                    weatherIcon!!.setImageResource(getWeatherIcon(status))

                    // Parse hourly forecast and set it to RecyclerView
                  /*  if (jsonResponse.has("daily")) {
                        val forecastList = parseDailyForecast(jsonResponse)
                        val recyclerView: RecyclerView = findViewById(R.id.RvWeather)
                        recyclerView.adapter = WeatherForecastAdapter(forecastList)
                        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    } else {
                        Log.e("MainActivity", "No hourly forecast data available")
                    }*/
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching weather data", e)
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

    private fun getWeatherIcon(status: String): Int {
        return when (status.toLowerCase()) {
            "clear sky" -> R.drawable.ic_clear_sky
            "few clouds" -> R.drawable.ic_few_clouds
            "scattered clouds" -> R.drawable.ic_scattered_clouds
            "broken clouds" -> R.drawable.ic_broken_clouds
            "shower rain" -> R.drawable.ic_shower_rain
            "rain" -> R.drawable.ic_rain
            "thunderstorm" -> R.drawable.ic_thunderstorm
            "snow" -> R.drawable.ic_snow
            "mist" -> R.drawable.ic_mist
            else -> R.mipmap.ic_launcher
        }
    }

    private fun parseDailyForecast(jsonResponse: JSONObject): List<ForecastItem> {
        val forecastList = mutableListOf<ForecastItem>()

        // Assuming your JSON has a field "daily" which is an array of forecasts
        val dailyArray = jsonResponse.getJSONArray("daily")
        for (i in 0 until dailyArray.length()) {
            val dayForecast = dailyArray.getJSONObject(i)
            val temp = dayForecast.getJSONObject("temp").getDouble("day")
            val status = dayForecast.getJSONArray("weather").getJSONObject(0).getString("description")

            // Convert the timestamp to a readable day format
            val day = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).format(java.util.Date(dayForecast.getLong("dt") * 1000))

            forecastList.add(ForecastItem(day, temp, status))
        }

        return forecastList
    }




}
