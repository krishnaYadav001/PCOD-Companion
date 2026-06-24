package com.pcodcompanion.data.repository

import com.pcodcompanion.data.local.dao.CycleEntryDao
import com.pcodcompanion.data.local.dao.DailyLogDao
import com.pcodcompanion.data.local.dao.InsightDao
import com.pcodcompanion.data.local.dao.PlanItemDao
import com.pcodcompanion.data.local.entity.CycleEntry
import com.pcodcompanion.data.local.entity.DailyLog
import com.pcodcompanion.data.local.entity.Insight
import com.pcodcompanion.data.local.entity.PlanItem
import com.pcodcompanion.data.local.entity.dietScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class WeeklyStats(
    val avgWaterIntake: Double,
    val avgSleepHours: Double,
    val totalExercises: Int,
    val avgDietScore: Double,
    val daysLogged: Int
)

@Singleton
class PCODRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val cycleEntryDao: CycleEntryDao,
    private val planItemDao: PlanItemDao,
    private val insightDao: InsightDao
) {
    // Daily Logs
    fun getAllLogs(): Flow<List<DailyLog>> = dailyLogDao.getAllLogs()
    suspend fun getLatestLogSync(): DailyLog? = dailyLogDao.getLatestLogSync()
    fun getLogByDate(date: String): Flow<DailyLog?> = dailyLogDao.getLogByDate(date)
    suspend fun getLogByDateSync(date: String): DailyLog? = dailyLogDao.getLogByDateSync(date)
    suspend fun insertLog(log: DailyLog) = dailyLogDao.insertLog(log)
    suspend fun updateLog(log: DailyLog) = dailyLogDao.updateLog(log)
    suspend fun deleteLog(log: DailyLog) = dailyLogDao.deleteLog(log)
    suspend fun existsLogForDate(date: String): Boolean = dailyLogDao.existsForDate(date)
    suspend fun insertLogIfNotExists(log: DailyLog) = dailyLogDao.insertIfNotExists(log)
    suspend fun resetAllPlanCompletions() = planItemDao.resetAllCompletions()
    suspend fun getRecentLogs(limit: Int): List<DailyLog> = dailyLogDao.getRecentLogs(limit)
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLog>> = dailyLogDao.getLogsBetweenDates(startDate, endDate)

    /**
     * Calculates the current streak of consecutive days with logs.
     */
    suspend fun calculateStreak(): Int {
        val logs = dailyLogDao.getRecentLogs(365).sortedByDescending { it.date }
        if (logs.isEmpty()) return 0

        var streak = 0
        var expectedDate = LocalDate.now()

        for (log in logs) {
            val logDate = LocalDate.parse(log.date)
            if (logDate == expectedDate || logDate == expectedDate.minusDays(1)) {
                streak++
                expectedDate = logDate.minusDays(1)
            } else if (logDate.isBefore(expectedDate)) {
                break
            }
        }
        return streak
    }

    /**
     * Calculates weekly stats for a given week offset (0 = current week, 1 = last week, etc.)
     */
    suspend fun getWeeklyStats(weekOffset: Int = 0): WeeklyStats {
        val today = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())

        val startOfWeek = today.with(weekFields.dayOfWeek(), 1).minusWeeks(weekOffset.toLong())
        val endOfWeek = startOfWeek.plusDays(6)

        val logs: List<DailyLog> = dailyLogDao.getLogsBetweenDates(startOfWeek.toString(), endOfWeek.toString())
            .first() // Get the flow's first value

        if (logs.isEmpty()) {
            return WeeklyStats(0.0, 0.0, 0, 0.0, 0)
        }

        val avgWater = logs.map { it.waterIntake }.average()
        val avgSleep = logs.map { it.sleepHours }.average()
        val totalExercises = logs.count { it.exerciseDone }
        val avgDietScore = logs.map { it.dietScore }.average()

        return WeeklyStats(avgWater, avgSleep, totalExercises, avgDietScore, logs.size)
    }

    // Cycle Entries
    fun getAllCycleEntries(): Flow<List<CycleEntry>> = cycleEntryDao.getAllEntries()
    fun getActiveCycle(): Flow<CycleEntry?> = cycleEntryDao.getActiveCycle()
    fun getLatestCycleEntry(): Flow<CycleEntry?> = cycleEntryDao.getLatestEntry()
    suspend fun insertCycleEntry(entry: CycleEntry) = cycleEntryDao.insertEntry(entry)
    suspend fun updateCycleEntry(entry: CycleEntry) = cycleEntryDao.updateEntry(entry)
    suspend fun deleteCycleEntry(entry: CycleEntry) = cycleEntryDao.deleteEntry(entry)

    // Backup snapshots & wipes
    suspend fun getAllLogsSnapshot(): List<DailyLog> = dailyLogDao.getAllLogsSnapshot()
    suspend fun getAllCyclesSnapshot(): List<CycleEntry> = cycleEntryDao.getAllEntriesSnapshot()
    suspend fun getAllPlanItemsSnapshot(): List<PlanItem> = planItemDao.getAllItemsSnapshot()

    suspend fun deleteAllLogs() = dailyLogDao.deleteAllLogs()
    suspend fun deleteAllCycles() = cycleEntryDao.deleteAllEntries()
    suspend fun deleteAllInsights() = insightDao.deleteAll()

    // Insights
    fun getAllInsights(): Flow<List<Insight>> = insightDao.getAllInsights()
    suspend fun getAllInsightsSnapshot(): List<Insight> = insightDao.getAllInsightsSnapshot()
    suspend fun markInsightShown(id: Long, now: Long = System.currentTimeMillis()) =
        insightDao.markShown(id, now)

    // Plan Items
    fun getAllPlanItems(): Flow<List<PlanItem>> = planItemDao.getAllItems()
    fun getPlanItemsByCategory(category: String): Flow<List<PlanItem>> = planItemDao.getItemsByCategory(category)
    suspend fun insertPlanItem(item: PlanItem) = planItemDao.insertItem(item)
    suspend fun updatePlanItem(item: PlanItem) = planItemDao.updateItem(item)
    suspend fun updatePlanItems(items: List<PlanItem>) = planItemDao.updateItems(items)
    suspend fun deletePlanItem(item: PlanItem) = planItemDao.deleteItem(item)
    suspend fun deleteAllPlanItems() = planItemDao.deleteAllItems()
}
