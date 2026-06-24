package com.pcodcompanion.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pcodcompanion.ui.components.PastelCard
import com.pcodcompanion.ui.components.StatCard
import java.util.Locale

@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Summary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Your health insights at a glance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Weekly Progress ──
        WeeklyProgressCard(
            overallPct = state.weeklyOverallPct,
            consistencyPct = state.weeklyConsistencyPct,
            exercisePct = state.weeklyExercisePct,
            waterPct = state.weeklyWaterGoalPct,
            dietPct = state.weeklyDietPct,
            daysLogged = state.weekDaysLogged,
            exerciseDays = state.weekExerciseDays,
            waterGoalHits = state.weekWaterGoalHits
        )

        Spacer(Modifier.height(16.dp))

        // ── Habit Consistency (last 7 days) ──
        if (state.last7Days.isNotEmpty()) {
            HabitConsistencyCard(days = state.last7Days)
            Spacer(Modifier.height(16.dp))
        }

        // ── Stats Grid ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Filled.NoteAlt,
                value = "${state.totalLogs}",
                label = "Total Logs",
                iconTint = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.CalendarMonth,
                value = "${state.averageCycleLength}d",
                label = "Avg. Cycle",
                iconTint = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Filled.FitnessCenter,
                value = "${state.exerciseDays}",
                label = "Exercise Days",
                iconTint = MaterialTheme.colorScheme.tertiary,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.LocalDrink,
                value = String.format(Locale.ROOT, "%.1f", state.avgWaterIntake),
                label = "Avg. Water",
                iconTint = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Filled.Restaurant,
                value = String.format(Locale.ROOT, "%.1f/10", state.avgDietScore),
                label = "Avg. Diet Score",
                iconTint = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // ── Top Symptoms ──
        if (state.commonSymptoms.isNotEmpty()) {
            PastelCard(containerColor = MaterialTheme.colorScheme.surface) {
                Text(
                    "Top Symptoms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                val maxCount = state.commonSymptoms.maxOf { it.second }
                state.commonSymptoms.forEach { (symptom, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            symptom,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(100.dp)
                        )
                        LinearProgressIndicator(
                            progress = { count.toFloat() / maxCount },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Mood Distribution ──
        if (state.moodDistribution.isNotEmpty()) {
            PastelCard(containerColor = MaterialTheme.colorScheme.surface) {
                Text(
                    "Mood Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                val maxMood = state.moodDistribution.maxOf { it.second }
                state.moodDistribution.forEach { (mood, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            mood,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(120.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(count.toFloat() / maxMood)
                                .height(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        )
                        if (count.toFloat() / maxMood < 1f) {
                            Spacer(Modifier.weight(1f - count.toFloat() / maxMood))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Insights ✨ ──
        if (state.insights.isNotEmpty()) {
            PastelCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    "Insights ✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(Modifier.height(12.dp))
                state.insights.forEach { insight ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("💡", modifier = Modifier.padding(end = 8.dp))
                        Text(
                            insight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // ── Empty State ──
        if (state.totalLogs == 0 && state.commonSymptoms.isEmpty()) {
            Spacer(Modifier.height(40.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✨", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A beautiful blank canvas 🌸",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Text(
                    "Begin logging to see your wellness journey unfold",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Export Button ──
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.material3.Button(
            onClick = {
                val exportText = viewModel.generateExportText()
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "PCOD Companion Data Export")
                    putExtra(android.content.Intent.EXTRA_TEXT, exportText)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Export Data"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(androidx.compose.material.icons.Icons.Filled.Share, contentDescription = "Export")
            Spacer(Modifier.width(8.dp))
            Text("Export for Doctor", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HabitConsistencyCard(days: List<DayHabits>) {
    PastelCard(containerColor = MaterialTheme.colorScheme.surface) {
        Text(
            "Habit Consistency",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Last 7 days",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(14.dp))

        HabitRow(emoji = "💧", label = "Water", days = days, selector = { it.water })
        Spacer(Modifier.height(10.dp))
        HabitRow(emoji = "🏋️", label = "Exercise", days = days, selector = { it.exercise })
        Spacer(Modifier.height(10.dp))
        HabitRow(emoji = "😴", label = "Sleep", days = days, selector = { it.sleep })

        Spacer(Modifier.height(8.dp))
        // Day letters under the dots — shown once
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(LABEL_WIDTH))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { d ->
                    Text(
                        d.dayLetter,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (d.isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = if (d.isToday) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(DOT_SIZE),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

private val LABEL_WIDTH = 88.dp
private val DOT_SIZE = 14.dp

@Composable
private fun HabitRow(
    emoji: String,
    label: String,
    days: List<DayHabits>,
    selector: (DayHabits) -> Boolean
) {
    val hits = days.count(selector)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(LABEL_WIDTH),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, modifier = Modifier.width(22.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.forEach { d ->
                val filled = selector(d)
                Box(
                    modifier = Modifier
                        .size(DOT_SIZE)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$hits/7",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun WeeklyProgressCard(
    overallPct: Float,
    consistencyPct: Float,
    exercisePct: Float,
    waterPct: Float,
    dietPct: Float,
    daysLogged: Int,
    exerciseDays: Int,
    waterGoalHits: Int
) {
    val pct = (overallPct * 100).toInt()
    PastelCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            "Weekly Progress",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "You completed $pct% of your plan this week 🌿",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(14.dp))

        ProgressRow(label = "Logging consistency", caption = "$daysLogged/7 days", progress = consistencyPct)
        Spacer(Modifier.height(10.dp))
        ProgressRow(label = "Exercise", caption = "$exerciseDays/7 days", progress = exercisePct)
        Spacer(Modifier.height(10.dp))
        ProgressRow(label = "Water goal", caption = "$waterGoalHits/7 days", progress = waterPct)
        Spacer(Modifier.height(10.dp))
        ProgressRow(label = "Diet score", caption = "${(dietPct * 10).let { String.format(Locale.ROOT, "%.1f", it) }}/10", progress = dietPct)
    }
}

@Composable
private fun ProgressRow(label: String, caption: String, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
        )
    }
}
