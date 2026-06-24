package com.pcodcompanion.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object WorkManagerHelper {

    private const val MORNING = "morning_reminder"
    private const val HYDRATION = "hydration_reminder"
    private const val EVENING = "evening_reminder"
    private const val REENGAGEMENT = "reengagement_reminder"
    // Old key, kept here only to cancel any leftover queued work from before the migration
    private const val LEGACY_WATER = "water_reminder"

    /**
     * Single entry point for the reminder schedule. Cancels everything first, then
     * schedules workers for the chosen intensity:
     *  - Low    : morning only          (≤ 1/day)
     *  - Medium : morning + evening     (≤ 2/day)
     *  - High   : morning + 2 PM hydration (skips if water OK) + evening (≤ 3/day)
     *
     * Each worker has its own skip logic, so the actual notification count
     * is usually lower than the cap.
     */
    fun applyReminderSchedule(context: Context, enabled: Boolean, intensity: String) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(MORNING)
        wm.cancelUniqueWork(HYDRATION)
        wm.cancelUniqueWork(EVENING)
        wm.cancelUniqueWork(REENGAGEMENT)
        wm.cancelUniqueWork(LEGACY_WATER)
        if (!enabled) return

        when (intensity) {
            "Low" -> {
                schedule(wm, MORNING, hour = 8, MorningReminderWorker::class.java)
            }
            "High" -> {
                schedule(wm, MORNING, hour = 8, MorningReminderWorker::class.java)
                schedule(wm, HYDRATION, hour = 14, WaterReminderWorker::class.java)
                schedule(wm, EVENING, hour = 20, EveningReminderWorker::class.java)
            }
            else -> { // Medium (default)
                schedule(wm, MORNING, hour = 8, MorningReminderWorker::class.java)
                schedule(wm, EVENING, hour = 20, EveningReminderWorker::class.java)
            }
        }

        // Re-engagement runs at every intensity (it's already self-limiting:
        // only fires when ≥ 2 days inactive, and only once per inactivity period).
        schedule(wm, REENGAGEMENT, hour = 11, ReengagementWorker::class.java)
    }

    private fun schedule(
        wm: WorkManager,
        uniqueName: String,
        hour: Int,
        worker: Class<out ListenableWorker>
    ) {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(LocalTime.of(hour, 0))
        if (now.isAfter(target)) target = target.plusDays(1)
        val delayMin = Duration.between(now, target).toMinutes()

        val request = PeriodicWorkRequest.Builder(worker, 1, TimeUnit.DAYS)
            .setInitialDelay(delayMin, TimeUnit.MINUTES)
            .build()

        wm.enqueueUniquePeriodicWork(uniqueName, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
