package com.example.speedometer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import kotlin.system.exitProcess
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.speech.tts.TextToSpeech
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import java.util.Locale

class MainActivity : ComponentActivity() {
    // mutableStateOf ensures that, the display value in GUI will be updated.
    private var speed by mutableStateOf(0)
    private var lastTTSSpeed = 0
    private var prevBrightness = -1f
    private var dim = false
    lateinit var locationManager: LocationManager
    lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prevBrightness = window.attributes.screenBrightness

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
            }
        }

        enableEdgeToEdge()
        requestPermissions()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Speedometer",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 60.dp)
                )
                Text(
                    text = "$speed km/h",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 180.dp)
                )
                Button(
                    onClick = { toggleDim() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier
                        .padding(top = 220.dp)
                        .size(width = 240.dp, height = 60.dp)
                ) {
                    Text(
                        text = "Dim/Bright",
                        color = Color.White,
                        fontSize = 30.sp
                    )
                }
            }
        }
    }

    // https://developer.android.com/training/permissions/requesting#kotlin
    @RequiresApi(Build.VERSION_CODES.N)
    fun requestPermissions() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                // Precise location is granted.
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    getSpeed()
                }
                // Only approximate location is granted.
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    killApp()
                }
                // No permission is granted.
                else -> {
                    killApp()
                }
            }
        }

        // Before you perform the actual permission request, check whether your app
        // already has the permissions, and whether your app needs to show a permission
        // rationale dialog. For more details, see Request permissions:
        // https://developer.android.com/training/permissions/requesting#request-permission
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun killApp() {
        finishAffinity()
        exitProcess(0)
    }

    fun getSpeed() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        // Update speed on every thread.
                        runOnUiThread {
                            // m/s -> km/h
                            speed = (location.speed * 3.6).toInt()
                            communicateSpeed()
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            )
        } catch (ex: SecurityException) {
        }
    }

    fun communicateSpeed() {
        if (speed < 10) {
            return
        }

        var tmpSpeed = speed

        // Simplest way to "cut" the last digit: 46 / 10 = 4 -> 4 * 10 = 40
        tmpSpeed /= 10
        tmpSpeed *= 10

        if (lastTTSSpeed == tmpSpeed) {
            return
        }

        lastTTSSpeed = tmpSpeed

        tts.speak(
            "$tmpSpeed kilometers per hour",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "SPEED_ANNOUNCE"
        )
    }

    fun toggleDim() {
        if (!dim) {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = 0f
            window.attributes = layoutParams
            dim = true
        }
        else {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = prevBrightness
            window.attributes = layoutParams
            dim = false
        }
    }
}
