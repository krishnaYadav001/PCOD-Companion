package com.pcodcompanion.ui.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pcodcompanion.data.local.entity.DailyLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            "History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Your past daily logs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))

        // Friendly hint when no logs exist at all (first-time user)
        if (state.logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Tap any day to add a log. Logged days will appear as filled circles 🌸",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ═══════════════════════════════════════
        // ── Calendar Header ──
        // ═══════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Month")
            }
            Text(
                text = "${state.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${state.currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // ── Calendar Grid ──
        // ═══════════════════════════════════════
        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val daysInMonth = state.currentMonth.lengthOfMonth()
        val firstDayOfWeek = state.currentMonth.atDay(1).dayOfWeek.value % 7 // 0 = Sunday
        val totalCells = daysInMonth + firstDayOfWeek
        val rows = if (totalCells % 7 == 0) totalCells / 7 else (totalCells / 7) + 1

        val loggedDates = state.logs.map { it.date }.toSet()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(rows * 7) { index ->
                if (index < firstDayOfWeek || index >= firstDayOfWeek + daysInMonth) {
                    Spacer(modifier = Modifier.size(40.dp))
                } else {
                    val day = index - firstDayOfWeek + 1
                    val date = state.currentMonth.atDay(day)
                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val hasLog = loggedDates.contains(dateStr)
                    val isToday = date == LocalDate.now()

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasLog) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(
                                width = if (isToday) 2.dp else 0.dp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.selectDate(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (hasLog || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (hasLog) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════
    // ── Bottom Sheet for Editing ──
    // ═══════════════════════════════════════
    if (state.isSheetOpen && state.selectedDate != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSheet() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            EditLogSheetContent(
                date = state.selectedDate!!,
                log = state.selectedLog,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditLogSheetContent(
    date: LocalDate,
    log: DailyLog?,
    viewModel: HistoryViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (log == null) {
                    Text("A fresh start! Adding details will create a new log for this day 🌸",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (log != null) {
                IconButton(onClick = { viewModel.deleteLog() }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Log", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Quick Track ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickTrackTile(
                emoji = "😴", label = "Sleep",
                value = "${log?.sleepHours ?: 0f}h",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primaryContainer,
                onPlus = {
                    val cur = log?.sleepHours ?: 0f
                    viewModel.updateSleepHours((cur + 0.5f).coerceAtMost(14f))
                },
                onMinus = {
                    val cur = log?.sleepHours ?: 0f
                    viewModel.updateSleepHours((cur - 0.5f).coerceAtLeast(0f))
                }
            )
            QuickTrackTile(
                emoji = "💧", label = "Water",
                value = "${log?.waterIntake ?: 0}",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                onPlus = {
                    val cur = log?.waterIntake ?: 0
                    viewModel.updateWaterIntake(cur + 1)
                },
                onMinus = {
                    val cur = log?.waterIntake ?: 0
                    viewModel.updateWaterIntake((cur - 1).coerceAtLeast(0))
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Sleep Quality ──
        Text("Sleep Quality", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Good", "Disturbed", "Poor").forEach { quality ->
                val isSelected = log?.sleepQuality == quality
                val chipBg by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    label = "historySleepQ"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBg)
                        .then(
                            if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable { viewModel.updateSleepQuality(quality) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quality,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Protein Inclusion ──
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.3f)).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Included Protein Today? 🥩", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            androidx.compose.material3.Switch(
                checked = log?.proteinIncluded == true,
                onCheckedChange = { viewModel.updateProteinIncluded(it) }
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Mood ──
        Text("Mood", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val moods = listOf("😊" to "Happy", "😌" to "Calm", "😰" to "Anxious", "😴" to "Tired", "😢" to "Sad")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            moods.forEach { (emoji, label) ->
                val sel = log?.mood == label
                val bg by animateColorAsState(
                    if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                    label = "moodBg"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .clickable { viewModel.updateMood(label) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(emoji, fontSize = 24.sp)
                    Text(
                        label, fontSize = 10.sp,
                        color = if (sel) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Symptoms ──
        Text("Symptoms", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val symptomChips = listOf(
            "💇" to "Hair Loss", "🔴" to "Acne",
            "⚡" to "Low Energy", "🫧" to "Bloating"
        )
        val currentSymptoms = log?.symptoms
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            symptomChips.forEach { (emoji, symptom) ->
                val isSelected = symptom in currentSymptoms
                val chipBg by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "chipBg"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(chipBg)
                        .then(
                            if (isSelected) Modifier else Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(24.dp)
                            )
                        )
                        .clickable {
                            val updated = if (isSelected)
                                currentSymptoms.filter { it != symptom }
                            else currentSymptoms + symptom
                            viewModel.updateSymptoms(updated.joinToString(", "))
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .animateContentSize()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            symptom,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTrackTile(
    emoji: String, label: String, value: String,
    modifier: Modifier = Modifier, color: Color,
    onPlus: () -> Unit, onMinus: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniButton("−", onMinus)
            MiniButton("+", onPlus)
        }
    }
}

@Composable
private fun MiniButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
