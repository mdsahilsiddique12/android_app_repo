package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object ReportWizard : Screen("report_wizard")
    object RecentReports : Screen("recent_reports")
    object PatientSearch : Screen("patient_search")
    object TemplateManager : Screen("template_manager")
    object Settings : Screen("settings")
    object Updates : Screen("updates")
}
