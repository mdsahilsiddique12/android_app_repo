package com.example.data.model

data class User(
    val username: String,
    val labId: String,
    val role: String,
    val token: String,
    val refreshToken: String
)

data class Patient(
    val id: String = "",
    val labNumber: String,
    val name: String,
    val age: Int,
    val ageUnit: String = "Years", // Years, Months, Days
    val gender: String, // Male, Female, Other
    val doctor: String,
    val phone: String,
    val email: String = "",
    val collectionDate: String,
    val collectionTime: String,
    val paymentMode: String = "Cash", // Cash, Card, UPI, Credit
    val amountPaid: Double = 0.0,
    val totalAmount: Double = 0.0,
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class TestCategory(
    val id: String,
    val name: String,
    val description: String = ""
)

data class TestParameter(
    val id: String,
    val code: String,
    val name: String,
    val unit: String,
    val minNormal: Double?,
    val maxNormal: Double?,
    val textNormalRange: String = "",
    val defaultValue: String = "",
    val order: Int = 0,
    val placeholderCode: String = "" // e.g. "HB", "WBC" — used for {{HB}}, {{WBC}} placeholders
)

data class TestMaster(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val price: Double,
    val sampleType: String = "",
    val parameters: List<TestParameter>,
    val formula: String? = null,
    val isActive: Boolean = true
)

data class ResultValue(
    val parameterId: String,
    val parameterName: String,
    val value: String,
    val unit: String,
    val normalRange: String,
    val statusFlag: String = "NORMAL", // NORMAL, HIGH, LOW, CRITICAL
    val placeholderCode: String = "" // For dynamic placeholder mapping
)

data class TestResultGroup(
    val testId: String,
    val testName: String,
    val results: List<ResultValue>
)

enum class ReportStatus {
    DRAFT, GENERATED, SYNCED, PENDING_SYNC, ERROR
}

data class Report(
    val id: String,
    val reportNumber: String,
    val patient: Patient,
    val selectedTests: List<TestMaster>,
    val testResults: List<TestResultGroup>,
    val pdfFilePath: String? = null,
    val docxFilePath: String? = null,
    val colorDocxFilePath: String? = null,
    val colorPdfFilePath: String? = null,
    val status: ReportStatus = ReportStatus.DRAFT,
    val generatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val syncError: String? = null
)

data class ReportTemplate(
    val id: String,
    val code: String,
    val name: String,
    val version: Int,
    val checksum: String,
    val driveFileId: String,
    val localDocxPath: String,
    val isDownloaded: Boolean = false,
    val coloredDriveFileId: String = "",
    val coloredLocalDocxPath: String = "",
    val isColoredDownloaded: Boolean = false,
    val templateType: String = "DOCX",
    val lastUpdated: Long = System.currentTimeMillis()
)

data class SyncQueueItem(
    val id: String,
    val reportId: String,
    val action: String, // "UPLOAD_REPORT", "SYNC_PATIENT"
    val payloadJson: String,
    val attemptCount: Int = 0,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, FAILED, COMPLETED
    val createdAt: Long = System.currentTimeMillis()
)

data class ServerSettings(
    val serverUrl: String = "",
    val labId: String = "",
    val driveSyncEnabled: Boolean = true,
    val offlineMode: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val autoPrintOnGenerate: Boolean = false,
    val themeMode: String = "SYSTEM"
)

data class AppUpdateInfo(
    val currentVersion: String = "",
    val latestVersion: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val checksum: String = "",
    val isMandatory: Boolean = false,
    val updateAvailable: Boolean = false
)
