package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AeroPulseMainScreen
import com.example.ui.AeroPulseViewModel
import com.example.ui.SplashScreen
import com.example.ui.theme.AeroPulseTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            val viewModel: AeroPulseViewModel = viewModel()
            var showSplash by remember { mutableStateOf(true) }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

                if (fineGranted || coarseGranted) {
                    fetchGpsLocation(viewModel)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.location_permission_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            AeroPulseTheme {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(500),
                    label = "splashCrossfade"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onSplashComplete = { showSplash = false }
                        )
                    } else {
                        AeroPulseMainScreen(
                            viewModel = viewModel,
                            onRequestGpsLocation = {
                                val hasFine = ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasFine || hasCoarse) {
                                    fetchGpsLocation(viewModel)
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchGpsLocation(viewModel: AeroPulseViewModel) {
        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                var cityName = "GPS Location"
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Current Location"
                    }
                } catch (_: Exception) {
                    cityName = "Current GPS"
                }
                viewModel.setGpsLocation(location.latitude, location.longitude, cityName)
                Toast.makeText(
                    this,
                    getString(R.string.acquired_gps_toast, cityName),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Try last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        viewModel.setGpsLocation(lastLoc.latitude, lastLoc.longitude, "GPS Location")
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.gps_not_ready),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(
                this,
                getString(R.string.unable_to_get_gps),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
