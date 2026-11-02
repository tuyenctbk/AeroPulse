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

import com.example.ui.engagement.AppUsageMetrics
import com.example.ui.notifications.AeroPulseNotificationHelper
import com.example.ui.notifications.NotificationPreferences
import com.example.ui.notifications.NotificationSettingsManager

enum class AppViewMode {
    DASHBOARD,
    WEAR_OS_WATCH,
    WEAR_OS_TILE
}

sealed interface TelemetryUiState {
    data object Empty : TelemetryUiState
    data object Loading : TelemetryUiState
    data class Success(val data: AeroPulseTelemetry) : TelemetryUiState
    data class Error(val message: String) : TelemetryUiState
}

data class AeroPulseUiState(
    val activeProfile: UserProfile = UserProfile.RUNNER,
    val activeLocation: SavedLocation? = null,
    val telemetryState: TelemetryUiState = TelemetryUiState.Empty,
    val viewMode: AppViewMode = AppViewMode.DASHBOARD,
    val isRefreshing: Boolean = false,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
    val isGpsActive: Boolean = false,
    val activeDialog: EngagementDialogType = EngagementDialogType.NONE,
    val isPrivacyPolicyOpen: Boolean = false,
    val isUsageStatsOpen: Boolean = false,
    val isNotificationSettingsOpen: Boolean = false,
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val usageMetrics: AppUsageMetrics = AppUsageMetrics(1, 0, 1, false, false, false)
)

class AeroPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val engagementManager = EngagementManager(application)
    private val notificationHelper = AeroPulseNotificationHelper(application)
    private val notificationSettingsManager = NotificationSettingsManager(application)

    private val repository: AeroPulseRepository by lazy {
        val db = AppDatabase.getDatabase(application)
        AeroPulseRepository(db.locationDao(), db.telemetryCacheDao())
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
        val initialPrefs = notificationSettingsManager.getPreferences()
        _uiState.update {
            it.copy(
                usageMetrics = engagementManager.getUsageMetrics(),
                notificationPreferences = initialPrefs
            )
        }
        viewModelScope.launch {
            repository.savedLocations.collect { locations ->
                if (_uiState.value.activeLocation == null && locations.isNotEmpty()) {
                    val defaultLoc = locations.firstOrNull { it.isDefault } ?: locations.first()
                    _uiState.update { it.copy(activeLocation = defaultLoc) }
                    loadTelemetryForActiveLocation()
                }
            }
        }
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
        val current = _uiState.value.activeLocation ?: return
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
        val app = getApplication<Application>()
        val name = locality ?: app.getString(com.example.R.string.current_gps_location)
        val gpsLoc = SavedLocation(
            name = name,
            region = app.getString(com.example.R.string.local_radar_label),
            country = app.getString(com.example.R.string.gps_device_label),
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
            if (loc == null) {
                _uiState.update { it.copy(telemetryState = TelemetryUiState.Empty, isRefreshing = false) }
                return@launch
            }
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
                val suggestedDialog = engagementManager.recordRadarCheck(telemetry.verdict.score)
                val updatedMetrics = engagementManager.getUsageMetrics()
                _uiState.update {
                    it.copy(
                        telemetryState = TelemetryUiState.Success(telemetry),
                        isRefreshing = false,
                        usageMetrics = updatedMetrics,
                        activeDialog = if (it.activeDialog == EngagementDialogType.NONE) suggestedDialog else it.activeDialog
                    )
                }
                if (suggestedDialog != EngagementDialogType.NONE) {
                    engagementManager.markPromptShown(suggestedDialog)
                }

                // Check and trigger outdoor activity window push notification if conditions met
                checkAndNotifyOutdoorWindow(loc, telemetry)
            } catch (e: Exception) {
                val failMsg = getApplication<Application>().getString(com.example.R.string.failed_to_load_telemetry)
                _uiState.update {
                    it.copy(
                        telemetryState = TelemetryUiState.Error(e.message ?: failMsg),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    private fun checkAndNotifyOutdoorWindow(loc: SavedLocation, telemetry: AeroPulseTelemetry) {
        val prefs = _uiState.value.notificationPreferences
        if (!prefs.isEnabled) return
        if (prefs.notifyFavoritesOnly && !loc.isFavorite) return

        val score = telemetry.verdict.score
        if (score >= prefs.minScoreThreshold) {
            if (notificationSettingsManager.shouldNotifyForLocation(loc.name)) {
                val app = getApplication<Application>()
                val statusLabel = app.getString(telemetry.verdict.level.shortLabelRes)
                val recommendation = telemetry.verdict.primaryLimitingFactor
                val sent = notificationHelper.sendOutdoorWindowNotification(
                    locationName = loc.name,
                    score = score,
                    statusLabel = statusLabel,
                    recommendation = recommendation
                )
                if (sent) {
                    notificationSettingsManager.recordNotificationSent(loc.name)
                }
            }
        }
    }

    fun openNotificationSettings() {
        _uiState.update { it.copy(isNotificationSettingsOpen = true) }
    }

    fun closeNotificationSettings() {
        _uiState.update { it.copy(isNotificationSettingsOpen = false) }
    }

    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        notificationSettingsManager.setEnabled(preferences.isEnabled)
        notificationSettingsManager.setMinScoreThreshold(preferences.minScoreThreshold)
        notificationSettingsManager.setNotifyFavoritesOnly(preferences.notifyFavoritesOnly)
        _uiState.update { it.copy(notificationPreferences = preferences) }
    }

    fun sendTestNotification() {
        notificationHelper.sendTestNotification()
    }

    fun dismissDialog() {
        engagementManager.onUserRemindLater()
        _uiState.update { it.copy(activeDialog = EngagementDialogType.NONE) }
    }

    fun onRateNow() {
        engagementManager.onUserRated()
        _uiState.update {
            it.copy(
                activeDialog = EngagementDialogType.NONE,
                usageMetrics = engagementManager.getUsageMetrics()
            )
        }
    }

    fun onDismissRatingPermanently() {
        engagementManager.onUserDismissedPermanently()
        _uiState.update {
            it.copy(
                activeDialog = EngagementDialogType.NONE,
                usageMetrics = engagementManager.getUsageMetrics()
            )
        }
    }

    fun onShareNow(shareText: String) {
        engagementManager.onUserShared(shareText)
        _uiState.update {
            it.copy(
                activeDialog = EngagementDialogType.NONE,
                usageMetrics = engagementManager.getUsageMetrics()
            )
        }
    }

    fun triggerManualShare() {
        val loc = _uiState.value.activeLocation?.name ?: return
        val telemetry = (_uiState.value.telemetryState as? TelemetryUiState.Success)?.data
        val score = telemetry?.verdict?.score ?: 85
        val app = getApplication<Application>()
        val label = telemetry?.verdict?.level?.let { app.getString(it.shortLabelRes) }
            ?: app.getString(com.example.R.string.verdict_go_short)
        val shareText = app.getString(com.example.R.string.share_summary_text, loc, score, label)
        engagementManager.launchShareIntent(shareText)
    }

    fun openPrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = true) }
    }

    fun closePrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = false) }
    }

    fun openUsageStats() {
        _uiState.update {
            it.copy(
                isUsageStatsOpen = true,
                usageMetrics = engagementManager.getUsageMetrics()
            )
        }
    }

    fun closeUsageStats() {
        _uiState.update { it.copy(isUsageStatsOpen = false) }
    }

    fun triggerUpdateDialogManually() {
        _uiState.update { it.copy(activeDialog = EngagementDialogType.UPDATE_APP) }
    }
}
