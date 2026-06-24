package com.pcodcompanion.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pcodcompanion.data.local.entity.DailyLog
import com.pcodcompanion.data.local.entity.Medication
import com.pcodcompanion.data.local.entity.PlanItem
import com.pcodcompanion.data.local.entity.getMedicationsList
import com.pcodcompanion.data.local.entity.withUpdatedMedications
import com.pcodcompanion.data.insight.InsightGenerator
import com.pcodcompanion.data.local.entity.Insight
import com.pcodcompanion.data.repository.PCODRepository
import com.pcodcompanion.data.repository.SettingsRepository
import com.pcodcompanion.data.repository.WeeklyStats
import com.pcodcompanion.worker.WorkManagerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

object FeedbackMessages {
    private val task = listOf(
        "Nice! You're staying consistent 🌱",
        "One step closer to your goal 🌸",
        "Way to follow through ✨",
        "Small wins add up 💫"
    )
    private val water = listOf(
        "Great hydration today 💧",
        "Sip sip hooray 🌊",
        "Your body thanks you 💧",
        "Keep that flow going 🪷"
    )
    private val exercise = listOf(
        "Well done on your workout 🧘‍♀️",
        "Movement is medicine 💪",
        "You showed up for yourself ✨",
        "Strong work today 🌟"
    )
    private val fullDay = listOf(
        "Full plan done — proud of you 🌷",
        "You did it all today, glow on ✨",
        "Beautiful day completed 🌸"
    )

    private var lastTask = -1
    private var lastWater = -1
    private var lastExercise = -1
    private var lastFullDay = -1

    fun task(): String = pick(task, lastTask).also { lastTask = task.indexOf(it) }
    fun water(): String = pick(water, lastWater).also { lastWater = water.indexOf(it) }
    fun exercise(): String = pick(exercise, lastExercise).also { lastExercise = exercise.indexOf(it) }
    fun fullDay(): String = pick(fullDay, lastFullDay).also { lastFullDay = fullDay.indexOf(it) }

    private fun pick(list: List<String>, lastIndex: Int): String {
        if (list.size <= 1) return list.first()
        var idx: Int
        do { idx = list.indices.random() } while (idx == lastIndex)
        return list[idx]
    }
}

data class DailyFocus(
    val message: String,
    val emoji: String,
    val accentHue: Float   // HSL hue for pastel tint (0-360)
)

data class PlanSuggestion(
    val title: String,
    val message: String,
    val actionLabel: String
)

enum class CyclePhase { PERIOD, EARLY, LATER }

data class CycleRecommendation(
    val phase: CyclePhase,
    val title: String,
    val message: String,
    val emoji: String
)

data class YesterdaysClosure(
    val percentage: Int?,          // 0..100 — null when there was no log to reflect on
    val supportiveLine: String     // tone scales with %; soft for low / no-log
)

data class TodayUiState(
    val todayLog: DailyLog? = null,
    val planItems: List<PlanItem> = emptyList(),
    val todayDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
    val greeting: String = "",
    val userName: String = "",
    val streakDays: Int = 0,
    val currentWeekStats: WeeklyStats? = null,
    val lastWeekStats: WeeklyStats? = null,
    val isLowEnergyMode: Boolean = false,
    val dailyFocus: DailyFocus? = null,
    val planSuggestion: PlanSuggestion? = null,
    val cycleRecommendation: CycleRecommendation? = null,
    val showFirstLaunchReminderPrompt: Boolean = false,
    val currentInsight: Insight? = null,
    val quietMode: Boolean = false,
    val closureCard: YesterdaysClosure? = null,
    val showFirstInsightHelper: Boolean = false
)

