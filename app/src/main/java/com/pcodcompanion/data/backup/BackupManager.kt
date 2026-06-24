package com.pcodcompanion.data.backup

import com.pcodcompanion.data.local.entity.CycleEntry
import com.pcodcompanion.data.local.entity.DailyLog
import com.pcodcompanion.data.local.entity.PlanItem
import com.pcodcompanion.data.repository.PCODRepository
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

sealed class ImportResult {
    data class Success(val logs: Int, val cycles: Int, val plans: Int) : ImportResult()
    data class Failure(val message: String) : ImportResult()
}

@Singleton
class BackupManager @Inject constructor(
    private val repository: PCODRepository
) {

    suspend fun exportToJson(): String {
        val logs = repository.getAllLogsSnapshot()
        val cycles = repository.getAllCyclesSnapshot()
        val plans = repository.getAllPlanItemsSnapshot()

        val root = JSONObject().apply {
            put("schema", "pcod-companion-backup")
            put("version", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("dailyLogs", JSONArray().apply { logs.forEach { put(it.toJson()) } })
            put("cycleEntries", JSONArray().apply { cycles.forEach { put(it.toJson()) } })
            put("planItems", JSONArray().apply { plans.forEach { put(it.toJson()) } })
        }
        return root.toString(2)
    }

    /**
     * Replace mode: wipes existing logs/cycles/plans and inserts everything from the backup.
     * Returns Success/Failure; on Failure, the database is left untouched.
     */
    suspend fun importFromJsonReplace(json: String): ImportResult {
        val parsed = try {
            parse(json)
        } catch (e: Exception) {
            return ImportResult.Failure("Backup file is not valid: ${e.message}")
        }
        return try {
            repository.deleteAllLogs()
            repository.deleteAllCycles()
            repository.deleteAllPlanItems()
            // Insights are derived; wipe so they regenerate from the new data set
            repository.deleteAllInsights()
            parsed.logs.forEach { repository.insertLog(it) }
            parsed.cycles.forEach { repository.insertCycleEntry(it) }
            parsed.plans.forEach { repository.insertPlanItem(it) }
            ImportResult.Success(parsed.logs.size, parsed.cycles.size, parsed.plans.size)
        } catch (e: Exception) {
            ImportResult.Failure("Import failed: ${e.message}")
        }
    }

    private data class Parsed(
        val logs: List<DailyLog>,
        val cycles: List<CycleEntry>,
        val plans: List<PlanItem>
    )

    private fun parse(json: String): Parsed {
        val root = JSONObject(json)
        val schema = root.optString("schema")
        require(schema == "pcod-companion-backup") { "Not a PCOD Companion backup file" }
        val version = root.optInt("version", 0)
        require(version in 1..BACKUP_VERSION) { "Unsupported backup version: $version" }

        val logs = root.optJSONArray("dailyLogs")?.toList { dailyLogFromJson(it) } ?: emptyList()
        val cycles = root.optJSONArray("cycleEntries")?.toList { cycleEntryFromJson(it) } ?: emptyList()
        val plans = root.optJSONArray("planItems")?.toList { planItemFromJson(it) } ?: emptyList()
        return Parsed(logs, cycles, plans)
    }

    // ── Entity ↔ JSON ──

    private fun DailyLog.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("date", date)
        put("mood", mood)
        put("symptoms", symptoms)
        put("waterIntake", waterIntake)
        put("exerciseDone", exerciseDone)
        put("exerciseName", exerciseName ?: JSONObject.NULL)
        put("notes", notes)
        put("sleepHours", sleepHours.toDouble())
        put("fruitServings", fruitServings)
        put("sugarLevel", sugarLevel)
        put("stressLevel", stressLevel)
        put("sleepQuality", sleepQuality)
        put("proteinIncluded", proteinIncluded)
        put("medications", medications)
        put("streakDays", streakDays)
        put("emotionalCheckIn", emotionalCheckIn)
        put("planCompletionPct", planCompletionPct)
    }

    private fun dailyLogFromJson(o: JSONObject): DailyLog = DailyLog(
        id = o.optLong("id", 0L),
        date = o.getString("date"),
        mood = o.optString("mood", ""),
        symptoms = o.optString("symptoms", ""),
        waterIntake = o.optInt("waterIntake", 0),
        exerciseDone = o.optBoolean("exerciseDone", false),
        exerciseName = if (o.isNull("exerciseName")) null else o.optString("exerciseName", "").ifBlank { null },
        notes = o.optString("notes", ""),
        sleepHours = o.optDouble("sleepHours", 0.0).toFloat(),
        fruitServings = o.optInt("fruitServings", 0),
        sugarLevel = o.optInt("sugarLevel", 0),
        stressLevel = o.optString("stressLevel", ""),
        sleepQuality = o.optString("sleepQuality", ""),
        proteinIncluded = o.optBoolean("proteinIncluded", false),
        medications = o.optString("medications", ""),
        streakDays = o.optInt("streakDays", 0),
        emotionalCheckIn = o.optString("emotionalCheckIn", ""),
        planCompletionPct = o.optInt("planCompletionPct", -1)
    )

    private fun CycleEntry.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("startDate", startDate)
        put("endDate", endDate ?: JSONObject.NULL)
        put("flowLevel", flowLevel)
        put("symptoms", symptoms)
        put("notes", notes)
    }

    private fun cycleEntryFromJson(o: JSONObject): CycleEntry = CycleEntry(
        id = o.optLong("id", 0L),
        startDate = o.getString("startDate"),
        endDate = if (o.isNull("endDate")) null else o.optString("endDate", "").ifBlank { null },
        flowLevel = o.optString("flowLevel", "Medium"),
        symptoms = o.optString("symptoms", ""),
        notes = o.optString("notes", "")
    )

    private fun PlanItem.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("description", description)
        put("category", category)
        put("isCompleted", isCompleted)
        put("orderIndex", orderIndex)
        put("daysOfWeek", daysOfWeek)
    }

    private fun planItemFromJson(o: JSONObject): PlanItem = PlanItem(
        id = o.optLong("id", 0L),
        title = o.getString("title"),
        description = o.optString("description", ""),
        category = o.optString("category", "Lifestyle"),
        isCompleted = o.optBoolean("isCompleted", false),
        orderIndex = o.optInt("orderIndex", 0),
        daysOfWeek = o.optString("daysOfWeek", "Mon,Tue,Wed,Thu,Fri,Sat,Sun")
    )

    private fun <T> JSONArray.toList(map: (JSONObject) -> T): List<T> {
        val out = ArrayList<T>(length())
        for (i in 0 until length()) {
            out.add(map(getJSONObject(i)))
        }
        return out
    }

    companion object {
        const val BACKUP_VERSION = 3
    }
}
