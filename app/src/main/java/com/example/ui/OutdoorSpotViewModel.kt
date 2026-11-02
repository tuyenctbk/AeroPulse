package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.OutdoorSpotDao
import com.example.data.model.OutdoorSpot
import com.example.data.network.AirQualityApiService
import com.example.data.network.NetworkClient
import com.example.data.network.WeatherApiService
import com.example.domain.SpotEvaluationResult
import com.example.domain.SpotThresholdEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OutdoorSpotUiState(
    val spots: List<OutdoorSpot> = emptyList(),
    val selectedSpot: OutdoorSpot? = null,
    val evaluation: SpotEvaluationResult? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class OutdoorSpotViewModel(
    application: Application,
    private val spotDao: OutdoorSpotDao = AppDatabase.getDatabase(application).outdoorSpotDao(),
    private val airQualityApi: AirQualityApiService = NetworkClient.airQualityApi,
    private val weatherApi: WeatherApiService = NetworkClient.weatherApi
) : AndroidViewModel(application) {

    val allSpots: StateFlow<List<OutdoorSpot>> = spotDao.getAllSpots()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(OutdoorSpotUiState())
    val uiState: StateFlow<OutdoorSpotUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            allSpots.collect { spotsList ->
                _uiState.update { current ->
                    val selected = current.selectedSpot?.let { cur ->
                        spotsList.find { it.id == cur.id }
                    } ?: spotsList.firstOrNull()

                    current.copy(
                        spots = spotsList,
                        selectedSpot = selected
                    )
                }
                // Automatically evaluate initial selected spot if needed
                if (_uiState.value.evaluation == null && _uiState.value.selectedSpot != null) {
                    _uiState.value.selectedSpot?.let { evaluateSpot(it) }
                }
            }
        }
    }

    fun selectSpot(spot: OutdoorSpot) {
        if (_uiState.value.selectedSpot?.id == spot.id && _uiState.value.evaluation != null) return
        _uiState.update { it.copy(selectedSpot = spot) }
        evaluateSpot(spot)
    }

    fun refreshEvaluation() {
        val spot = _uiState.value.selectedSpot ?: return
        evaluateSpot(spot, isRefresh = true)
    }

    fun evaluateSpot(spot: OutdoorSpot, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            try {
                val evaluation = withContext(Dispatchers.IO) {
                    coroutineScope {
                        val aqDeferred = async {
                            airQualityApi.getAirQuality(
                                latitude = spot.latitude,
                                longitude = spot.longitude
                            )
                        }
                        val weatherDeferred = async {
                            weatherApi.getWeather(
                                latitude = spot.latitude,
                                longitude = spot.longitude
                            )
                        }

                        val aq = aqDeferred.await()
                        val weather = weatherDeferred.await()
                        SpotThresholdEvaluator.evaluate(spot, aq, weather)
                    }
                }

                _uiState.update {
                    it.copy(
                        selectedSpot = spot,
                        evaluation = evaluation,
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                val fallback = getApplication<Application>().getString(com.example.R.string.failed_to_load_telemetry)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.message ?: fallback
                    )
                }
            }
        }
    }

    fun saveOrUpdateSpot(spot: OutdoorSpot) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = spotDao.insertSpot(spot)
            val savedSpot = if (spot.id == 0L) spot.copy(id = id) else spot
            withContext(Dispatchers.Main) {
                selectSpot(savedSpot)
            }
        }
    }

    fun deleteSpot(spot: OutdoorSpot) {
        viewModelScope.launch(Dispatchers.IO) {
            spotDao.deleteSpot(spot)
        }
    }
}