private fun getGreeting(userName: String): String {
    val hour = java.time.LocalTime.now().hour
    val baseGreeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    return if (userName.isNotBlank()) {
        "$baseGreeting, $userName ☀️"
    } else {
        "$baseGreeting ☀️"
    }
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: PCODRepository,
    private val settingsRepository: SettingsRepository,
    private val insightGenerator: InsightGenerator,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _feedbackEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val feedbackEvents: SharedFlow<String> = _feedbackEvents.asSharedFlow()

    /** Emit a feedback snackbar unless Quiet Mode is on. */
    private fun emitFeedback(message: String) {
        if (settingsRepository.quietMode.value) return
        _feedbackEvents.tryEmit(message)
    }

    init {
        // Show the one-time reminder opt-in prompt only on the very first launch,
        // and only if the user hasn't already enabled reminders elsewhere.
        if (!settingsRepository.wasFirstLaunchReminderPromptShown() &&
            !settingsRepository.remindersEnabled.value
        ) {
            _uiState.update { it.copy(showFirstLaunchReminderPrompt = true) }
        }
    }

    private var todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    private var logCollectorJob: Job? = null

    init {
        // Collect user name and update greeting
        viewModelScope.launch {
            settingsRepository.userName.collect { name ->
                _uiState.update { it.copy(userName = name, greeting = getGreeting(name)) }
            }
        }

        // Load streak
        viewModelScope.launch {
            val streak = repository.calculateStreak()
            _uiState.update { it.copy(streakDays = streak) }
        }

        // Generate daily focus from yesterday's data
        viewModelScope.launch {
            val focus = generateDailyFocus()
            _uiState.update { it.copy(dailyFocus = focus) }
        }

        // Load weekly stats
        viewModelScope.launch {
            val currentWeek = repository.getWeeklyStats(0)
            val lastWeek = repository.getWeeklyStats(1)
            _uiState.update { it.copy(currentWeekStats = currentWeek, lastWeekStats = lastWeek) }
        }

        // Smart plan suggestion (only if not already handled today)
        viewModelScope.launch {
            val handledToday = settingsRepository.suggestionHandledDate.value == todayStr
            if (!handledToday) {
                val suggestion = detectPlanSuggestion()
                _uiState.update { it.copy(planSuggestion = suggestion) }
            }
        }

        // Cycle-based recommendation (re-computed whenever cycle data changes)
        viewModelScope.launch {
            repository.getAllCycleEntries().collect { entries ->
                _uiState.update { it.copy(cycleRecommendation = computeCycleRecommendation(entries)) }
            }
        }

        // Quiet Mode — drives card visibility on Today screen and gates feedback snackbars
        viewModelScope.launch {
            settingsRepository.quietMode.collect { quiet ->
                _uiState.update { it.copy(quietMode = quiet) }
            }
        }

        // Insights — detect new patterns, then pick one to show (rotates every 3 days)
        viewModelScope.launch {
            insightGenerator.generateAndPersist()
            val insight = pickRotatingInsight()
            // First time we ever surface an insight → also show the helper caption.
            val showHelper = insight != null && !settingsRepository.wasFirstInsightHelperShown()
            if (showHelper) settingsRepository.markFirstInsightHelperShown()
            _uiState.update {
                it.copy(currentInsight = insight, showFirstInsightHelper = showHelper)
            }
        }

        // End-of-day closure — show ONE soft reflection card on the first app open per day
        viewModelScope.launch {
            if (settingsRepository.getLastClosureShownDate() != todayStr) {
                val closure = computeYesterdaysClosure()
                if (closure != null) {
                    _uiState.update { it.copy(closureCard = closure) }
                    settingsRepository.setLastClosureShownDate(todayStr)
                }
            }
        }

        startCollectingForToday()
        viewModelScope.launch {
            repository.getAllPlanItems().collect { items ->
                val currentDay = LocalDate.now().dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                val todaysItems = items.filter { it.daysOfWeek.contains(currentDay) }
                _uiState.update { it.copy(planItems = todaysItems) }
            }
        }
        scheduleMidnightRefresh()
    }

    // ═══════════════════════════════════════
    // ── Date-aware loading ──
    // ═══════════════════════════════════════

    private fun startCollectingForToday() {
        logCollectorJob?.cancel()
        logCollectorJob = viewModelScope.launch {
            repository.getLogByDate(todayStr).collect { log ->
                _uiState.update { it.copy(todayLog = log) }
            }
        }
    }

    /**
     * Waits until midnight, then refreshes the date, greeting, and
     * switches the log collector to the new day. Previous data stays
     * untouched in the database.
     */
    private fun scheduleMidnightRefresh() {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT)
                val delayMs = Duration.between(now, nextMidnight).toMillis() + 500 // +500ms buffer

                delay(delayMs)

                // New day!
                todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val currentName = _uiState.value.userName
                val newStreak = repository.calculateStreak()
                val newFocus = generateDailyFocus()
                val newSuggestion = if (settingsRepository.suggestionHandledDate.value != todayStr) {
                    detectPlanSuggestion()
                } else null
                _uiState.update {
                    it.copy(
                        todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        greeting = getGreeting(currentName),
                        todayLog = null,  // clear old day instantly
                        streakDays = newStreak,
                        dailyFocus = newFocus,
                        planSuggestion = newSuggestion
                    )
                }
                // Re-subscribe to today's log
                startCollectingForToday()

                // Reset plan checkboxes & create empty log
                val lastLog = repository.getLatestLogSync()
                val inheritedMeds = lastLog?.getMedicationsList()?.map { it.copy(isTaken = false) } ?: emptyList()
                repository.insertLogIfNotExists(DailyLog(date = todayStr).withUpdatedMedications(inheritedMeds))
                repository.resetAllPlanCompletions()
            }
        }
    }

    // ═══════════════════════════════════════
    // ── Daily Focus Generator ──
    // ═══════════════════════════════════════

    /**
     * Generates ONE focus for the day based on yesterday's log.
     * Priority order: low water → poor sleep → high stress → skipped exercise.
     * Falls back to a positive default when data is missing or all metrics are healthy.
     */
    private suspend fun generateDailyFocus(): DailyFocus {
        val yesterdayStr = LocalDate.now().minusDays(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Try yesterday first, then fall back to most recent log
        val recentLogs = repository.getRecentLogs(3)
        val yesterdayLog = recentLogs.firstOrNull { it.date == yesterdayStr }
        val referenceLog = yesterdayLog ?: recentLogs.firstOrNull()

        if (referenceLog == null) {
            // No data at all — encouraging default
            return DailyFocus(
                message = "Start your day with a healthy habit",
                emoji = "🌟",
                accentHue = 45f  // warm gold
            )
        }

        // Check metrics in priority order (return the first problem found)
        // 1. Low water (< 5 glasses)
        if (referenceLog.waterIntake < 5) {
            return DailyFocus(
                message = "Focus on hydration today",
                emoji = "💧",
                accentHue = 200f  // soft blue
            )
        }

        // 2. Poor sleep (< 6 hours or quality is "Poor" / "Disturbed")
        if (referenceLog.sleepHours < 6f ||
            referenceLog.sleepQuality.equals("Poor", ignoreCase = true) ||
            referenceLog.sleepQuality.equals("Disturbed", ignoreCase = true)
        ) {
            return DailyFocus(
                message = "Try to rest more today",
                emoji = "😴",
                accentHue = 260f  // soft lavender
            )
        }

        // 3. High stress
        if (referenceLog.stressLevel.equals("High", ignoreCase = true)) {
            return DailyFocus(
                message = "Take it slow today",
                emoji = "🌸",
                accentHue = 330f  // soft pink
            )
        }

        // 4. Skipped exercise
        if (!referenceLog.exerciseDone) {
            return DailyFocus(
                message = "Try a short walk today",
                emoji = "🚶\u200D♀️",
                accentHue = 140f  // soft green
            )
        }

        // Everything looks good!
        return DailyFocus(
            message = "You're doing great — keep it up!",
            emoji = "✨",
            accentHue = 45f  // warm gold
        )
    }

    // ═══════════════════════════════════════
    // ── Smart Plan Suggestion ──
    // ═══════════════════════════════════════

    /**
     * Looks at the most recent 5 days of logs and returns ONE suggestion if a
     * pattern is detected, in priority order:
     *   1. High stress (≥3 of 5 days) → rest-focused day
     *   2. Low energy (≥3 of 5 days)  → simplify tasks
     *   3. Exercise skipped (≥3 of 5 days) → lighter routine
     */
    private suspend fun detectPlanSuggestion(): PlanSuggestion? {
        val recent = repository.getRecentLogs(5)
        if (recent.size < 3) return null  // not enough data to spot a pattern

        val highStressDays = recent.count { it.stressLevel.equals("High", ignoreCase = true) }
        if (highStressDays >= 3) {
            return PlanSuggestion(
                title = "Rest-focused day?",
                message = "Stress has been high lately. Let's lean into rest today 💛",
                actionLabel = "Lighten up"
            )
        }

        val lowEnergyDays = recent.count {
            it.symptoms.contains("Low Energy", ignoreCase = true) ||
                it.mood.equals("Tired", ignoreCase = true)
        }
        if (lowEnergyDays >= 3) {
            return PlanSuggestion(
                title = "Take it slow",
                message = "You've been low on energy — let's simplify your plan for now 💛",
                actionLabel = "Simplify today"
            )
        }

        val skippedExerciseDays = recent.count { !it.exerciseDone }
        if (skippedExerciseDays >= 3) {
            return PlanSuggestion(
                title = "Lighter routine?",
                message = "Exercise has been on pause a few days. Want a gentler plan today? 💛",
                actionLabel = "Use light plan"
            )
        }

        return null
    }

    /**
     * Picks a recommendation based on the most recent cycle entry:
     *   - PERIOD: today is between startDate and endDate (or endDate is null/ongoing)
     *   - EARLY:  0–13 days since period started → light activity
     *   - LATER:  14–34 days since period started → normal activity
     *   - null:   no entries, or last entry too old to infer
     */
    private fun computeCycleRecommendation(entries: List<com.pcodcompanion.data.local.entity.CycleEntry>): CycleRecommendation? {
        if (entries.isEmpty()) return null
        val today = LocalDate.now()
        val mostRecent = entries.maxByOrNull { it.startDate } ?: return null
        val start = try { LocalDate.parse(mostRecent.startDate) } catch (e: Exception) { return null }
        val end = mostRecent.endDate?.let {
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        }

        val inPeriod = !today.isBefore(start) && (end == null || !today.isAfter(end))
        if (inPeriod) {
            return CycleRecommendation(
                phase = CyclePhase.PERIOD,
                title = "Take it easy today",
                message = "You may feel low energy today — rest and sip water 💛",
                emoji = "🌷"
            )
        }

        val daysSince = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt()
        return when {
            daysSince in 0..13 -> CycleRecommendation(
                phase = CyclePhase.EARLY,
                title = "Energy returning",
                message = "A great window for light, gentle movement 🌸",
                emoji = "🌸"
            )
            daysSince in 14..34 -> CycleRecommendation(
                phase = CyclePhase.LATER,
                title = "Steady & strong",
                message = "Normal activity suits you well — keep listening to your body 🌿",
                emoji = "🌿"
            )
            else -> null
        }
    }

    /**
     * Builds a soft reflection of yesterday based on logged data only
     * (plan items reset at midnight, so we don't include them).
     *
     * Returns:
     *  - null if the user has no log history at all (brand-new user — nothing to reflect on)
     *  - YesterdaysClosure(percentage = null, ...) if user has history but no log yesterday
     *  - YesterdaysClosure(percentage = X, ...) for a logged day, with a tone scaling by %
     */
    private suspend fun computeYesterdaysClosure(): YesterdaysClosure? {
        val softFailure = "That's okay, tomorrow is a fresh start 🌱"
        val yesterdayStr = LocalDate.now().minusDays(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        val log = repository.getLogByDate(yesterdayStr).first()

        if (log == null) {
            // Don't surface a "you missed it" card to a brand-new user with no history
            val anyHistory = repository.getRecentLogs(7).isNotEmpty()
            return if (anyHistory) {
                YesterdaysClosure(percentage = null, supportiveLine = softFailure)
            } else null
        }

        val waterPart = (log.waterIntake.toFloat() / 8f).coerceIn(0f, 1f)
        val exercisePart = if (log.exerciseDone) 1f else 0f
        val checkInPart = if (log.emotionalCheckIn.isNotBlank()) 1f else 0f
        val pct = (((waterPart + exercisePart + checkInPart) / 3f) * 100).toInt()

        val line = when {
            pct >= 80 -> "What a beautiful day yesterday — keep flowing 🌷"
            pct >= 50 -> "Small steps matter — today is fresh"
            pct >= 30 -> "Yesterday counts. Today is a soft restart 💛"
            else      -> softFailure   // < 30% completion
        }
        return YesterdaysClosure(percentage = pct, supportiveLine = line)
    }

    fun dismissClosureCard() {
        _uiState.update { it.copy(closureCard = null) }
    }

    fun dismissFirstInsightHelper() {
        // Flag is already marked when the helper is first shown; this just hides the caption.
        _uiState.update { it.copy(showFirstInsightHelper = false) }
    }

    /**
     * Returns the insight to surface today. Rules:
     *  - If the most-recently-shown insight was shown within the last 3 days,
     *    keep showing it (continuity feels personal).
     *  - Otherwise rotate to the least-recently-shown insight, mark it shown.
     */
    private suspend fun pickRotatingInsight(): Insight? {
        val all = repository.getAllInsightsSnapshot()
        if (all.isEmpty()) return null

        val now = System.currentTimeMillis()
        val threeDaysMs = 3L * 24 * 60 * 60 * 1000

        val current = all.maxByOrNull { it.lastShownAtMillis }
        if (current != null && current.lastShownAtMillis > 0 &&
            (now - current.lastShownAtMillis) < threeDaysMs
        ) {
            return current
        }

        val next = all.minByOrNull { it.lastShownAtMillis } ?: return null
        repository.markInsightShown(next.id, now)
        return next.copy(lastShownAtMillis = now, showCount = next.showCount + 1)
    }

    fun acceptSuggestion() {
        _uiState.update { it.copy(planSuggestion = null, isLowEnergyMode = true) }
        settingsRepository.setSuggestionHandledDate(todayStr)
    }

    fun dismissSuggestion() {
        _uiState.update { it.copy(planSuggestion = null) }
        settingsRepository.setSuggestionHandledDate(todayStr)
    }

    // ═══════════════════════════════════════
    // ── Optimistic log helper ──
    // ═══════════════════════════════════════
    private fun updateLogOptimistic(transform: (DailyLog) -> DailyLog) {
        val current = _uiState.value.todayLog
        if (current != null) {
            val updated = transform(current)
            _uiState.update { it.copy(todayLog = updated) }
            viewModelScope.launch { repository.updateLog(updated) }
        } else {
            viewModelScope.launch {
                val lastLog = repository.getLatestLogSync()
                val inheritedMeds = lastLog?.getMedicationsList()?.map { it.copy(isTaken = false) } ?: emptyList()
                val newLog = transform(DailyLog(date = todayStr).withUpdatedMedications(inheritedMeds))
                _uiState.update { it.copy(todayLog = newLog) }
                repository.insertLog(newLog)
            }
        }
    }

    // ── Trackers ──
    fun updateMood(mood: String) = updateLogOptimistic { it.copy(mood = mood) }

    fun updateEmotionalCheckIn(value: String) {
        updateLogOptimistic { it.copy(emotionalCheckIn = value) }
        val message = when (value) {
            "Good" -> "Wonderful! Keep that energy flowing 🌸"
            "Okay" -> "Steady wins the day. You've got this 🤍"
            "Low"  -> "It's okay to feel low. Be gentle with yourself today 💛"
            else   -> null
        }
        message?.let { emitFeedback(it) }
    }
    fun updateWaterIntake(glasses: Int) {
        val prev = _uiState.value.todayLog?.waterIntake ?: 0
        updateLogOptimistic { it.copy(waterIntake = glasses) }
        if (glasses > prev) emitFeedback(FeedbackMessages.water())
    }
    fun completeExercise(name: String) {
        val wasDone = _uiState.value.todayLog?.exerciseDone == true
        updateLogOptimistic { it.copy(exerciseDone = true, exerciseName = name) }
        if (!wasDone) emitFeedback(FeedbackMessages.exercise())
    }

    fun onMiniActionCompleted(name: String) {
        val msg = when {
            name.contains("Breath", ignoreCase = true)  -> "Mind feels lighter 🌬️"
            name.contains("Stretch", ignoreCase = true) -> "Body thanks you 🤸"
            name.contains("Relax", ignoreCase = true)   -> "A soft pause well taken 🌿"
            else -> "Nice break ✨"
        }
        emitFeedback(msg)
    }
    fun updateSymptoms(symptoms: String) = updateLogOptimistic { it.copy(symptoms = symptoms) }
    fun updateSleepHours(hours: Float) = updateLogOptimistic { it.copy(sleepHours = hours) }
    fun updateFruitServings(servings: Int) = updateLogOptimistic { it.copy(fruitServings = servings) }
    fun updateSugarLevel(level: Int) = updateLogOptimistic { it.copy(sugarLevel = level) }
    fun updateStressLevel(level: String) = updateLogOptimistic { it.copy(stressLevel = level) }
    fun updateSleepQuality(quality: String) = updateLogOptimistic { it.copy(sleepQuality = quality) }
    fun updateProteinIncluded(included: Boolean) = updateLogOptimistic { it.copy(proteinIncluded = included) }
    
    // ── Medications ──
    fun addMedication(name: String) = updateLogOptimistic { log ->
        val currentMeds = log.getMedicationsList().toMutableList()
        if (currentMeds.none { it.name.equals(name, ignoreCase = true) }) {
            currentMeds.add(Medication(name, false))
        }
        log.withUpdatedMedications(currentMeds)
    }

    fun toggleMedication(name: String) = updateLogOptimistic { log ->
        val currentMeds = log.getMedicationsList().map {
            if (it.name == name) it.copy(isTaken = !it.isTaken) else it
        }
        log.withUpdatedMedications(currentMeds)
    }

    fun removeMedication(name: String) = updateLogOptimistic { log ->
        val currentMeds = log.getMedicationsList().filter { it.name != name }
        log.withUpdatedMedications(currentMeds)
    }
    fun toggleLowEnergyMode(enabled: Boolean) {
        _uiState.update { it.copy(isLowEnergyMode = enabled) }
    }

    // ═══════════════════════════════════════
    // ── Plan Items – CRUD + Reorder ──
    // ═══════════════════════════════════════

    fun addPlanItem(title: String, category: String = "Lifestyle") {
        if (title.isBlank()) return
        val maxOrder = (_uiState.value.planItems.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val newItem = PlanItem(title = title, category = category, orderIndex = maxOrder)
        _uiState.update { it.copy(planItems = it.planItems + newItem) }
        viewModelScope.launch { repository.insertPlanItem(newItem) }
    }

    fun editPlanItem(item: PlanItem, newTitle: String) {
        if (newTitle.isBlank()) return
        val updated = item.copy(title = newTitle)
        _uiState.update { state ->
            state.copy(planItems = state.planItems.map { if (it.id == item.id) updated else it })
        }
        viewModelScope.launch { repository.updatePlanItem(updated) }
    }

    fun togglePlanItem(item: PlanItem) {
        val updated = item.copy(isCompleted = !item.isCompleted)
        _uiState.update { state ->
            state.copy(planItems = state.planItems.map { if (it.id == item.id) updated else it })
        }
        viewModelScope.launch { repository.updatePlanItem(updated) }
        if (updated.isCompleted) {
            val items = _uiState.value.planItems
            val allDone = items.isNotEmpty() && items.all { it.isCompleted }
            emitFeedback(if (allDone) FeedbackMessages.fullDay() else FeedbackMessages.task())
        }
    }

    fun deletePlanItem(item: PlanItem) {
        _uiState.update { state ->
            state.copy(planItems = state.planItems.filter { it.id != item.id })
        }
        viewModelScope.launch { repository.deletePlanItem(item) }
    }

    fun movePlanItem(fromIndex: Int, toIndex: Int) {
        val items = _uiState.value.planItems.toMutableList()
        if (fromIndex !in items.indices || toIndex !in items.indices) return
        val moved = items.removeAt(fromIndex)
        items.add(toIndex, moved)
        val reindexed = items.mapIndexed { idx, item -> item.copy(orderIndex = idx) }
        _uiState.update { it.copy(planItems = reindexed) }
        viewModelScope.launch { repository.updatePlanItems(reindexed) }
    }

    // Reminders are now configured in Settings (Summary → ⚙️) — see SettingsViewModel.

    fun acceptFirstLaunchReminderPrompt() {
        settingsRepository.markFirstLaunchReminderPromptShown()
        settingsRepository.setRemindersEnabled(true)
        // Default intensity is "Medium" from SettingsRepository, so just apply the current setting.
        WorkManagerHelper.applyReminderSchedule(
            context,
            enabled = true,
            intensity = settingsRepository.reminderIntensity.value
        )
        _uiState.update { it.copy(showFirstLaunchReminderPrompt = false) }
    }

    fun dismissFirstLaunchReminderPrompt() {
        settingsRepository.markFirstLaunchReminderPromptShown()
        _uiState.update { it.copy(showFirstLaunchReminderPrompt = false) }
    }

    fun setUserName(name: String) {
        settingsRepository.setUserName(name)
    }
}
