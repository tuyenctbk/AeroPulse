package com.example.ui.engagement

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri

enum class EngagementDialogType {
    NONE,
    RATE_APP,
    SHARE_APP,
    UPDATE_APP
}

class EngagementManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aeropulse_engagement_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FIRST_LAUNCH = "first_launch_timestamp"
        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_RADAR_CHECKS = "radar_check_count"
        private const val KEY_LAST_PROMPT = "last_prompt_timestamp"
        private const val KEY_HAS_RATED = "has_rated"
        private const val KEY_HAS_SHARED = "has_shared"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"

        private const val MIN_RADAR_CHECKS_FOR_RATING = 3
        private const val MIN_DAYS_BETWEEN_PROMPTS_MS = 3 * 24 * 60 * 60 * 1000L // 3 days
    }

    init {
        if (!prefs.contains(KEY_FIRST_LAUNCH)) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0)
        prefs.edit().putInt(KEY_SESSION_COUNT, sessions + 1).apply()
    }

    fun recordRadarCheck(): EngagementDialogType {
        val checks = prefs.getInt(KEY_RADAR_CHECKS, 0) + 1
        prefs.edit().putInt(KEY_RADAR_CHECKS, checks).apply()
        return evaluateSmartPrompt()
    }

    fun evaluateSmartPrompt(): EngagementDialogType {
        val now = System.currentTimeMillis()
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0L)
        val hasRated = prefs.getBoolean(KEY_HAS_RATED, false)
        val hasShared = prefs.getBoolean(KEY_HAS_SHARED, false)
        val checks = prefs.getInt(KEY_RADAR_CHECKS, 0)

        // Don't disturb user if prompted recently
        if (now - lastPrompt < MIN_DAYS_BETWEEN_PROMPTS_MS) {
            return EngagementDialogType.NONE
        }

        // Priority 1: Rating prompt after user experienced multiple successful radar scans
        if (!hasRated && checks >= MIN_RADAR_CHECKS_FOR_RATING) {
            return EngagementDialogType.RATE_APP
        }

        // Priority 2: Share prompt after 6 checks
        if (!hasShared && checks >= 6) {
            return EngagementDialogType.SHARE_APP
        }

        return EngagementDialogType.NONE
    }

    fun markPromptShown() {
        prefs.edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis()).apply()
    }

    fun onUserRated() {
        prefs.edit()
            .putBoolean(KEY_HAS_RATED, true)
            .putLong(KEY_LAST_PROMPT, System.currentTimeMillis())
            .apply()
        openPlayStore()
    }

    fun onUserShared(summaryText: String) {
        prefs.edit()
            .putBoolean(KEY_HAS_SHARED, true)
            .putLong(KEY_LAST_PROMPT, System.currentTimeMillis())
            .apply()
        launchShareIntent(summaryText)
    }

    fun onUserRemindLater() {
        // Postpone by setting last prompt to now
        prefs.edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis()).apply()
    }

    fun onUserDismissedPermanently() {
        prefs.edit()
            .putBoolean(KEY_HAS_RATED, true)
            .putLong(KEY_LAST_PROMPT, System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L))
            .apply()
    }

    fun openPlayStore() {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun launchShareIntent(summaryText: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, summaryText)
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Outdoor Window via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }
}
