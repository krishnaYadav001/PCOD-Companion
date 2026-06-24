package com.pcodcompanion.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pcodcompanion.data.repository.PCODRepository
import com.pcodcompanion.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Adaptive backoff: if the user keeps ignoring reminders, we fire less often.
 *  - 0–2 ignores : every day
 *  - 3–4 ignores : every other day
 *  - 5+   ignores : once a week (Mondays)
 *
 * Also tracks engagement: if the user hasn't opened the app since yesterday,
 * the ignore count is incremented before deciding whether to fire.
 */
private fun guard(settings: SettingsRepository): Boolean {
    if (!settings.remindersEnabled.value) return false
    // Quiet Mode silences routine pings; re-engagement (the safety net) is exempt
    // because it has its own dedicated worker that doesn't go through this guard.
    if (settings.quietMode.value) return false

    val today = LocalDate.now()
    val lastOpen = settings.getLastAppOpenDate()
    val lastOpenDate = runCatching { LocalDate.parse(lastOpen) }.getOrNull()

    // If user didn't open the app since at least yesterday, count this firing as "ignored"
    if (lastOpenDate != null && today.toEpochDay() - lastOpenDate.toEpochDay() >= 1L) {
        settings.setReminderIgnoreCount(settings.getReminderIgnoreCount() + 1)
    }

    val ignores = settings.getReminderIgnoreCount()
    return when {
        ignores <= 2 -> true
        ignores <= 4 -> today.toEpochDay() % 2L == 0L
        else         -> today.dayOfWeek == DayOfWeek.MONDAY
    }
}

private fun userOpenedAppToday(settings: SettingsRepository): Boolean {
    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    return settings.getLastAppOpenDate() == today
}

@HiltWorker
class MorningReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settings: SettingsRepository
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (!guard(settings)) return Result.success()
        // If the user already opened the app today, the morning ping is redundant
        if (userOpenedAppToday(settings)) return Result.success()

        NotificationHelper.showNotification(
            context, NOTIF_ID,
            "Good Morning ☀️",
            "A small step today is a kind thing for your future self."
        )
        return Result.success()
    }
    companion object { const val NOTIF_ID = 1001 }
}

/**
 * Hydration check — fires ONLY if water intake is low when this worker runs.
 * Scheduled for ~2 PM (configured by [WorkManagerHelper]).
 */
@HiltWorker
class WaterReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PCODRepository,
    private val settings: SettingsRepository
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (!guard(settings)) return Result.success()

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val log = repository.getLogByDate(today).first()
        val water = log?.waterIntake ?: 0

        // Already met half the daily goal — no need to nag
        if (water >= 4) return Result.success()

        NotificationHelper.showNotification(
            context, NOTIF_ID,
            "Hydration check 💧",
            "You've had $water glass${if (water == 1) "" else "es"} today. A glass now would feel good."
        )
        return Result.success()
    }
    companion object { const val NOTIF_ID = 1002 }
}

@HiltWorker
class EveningReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PCODRepository,
    private val settings: SettingsRepository
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (!guard(settings)) return Result.success()

        // Skip if the user has already done their evening reflection (emotional check-in implies engagement)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val log = repository.getLogByDate(today).first()
        if (!log?.emotionalCheckIn.isNullOrBlank()) return Result.success()

        NotificationHelper.showNotification(
            context, NOTIF_ID,
            "Wind down 🌙",
            "A short check-in now will round out your day."
        )
        return Result.success()
    }
    companion object { const val NOTIF_ID = 1003 }
}

/**
 * Gentle re-engagement: fires ONCE per inactivity period when the user hasn't opened
 * the app for ≥ 2 days. Resets the moment the user opens the app again.
 *
 *  - Respects the master reminders toggle
 *  - Doesn't repeat — `lastReengagementDate` is updated once we fire, and we won't
 *    fire again until `lastAppOpenDate` advances past it (i.e. user came back at
 *    least once and then went silent again).
 */
@HiltWorker
class ReengagementWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settings: SettingsRepository
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (!settings.remindersEnabled.value) return Result.success()

        val today = LocalDate.now()
        val lastOpen = runCatching { LocalDate.parse(settings.getLastAppOpenDate()) }.getOrNull()
            ?: return Result.success()  // never opened — first-launch flow handles this case

        val daysAway = today.toEpochDay() - lastOpen.toEpochDay()
        if (daysAway < 2L) return Result.success()  // still active

        // Already nudged for this inactivity period — wait until they come back first
        val lastNudge = runCatching { LocalDate.parse(settings.getLastReengagementDate()) }.getOrNull()
        if (lastNudge != null && !lastNudge.isBefore(lastOpen)) return Result.success()

        NotificationHelper.showNotification(
            context, NOTIF_ID,
            "We missed you 🌸",
            "Start small today — even one little check-in counts."
        )
        settings.setLastReengagementDate(today.toString())
        return Result.success()
    }
    companion object { const val NOTIF_ID = 1004 }
}
