package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.login.LoginScreen
import com.example.ui.screens.login.LoginViewModel
import com.example.ui.screens.register.RegisterScreen
import com.example.ui.screens.register.RegisterViewModel
import com.example.ui.screens.reports.RecentReportsScreen
import com.example.ui.screens.reports.RecentReportsViewModel
import com.example.ui.screens.search.PatientSearchScreen
import com.example.ui.screens.search.PatientSearchViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.templates.TemplateManagerScreen
import com.example.ui.screens.templates.TemplateManagerViewModel
import com.example.ui.screens.updates.UpdateScreen
import com.example.ui.screens.updates.UpdateViewModel
import com.example.ui.screens.wizard.ReportWizardScreen
import com.example.ui.screens.wizard.ReportWizardViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Register.route) {
            val viewModel: RegisterViewModel = viewModel()
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = viewModel()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToWizard = { navController.navigate(Screen.ReportWizard.route) },
                onNavigateToReports = { navController.navigate(Screen.RecentReports.route) },
                onNavigateToSearch = { navController.navigate(Screen.PatientSearch.route) },
                onNavigateToTemplates = { navController.navigate(Screen.TemplateManager.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToUpdates = { navController.navigate(Screen.Updates.route) }
            )
        }

        composable(Screen.ReportWizard.route) {
            val viewModel: ReportWizardViewModel = viewModel()
            ReportWizardScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RecentReports.route) {
            val viewModel: RecentReportsViewModel = viewModel()
            RecentReportsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PatientSearch.route) {
            val viewModel: PatientSearchViewModel = viewModel()
            PatientSearchScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TemplateManager.route) {
            val viewModel: TemplateManagerViewModel = viewModel()
            TemplateManagerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Updates.route) {
            val viewModel: UpdateViewModel = viewModel()
            UpdateScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
