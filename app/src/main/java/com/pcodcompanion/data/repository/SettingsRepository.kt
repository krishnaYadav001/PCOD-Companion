package com.pcodcompanion.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pcod_settings", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _suggestionHandledDate = MutableStateFlow(prefs.getString("suggestion_handled_date", "") ?: "")
    val suggestionHandledDate: StateFlow<String> = _suggestionHandledDate.asStateFlow()

    // ── Adaptive reminders ──
    // Master switch (default OFF — opt-in, since notifications need permission)
    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean("reminders_enabled", false))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    // "Low" | "Medium" | "High" — Low ≈ 1/day, Medium ≈ 2/day, High ≈ 3/day
    private val _reminderIntensity = MutableStateFlow(prefs.getString("reminder_intensity", "Medium") ?: "Medium")
    val reminderIntensity: StateFlow<String> = _reminderIntensity.asStateFlow()

    // ── Appearance ──
    // "Light" | "Dark" | "System"
    private val _themePreference = MutableStateFlow(prefs.getString("theme_preference", "System") ?: "System")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    // ── Quiet Mode ──
    // When true: skip non-essential snackbars, suggestion cards, and routine reminder pings.
    // Logging, tracking, and re-engagement (safety) reminders still work.
    private val _quietMode = MutableStateFlow(prefs.getBoolean("quiet_mode", false))
    val quietMode: StateFlow<Boolean> = _quietMode.asStateFlow()

    fun setUserName(name: String) {
        prefs.edit { putString("user_name", name) }
        _userName.value = name
    }

    fun getUserName(): String = prefs.getString("user_name", "") ?: ""

    fun setSuggestionHandledDate(date: String) {
        prefs.edit { putString("suggestion_handled_date", date) }
        _suggestionHandledDate.value = date
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("reminders_enabled", enabled) }
        _remindersEnabled.value = enabled
    }

    fun setReminderIntensity(level: String) {
        prefs.edit { putString("reminder_intensity", level) }
        _reminderIntensity.value = level
    }

    fun setThemePreference(value: String) {
        prefs.edit { putString("theme_preference", value) }
        _themePreference.value = value
    }

    fun setQuietMode(enabled: Boolean) {
        prefs.edit { putBoolean("quiet_mode", enabled) }
        _quietMode.value = enabled
    }

    // Direct prefs (no flow needed — workers read these synchronously)
    fun getLastAppOpenDate(): String = prefs.getString("last_app_open_date", "") ?: ""
    fun setLastAppOpenDate(date: String) {
        prefs.edit { putString("last_app_open_date", date) }
    }

    fun getReminderIgnoreCount(): Int = prefs.getInt("reminder_ignore_count", 0)
    fun setReminderIgnoreCount(count: Int) {
        prefs.edit { putInt("reminder_ignore_count", count) }
    }

    fun wasFirstLaunchReminderPromptShown(): Boolean =
        prefs.getBoolean("first_launch_reminder_prompt_shown", false)
    fun markFirstLaunchReminderPromptShown() {
        prefs.edit { putBoolean("first_launch_reminder_prompt_shown", true) }
    }

    fun getLastReengagementDate(): String = prefs.getString("last_reengagement_date", "") ?: ""
    fun setLastReengagementDate(date: String) {
        prefs.edit { putString("last_reengagement_date", date) }
    }

    fun getLastClosureShownDate(): String = prefs.getString("last_closure_shown_date", "") ?: ""
    fun setLastClosureShownDate(date: String) {
        prefs.edit { putString("last_closure_shown_date", date) }
    }

    fun wasFirstInsightHelperShown(): Boolean =
        prefs.getBoolean("first_insight_helper_shown", false)
    fun markFirstInsightHelperShown() {
        prefs.edit { putBoolean("first_insight_helper_shown", true) }
    }

    /**
     * Wipes all stored preferences and resets in-memory flows to their defaults.
     * Used by the "Reset All Data" action. Caller is responsible for cancelling
     * any WorkManager schedules separately.
     */
    fun clearAll() {
        prefs.edit { clear() }
        _userName.value = ""
        _suggestionHandledDate.value = ""
        _remindersEnabled.value = false
        _reminderIntensity.value = "Medium"
        _themePreference.value = "System"
        _quietMode.value = false
    }
}
