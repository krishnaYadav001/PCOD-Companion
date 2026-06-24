package com.pcodcompanion.ui.planbuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pcodcompanion.data.local.entity.PlanItem
import com.pcodcompanion.data.repository.PCODRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class PlanBuilderUiState(
    val items: List<PlanItem> = emptyList(),
    val selectedCategory: String = "All",
    val selectedDayTab: String = LocalDate.now().dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
    val showAddDialog: Boolean = false,
    val showGeneratorDialog: Boolean = false,
    val dayHistoryHint: String? = null
)

@HiltViewModel
class PlanBuilderViewModel @Inject constructor(
    private val repository: PCODRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanBuilderUiState())
    val uiState: StateFlow<PlanBuilderUiState> = _uiState.asStateFlow()

    private val _feedbackEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedbackEvents: SharedFlow<String> = _feedbackEvents.asSharedFlow()

    private val planFeedback = listOf(
        "Your week looks balanced 🌿",
        "Nice and manageable plan 💛",
        "A gentle routine in motion 🌸",
        "Small steps, big care ✨",
        "This rhythm feels good 💧"
    )

    private fun emitPlanFeedback() {
        _feedbackEvents.tryEmit(planFeedback.random())
    }

    init {
        loadItems()
        computeDayHistoryHint(_uiState.value.selectedDayTab)
    }

    private fun loadItems() {
        viewModelScope.launch {
            repository.getAllPlanItems().collect { items ->
                val state = _uiState.value
                val filtered = items.filter { item -> 
                    (state.selectedCategory == "All" || item.category == state.selectedCategory) &&
                    item.daysOfWeek.contains(state.selectedDayTab)
                }
                _uiState.value = state.copy(items = filtered)
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadItems()
    }

    fun selectDayTab(day: String) {
        _uiState.value = _uiState.value.copy(selectedDayTab = day)
        loadItems()
        computeDayHistoryHint(day)
    }

    /**
     * Build a small "you usually complete X% of your <Day> plan" preview from
     * `DailyLog.planCompletionPct`, which DailyResetWorker snapshots at midnight
     * before wiping plan-item checkboxes. Logs with -1 are "not measured yet"
     * (e.g. all pre-existing logs from before this column existed) and are
     * filtered out. Hidden when fewer than 3 measured logs exist for that
     * weekday — not enough signal.
     */
    private fun computeDayHistoryHint(dayTab: String) {
        viewModelScope.launch {
            val targetDow = dayTabToDow(dayTab)
            if (targetDow == null) {
                _uiState.value = _uiState.value.copy(dayHistoryHint = null)
                return@launch
            }
            val measuredLogs = repository.getAllLogsSnapshot().filter { log ->
                log.planCompletionPct >= 0 &&
                    runCatching { LocalDate.parse(log.date).dayOfWeek == targetDow }
                        .getOrDefault(false)
            }
            if (measuredLogs.size < 3) {
                _uiState.value = _uiState.value.copy(dayHistoryHint = null)
                return@launch
            }
            val avgPct = measuredLogs.map { it.planCompletionPct }.average().toInt()

            _uiState.value = _uiState.value.copy(
                dayHistoryHint = "You usually complete $avgPct% of your $dayTab plan 🌿"
            )
        }
    }

    private fun dayTabToDow(day: String): DayOfWeek? = when (day) {
        "Mon" -> DayOfWeek.MONDAY
        "Tue" -> DayOfWeek.TUESDAY
        "Wed" -> DayOfWeek.WEDNESDAY
        "Thu" -> DayOfWeek.THURSDAY
        "Fri" -> DayOfWeek.FRIDAY
        "Sat" -> DayOfWeek.SATURDAY
        "Sun" -> DayOfWeek.SUNDAY
        else -> null
    }

    fun toggleShowAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = !_uiState.value.showAddDialog)
    }
    
    fun toggleShowGeneratorDialog() {
        _uiState.value = _uiState.value.copy(showGeneratorDialog = !_uiState.value.showGeneratorDialog)
    }

    fun addItem(title: String, description: String, category: String, daysOfWeek: String) {
        viewModelScope.launch {
            // Find max orderIndex
            val currentItems = _uiState.value.items
            val maxOrder = currentItems.maxOfOrNull { it.orderIndex } ?: -1
            repository.insertPlanItem(
                PlanItem(
                    title = title,
                    description = description,
                    category = category,
                    orderIndex = maxOrder + 1,
                    daysOfWeek = daysOfWeek
                )
            )
            _uiState.value = _uiState.value.copy(showAddDialog = false)
            emitPlanFeedback()
        }
    }

    fun editItem(item: PlanItem, newTitle: String, newDesc: String, newCat: String, newDays: String) {
        viewModelScope.launch {
            repository.updatePlanItem(
                item.copy(
                    title = newTitle,
                    description = newDesc,
                    category = newCat,
                    daysOfWeek = newDays
                )
            )
            emitPlanFeedback()
        }
    }

    fun deleteItem(item: PlanItem) {
        viewModelScope.launch {
            repository.deletePlanItem(item)
        }
    }

    fun generateRoutine(wakeTime: String, sleepTime: String, diet: String, goal: String) {
        viewModelScope.launch {
            // Clear existing plan
            repository.deleteAllPlanItems()

            val newItems = mutableListOf<PlanItem>()
            var order = 0
            val allDays = "Mon,Tue,Wed,Thu,Fri,Sat,Sun"
            
            // Helper to add item
            fun addPlan(title: String, desc: String, cat: String, days: String = allDays) {
                newItems.add(PlanItem(title = title, description = desc, category = cat, orderIndex = order++, daysOfWeek = days))
            }

            // Morning
            val drinkRotation = listOf("Fennel Seed Water", "Lemon & Warm Water", "Cinnamon Tea")
            val morningDrink = drinkRotation.random()
            addPlan("Morning Drink: $morningDrink", "Have this shortly after waking up at $wakeTime to help with digestion and insulin.", "Diet")
            
            // Breakfast
            val breakfastDesc = if (diet == "Vegan") "High protein tofu scramble or protein smoothie (low sugar)."
                                else if (diet == "Non-Veg") "Eggs (boiled or scrambled) with spinach and avocado."
                                else "Besan chilla or Greek yogurt with nuts and seeds."
            addPlan("High-Protein Breakfast", breakfastDesc, "Diet")
            
            // Supplements (if any based on goal)
            if (goal.contains("Insulin", ignoreCase = true) || goal.contains("Weight", ignoreCase = true)) {
                addPlan("Take Supplements", "Inositol or Omega-3 as prescribed.", "Lifestyle")
            }

            // Exercise
            val exerciseDesc = if (goal.contains("Weight", ignoreCase = true)) "30 min brisk walk + 15 min strength training."
                               else if (goal.contains("Cycle", ignoreCase = true)) "30 min Yoga (butterfly pose, cobra pose)."
                               else "30 min walk or stretching."
            addPlan("Daily Movement", exerciseDesc, "Exercise")

            // Lunch
            addPlan("Balanced Lunch", "Include complex carbs (quinoa/brown rice), protein, and lots of veggies.", "Diet")

            // Evening Snack
            addPlan("Evening Snack", "Handful of roasted makhana or almonds. Avoid sugary snacks.", "Diet")

            // Dinner
            addPlan("Light Dinner", "Soup, salad, or light protein. Finish 2 hours before your $sleepTime sleep time.", "Diet")

            // Bedtime
            addPlan("Bedtime Routine", "Spearmint tea (good for hormones) and digital detox.", "Lifestyle")
            
            // Insert all generated items
            newItems.forEach {
                repository.insertPlanItem(it)
            }

            _uiState.value = _uiState.value.copy(showGeneratorDialog = false)
            emitPlanFeedback()
        }
    }
}
