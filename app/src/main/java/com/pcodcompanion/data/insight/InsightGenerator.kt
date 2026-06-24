package com.pcodcompanion.data.insight

import com.pcodcompanion.data.local.dao.InsightDao
import com.pcodcompanion.data.local.entity.DailyLog
import com.pcodcompanion.data.local.entity.Insight
import com.pcodcompanion.data.repository.PCODRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects behavioural patterns in the user's daily logs and persists each as
 * an [Insight] with a stable [Insight.key]. Re-running this is safe — duplicate
 * keys are ignored at the DAO level.
 *
 * Patterns are intentionally gentle and observational; never prescriptive.
 */
@Singleton
class InsightGenerator @Inject constructor(
    private val repository: PCODRepository,
    private val insightDao: InsightDao
) {

    /**
     * Look at the user's recent history and persist any newly-detectable
     * patterns. Safe to call frequently; no-op for already-stored insights.
     */
    suspend fun generateAndPersist() {
        val logs = repository.getAllLogsSnapshot()
        if (logs.size < 3) return  // not enough signal

        val now = System.currentTimeMillis()
        detectAll(logs).forEach { (key, message) ->
            insightDao.insertIfNew(
                Insight(key = key, message = message, createdAtMillis = now)
            )
        }
    }

    private fun detectAll(logs: List<DailyLog>): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()

        // Sleep ↔ Mood
        val goodSleep = logs.filter { it.sleepHours > 7f }
        if (goodSleep.size >= 3) {
            val happyOnGoodSleep = goodSleep.count { it.mood == "Happy" || it.mood == "Calm" }
            if (happyOnGoodSleep.toFloat() / goodSleep.size >= 0.5f) {
                out += "sleep_mood" to "You felt better on days you slept more 🌸"
            }
        }

        // Sugar ↔ Acne
        val highSugar = logs.filter { it.sugarLevel >= 2 }
        if (highSugar.isNotEmpty() && highSugar.any { it.symptoms.contains("Acne", ignoreCase = true) }) {
            out += "sugar_acne" to "Higher sugar days seem linked to acne — worth noticing 🌷"
        }

        // Exercise ↔ Energy
        val exerciseDays = logs.filter { it.exerciseDone }
        if (exerciseDays.size >= 3) {
            val energizedAfterExercise = exerciseDays.count {
                !it.symptoms.contains("Low Energy", ignoreCase = true)
            }
            if (energizedAfterExercise.toFloat() / exerciseDays.size >= 0.6f) {
                out += "exercise_energy" to "Movement seems to lift your energy ⚡"
            }
        }

        // Hydration ↔ Energy
        val hydratedDays = logs.filter { it.waterIntake >= 6 }
        if (hydratedDays.size >= 3) {
            val energizedHydrated = hydratedDays.count {
                !it.symptoms.contains("Low Energy", ignoreCase = true)
            }
            if (energizedHydrated.toFloat() / hydratedDays.size >= 0.6f) {
                out += "water_energy" to "Drinking more water seems to keep your energy steady 💧"
            }
        }

        // Stress ↔ Sleep quality
        val highStress = logs.filter { it.stressLevel.equals("High", ignoreCase = true) }
        if (highStress.size >= 3) {
            val poorSleepOnStress = highStress.count {
                it.sleepQuality.equals("Poor", ignoreCase = true) ||
                    it.sleepQuality.equals("Disturbed", ignoreCase = true)
            }
            if (poorSleepOnStress.toFloat() / highStress.size >= 0.5f) {
                out += "stress_sleep" to "Stressful days seem to disturb your sleep — be gentle 💛"
            }
        }

        // Protein ↔ Mood
        val proteinDays = logs.filter { it.proteinIncluded }
        if (proteinDays.size >= 3) {
            val betterMood = proteinDays.count { it.mood == "Happy" || it.mood == "Calm" }
            if (betterMood.toFloat() / proteinDays.size >= 0.5f) {
                out += "protein_mood" to "Protein-rich days seem to support a calmer mood 🥗"
            }
        }

        return out
    }
}
