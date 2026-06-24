package com.pcodcompanion.ui.cycletracker

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pcodcompanion.data.local.entity.CycleEntry
import com.pcodcompanion.ui.components.ChipRow
import com.pcodcompanion.ui.components.PastelCard
import com.pcodcompanion.ui.components.StatCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CycleTrackerScreen(viewModel: CycleTrackerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isActive = state.activeCycle != null

    var editingEntry by remember { mutableStateOf<CycleEntry?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Past Cycle")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Cycle Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Track your menstrual cycle",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(20.dp))

            // ── Current Status Card ──
            PastelCard(
                modifier = Modifier.animateContentSize(),
                containerColor = if (isActive)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isActive) "Period Active" else "Waiting for next cycle 🌸",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isActive -> "Started: ${state.activeCycle?.startDate ?: ""}"
                            state.entries.isEmpty() -> "Tap below when your period starts 🌷"
                            else -> "Day ${state.daysSinceLastPeriod} of cycle"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(16.dp))

                    if (isActive) {
                        OutlinedButton(
                            onClick = { viewModel.endPeriod() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("End Period") }
                    } else {
                        Button(
                            onClick = { viewModel.startPeriod() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Start Period", color = MaterialTheme.colorScheme.onPrimary) }
                    }
                }
            }

            // ── Flow Level (when active) ──
            if (isActive) {
                Spacer(Modifier.height(16.dp))
                PastelCard(containerColor = MaterialTheme.colorScheme.surface) {
                    Text(
                        "Flow Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    ChipRow(
                        chips = listOf("Light", "Medium", "Heavy"),
                        selectedChip = state.activeCycle?.flowLevel ?: "Medium",
                        onChipSelected = { viewModel.updateFlowLevel(it) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Stats Row ── (replaced with friendly empty state when no entries)
            if (state.entries.isEmpty()) {
                PastelCard(containerColor = MaterialTheme.colorScheme.surface) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌸", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "No cycles logged yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Once you log your first cycle, your stats will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Filled.CalendarMonth,
                        value = "${state.averageCycleLength}",
                        label = "Avg. Cycle",
                        iconTint = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.Timer,
                        value = "${state.daysSinceLastPeriod}",
                        label = "Day of Cycle",
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Recent Cycles ──
            if (state.entries.isNotEmpty()) {
                Text(
                    "All Cycles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                
                // Sort by startDate descending just to be safe visually
                val sortedEntries = state.entries.sortedByDescending { it.startDate }
                
                sortedEntries.forEachIndexed { index, entry ->
                    // Calculate individual cycle length based on the NEXT chronological period (which is at index - 1)
                    val cycleLengthStr = if (index > 0) {
                        val nextChronological = sortedEntries[index - 1]
                        try {
                            val start1 = LocalDate.parse(entry.startDate)
                            val start2 = LocalDate.parse(nextChronological.startDate)
                            val len = ChronoUnit.DAYS.between(start1, start2)
                            "$len days"
                        } catch (e: Exception) { "Ongoing" }
                    } else "Current"

                    PastelCard(
                        containerColor = MaterialTheme.colorScheme.surface,
                        elevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Started: ${entry.startDate}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { editingEntry = entry },
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "Ended: ${entry.endDate ?: "Ongoing"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    entry.flowLevel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    cycleLengthStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(64.dp))
        }
    }

    // ── Dialogs ──
    if (editingEntry != null) {
        val entry = editingEntry!!
        var startDate by remember { mutableStateOf(entry.startDate) }
        var endDate by remember { mutableStateOf(entry.endDate ?: "") }
        var flowLevel by remember { mutableStateOf(entry.flowLevel) }
        var errorMsg by remember { mutableStateOf("") }

        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { editingEntry = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("Edit Cycle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Flow Level", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                ChipRow(
                    chips = listOf("Light", "Medium", "Heavy"),
                    selectedChip = flowLevel,
                    onChipSelected = { flowLevel = it }
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.deleteCycle(entry)
                            editingEntry = null
                        }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    
                    Row {
                        TextButton(onClick = { editingEntry = null }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    LocalDate.parse(startDate) // Validate
                                    if (endDate.isNotBlank()) LocalDate.parse(endDate) // Validate
                                    viewModel.editCycle(entry, startDate, endDate.takeIf { it.isNotBlank() }, flowLevel)
                                    editingEntry = null
                                } catch (e: DateTimeParseException) {
                                    errorMsg = "Invalid date format. Use YYYY-MM-DD."
                                }
                            }
                        ) { Text("Save", fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAddDialog) {
        var startDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
        var endDate by remember { mutableStateOf("") }
        var flowLevel by remember { mutableStateOf("Medium") }
        var errorMsg by remember { mutableStateOf("") }

        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("Add Past Cycle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Flow Level", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                ChipRow(
                    chips = listOf("Light", "Medium", "Heavy"),
                    selectedChip = flowLevel,
                    onChipSelected = { flowLevel = it }
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                LocalDate.parse(startDate) // Validate
                                if (endDate.isNotBlank()) LocalDate.parse(endDate) // Validate
                                viewModel.addManualCycle(startDate, endDate.takeIf { it.isNotBlank() }, flowLevel)
                                showAddDialog = false
                            } catch (e: DateTimeParseException) {
                                errorMsg = "Invalid date format. Use YYYY-MM-DD."
                            }
                        }
                    ) { Text("Add", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
