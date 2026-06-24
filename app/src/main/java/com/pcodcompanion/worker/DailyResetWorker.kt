package com.pcodcompanion.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pcodcompanion.data.local.entity.DailyLog
import com.pcodcompanion.data.repository.PCODRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Runs at midnight to:
 *  1. Create an empty DailyLog for the new day (if one doesn't exist yet)
 *  2. Reset all plan-item checkboxes so users start fresh
 *  Previous days' data is never deleted — it stays in the database.
 */
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: PCODRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = LocalDate.now()
        val todayStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val yesterday = now.minusDays(1)
        val yesterdayStr = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val yesterdayDayName = yesterday.dayOfWeek.name
            .take(3).lowercase().replaceFirstChar { it.uppercase() }

        // 0. Snapshot yesterday's plan completion BEFORE we wipe checkboxes.
        // Only writes when (a) a log already exists for yesterday and (b) at least
        // one plan item was assigned to that weekday — otherwise leave the column
        // at its -1 sentinel ("not measured"). Wrapped in runCatching so a single
        // bad day cannot stop the daily reset itself from running.
        runCatching {
            val yesterdayLog = repository.getLogByDateSync(yesterdayStr)
            if (yesterdayLog != null && yesterdayLog.planCompletionPct < 0) {
                val applicable = repository.getAllPlanItemsSnapshot()
                    .filter { it.daysOfWeek.contains(yesterdayDayName) }
                if (applicable.isNotEmpty()) {
                    val pct = (applicable.count { it.isCompleted } * 100) / applicable.size
                    repository.updateLog(yesterdayLog.copy(planCompletionPct = pct))
                }
            }
        }

        // 1. Create empty log for today (IGNORE if already exists)
        repository.insertLogIfNotExists(DailyLog(date = todayStr))

        // 2. Reset plan-item checkboxes for the new day
        repository.resetAllPlanCompletions()

        return Result.success()
    }
}
