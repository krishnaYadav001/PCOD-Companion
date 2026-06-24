package com.pcodcompanion.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pcodcompanion.ui.cycletracker.CycleTrackerScreen
import com.pcodcompanion.ui.history.HistoryScreen
import com.pcodcompanion.ui.planbuilder.PlanBuilderScreen
import com.pcodcompanion.ui.settings.SettingsScreen
import com.pcodcompanion.ui.summary.SummaryScreen
import com.pcodcompanion.ui.today.TodayScreen

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Today.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Today.route) { TodayScreen(onOpenSettings = { navController.navigate("settings") }) }
        composable(BottomNavItem.PlanBuilder.route) { PlanBuilderScreen() }
        composable(BottomNavItem.CycleTracker.route) { CycleTrackerScreen() }
        composable(BottomNavItem.Summary.route) { SummaryScreen(onOpenSettings = { navController.navigate("settings") }) }
        composable(BottomNavItem.History.route) { HistoryScreen() }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
