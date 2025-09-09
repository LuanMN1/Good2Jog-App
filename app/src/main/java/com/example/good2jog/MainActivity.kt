package com.example.good2jog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.good2jog.model.WeatherResponse
import com.example.good2jog.network.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1
    private val API_KEY = "327c80074cdbff863f902f5231dcb97d"

    private lateinit var tvCity: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvResult: TextView
    private lateinit var cardUseLocation: LinearLayout
    private lateinit var cardSearchCity: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // === STATUS BAR ===
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Muda a cor da barra de status para preto
            window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Remove texto/ícones escuros para ficar branco
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }

        // === FIM DA ALTERAÇÃO ===

        // Inicializa views
        tvCity = findViewById(R.id.tvCity)
        tvTemp = findViewById(R.id.tvTemp)
        tvCondition = findViewById(R.id.tvCondition)
        tvWind = findViewById(R.id.tvWind)
        tvResult = findViewById(R.id.tvResult)
        cardUseLocation = findViewById(R.id.cardUseLocation)
        cardSearchCity = findViewById(R.id.cardSearchCity)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Click no card da localização
        cardUseLocation.setOnClickListener {
            getUserLocation { lat, lon ->
                fetchWeatherByLocation(lat, lon)
            }
        }

        // Click no card de busca de cidade via popup
        cardSearchCity.setOnClickListener {
            val input = EditText(this)
            input.hint = "Enter city name"

            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Search City")
                .setView(input)
                .setPositiveButton("Search") { _, _ ->
                    val cityName = input.text.toString().trim()
                    if (cityName.isNotEmpty()) {
                        fetchWeatherByCity(cityName)
                    } else {
                        Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }
    }

    // Função para obter localização do usuário
    private fun getUserLocation(onLocationReceived: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                onLocationReceived(it.latitude, it.longitude)
            } ?: run {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Callback de permissão
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getUserLocation { lat, lon ->
                fetchWeatherByLocation(lat, lon)
            }
        }
    }

    // Busca por clima usando coordenadas
    private fun fetchWeatherByLocation(lat: Double, lon: Double) {
        val call = RetrofitInstance.api.getWeatherData(lat, lon, API_KEY)
        call.enqueue(weatherCallback)
    }

    // Busca por clima usando nome da cidade
    private fun fetchWeatherByCity(cityName: String) {
        val call = RetrofitInstance.api.getWeatherByCity(cityName, API_KEY)
        call.enqueue(weatherCallback)
    }

    // Callback compartilhado para mostrar resultados
    private val weatherCallback = object : Callback<WeatherResponse> {
        override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
            if (response.isSuccessful) {
                val data = response.body()
                data?.let {
                    tvCity.text = "City: ${it.name}"
                    tvTemp.text = "Temperature: ${it.main.temp}°C"
                    tvCondition.text =
                        "Weather: ${it.weather[0].description.replaceFirstChar { c -> c.uppercase() }}"
                    tvWind.text = "Wind: ${it.wind.speed} m/s"

                    val resultado =
                        if (it.main.temp in 15.0..25.0 &&
                            !it.weather[0].description.contains("rain", ignoreCase = true) &&
                            it.wind.speed < 5
                        ) {
                            "✅ Perfect for running!"
                        } else {
                            "❌ Better avoid running now."
                        }
                    tvResult.text = resultado
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${response.code()} - ${response.message()}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
            Toast.makeText(this@MainActivity, "Failure: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
