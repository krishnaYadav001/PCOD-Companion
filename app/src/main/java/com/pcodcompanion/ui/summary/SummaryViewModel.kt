package com.pcodcompanion.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pcodcompanion.data.local.entity.CycleEntry
import com.pcodcompanion.data.local.entity.DailyLog
import com.pcodcompanion.data.local.entity.dietScore
import com.pcodcompanion.data.repository.PCODRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayHabits(
    val dayLetter: String,   // "M", "T", "W", ...
    val water: Boolean,
    val exercise: Boolean,
    val sleep: Boolean,
    val isToday: Boolean
)

data class SummaryUiState(
    val totalLogs: Int = 0,
    val averageCycleLength: Int = 28,
    val totalCycles: Int = 0,
    val commonSymptoms: List<Pair<String, Int>> = emptyList(),
    val moodDistribution: List<Pair<String, Int>> = emptyList(),
    val exerciseDays: Int = 0,
    val avgWaterIntake: Float = 0f,
    val avgDietScore: Float = 0f,
    val insights: List<String> = emptyList(),
    // Weekly progress (last 7 days)
    val weekDaysLogged: Int = 0,
    val weekExerciseDays: Int = 0,
    val weekWaterGoalHits: Int = 0,
    val weeklyConsistencyPct: Float = 0f,
    val weeklyExercisePct: Float = 0f,
    val weeklyWaterGoalPct: Float = 0f,
    val weeklyDietPct: Float = 0f,
    val weeklyOverallPct: Float = 0f,
    // Habit consistency (last 7 days, oldest → today)
    val last7Days: List<DayHabits> = emptyList()
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: PCODRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private var currentLogs: List<DailyLog> = emptyList()
    private var currentCycles: List<CycleEntry> = emptyList()

    init {
        viewModelScope.launch {
            repository.getAllLogs().collect { logs ->
                currentLogs = logs
                val weekly = computeWeeklyProgress(logs)
                _uiState.value = _uiState.value.copy(
                    totalLogs = logs.size,
                    commonSymptoms = computeTopSymptoms(logs),
                    moodDistribution = computeMoodDistribution(logs),
                    exerciseDays = logs.count { it.exerciseDone },
                    avgWaterIntake = if (logs.isNotEmpty()) logs.map { it.waterIntake }.average().toFloat() else 0f,
                    avgDietScore = if (logs.isNotEmpty()) logs.map { it.dietScore }.average().toFloat() else 0f,
                    insights = generateInsights(logs),
                    weekDaysLogged = weekly.daysLogged,
                    weekExerciseDays = weekly.exerciseDays,
                    weekWaterGoalHits = weekly.waterGoalHits,
                    weeklyConsistencyPct = weekly.consistency,
                    weeklyExercisePct = weekly.exercise,
                    weeklyWaterGoalPct = weekly.water,
                    weeklyDietPct = weekly.diet,
                    weeklyOverallPct = weekly.overall,
                    last7Days = computeLast7DayHabits(logs)
                )
            }
        }
        viewModelScope.launch {
            repository.getAllCycleEntries().collect { entries ->
                currentCycles = entries
                _uiState.value = _uiState.value.copy(
                    totalCycles = entries.size,
                    averageCycleLength = computeAvgCycle(entries)
                )
            }
        }
    }

    private data class WeeklyProgress(
        val daysLogged: Int,
        val exerciseDays: Int,
        val waterGoalHits: Int,
        val consistency: Float,
        val exercise: Float,
        val water: Float,
        val diet: Float,
        val overall: Float
    )

    private fun computeWeeklyProgress(logs: List<DailyLog>): WeeklyProgress {
        val today = java.time.LocalDate.now()
        val sevenDaysAgo = today.minusDays(6)
        val weekLogs = logs.mapNotNull { log ->
            try {
                val d = java.time.LocalDate.parse(log.date)
                if (!d.isBefore(sevenDaysAgo) && !d.isAfter(today)) log else null
            } catch (e: Exception) { null }
        }
        val daysLogged = weekLogs.size
        val exerciseDays = weekLogs.count { it.exerciseDone }
        val waterGoalHits = weekLogs.count { it.waterIntake >= WATER_GOAL }
        val dietAvg = if (weekLogs.isNotEmpty()) weekLogs.map { it.dietScore }.average() else 0.0

        val consistency = (daysLogged / 7f).coerceIn(0f, 1f)
        val exercise = (exerciseDays / 7f).coerceIn(0f, 1f)
        val water = (waterGoalHits / 7f).coerceIn(0f, 1f)
        val diet = (dietAvg / 10.0).toFloat().coerceIn(0f, 1f)
        val overall = ((consistency + exercise + water + diet) / 4f).coerceIn(0f, 1f)

        return WeeklyProgress(daysLogged, exerciseDays, waterGoalHits, consistency, exercise, water, diet, overall)
    }

    companion object {
        const val WATER_GOAL = 8
        const val SLEEP_GOAL_HOURS = 7f
    }

    private fun computeLast7DayHabits(logs: List<DailyLog>): List<DayHabits> {
        val today = java.time.LocalDate.now()
        val byDate = logs.associateBy { it.date }
        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val log = byDate[date.toString()]
            DayHabits(
                dayLetter = date.dayOfWeek.getDisplayName(
                    java.time.format.TextStyle.NARROW,
                    java.util.Locale.getDefault()
                ),
                water = (log?.waterIntake ?: 0) >= WATER_GOAL,
                exercise = log?.exerciseDone == true,
                sleep = (log?.sleepHours ?: 0f) >= SLEEP_GOAL_HOURS,
                isToday = offset == 0
            )
        }
    }

    private fun computeTopSymptoms(logs: List<DailyLog>): List<Pair<String, Int>> {
        return logs.flatMap { it.symptoms.split(",").map { s -> s.trim() } }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }
    }

    private fun computeMoodDistribution(logs: List<DailyLog>): List<Pair<String, Int>> {
        return logs.filter { it.mood.isNotBlank() }
            .groupingBy { it.mood }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    private fun computeAvgCycle(entries: List<CycleEntry>): Int {
        if (entries.size < 2) return 28
        val lengths = entries.zipWithNext().mapNotNull { (newer, older) ->
            try {
                val start1 = java.time.LocalDate.parse(older.startDate)
                val start2 = java.time.LocalDate.parse(newer.startDate)
                java.time.temporal.ChronoUnit.DAYS.between(start1, start2).toInt()
            } catch (e: Exception) { null }
        }
        return if (lengths.isNotEmpty()) lengths.average().toInt() else 28
    }

    private fun generateInsights(logs: List<DailyLog>): List<String> {
        val insights = mutableListOf<String>()
        if (logs.isEmpty()) return insights

        val goodSleepLogs = logs.filter { it.sleepHours > 7f }
        val goodSleepMoods = goodSleepLogs.count { it.mood == "Happy" || it.mood == "Calm" }
        if (goodSleepLogs.isNotEmpty() && goodSleepMoods.toFloat() / goodSleepLogs.size >= 0.5f) {
            insights.add("You felt better on days you slept more 🌸")
        }

        val highSugarLogs = logs.filter { it.sugarLevel >= 2 }
        val highSugarAcne = highSugarLogs.count { it.symptoms.contains("Acne", ignoreCase = true) }
        if (highSugarLogs.isNotEmpty() && highSugarAcne > 0) {
            insights.add("Higher sugar intake may be linked to acne")
        }

        val exerciseLogs = logs.filter { it.exerciseDone }
        val exerciseEnergy = exerciseLogs.count { !it.symptoms.contains("Low Energy", ignoreCase = true) }
        if (exerciseLogs.isNotEmpty() && exerciseEnergy.toFloat() / exerciseLogs.size >= 0.5f) {
            insights.add("Exercising helped improve your energy levels ⚡")
        }

        val highWaterLogs = logs.filter { it.waterIntake >= 6 }
        val highWaterEnergy = highWaterLogs.count { !it.symptoms.contains("Low Energy", ignoreCase = true) }
        if (highWaterLogs.isNotEmpty() && highWaterEnergy.toFloat() / highWaterLogs.size >= 0.5f) {
            insights.add("Drinking more water kept your energy up 💧")
        }

        return insights
    }

    fun generateExportText(daysWindow: Int = 30): String {
        val today = java.time.LocalDate.now()
        val windowStart = today.minusDays((daysWindow - 1).toLong())
        val recentLogs = currentLogs.mapNotNull { log ->
            try {
                val d = java.time.LocalDate.parse(log.date)
                if (!d.isBefore(windowStart) && !d.isAfter(today)) log else null
            } catch (e: Exception) { null }
        }.sortedByDescending { it.date }

        val sb = java.lang.StringBuilder()
        val divider = "─".repeat(56)
        val heading = "═".repeat(56)

        // ── Header ──
        sb.append(heading).append('\n')
        sb.append("  RAGINI PCOD COMPANION — Health Export\n")
        sb.append(heading).append('\n')
        sb.append("Exported : ").append(today).append('\n')
        sb.append("Window   : Last ").append(daysWindow).append(" days (")
            .append(windowStart).append(" to ").append(today).append(")\n\n")

        // ── 1. OVERVIEW ──
        sb.append(divider).append('\n')
        sb.append("1. OVERVIEW\n")
        sb.append(divider).append('\n')
        val consistencyPct = (recentLogs.size.toFloat() / daysWindow * 100).toInt()
        sb.append("Days logged          : ").append(recentLogs.size).append(" / ").append(daysWindow).append('\n')
        sb.append("Logging consistency  : ").append(consistencyPct).append("%\n")
        sb.append("Total cycles tracked : ").append(currentCycles.size).append('\n')
        sb.append("Average cycle length : ").append(_uiState.value.averageCycleLength).append(" days\n\n")

        // ── 2. CYCLE HISTORY ──
        sb.append(divider).append('\n')
        sb.append("2. CYCLE HISTORY\n")
        sb.append(divider).append('\n')
        if (currentCycles.isEmpty()) {
            sb.append("No cycle entries recorded.\n\n")
        } else {
            currentCycles
                .sortedByDescending { it.startDate }
                .forEachIndexed { idx, c ->
                    val durationStr = if (c.endDate != null) {
                        try {
                            val s = java.time.LocalDate.parse(c.startDate)
                            val e = java.time.LocalDate.parse(c.endDate)
                            "${java.time.temporal.ChronoUnit.DAYS.between(s, e) + 1} days"
                        } catch (e: Exception) { "—" }
                    } else "Ongoing"
                    sb.append("Cycle ").append(idx + 1)
                    if (idx == 0) sb.append(" (most recent)")
                    sb.append(":\n")
                    sb.append("  Started : ").append(c.startDate).append('\n')
                    sb.append("  Ended   : ").append(c.endDate ?: "—").append('\n')
                    sb.append("  Duration: ").append(durationStr).append('\n')
                    sb.append("  Flow    : ").append(c.flowLevel).append('\n')
                    if (c.symptoms.isNotBlank()) sb.append("  Symptoms: ").append(c.symptoms).append('\n')
                    if (c.notes.isNotBlank()) sb.append("  Notes   : ").append(c.notes).append('\n')
                    sb.append('\n')
                }
        }

        // ── 3. SYMPTOMS SUMMARY ──
        sb.append(divider).append('\n')
        sb.append("3. SYMPTOMS SUMMARY (last ").append(daysWindow).append(" days)\n")
        sb.append(divider).append('\n')
        val symptomCounts = recentLogs
            .flatMap { it.symptoms.split(",").map { s -> s.trim() } }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
        if (symptomCounts.isEmpty()) {
            sb.append("No symptoms reported in this window.\n\n")
        } else {
            symptomCounts.forEach { (symptom, count) ->
                sb.append("  • ").append(symptom.padEnd(24, '.'))
                    .append(' ').append(count.toString().padStart(2)).append(" days\n")
            }
            sb.append('\n')
        }

        // ── 4. MOOD TRENDS ──
        sb.append(divider).append('\n')
        sb.append("4. MOOD TRENDS (last ").append(daysWindow).append(" days)\n")
        sb.append(divider).append('\n')
        val moodCounts = recentLogs
            .filter { it.mood.isNotBlank() }
            .groupingBy { it.mood }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
        val moodTotal = moodCounts.sumOf { it.value }
        if (moodCounts.isEmpty()) {
            sb.append("No mood data in this window.\n")
        } else {
            moodCounts.forEach { (mood, count) ->
                val pct = if (moodTotal > 0) (count * 100 / moodTotal) else 0
                sb.append("  • ").append(mood.padEnd(12, '.'))
                    .append(' ').append(count.toString().padStart(2)).append(" days  (")
                    .append(pct).append("%)\n")
            }
        }
        // Emotional check-in summary
        val checkInCounts = recentLogs
            .filter { it.emotionalCheckIn.isNotBlank() }
            .groupingBy { it.emotionalCheckIn }
            .eachCount()
        if (checkInCounts.isNotEmpty()) {
            sb.append("\nEmotional check-in:\n")
            listOf("Good", "Okay", "Low").forEach { key ->
                val v = checkInCounts[key] ?: 0
                if (v > 0) sb.append("  • ").append(key.padEnd(6)).append(": ").append(v).append(" days\n")
            }
        }
        sb.append('\n')

        // ── 5. HABIT TRACKING ──
        sb.append(divider).append('\n')
        sb.append("5. HABIT TRACKING (last ").append(daysWindow).append(" days)\n")
        sb.append(divider).append('\n')
        if (recentLogs.isEmpty()) {
            sb.append("No daily logs in this window.\n\n")
        } else {
            // Sleep
            val sleepLogs = recentLogs.filter { it.sleepHours > 0f }
            val avgSleep = if (sleepLogs.isNotEmpty()) sleepLogs.map { it.sleepHours }.average() else 0.0
            val lowSleepDays = recentLogs.count { it.sleepHours in 0.01f..6f }
            val sleepQualities = recentLogs.filter { it.sleepQuality.isNotBlank() }
                .groupingBy { it.sleepQuality }.eachCount()
            sb.append("SLEEP\n")
            sb.append("  Average        : ").append(String.format(java.util.Locale.US, "%.1f", avgSleep)).append(" hrs/night\n")
            sb.append("  Days < 6 hrs   : ").append(lowSleepDays).append('\n')
            if (sleepQualities.isNotEmpty()) {
                sb.append("  Quality breakdown:\n")
                listOf("Good", "Disturbed", "Poor").forEach { q ->
                    val v = sleepQualities[q] ?: 0
                    if (v > 0) sb.append("    ").append(q.padEnd(10)).append(": ").append(v).append(" days\n")
                }
            }

            // Hydration
            val avgWater = recentLogs.map { it.waterIntake }.average()
            val waterGoalDays = recentLogs.count { it.waterIntake >= WATER_GOAL }
            val waterPct = if (recentLogs.isNotEmpty()) (waterGoalDays * 100 / recentLogs.size) else 0
            sb.append("\nHYDRATION\n")
            sb.append("  Average water  : ").append(String.format(java.util.Locale.US, "%.1f", avgWater)).append(" glasses/day\n")
            sb.append("  Days hit goal  : ").append(waterGoalDays).append(" / ").append(recentLogs.size)
                .append(" (").append(waterPct).append("%)\n")

            // Exercise
            val exerciseDays = recentLogs.count { it.exerciseDone }
            val exercisePct = if (recentLogs.isNotEmpty()) (exerciseDays * 100 / recentLogs.size) else 0
            val mostCommonExercise = recentLogs
                .filter { it.exerciseDone && !it.exerciseName.isNullOrBlank() }
                .groupingBy { it.exerciseName!! }
                .eachCount()
                .maxByOrNull { it.value }
            sb.append("\nEXERCISE\n")
            sb.append("  Days exercised : ").append(exerciseDays).append(" / ").append(recentLogs.size)
                .append(" (").append(exercisePct).append("%)\n")
            mostCommonExercise?.let {
                sb.append("  Most common    : ").append(it.key).append(" (").append(it.value).append(" days)\n")
            }

            // Diet
            val avgDietScore = recentLogs.map { it.dietScore }.average()
            val proteinDays = recentLogs.count { it.proteinIncluded }
            sb.append("\nDIET\n")
            sb.append("  Avg diet score : ").append(String.format(java.util.Locale.US, "%.1f", avgDietScore)).append(" / 10\n")
            sb.append("  Days w/ protein: ").append(proteinDays).append(" / ").append(recentLogs.size).append('\n')

            // Stress
            val stressCounts = recentLogs.filter { it.stressLevel.isNotBlank() }
                .groupingBy { it.stressLevel }.eachCount()
            if (stressCounts.isNotEmpty()) {
                sb.append("\nSTRESS\n")
                listOf("Low", "Medium", "High").forEach { lvl ->
                    val v = stressCounts[lvl] ?: 0
                    if (v > 0) sb.append("  ").append(lvl.padEnd(6)).append(": ").append(v).append(" days\n")
                }
            }
            sb.append('\n')
        }

        // ── 6. DAILY LOG ──
        sb.append(divider).append('\n')
        sb.append("6. DAILY LOG (last ").append(daysWindow).append(" days)\n")
        sb.append(divider).append('\n')
        if (recentLogs.isEmpty()) {
            sb.append("No daily logs in this window.\n")
        } else {
            recentLogs.forEach { log ->
                val dayLetters = try {
                    java.time.LocalDate.parse(log.date).dayOfWeek
                        .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                } catch (e: Exception) { "" }
                sb.append(log.date).append(if (dayLetters.isNotEmpty()) " ($dayLetters)" else "").append('\n')
                val line1 = buildList {
                    if (log.mood.isNotBlank()) add("Mood: ${log.mood}")
                    if (log.stressLevel.isNotBlank()) add("Stress: ${log.stressLevel}")
                    if (log.emotionalCheckIn.isNotBlank()) add("Check-in: ${log.emotionalCheckIn}")
                }
                if (line1.isNotEmpty()) sb.append("  ").append(line1.joinToString(" | ")).append('\n')
                sb.append("  Sleep: ").append(log.sleepHours).append(" hrs")
                if (log.sleepQuality.isNotBlank()) sb.append(" (").append(log.sleepQuality).append(")")
                sb.append('\n')
                sb.append("  Water: ").append(log.waterIntake)
                if (log.exerciseDone) {
                    sb.append(" | Exercise: ").append(log.exerciseName ?: "yes")
                }
                sb.append('\n')
                sb.append("  Diet score: ").append(log.dietScore).append("/10")
                if (log.proteinIncluded) sb.append(" | Protein: yes")
                sb.append('\n')
                if (log.symptoms.isNotBlank()) sb.append("  Symptoms: ").append(log.symptoms).append('\n')
                if (log.medications.isNotBlank()) {
                    val meds = log.medications.split("||").mapNotNull {
                        val parts = it.split("|")
                        if (parts.size == 2) "${parts[0]} (${if (parts[1].toBooleanStrictOrNull() == true) "taken" else "missed"})" else null
                    }
                    if (meds.isNotEmpty()) sb.append("  Medications: ").append(meds.joinToString(", ")).append('\n')
                }
                if (log.notes.isNotBlank()) sb.append("  Notes: ").append(log.notes).append('\n')
                sb.append('\n')
            }
        }

        sb.append(heading).append('\n')
        sb.append("End of report\n")
        sb.append(heading).append('\n')

        return sb.toString()
    }
}
