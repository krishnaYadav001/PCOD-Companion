package com.pcodcompanion.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pcodcompanion.data.local.entity.DailyLog
import com.pcodcompanion.data.repository.PCODRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HistoryUiState(
    val logs: List<DailyLog> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val selectedLog: DailyLog? = null,
    val isSheetOpen: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: PCODRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var logCollectorJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getAllLogs().collect { logs ->
                _uiState.update { it.copy(logs = logs) }
            }
        }
    }

    fun nextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
    }

    fun previousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, isSheetOpen = true) }
        
        logCollectorJob?.cancel()
        logCollectorJob = viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.getLogByDate(dateStr).collect { log ->
                _uiState.update { it.copy(selectedLog = log) }
            }
        }
    }

    fun closeSheet() {
        _uiState.update { it.copy(isSheetOpen = false, selectedDate = null, selectedLog = null) }
        logCollectorJob?.cancel()
    }

    // ═══════════════════════════════════════
    // ── Optimistic log helper ──
    // ═══════════════════════════════════════
    private fun updateLogOptimistic(transform: (DailyLog) -> DailyLog) {
        val date = _uiState.value.selectedDate ?: return
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val current = _uiState.value.selectedLog

        if (current != null) {
            val updated = transform(current)
            // Optimistic update of selected log
            _uiState.update { it.copy(selectedLog = updated) }
            viewModelScope.launch { repository.updateLog(updated) }
        } else {
            val newLog = transform(DailyLog(date = dateStr))
            _uiState.update { it.copy(selectedLog = newLog) }
            viewModelScope.launch { repository.insertLog(newLog) }
        }
    }

    // ── Trackers ──
    fun updateMood(mood: String) = updateLogOptimistic { it.copy(mood = mood) }
    fun updateWaterIntake(glasses: Int) = updateLogOptimistic { it.copy(waterIntake = glasses) }
    fun updateSymptoms(symptoms: String) = updateLogOptimistic { it.copy(symptoms = symptoms) }
    fun updateSleepHours(hours: Float) = updateLogOptimistic { it.copy(sleepHours = hours) }
    fun updateSleepQuality(quality: String) = updateLogOptimistic { it.copy(sleepQuality = quality) }
    fun updateProteinIncluded(included: Boolean) = updateLogOptimistic { it.copy(proteinIncluded = included) }
    
    fun deleteLog() {
        val current = _uiState.value.selectedLog ?: return
        viewModelScope.launch {
            repository.deleteLog(current)
            closeSheet()
        }
    }
}
