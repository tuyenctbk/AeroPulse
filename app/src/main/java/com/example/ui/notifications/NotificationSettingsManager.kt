package com.example.ui.notifications

import android.content.Context
import android.content.SharedPreferences

data class NotificationPreferences(
    val isEnabled: Boolean = true,
    val minScoreThreshold: Int = 75,
    val notifyFavoritesOnly: Boolean = false
)

class NotificationSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aeropulse_notification_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED = "notifications_enabled"
        private const val KEY_MIN_SCORE = "min_score_threshold"
        private const val KEY_FAVS_ONLY = "notify_favs_only"
        private const val KEY_LAST_ALERT_PREFIX = "last_alert_time_"
        private const val MIN_NOTIFICATION_INTERVAL_MS = 60 * 60 * 1000L // 1 hour cooldown per location
    }

    fun getPreferences(): NotificationPreferences {
        return NotificationPreferences(
            isEnabled = prefs.getBoolean(KEY_ENABLED, true),
            minScoreThreshold = prefs.getInt(KEY_MIN_SCORE, 75),
            notifyFavoritesOnly = prefs.getBoolean(KEY_FAVS_ONLY, false)
        )
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setMinScoreThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_MIN_SCORE, threshold).apply()
    }

    fun setNotifyFavoritesOnly(favsOnly: Boolean) {
        prefs.edit().putBoolean(KEY_FAVS_ONLY, favsOnly).apply()
    }

    fun shouldNotifyForLocation(locationName: String): Boolean {
        val lastTime = prefs.getLong(KEY_LAST_ALERT_PREFIX + locationName, 0L)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastTime) > MIN_NOTIFICATION_INTERVAL_MS
    }

    fun recordNotificationSent(locationName: String) {
        prefs.edit().putLong(KEY_LAST_ALERT_PREFIX + locationName, System.currentTimeMillis()).apply()
    }
}
