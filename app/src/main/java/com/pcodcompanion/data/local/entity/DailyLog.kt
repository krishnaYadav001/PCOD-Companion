package com.pcodcompanion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,             // yyyy-MM-dd
    val mood: String = "",        // e.g. "Happy", "Anxious", "Tired"
    val symptoms: String = "",    // comma-separated
    val waterIntake: Int = 0,     // glasses
    val exerciseDone: Boolean = false,
    val exerciseName: String? = null,
    val notes: String = "",
    val sleepHours: Float = 0f,   // hours of sleep
    val fruitServings: Int = 0,   // fruit servings eaten
    val sugarLevel: Int = 0,      // 0=none, 1=low, 2=moderate, 3=high
    @ColumnInfo(defaultValue = "")
    val stressLevel: String = "", // e.g. "Low", "Medium", "High"
    @ColumnInfo(defaultValue = "")
    val sleepQuality: String = "",// "Good", "Disturbed", "Poor"
    @ColumnInfo(defaultValue = "0")
    val proteinIncluded: Boolean = false,
    @ColumnInfo(defaultValue = "")
    val medications: String = "",  // Serialized string: "Name1|false||Name2|true"
    @ColumnInfo(defaultValue = "0")
    val streakDays: Int = 0,      // Consecutive days logged
    @ColumnInfo(defaultValue = "")
    val emotionalCheckIn: String = "",  // "Good", "Okay", "Low" — daily one-shot
    @ColumnInfo(defaultValue = "-1")
    val planCompletionPct: Int = -1     // 0..100, snapshotted at midnight; -1 = not measured
)

data class Medication(val name: String, val isTaken: Boolean)

fun DailyLog.getMedicationsList(): List<Medication> {
    if (medications.isBlank()) return emptyList()
    return medications.split("||").mapNotNull {
        val parts = it.split("|")
        if (parts.size == 2) Medication(parts[0], parts[1].toBooleanStrictOrNull() ?: false) else null
    }
}

fun DailyLog.withUpdatedMedications(list: List<Medication>): DailyLog {
    val str = list.joinToString("||") { "${it.name}|${it.isTaken}" }
    return this.copy(medications = str)
}

val DailyLog.dietScore: Int
    get() {
        var score = 0
        if (fruitServings > 0) score += 2
        if (waterIntake >= 8) score += 3
        else if (waterIntake >= 5) score += 2
        else if (waterIntake >= 3) score += 1
        score += when (sugarLevel) {
            0 -> 3
            1 -> 2
            2 -> 1
            else -> 0
        }
        if (proteinIncluded) score += 2
        return score
    }
