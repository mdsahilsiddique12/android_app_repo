package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val labNumber: String,
    val name: String,
    val age: Int,
    val ageUnit: String,
    val gender: String,
    val doctor: String,
    val phone: String,
    val email: String,
    val collectionDate: String,
    val collectionTime: String,
    val paymentMode: String,
    val amountPaid: Double,
    val totalAmount: Double,
    val remarks: String,
    val createdAt: Long
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reportNumber: String,
    val patientId: String,
    val patientName: String,
    val doctor: String,
    val patientJson: String = "", // Full patient data as JSON for offline reconstruction
    val selectedTestIdsJson: String,
    val testResultsJson: String,
    val pdfFilePath: String?,
    val docxFilePath: String?,
    val colorDocxFilePath: String? = null,
    val colorPdfFilePath: String? = null,
    val status: String,
    val generatedAt: Long,
    val syncedAt: Long?,
    val syncError: String?
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val version: Int,
    val checksum: String,
    val driveFileId: String,
    val localDocxPath: String,
    val isDownloaded: Boolean,
    val coloredDriveFileId: String = "",
    val coloredLocalDocxPath: String = "",
    val isColoredDownloaded: Boolean = false,
    val templateType: String = "DOCX",
    val lastUpdated: Long
)

@Entity(tableName = "sync_queue")
data class SyncItemEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val action: String,
    val payloadJson: String,
    val attemptCount: Int,
    val status: String,
    val createdAt: Long
)

@Entity(tableName = "test_masters")
data class TestMasterEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val category: String,
    val price: Double,
    val sampleType: String = "",
    val parametersJson: String,
    val normalRangesJson: String = "{}",
    val formula: String? = null,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
