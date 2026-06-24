package com.pcodcompanion.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pcodcompanion.data.backup.BackupManager
import com.pcodcompanion.data.backup.ImportResult
import com.pcodcompanion.data.repository.PCODRepository
import com.pcodcompanion.data.repository.SettingsRepository
import com.pcodcompanion.worker.WorkManagerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ExportReadyEvent(val file: File)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val settings: SettingsRepository,
    private val repository: PCODRepository
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    private val _exportReady = MutableSharedFlow<ExportReadyEvent>(extraBufferCapacity = 1)
    val exportReady: SharedFlow<ExportReadyEvent> = _exportReady.asSharedFlow()

    val remindersEnabled: StateFlow<Boolean> = settings.remindersEnabled
    val reminderIntensity: StateFlow<String> = settings.reminderIntensity
    val userName: StateFlow<String> = settings.userName
    val themePreference: StateFlow<String> = settings.themePreference
    val quietMode: StateFlow<Boolean> = settings.quietMode

    fun setThemePreference(value: String) {
        settings.setThemePreference(value)
    }

    fun setQuietMode(enabled: Boolean) {
        settings.setQuietMode(enabled)
    }

    fun setUserName(name: String) {
        settings.setUserName(name.trim())
    }

    fun resetAllData() {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.deleteAllLogs()
                repository.deleteAllCycles()
                repository.deleteAllPlanItems()
                repository.deleteAllInsights()
                settings.clearAll()
                WorkManagerHelper.applyReminderSchedule(context, enabled = false, intensity = "Medium")
                _toast.tryEmit("All data cleared. Fresh start 🌷")
            } catch (e: Exception) {
                _toast.tryEmit("Reset failed: ${e.message}")
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        settings.setRemindersEnabled(enabled)
        WorkManagerHelper.applyReminderSchedule(context, enabled, settings.reminderIntensity.value)
    }

    fun setReminderIntensity(level: String) {
        settings.setReminderIntensity(level)
        if (settings.remindersEnabled.value) {
            WorkManagerHelper.applyReminderSchedule(context, true, level)
        }
    }

    fun sendFeedback(rating: Int, message: String) {
        val ratingLine = if (rating in 1..5) "Rating: " + "★".repeat(rating) + "☆".repeat(5 - rating) + "\n\n" else ""
        val body = ratingLine + message.trim() + "\n\n---\nSent from PCOD Companion"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "PCOD Companion — Feedback")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            val chooser = Intent.createChooser(intent, "Send feedback via")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            _toast.tryEmit("Thanks for sharing 🌷")
        } catch (e: Exception) {
            _toast.tryEmit("No email app found on this device")
        }
    }

    companion object {
        const val FEEDBACK_EMAIL = "krishnayadav123345@gmail.com"
    }

    fun exportBackup() {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val json = backupManager.exportToJson()
                val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
                val file = File(context.cacheDir, "pcod-companion-backup-$stamp.json")
                withContext(Dispatchers.IO) { file.writeText(json) }
                _exportReady.tryEmit(ExportReadyEvent(file))
            } catch (e: Exception) {
                _toast.tryEmit("Export failed: ${e.message}")
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun importBackup(uri: Uri) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                } ?: throw IllegalStateException("Could not read file")
                when (val result = backupManager.importFromJsonReplace(json)) {
                    is ImportResult.Success -> _toast.tryEmit(
                        "Restored ${result.logs} logs · ${result.cycles} cycles · ${result.plans} plan items 🌷"
                    )
                    is ImportResult.Failure -> _toast.tryEmit(result.message)
                }
            } catch (e: Exception) {
                _toast.tryEmit("Import failed: ${e.message}")
            } finally {
                _isBusy.value = false
            }
        }
    }
}
