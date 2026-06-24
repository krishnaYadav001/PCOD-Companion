package com.pcodcompanion.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pcodcompanion.data.local.entity.PlanItem
import com.pcodcompanion.data.local.entity.dietScore
import com.pcodcompanion.data.local.entity.getMedicationsList
import com.pcodcompanion.data.model.Exercise
import com.pcodcompanion.data.model.PredefinedExercises
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    // ── Dialog state ──
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PlanItem?>(null) }
    var activeExercise by remember { mutableStateOf<Exercise?>(null) }
    var activeMiniAction by remember { mutableStateOf<Exercise?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.feedbackEvents.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        }
    }

    val haptics = LocalHapticFeedback.current

    val notifPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { /* result is informational; reminders are scheduled regardless */ }
    )

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ═══════════════════════════════════════
        // ── Header: Greeting + Date + Streak ──
        // ═══════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.clickable { showNameDialog = true }
                        )
                        if (state.userName.isBlank()) {
                            Icon(
                                androidx.compose.material.icons.Icons.Outlined.Edit,
                                contentDescription = "Set name",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp).clickable { showNameDialog = true }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.todayDate,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    // Streak badge
                    if (state.streakDays > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when {
                                    state.streakDays >= 3 -> "🌱 ${state.streakDays} days of mindful tracking"
                                    state.streakDays == 1 -> "🌱 First day in"
                                    else -> "🌱 ${state.streakDays} days going"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Suggestion cards (Focus, Cycle, Insight, Plan Suggestion, Closure) are hidden in Quiet Mode.
        if (!state.quietMode) {
            // ═══════════════════════════════════════
            // ── End-of-Day Closure (yesterday's reflection) ──
            // ═══════════════════════════════════════
            state.closureCard?.let { closure ->
                ClosureCard(
                    closure = closure,
                    onDismiss = { viewModel.dismissClosureCard() }
                )
                Spacer(Modifier.height(16.dp))
            }

            // ═══════════════════════════════════════
            // ── Today's Focus Card ──
            // ═══════════════════════════════════════
            state.dailyFocus?.let { focus ->
                TodaysFocusCard(focus = focus)
                Spacer(Modifier.height(16.dp))
            }

            // ═══════════════════════════════════════
            // ── Cycle-Based Recommendation ──
            // ═══════════════════════════════════════
            state.cycleRecommendation?.let { rec ->
                CycleRecommendationCard(rec = rec)
                Spacer(Modifier.height(16.dp))
            }

            // ═══════════════════════════════════════
            // ── Personal Insight (rotates every 3 days) ──
            // ═══════════════════════════════════════
            state.currentInsight?.let { insight ->
                InsightCard(message = insight.message)
                if (state.showFirstInsightHelper) {
                    Spacer(Modifier.height(8.dp))
                    FirstInsightHelper(onDismiss = { viewModel.dismissFirstInsightHelper() })
                }
                Spacer(Modifier.height(16.dp))
            }

            // ═══════════════════════════════════════
            // ── Smart Plan Suggestion ──
            // ═══════════════════════════════════════
            state.planSuggestion?.let { suggestion ->
                PlanSuggestionCard(
                    suggestion = suggestion,
                    onAccept = { viewModel.acceptSuggestion() },
                    onDismiss = { viewModel.dismissSuggestion() }
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // ═══════════════════════════════════════
        // ── Daily Emotional Check-In ──
        // ═══════════════════════════════════════
        if (state.todayLog?.emotionalCheckIn.isNullOrBlank()) {
            EmotionalCheckInCard(
                onSelect = { viewModel.updateEmotionalCheckIn(it) }
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Low Energy Mode Toggle ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (state.isLowEnergyMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Not feeling well today? 🫂", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (state.isLowEnergyMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                if (state.isLowEnergyMode) {
                    Spacer(Modifier.height(4.dp))
                    Text("Take it easy. We've simplified your plan to help you rest.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
            }
            androidx.compose.material3.Switch(
                checked = state.isLowEnergyMode,
                onCheckedChange = { viewModel.toggleLowEnergyMode(it) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // ── Today's Progress ──
        // ═══════════════════════════════════════
        TodayProgressCard(
            tasksCompleted = state.planItems.count { it.isCompleted },
            tasksTotal = state.planItems.size,
            water = state.todayLog?.waterIntake ?: 0,
            waterGoal = 8,
            exerciseDone = state.todayLog?.exerciseDone == true
        )

        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // ── Today's Plan ──
        // ═══════════════════════════════════════
        SectionCard(
            title = "📋 Today's Plan",
            trailing = {
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add task",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        ) {
            val items = if (state.isLowEnergyMode) state.planItems.take(1) else state.planItems
            if (items.isEmpty()) {
                Text(
                    "Take it easy! Your schedule is open today 🌸",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                items.forEachIndexed { index, item ->
                    key(item.id) {
                        PlanTaskRow(
                            item = item,
                            onToggle = { viewModel.togglePlanItem(item) },
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.deletePlanItem(item) },
                            onMoveUp = if (index > 0 && !state.isLowEnergyMode) {{ viewModel.movePlanItem(index, index - 1) }} else null,
                            onMoveDown = if (index < items.lastIndex && !state.isLowEnergyMode) {{ viewModel.movePlanItem(index, index + 1) }} else null
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // ── Daily Movement (Exercise) ──
        // ═══════════════════════════════════════
        SectionCard(title = "🏋️ Daily Movement") {
            val completedExerciseName = state.todayLog?.exerciseName
            val displayExercises = if (state.isLowEnergyMode) {
                PredefinedExercises.list.filter { it.name.contains("Yoga", ignoreCase = true) || it.name.contains("Stretch", ignoreCase = true) }
            } else {
                PredefinedExercises.list
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(displayExercises) { exercise ->
                    val isCompleted = completedExerciseName == exercise.name
                    val isAnyCompleted = state.todayLog?.exerciseDone == true
                    
                    // If an exercise is completed, highlight it. If a different one is completed, dim this one.
                    val alpha = if (isAnyCompleted && !isCompleted) 0.5f else 1f
                    
                    val bg by animateColorAsState(
                        if (isCompleted) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        label = "ex_bg"
                    )
                    val borderColor by animateColorAsState(
                        if (isCompleted) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        label = "ex_border"
                    )

                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .alpha(alpha)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { activeExercise = exercise }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = exercise.emoji, fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${exercise.durationMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isCompleted) {
                            Spacer(Modifier.height(4.dp))
                            Icon(
                                Icons.Filled.Add, // Placeholder for Check icon, actually let's just use Text
                                contentDescription = null,
                                modifier = Modifier.size(0.dp) // hiding the icon to use text
                            )
                            Text("✅ Done", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // ── Mini Wellness Break ──
        // ═══════════════════════════════════════
        SectionCard(title = "🌿 Mini Wellness Break") {
            Text(
                "Optional 2–5 minute resets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                com.pcodcompanion.data.model.PredefinedMiniActions.list.forEach { action ->
                    MiniActionPill(
                        action = action,
                        modifier = Modifier.weight(1f),
                        onClick = { activeMiniAction = action }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // ── Quick Track ──
        // ═══════════════════════════════════════
        SectionCard(title = "⚡ Quick Track") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickTrackTile(
                    emoji = "😴", label = "Sleep",
                    value = "${state.todayLog?.sleepHours ?: 0f}h",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onPlus = {
                        val cur = state.todayLog?.sleepHours ?: 0f
                        viewModel.updateSleepHours((cur + 0.5f).coerceAtMost(14f))
                    },
                    onMinus = {
                        val cur = state.todayLog?.sleepHours ?: 0f
                        viewModel.updateSleepHours((cur - 0.5f).coerceAtLeast(0f))
                    }
                )
                QuickTrackTile(
                    emoji = "🍎", label = "Fruit",
                    value = "${state.todayLog?.fruitServings ?: 0}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    onPlus = {
                        val cur = state.todayLog?.fruitServings ?: 0
                        viewModel.updateFruitServings((cur + 1).coerceAtMost(10))
                    },
                    onMinus = {
                        val cur = state.todayLog?.fruitServings ?: 0
                        viewModel.updateFruitServings((cur - 1).coerceAtLeast(0))
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Sleep Quality
            Text("Sleep Quality", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Good", "Disturbed", "Poor").forEach { quality ->
                    val isSelected = state.todayLog?.sleepQuality == quality
                    val chipBg by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        label = "sleepQ"
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

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sugar level selector
                val sugarLevels = listOf("None", "Low", "Med", "High")
                val currentSugar = state.todayLog?.sugarLevel ?: 0
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🍬", fontSize = 24.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Sugar", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sugarLevels.forEachIndexed { idx, lbl ->
                            val sel = idx == currentSugar
                            val bg by animateColorAsState(
                                if (sel) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                label = "sugarBg"
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { viewModel.updateSugarLevel(idx) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    lbl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sel) MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                QuickTrackTile(
                    emoji = "💧", label = "Water",
                    value = "${state.todayLog?.waterIntake ?: 0}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    onPlus = {
                        val cur = state.todayLog?.waterIntake ?: 0
                        viewModel.updateWaterIntake(cur + 1)
                    },
                    onMinus = {
                        val cur = state.todayLog?.waterIntake ?: 0
                        viewModel.updateWaterIntake((cur - 1).coerceAtLeast(0))
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Mood row
            val moods = listOf("😊" to "Happy", "😌" to "Calm", "😰" to "Anxious", "😴" to "Tired", "😢" to "Sad")
            Text("Mood", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                moods.forEach { (emoji, label) ->
                    val sel = state.todayLog?.mood == label
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

            Spacer(Modifier.height(16.dp))

            // Stress Level row
            val stressLevels = listOf("Low", "Medium", "High")
            Text("Stress Level", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stressLevels.forEach { level ->
                    val isSelected = state.todayLog?.stressLevel == level
                    val chipBg by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "stressBg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(chipBg)
                            .then(
                                if (isSelected) Modifier else Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(24.dp)
                                )
                            )
                            .clickable { viewModel.updateStressLevel(level) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .animateContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            level,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Protein Inclusion
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.3f)).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Included Protein Today? 🥩", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                androidx.compose.material3.Switch(
                    checked = state.todayLog?.proteinIncluded == true,
                    onCheckedChange = { viewModel.updateProteinIncluded(it) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // ── Symptoms ──
        // ═══════════════════════════════════════
        SectionCard(title = "🩺 Symptoms") {
            val symptomChips = listOf(
                "💇" to "Hair Loss", "🔴" to "Acne",
                "⚡" to "Low Energy", "🫧" to "Bloating"
            )
            val symptomsRaw = state.todayLog?.symptoms
            val currentSymptoms = remember(symptomsRaw) {
                symptomsRaw?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            }

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

        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // ── Medications & Supplements ──
        // ═══════════════════════════════════════
        SectionCard(
            title = "💊 Medications & Supplements",
            trailing = {
                IconButton(
                    onClick = { showAddMedicationDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add medication",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        ) {
            val meds = state.todayLog?.getMedicationsList() ?: emptyList()
            if (meds.isEmpty()) {
                Text(
                    "No medications tracked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meds.forEach { med ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleMedication(med.name)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = med.isTaken,
                                onCheckedChange = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleMedication(med.name)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = med.name,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (med.isTaken) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                color = if (med.isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.removeMedication(med.name) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // ── Weekly Reflection ──
        // ═══════════════════════════════════════
        val currentWeek = state.currentWeekStats
        val lastWeek = state.lastWeekStats
        if (currentWeek != null && lastWeek != null && currentWeek.daysLogged > 0) {
            SectionCard(title = "📈 Weekly Reflection") {
                val waterDiff = currentWeek.avgWaterIntake - lastWeek.avgWaterIntake
                val sleepDiff = currentWeek.avgSleepHours - lastWeek.avgSleepHours
                val dietDiff = currentWeek.avgDietScore - lastWeek.avgDietScore

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("This Week", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("${currentWeek.daysLogged} days logged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("💧 ${String.format(Locale.ROOT, "%.1f", currentWeek.avgWaterIntake)} avg", style = MaterialTheme.typography.bodySmall)
                        Text("😴 ${String.format(Locale.ROOT, "%.1f", currentWeek.avgSleepHours)}h avg", style = MaterialTheme.typography.bodySmall)
                        Text("🌿 ${String.format(Locale.ROOT, "%.1f", currentWeek.avgDietScore)} diet", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Last Week", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(Modifier.height(4.dp))
                        Text("${lastWeek.daysLogged} days logged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Text("💧 ${String.format(Locale.ROOT, "%.1f", lastWeek.avgWaterIntake)} avg", style = MaterialTheme.typography.bodySmall)
                        Text("😴 ${String.format(Locale.ROOT, "%.1f", lastWeek.avgSleepHours)}h avg", style = MaterialTheme.typography.bodySmall)
                        Text("🌿 ${String.format(Locale.ROOT, "%.1f", lastWeek.avgDietScore)} diet", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Comparison chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ComparisonChip(
                        label = "Water",
                        diff = waterDiff,
                        modifier = Modifier.weight(1f)
                    )
                    ComparisonChip(
                        label = "Sleep",
                        diff = sleepDiff,
                        modifier = Modifier.weight(1f)
                    )
                    ComparisonChip(
                        label = "Diet",
                        diff = dietDiff,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ═══════════════════════════════════════
        // ── Summary Strip ──
        // ═══════════════════════════════════════
        val log = state.todayLog
        val completedTasks = remember(state.planItems) { state.planItems.count { it.isCompleted } }
        val totalTasks = state.planItems.size
        val symptomCount = remember(log?.symptoms) {
            log?.symptoms?.split(",")?.map { it.trim() }?.count { it.isNotBlank() } ?: 0
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "📊 Today's Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (totalTasks > 0 && completedTasks == totalTasks) {
                        Spacer(Modifier.width(8.dp))
                        Text("You're doing well 🌸", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Hide a 0/10 score — feels like a failing grade for a fresh morning.
                val dietScore = log?.dietScore ?: 0
                if (dietScore > 0) {
                    Text(
                        "Today's Diet Score: $dietScore/10 🌿",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryChip("✅ $completedTasks/$totalTasks", "Tasks")
                    SummaryChip("💧 ${log?.waterIntake ?: 0}", "Water")
                    SummaryChip("😴 ${log?.sleepHours ?: 0f}h", "Sleep")
                    SummaryChip("🩺 $symptomCount", "Symp.")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
    )
  }

    // ═══════════════════════════════════════
    // ── Dialogs ──
    // ═══════════════════════════════════════
    
    activeExercise?.let { ex ->
        ExerciseTimerDialog(
            exercise = ex,
            onDismiss = { activeExercise = null },
            onComplete = {
                viewModel.completeExercise(it)
                activeExercise = null
            }
        )
    }

    activeMiniAction?.let { action ->
        ExerciseTimerDialog(
            exercise = action,
            onDismiss = { activeMiniAction = null },
            onComplete = {
                viewModel.onMiniActionCompleted(it)
                activeMiniAction = null
            }
        )
    }

    if (showAddDialog) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            TaskBottomSheetContent(
                title = "Add Task",
                initialText = "",
                showPresets = true,
                onConfirm = { text ->
                    viewModel.addPlanItem(text)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }

    editingItem?.let { item ->
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { editingItem = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            TaskBottomSheetContent(
                title = "Edit Task",
                initialText = item.title,
                onConfirm = { text ->
                    viewModel.editPlanItem(item, text)
                    editingItem = null
                },
                onDismiss = { editingItem = null }
            )
        }
    }

    if (showAddMedicationDialog) {
        var medName by remember { mutableStateOf("") }
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAddMedicationDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Medication", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Text(
                    "Common supplements",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                val medPresets = listOf("Inositol", "Vitamin D", "Folate", "Magnesium", "Berberine", "Metformin")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    medPresets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addMedication(preset)
                                    showAddMedicationDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                preset,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Text(
                    "Or type your own:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                androidx.compose.material3.OutlinedTextField(
                    value = medName,
                    onValueChange = { medName = it },
                    label = { Text("Medication / Supplement Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextButton(onClick = { showAddMedicationDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            if (medName.isNotBlank()) {
                                viewModel.addMedication(medName.trim())
                            }
                            showAddMedicationDialog = false
                        }
                    ) {
                        Text("Add")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showNameDialog) {
        var nameInput by remember { mutableStateOf(state.userName) }
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showNameDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Your Name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                androidx.compose.material3.OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("What should we call you?") },
                    placeholder = { Text("e.g., Priya") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextButton(onClick = { showNameDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            viewModel.setUserName(nameInput.trim())
                            showNameDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ═══════════════════════════════════════
    // ── First-launch reminder opt-in prompt ──
    // ═══════════════════════════════════════
    if (state.showFirstLaunchReminderPrompt) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissFirstLaunchReminderPrompt() },
            title = {
                Text(
                    "Want gentle daily reminders? 💛",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "I'll send a soft nudge in the morning and evening — only when it's helpful. " +
                        "You can change this anytime in Settings."
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.acceptFirstLaunchReminderPrompt()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) { Text("Enable") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.dismissFirstLaunchReminderPrompt() }
                ) { Text("Not now") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ═══════════════════════════════════════════════
// ── Reusable Composables ──
// ═══════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskBottomSheetContent(
    title: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    showPresets: Boolean = false
) {
    var text by remember { mutableStateOf(initialText) }
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        if (showPresets) {
            Text(
                "Quick add",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            val presets = listOf(
                "💧 Drink water",
                "🚶 10-min walk",
                "🧘 5-min stretch",
                "🍎 Eat a fruit",
                "💊 Take supplement"
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirm(preset)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            preset,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Text(
                "Or type your own:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Task name…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ClosureCard(closure: YesterdaysClosure, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌸", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Yesterday's reflection",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    letterSpacing = 0.6.sp
                )
                Spacer(Modifier.height(2.dp))
                // Show the % only when it's a non-soft-failure number — leading with
                // "You completed 7% yesterday" still feels like a grade, even with a
                // soft tagline below.
                val pct = closure.percentage
                val showNumber = pct != null && pct >= 30
                if (showNumber) {
                    Text(
                        "You completed $pct% yesterday",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    closure.supportiveLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (showNumber) FontWeight.Normal else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FirstInsightHelper(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💡", modifier = Modifier.padding(end = 10.dp))
        Text(
            text = "These insights help you understand your patterns. Tap to dismiss.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InsightCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✨", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Something I've noticed",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                    letterSpacing = 0.6.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun CycleRecommendationCard(rec: CycleRecommendation) {
    val accent = when (rec.phase) {
        CyclePhase.PERIOD -> MaterialTheme.colorScheme.primaryContainer
        CyclePhase.EARLY  -> MaterialTheme.colorScheme.secondaryContainer
        CyclePhase.LATER  -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val onAccent = when (rec.phase) {
        CyclePhase.PERIOD -> MaterialTheme.colorScheme.onPrimaryContainer
        CyclePhase.EARLY  -> MaterialTheme.colorScheme.onSecondaryContainer
        CyclePhase.LATER  -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(rec.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rec.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onAccent.copy(alpha = 0.8f),
                    letterSpacing = 0.6.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    rec.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = onAccent
                )
            }
        }
    }
}

@Composable
private fun MiniActionPill(
    action: Exercise,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(action.emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            action.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${action.durationMinutes} min",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EmotionalCheckInCard(onSelect: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "How are you feeling today?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CheckInChoice(emoji = "😊", label = "Good", modifier = Modifier.weight(1f), onClick = { onSelect("Good") })
                CheckInChoice(emoji = "😐", label = "Okay", modifier = Modifier.weight(1f), onClick = { onSelect("Okay") })
                CheckInChoice(emoji = "😞", label = "Low",  modifier = Modifier.weight(1f), onClick = { onSelect("Low") })
            }
        }
    }
}

@Composable
private fun CheckInChoice(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PlanSuggestionCard(
    suggestion: PlanSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💛", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    suggestion.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                suggestion.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Not now", color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(suggestion.actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TodayProgressCard(
    tasksCompleted: Int,
    tasksTotal: Int,
    water: Int,
    waterGoal: Int,
    exerciseDone: Boolean
) {
    val tasksPct = if (tasksTotal > 0) tasksCompleted.toFloat() / tasksTotal else 0f
    val waterPct = (water.toFloat() / waterGoal).coerceIn(0f, 1f)
    val overallPct = ((tasksPct + waterPct + (if (exerciseDone) 1f else 0f)) / 3f).coerceIn(0f, 1f)
    val overallInt = (overallPct * 100).toInt()
    val animatedOverallInt by animateIntAsState(targetValue = overallInt, animationSpec = tween(600), label = "overallInt")

    val emoji = when {
        overallPct >= 1f    -> "🌟"
        overallPct >= 0.75f -> "✨"
        overallPct >= 0.50f -> "🌸"
        overallPct >= 0.25f -> "🌿"
        else                -> "🌱"
    }

    val isComplete = overallPct >= 1f
    val containerColor by animateColorAsState(
        targetValue = if (isComplete)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        else
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        animationSpec = tween(500),
        label = "todayCardBg"
    )

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isComplete) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$emoji Today's Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "$animatedOverallInt%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = isComplete) {
                Text(
                    "All done for today — beautiful work 🌷",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            TodayProgressRow(emoji = "📋", label = "Plan", caption = "$tasksCompleted/$tasksTotal", progress = tasksPct)
            Spacer(Modifier.height(8.dp))
            TodayProgressRow(emoji = "💧", label = "Water", caption = "$water/$waterGoal", progress = waterPct)
            Spacer(Modifier.height(8.dp))
            TodayProgressRow(
                emoji = "🏋️",
                label = "Exercise",
                caption = if (exerciseDone) "Done" else "Pending",
                progress = if (exerciseDone) 1f else 0f
            )
        }
    }
}

@Composable
private fun TodayProgressRow(emoji: String, label: String, caption: String, progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "rowProgress"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp, modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PlanTaskRow(
    item: PlanItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (!item.isCompleted) FontWeight.Medium else FontWeight.Normal,
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            Icons.Outlined.Edit,
            contentDescription = "Edit",
            modifier = Modifier
                .size(16.dp)
                .clickable { onEdit() },
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(Modifier.width(4.dp))
        Box {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "Menu",
                modifier = Modifier
                    .size(18.dp)
                    .clickable { showMenu = true },
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (onMoveUp != null) {
                    DropdownMenuItem(
                        text = { Text("Move Up") },
                        onClick = { onMoveUp(); showMenu = false },
                        leadingIcon = {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                if (onMoveDown != null) {
                    DropdownMenuItem(
                        text = { Text("Move Down") },
                        onClick = { onMoveDown(); showMenu = false },
                        leadingIcon = {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { onDelete(); showMenu = false },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Delete, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)
                        )
                    }
                )
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
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniButton("−", onMinus)
            MiniButton("+", onPlus)
        }
    }
}

@Composable
private fun MiniButton(symbol: String, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun SummaryChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ComparisonChip(label: String, diff: Double, modifier: Modifier = Modifier) {
    // Down-trends use a soft neutral color, never error red — calm app, no scolding.
    val onSurfaceMuted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val (arrow, color) = when {
        diff > 0 -> Pair("↑", MaterialTheme.colorScheme.primary)
        diff < 0 -> Pair("↓", onSurfaceMuted)
        else     -> Pair("→", onSurfaceMuted)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$arrow ${String.format(Locale.ROOT, "%.1f", kotlin.math.abs(diff))}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TodaysFocusCard(focus: DailyFocus) {
    // Card background is intentionally always a light pastel (doesn't follow dark mode),
    // so all text on this card uses fixed dark colors for contrast.
    // pastelAccent is deep (lightness 0.35) so the label reads cleanly on the light bg.
    val (pastelBase, pastelLight, pastelAccent) = remember(focus.accentHue) {
        Triple(
            Color.hsl(focus.accentHue, 0.55f, 0.88f),
            Color.hsl(focus.accentHue, 0.40f, 0.94f),
            Color.hsl(focus.accentHue, 0.70f, 0.35f)
        )
    }
    val onPastelText = Color(0xFF2D2235) // Same as OnSurfaceLight — always-dark for contrast

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)) +
                slideInVertically(animationSpec = tween(500)) { -it / 3 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = pastelAccent.copy(alpha = 0.25f),
                    spotColor = pastelAccent.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(pastelLight, pastelBase, pastelLight)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Emoji circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = focus.emoji,
                        fontSize = 26.sp
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Focus",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = pastelAccent,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = focus.message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onPastelText
                    )
                }

                // Decorative sparkle dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(pastelAccent.copy(alpha = 0.5f))
                        .align(Alignment.Top)
                )
            }
        }
    }
}
