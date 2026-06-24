package com.pcodcompanion.ui.cycletracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pcodcompanion.data.local.entity.CycleEntry
import com.pcodcompanion.data.repository.PCODRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class CycleTrackerUiState(
    val entries: List<CycleEntry> = emptyList(),
    val activeCycle: CycleEntry? = null,
    val averageCycleLength: Int = 0,
    val daysSinceLastPeriod: Long = 0
)

@HiltViewModel
class CycleTrackerViewModel @Inject constructor(
    private val repository: PCODRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CycleTrackerUiState())
    val uiState: StateFlow<CycleTrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllCycleEntries().collect { entries ->
                val avg = calculateAverageCycleLength(entries)
                val daysSince = if (entries.isNotEmpty()) {
                    val lastStart = LocalDate.parse(entries.first().startDate)
                    ChronoUnit.DAYS.between(lastStart, LocalDate.now())
                } else 0L
                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    averageCycleLength = avg,
                    daysSinceLastPeriod = daysSince
                )
            }
        }
        viewModelScope.launch {
            repository.getActiveCycle().collect { active ->
                _uiState.value = _uiState.value.copy(activeCycle = active)
            }
        }
    }

    private fun calculateAverageCycleLength(entries: List<CycleEntry>): Int {
        if (entries.size < 2) return 28
        val lengths = entries.zipWithNext().mapNotNull { (newer, older) ->
            try {
                val start1 = LocalDate.parse(older.startDate)
                val start2 = LocalDate.parse(newer.startDate)
                ChronoUnit.DAYS.between(start1, start2).toInt()
            } catch (e: Exception) { null }
        }
        return if (lengths.isNotEmpty()) lengths.average().toInt() else 28
    }

    fun startPeriod() {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.insertCycleEntry(CycleEntry(startDate = today))
        }
    }

    fun endPeriod() {
        viewModelScope.launch {
            val active = _uiState.value.activeCycle ?: return@launch
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.updateCycleEntry(active.copy(endDate = today))
        }
    }

    fun updateFlowLevel(level: String) {
        viewModelScope.launch {
            val active = _uiState.value.activeCycle ?: return@launch
            repository.updateCycleEntry(active.copy(flowLevel = level))
        }
    }

    fun addManualCycle(start: String, end: String?, flow: String) {
        viewModelScope.launch {
            repository.insertCycleEntry(CycleEntry(startDate = start, endDate = end, flowLevel = flow))
        }
    }

    fun editCycle(entry: CycleEntry, newStart: String, newEnd: String?, newFlow: String) {
        viewModelScope.launch {
            repository.updateCycleEntry(
                entry.copy(startDate = newStart, endDate = newEnd, flowLevel = newFlow)
            )
        }
    }

    fun deleteCycle(entry: CycleEntry) {
        viewModelScope.launch {
            repository.deleteCycleEntry(entry)
        }
    }
}
