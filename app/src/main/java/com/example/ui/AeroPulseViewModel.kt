package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.GeocodingResult
import com.example.data.model.SavedLocation
import com.example.data.repository.AeroPulseRepository
import com.example.domain.AeroPulseTelemetry
import com.example.domain.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import com.example.ui.engagement.EngagementDialogType
import com.example.ui.engagement.EngagementManager
import kotlinx.coroutines.launch

enum class AppViewMode {
    DASHBOARD,
    WEAR_OS_WATCH,
    WEAR_OS_TILE
}

sealed interface TelemetryUiState {
    data object Loading : TelemetryUiState
    data class Success(val data: AeroPulseTelemetry) : TelemetryUiState
    data class Error(val message: String) : TelemetryUiState
}

data class AeroPulseUiState(
    val activeProfile: UserProfile = UserProfile.RUNNER,
    val activeLocation: SavedLocation = SavedLocation(
        name = "Boulder",
        region = "Colorado",
        country = "United States",
        latitude = 40.0150,
        longitude = -105.2705,
        isFavorite = true,
        isDefault = true
    ),
    val telemetryState: TelemetryUiState = TelemetryUiState.Loading,
    val viewMode: AppViewMode = AppViewMode.DASHBOARD,
    val isRefreshing: Boolean = false,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
    val isGpsActive: Boolean = false,
    val activeDialog: EngagementDialogType = EngagementDialogType.NONE,
    val isPrivacyPolicyOpen: Boolean = false
)

class AeroPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val engagementManager = EngagementManager(application)

    private val repository: AeroPulseRepository by lazy {
        val db = AppDatabase.getDatabase(application)
        AeroPulseRepository(db.locationDao())
    }

    val savedLocations: StateFlow<List<SavedLocation>> = repository.savedLocations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(AeroPulseUiState())
    val uiState: StateFlow<AeroPulseUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadTelemetryForActiveLocation()
    }

    fun selectProfile(profile: UserProfile) {
        if (_uiState.value.activeProfile == profile) return
        _uiState.update { it.copy(activeProfile = profile) }
        loadTelemetryForActiveLocation()
    }

    fun setViewMode(mode: AppViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun openSearch() {
        _uiState.update { it.copy(isSearchOpen = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeSearch() {
        _uiState.update { it.copy(isSearchOpen = false, searchQuery = "", searchResults = emptyList()) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(350)
            val results = repository.searchCities(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun selectSearchResult(result: GeocodingResult) {
        val newLoc = SavedLocation(
            name = result.name,
            region = result.adminRegion ?: "",
            country = result.country ?: "",
            latitude = result.latitude,
            longitude = result.longitude,
            isFavorite = false
        )
        viewModelScope.launch {
            repository.saveLocation(newLoc)
        }
        _uiState.update {
            it.copy(
                activeLocation = newLoc,
                isSearchOpen = false,
                isGpsActive = false
            )
        }
        loadTelemetryForActiveLocation()
    }

    fun selectSavedLocation(location: SavedLocation) {
        _uiState.update {
            it.copy(
                activeLocation = location,
                isSearchOpen = false,
                isGpsActive = false
            )
        }
        loadTelemetryForActiveLocation()
    }

    fun toggleFavoriteCurrent() {
        val current = _uiState.value.activeLocation
        val updated = current.copy(isFavorite = !current.isFavorite)
        _uiState.update { it.copy(activeLocation = updated) }
        viewModelScope.launch {
            repository.saveLocation(updated)
        }
    }

    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }

    fun setGpsLocation(lat: Double, lon: Double, locality: String? = null) {
        val name = locality ?: "Current GPS Location"
        val gpsLoc = SavedLocation(
            name = name,
            region = "Local Radar",
            country = "GPS Device",
            latitude = lat,
            longitude = lon,
            isFavorite = false
        )
        _uiState.update {
            it.copy(
                activeLocation = gpsLoc,
                isGpsActive = true
            )
        }
        loadTelemetryForActiveLocation()
    }

    fun refreshTelemetry() {
        loadTelemetryForActiveLocation(isManualRefresh = true)
    }

    private fun loadTelemetryForActiveLocation(isManualRefresh: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val loc = state.activeLocation
            val profile = state.activeProfile

            if (isManualRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else if (state.telemetryState !is TelemetryUiState.Success) {
                _uiState.update { it.copy(telemetryState = TelemetryUiState.Loading) }
            }

            try {
                val regionLabel = if (loc.region.isNotEmpty()) "${loc.region}, ${loc.country}" else loc.country
                val telemetry = repository.fetchTelemetry(
                    locationName = loc.name,
                    regionCountry = regionLabel,
                    lat = loc.latitude,
                    lon = loc.longitude,
                    profile = profile
                )
                val suggestedDialog = engagementManager.recordRadarCheck()
                _uiState.update {
                    it.copy(
                        telemetryState = TelemetryUiState.Success(telemetry),
                        isRefreshing = false,
                        activeDialog = if (it.activeDialog == EngagementDialogType.NONE) suggestedDialog else it.activeDialog
                    )
                }
                if (suggestedDialog != EngagementDialogType.NONE) {
                    engagementManager.markPromptShown()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        telemetryState = TelemetryUiState.Error(e.message ?: "Failed to load telemetry"),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun dismissDialog() {
        engagementManager.onUserRemindLater()
        _uiState.update { it.copy(activeDialog = EngagementDialogType.NONE) }
    }

    fun onRateNow() {
        engagementManager.onUserRated()
        _uiState.update { it.copy(activeDialog = EngagementDialogType.NONE) }
    }

    fun onDismissRatingPermanently() {
        engagementManager.onUserDismissedPermanently()
        _uiState.update { it.copy(activeDialog = EngagementDialogType.NONE) }
    }

    fun onShareNow(shareText: String) {
        engagementManager.onUserShared(shareText)
        _uiState.update { it.copy(activeDialog = EngagementDialogType.NONE) }
    }

    fun triggerManualShare() {
        val telemetry = (_uiState.value.telemetryState as? TelemetryUiState.Success)?.data
        val loc = _uiState.value.activeLocation.name
        val score = telemetry?.verdict?.score ?: 85
        val label = telemetry?.verdict?.level?.shortLabel ?: "GO"
        val shareText = "AeroPulse Environmental Radar for $loc: Score $score/100 ($label). Check outdoor air purity and UV before your run!"
        engagementManager.launchShareIntent(shareText)
    }

    fun openPrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = true) }
    }

    fun closePrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = false) }
    }

    fun triggerUpdateDialogManually() {
        _uiState.update { it.copy(activeDialog = EngagementDialogType.UPDATE_APP) }
    }
}
