package com.isa.whatstheweatherlike

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WeatherForecastAdapter(private val forecastList: List<ForecastItem>) : RecyclerView.Adapter<WeatherForecastAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val day: TextView = itemView.findViewById(R.id.day)
        val temp: TextView = itemView.findViewById(R.id.temp)
        val weatherIcon: ImageView = itemView.findViewById(R.id.weather_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val forecastItem = forecastList[position]
        holder.day.text = forecastItem.day
        holder.temp.text = "${forecastItem.temp}°C"
        holder.weatherIcon.setImageResource(getWeatherIcon(forecastItem.status))
    }

    override fun getItemCount(): Int {
        return forecastList.size
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
            else -> R.drawable.ic_few_clouds
        }
    }
}

