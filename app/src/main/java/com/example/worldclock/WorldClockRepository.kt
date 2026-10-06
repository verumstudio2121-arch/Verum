package com.example.worldclock

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class WorldClockRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("hitech_world_clock_db", Context.MODE_PRIVATE)

    private val _userCities = MutableStateFlow<List<WorldCity>>(emptyList())
    val userCities: StateFlow<List<WorldCity>> = _userCities.asStateFlow()

    init {
        loadCities()
    }

    private fun loadCities() {
        val jsonString = prefs.getString("cities_json", null)
        val list = mutableListOf<WorldCity>()
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        WorldCity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            country = obj.getString("country"),
                            timezoneId = obj.getString("timezoneId")
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        if (list.isEmpty()) {
            // Default reference cities requested by user
            val defaults = listOf(
                WorldCity(name = "Cupertino", country = "United States", timezoneId = "America/Los_Angeles"),
                WorldCity(name = "Paris", country = "France", timezoneId = "Europe/Paris"),
                WorldCity(name = "São Paulo", country = "Brazil", timezoneId = "America/Sao_Paulo"),
                WorldCity(name = "Dakar", country = "Senegal", timezoneId = "Africa/Dakar"),
                WorldCity(name = "Mumbai", country = "India", timezoneId = "Asia/Kolkata"),
                WorldCity(name = "Tokyo", country = "Japan", timezoneId = "Asia/Tokyo")
            )
            list.addAll(defaults)
            persistCities(list)
        }

        _userCities.value = list
    }

    private fun persistCities(list: List<WorldCity>) {
        val array = JSONArray()
        list.forEach { city ->
            val obj = JSONObject().apply {
                put("id", city.id)
                put("name", city.name)
                put("country", city.country)
                put("timezoneId", city.timezoneId)
            }
            array.put(obj)
        }
        prefs.edit().putString("cities_json", array.toString()).apply()
    }

    fun addCity(city: WorldCity) {
        val current = _userCities.value.toMutableList()
        if (current.none { it.timezoneId == city.timezoneId && it.name == city.name }) {
            current.add(city)
            _userCities.value = current
            persistCities(current)
        }
    }

    fun removeCity(cityId: String) {
        val current = _userCities.value.toMutableList()
        current.removeAll { it.id == cityId }
        _userCities.value = current
        persistCities(current)
    }

    fun moveCityUp(index: Int) {
        if (index > 0 && index < _userCities.value.size) {
            val current = _userCities.value.toMutableList()
            val item = current.removeAt(index)
            current.add(index - 1, item)
            _userCities.value = current
            persistCities(current)
        }
    }

    fun moveCityDown(index: Int) {
        if (index >= 0 && index < _userCities.value.size - 1) {
            val current = _userCities.value.toMutableList()
            val item = current.removeAt(index)
            current.add(index + 1, item)
            _userCities.value = current
            persistCities(current)
        }
    }

    companion object {
        val ALL_AVAILABLE_CITIES: List<WorldCity> = listOf(
            WorldCity(name = "Amsterdam", country = "Netherlands", timezoneId = "Europe/Amsterdam"),
            WorldCity(name = "Athens", country = "Greece", timezoneId = "Europe/Athens"),
            WorldCity(name = "Auckland", country = "New Zealand", timezoneId = "Pacific/Auckland"),
            WorldCity(name = "Bangkok", country = "Thailand", timezoneId = "Asia/Bangkok"),
            WorldCity(name = "Barcelona", country = "Spain", timezoneId = "Europe/Madrid"),
            WorldCity(name = "Beijing", country = "China", timezoneId = "Asia/Shanghai"),
            WorldCity(name = "Berlin", country = "Germany", timezoneId = "Europe/Berlin"),
            WorldCity(name = "Bogotá", country = "Colombia", timezoneId = "America/Bogota"),
            WorldCity(name = "Boston", country = "United States", timezoneId = "America/New_York"),
            WorldCity(name = "Buenos Aires", country = "Argentina", timezoneId = "America/Argentina/Buenos_Aires"),
            WorldCity(name = "Cairo", country = "Egypt", timezoneId = "Africa/Cairo"),
            WorldCity(name = "Cape Town", country = "South Africa", timezoneId = "Africa/Johannesburg"),
            WorldCity(name = "Chicago", country = "United States", timezoneId = "America/Chicago"),
            WorldCity(name = "Copenhagen", country = "Denmark", timezoneId = "Europe/Copenhagen"),
            WorldCity(name = "Cupertino", country = "United States", timezoneId = "America/Los_Angeles"),
            WorldCity(name = "Dakar", country = "Senegal", timezoneId = "Africa/Dakar"),
            WorldCity(name = "Delhi", country = "India", timezoneId = "Asia/Kolkata"),
            WorldCity(name = "Dubai", country = "United Arab Emirates", timezoneId = "Asia/Dubai"),
            WorldCity(name = "Dublin", country = "Ireland", timezoneId = "Europe/Dublin"),
            WorldCity(name = "Frankfurt", country = "Germany", timezoneId = "Europe/Berlin"),
            WorldCity(name = "Geneva", country = "Switzerland", timezoneId = "Europe/Zurich"),
            WorldCity(name = "Helsinki", country = "Finland", timezoneId = "Europe/Helsinki"),
            WorldCity(name = "Hong Kong", country = "Hong Kong", timezoneId = "Asia/Hong_Kong"),
            WorldCity(name = "Honolulu", country = "United States", timezoneId = "Pacific/Honolulu"),
            WorldCity(name = "Istanbul", country = "Turkey", timezoneId = "Europe/Istanbul"),
            WorldCity(name = "Jakarta", country = "Indonesia", timezoneId = "Asia/Jakarta"),
            WorldCity(name = "Jerusalem", country = "Israel", timezoneId = "Asia/Jerusalem"),
            WorldCity(name = "Johannesburg", country = "South Africa", timezoneId = "Africa/Johannesburg"),
            WorldCity(name = "Kuala Lumpur", country = "Malaysia", timezoneId = "Asia/Kuala_Lumpur"),
            WorldCity(name = "Kyiv", country = "Ukraine", timezoneId = "Europe/Kyiv"),
            WorldCity(name = "Lagos", country = "Nigeria", timezoneId = "Africa/Lagos"),
            WorldCity(name = "Lisbon", country = "Portugal", timezoneId = "Europe/Lisbon"),
            WorldCity(name = "London", country = "United Kingdom", timezoneId = "Europe/London"),
            WorldCity(name = "Los Angeles", country = "United States", timezoneId = "America/Los_Angeles"),
            WorldCity(name = "Madrid", country = "Spain", timezoneId = "Europe/Madrid"),
            WorldCity(name = "Manila", country = "Philippines", timezoneId = "Asia/Manila"),
            WorldCity(name = "Melbourne", country = "Australia", timezoneId = "Australia/Melbourne"),
            WorldCity(name = "Mexico City", country = "Mexico", timezoneId = "America/Mexico_City"),
            WorldCity(name = "Miami", country = "United States", timezoneId = "America/New_York"),
            WorldCity(name = "Milan", country = "Italy", timezoneId = "Europe/Rome"),
            WorldCity(name = "Montreal", country = "Canada", timezoneId = "America/Toronto"),
            WorldCity(name = "Moscow", country = "Russia", timezoneId = "Europe/Moscow"),
            WorldCity(name = "Mumbai", country = "India", timezoneId = "Asia/Kolkata"),
            WorldCity(name = "Nairobi", country = "Kenya", timezoneId = "Africa/Nairobi"),
            WorldCity(name = "New York", country = "United States", timezoneId = "America/New_York"),
            WorldCity(name = "Oslo", country = "Norway", timezoneId = "Europe/Oslo"),
            WorldCity(name = "Paris", country = "France", timezoneId = "Europe/Paris"),
            WorldCity(name = "Prague", country = "Czech Republic", timezoneId = "Europe/Prague"),
            WorldCity(name = "Reykjavik", country = "Iceland", timezoneId = "Atlantic/Reykjavik"),
            WorldCity(name = "Rio de Janeiro", country = "Brazil", timezoneId = "America/Sao_Paulo"),
            WorldCity(name = "Rome", country = "Italy", timezoneId = "Europe/Rome"),
            WorldCity(name = "San Francisco", country = "United States", timezoneId = "America/Los_Angeles"),
            WorldCity(name = "Santiago", country = "Chile", timezoneId = "America/Santiago"),
            WorldCity(name = "São Paulo", country = "Brazil", timezoneId = "America/Sao_Paulo"),
            WorldCity(name = "Seattle", country = "United States", timezoneId = "America/Los_Angeles"),
            WorldCity(name = "Seoul", country = "South Korea", timezoneId = "Asia/Seoul"),
            WorldCity(name = "Singapore", country = "Singapore", timezoneId = "Asia/Singapore"),
            WorldCity(name = "Stockholm", country = "Sweden", timezoneId = "Europe/Stockholm"),
            WorldCity(name = "Sydney", country = "Australia", timezoneId = "Australia/Sydney"),
            WorldCity(name = "Taipei", country = "Taiwan", timezoneId = "Asia/Taipei"),
            WorldCity(name = "Tokyo", country = "Japan", timezoneId = "Asia/Tokyo"),
            WorldCity(name = "Toronto", country = "Canada", timezoneId = "America/Toronto"),
            WorldCity(name = "Vancouver", country = "Canada", timezoneId = "America/Vancouver"),
            WorldCity(name = "Vienna", country = "Austria", timezoneId = "Europe/Vienna"),
            WorldCity(name = "Warsaw", country = "Poland", timezoneId = "Europe/Warsaw"),
            WorldCity(name = "Washington, D.C.", country = "United States", timezoneId = "America/New_York"),
            WorldCity(name = "Zurich", country = "Switzerland", timezoneId = "Europe/Zurich")
        )
    }
}
