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

data class AppUsageMetrics(
    val sessionCount: Int,
    val radarCheckCount: Int,
    val daysSinceFirstLaunch: Int,
    val hasRated: Boolean,
    val hasShared: Boolean,
    val ratingPermanentlyDismissed: Boolean
)

class EngagementManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aeropulse_engagement_prefs", Context.MODE_PRIVATE)

    private var promptShownThisSession: Boolean = false

    companion object {
        const val KEY_FIRST_LAUNCH = "first_launch_timestamp"
        const val KEY_SESSION_COUNT = "session_count"
        const val KEY_RADAR_CHECKS = "radar_check_count"
        const val KEY_LAST_PROMPT = "last_prompt_timestamp"
        const val KEY_HAS_RATED = "has_rated"
        const val KEY_RATING_DISMISSED_PERMANENTLY = "rating_dismissed_permanently"
        const val KEY_HAS_SHARED = "has_shared"
        const val KEY_LAST_UPDATE_PROMPT = "last_update_prompt_timestamp"

        const val MIN_GLOBAL_PROMPT_INTERVAL_MS = 5 * 24 * 60 * 60 * 1000L // 5 days minimum between any prompt to avoid bothering users
        const val MIN_POSTPONE_INTERVAL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days after 'Remind Later'
        const val MIN_UPDATE_PROMPT_INTERVAL_MS = 14 * 24 * 60 * 60 * 1000L // 14 days between update prompts
    }

    init {
        if (!prefs.contains(KEY_FIRST_LAUNCH)) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SESSION_COUNT, sessions).apply()
    }

    fun getUsageMetrics(): AppUsageMetrics {
        val firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, System.currentTimeMillis())
        val days = ((System.currentTimeMillis() - firstLaunch) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
        return AppUsageMetrics(
            sessionCount = prefs.getInt(KEY_SESSION_COUNT, 1),
            radarCheckCount = prefs.getInt(KEY_RADAR_CHECKS, 0),
            daysSinceFirstLaunch = days,
            hasRated = prefs.getBoolean(KEY_HAS_RATED, false),
            hasShared = prefs.getBoolean(KEY_HAS_SHARED, false),
            ratingPermanentlyDismissed = prefs.getBoolean(KEY_RATING_DISMISSED_PERMANENTLY, false)
        )
    }

    /**
     * Records a radar telemetry check and determines if a suggestion dialog should be triggered.
     * @param verdictScore The Go/No-Go score of the active window.
     * @param isInitialLaunch True if triggered during initial app startup (never prompts on start).
     */
    fun recordRadarCheck(verdictScore: Int = 85, isInitialLaunch: Boolean = false): EngagementDialogType {
        val checks = prefs.getInt(KEY_RADAR_CHECKS, 0) + 1
        prefs.edit().putInt(KEY_RADAR_CHECKS, checks).apply()

        // Critical UX Rule: Never interrupt the user with popups upon initial application launch
        if (isInitialLaunch) {
            return EngagementDialogType.NONE
        }

        return evaluateSmartPrompt(verdictScore)
    }

    fun evaluateSmartPrompt(verdictScore: Int = 85): EngagementDialogType {
        // Enforce maximum 1 auto-prompt per application session
        if (promptShownThisSession) {
            return EngagementDialogType.NONE
        }

        val now = System.currentTimeMillis()
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0L)

        // Strict anti-spam rule: minimum 5 days (120 hours) between auto-popups to avoid annoying users
        if (now - lastPrompt < MIN_GLOBAL_PROMPT_INTERVAL_MS) {
            return EngagementDialogType.NONE
        }

        val sessions = prefs.getInt(KEY_SESSION_COUNT, 1)
        val checks = prefs.getInt(KEY_RADAR_CHECKS, 0)
        val hasRated = prefs.getBoolean(KEY_HAS_RATED, false)
        val ratingDismissedPermanently = prefs.getBoolean(KEY_RATING_DISMISSED_PERMANENTLY, false)
        val hasShared = prefs.getBoolean(KEY_HAS_SHARED, false)
        val lastUpdatePrompt = prefs.getLong(KEY_LAST_UPDATE_PROMPT, 0L)

        // Priority 1: Rate App (User has used the app for >= 3 scans, and current score >= 60)
        if (!hasRated && !ratingDismissedPermanently && checks >= 3 && verdictScore >= 60) {
            return EngagementDialogType.RATE_APP
        }

        // Priority 2: Share App (User has verified value with >= 5 scans and hits a stellar window >= 80)
        if (!hasShared && checks >= 5 && verdictScore >= 80) {
            return EngagementDialogType.SHARE_APP
        }

        // Priority 3: Update App (User is active with 8+ sessions or 12+ scans, and 14+ days since last update prompt)
        if (sessions >= 8 && (now - lastUpdatePrompt > MIN_UPDATE_PROMPT_INTERVAL_MS)) {
            return EngagementDialogType.UPDATE_APP
        }

        return EngagementDialogType.NONE
    }

    fun markPromptShown(type: EngagementDialogType) {
        promptShownThisSession = true
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_PROMPT, now).apply()
        if (type == EngagementDialogType.UPDATE_APP) {
            prefs.edit().putLong(KEY_LAST_UPDATE_PROMPT, now).apply()
        }
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
        // Postpone by setting last prompt timestamp so user is not prompted again for at least 7 days
        prefs.edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis() + (MIN_POSTPONE_INTERVAL_MS - MIN_GLOBAL_PROMPT_INTERVAL_MS)).apply()
    }

    fun onUserDismissedPermanently() {
        prefs.edit()
            .putBoolean(KEY_RATING_DISMISSED_PERMANENTLY, true)
            .putBoolean(KEY_HAS_RATED, true)
            .putLong(KEY_LAST_PROMPT, System.currentTimeMillis())
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
        val chooserTitle = context.getString(com.example.R.string.share_chooser_title)
        val shareIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }
}
