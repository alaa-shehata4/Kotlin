package com.example.carebrief.presentation.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.carebrief.core.navigation.Routes
import com.example.carebrief.data.local.DemoData
import com.example.carebrief.presentation.analysis.AnalysisScreen
import com.example.carebrief.presentation.careplan.CarePlanEditorScreen
import com.example.carebrief.presentation.careplan.CarePlanScreen
import com.example.carebrief.presentation.dashboard.DashboardScreen
import com.example.carebrief.presentation.editor.NoteEditorScreen
import com.example.carebrief.presentation.more.MoreScreen
import com.example.carebrief.presentation.notes.NotesScreen
import com.example.carebrief.presentation.profile.RecipientProfileScreen
import com.example.carebrief.presentation.recipients.RecipientsScreen
import com.example.carebrief.presentation.summary.SummaryScreen

private data class Tab(val route: String, val label: String)

private fun displayName(id: String): String =
    DemoData.recipients.find { it.id == id }?.name ?: "Sarah Johnson"

@Composable
fun MainScaffold(onResetOnboarding: () -> Unit) {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab(Routes.HOME, "Home"),
        Tab(Routes.PEOPLE, "People"),
        Tab(Routes.NOTES, "Notes"),
        Tab(Routes.PLAN, "Plan"),
        Tab(Routes.MORE, "More")
    )
    val icons = mapOf(
        Routes.HOME to Icons.Filled.Home,
        Routes.PEOPLE to Icons.Filled.People,
        Routes.NOTES to Icons.Filled.Description,
        Routes.PLAN to Icons.Filled.Favorite,
        Routes.MORE to Icons.Filled.MoreHoriz
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by nav.currentBackStackEntryAsState()
                val current = backStack?.destination?.route
                fun selectedFor(tab: String): Boolean = when (tab) {
                    Routes.HOME -> current == Routes.HOME
                    Routes.PEOPLE -> current == Routes.PEOPLE || (current?.startsWith("profile/") == true)
                    Routes.NOTES -> current == Routes.NOTES || (current?.startsWith("note/") == true) ||
                        (current?.startsWith("analysis/") == true) || (current?.startsWith("summary/") == true)
                    Routes.PLAN -> current == Routes.PLAN || (current?.startsWith("plan/") == true)
                    else -> current == tab
                }
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedFor(tab.route),
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icons[tab.route]!!, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                DashboardScreen(
                    onAddNote = { nav.navigate(Routes.noteNew("sarah")) },
                    onAnalyze = { nav.navigate(Routes.analysis("sarah")) },
                    onViewPlan = { nav.navigate(Routes.planDetail("sarah")) },
                    onSelectRecipient = { nav.navigate(Routes.profile("sarah")) }
                )
            }
            composable(Routes.PEOPLE) {
                RecipientsScreen(onOpenRecipient = { id -> nav.navigate(Routes.profile(id)) })
            }
            composable(Routes.NOTES) {
                NotesScreen(
                    onAddNote = { id -> nav.navigate(Routes.noteNew(id)) },
                    onAnalyze = { id -> nav.navigate(Routes.analysis(id)) }
                )
            }
            composable(Routes.PLAN) {
                CarePlanScreen(
                    onEdit = { id -> nav.navigate(Routes.planEdit(id)) }
                )
            }
            composable(
                Routes.PLAN_DETAIL,
                arguments = listOf(navArgument("recipientId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("recipientId") ?: "sarah"
                CarePlanScreen(
                    recipientId = id,
                    recipientName = displayName(id),
                    onBack = { nav.popBackStack() },
                    onEdit = { rid -> nav.navigate(Routes.planEdit(rid)) }
                )
            }
            composable(
                Routes.PROFILE,
                arguments = listOf(navArgument("recipientId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("recipientId") ?: "sarah"
                RecipientProfileScreen(
                    recipientId = id,
                    onBack = { nav.popBackStack() },
                    onAddNote = { rid -> nav.navigate(Routes.noteNew(rid)) },
                    onAnalyze = { nav.navigate(Routes.analysis(id)) },
                    onViewPlan = { nav.navigate(Routes.planDetail(id)) }
                )
            }
            composable(
                Routes.NOTE_NEW,
                arguments = listOf(navArgument("recipientId") {
                    type = NavType.StringType
                    defaultValue = "sarah"
                })
            ) { entry ->
                val id = entry.arguments?.getString("recipientId") ?: "sarah"
                NoteEditorScreen(
                    recipientId = id,
                    recipientName = displayName(id),
                    onBack = { nav.popBackStack() },
                    onSavedAnalyze = { nav.navigate(Routes.analysis(id)) }
                )
            }
            composable(
                Routes.ANALYSIS,
                arguments = listOf(navArgument("recipientId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("recipientId") ?: "sarah"
                val count = DemoData.sarahNotes.size
                AnalysisScreen(
                    recipientName = displayName(id),
                    noteCount = count,
                    onComplete = {
                        nav.navigate(Routes.summary(id)) {
                            popUpTo(Routes.analysis(id)) { inclusive = true }
                        }
                    },
                    onCancel = { nav.popBackStack() }
                )
            }
            composable(
                Routes.SUMMARY,
                arguments = listOf(navArgument("recipientId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("recipientId") ?: "sarah"
                SummaryScreen(
                    recipientId = id,
                    recipientName = displayName(id),
                    onBack = { nav.popBackStack() },
                    onAddNote = { nav.navigate(Routes.noteNew(id)) },
                    onViewPlan = { nav.navigate(Routes.planDetail(id)) }
                )
            }
            composable(
                Routes.PLAN_EDIT,
                arguments = listOf(navArgument("recipientId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("recipientId") ?: "sarah"
                CarePlanEditorScreen(
                    recipientId = id,
                    recipientName = displayName(id),
                    onBack = { nav.popBackStack() },
                    onApproved = {
                        nav.navigate(Routes.planDetail(id)) {
                            popUpTo(Routes.planEdit(id)) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
