package com.example.data.repository

import com.example.data.local.ReportDao
import com.example.data.local.ReportEntity
import com.example.data.local.SyncItemEntity
import com.example.data.local.SyncQueueDao
import com.example.data.model.Patient
import com.example.data.model.Report
import com.example.data.model.ReportStatus
import com.example.data.model.TestMaster
import com.example.data.model.TestResultGroup
import com.example.engine.report.NativeReportEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ReportRepository(
    private val reportDao: ReportDao,
    private val syncQueueDao: SyncQueueDao,
    private val reportEngine: NativeReportEngine,
    private val templateManager: TemplateManager? = null
) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val testListType = Types.newParameterizedType(List::class.java, TestMaster::class.java)
    private val testAdapter = moshi.adapter<List<TestMaster>>(testListType)

    private val resultGroupListType = Types.newParameterizedType(List::class.java, TestResultGroup::class.java)
    private val resultAdapter = moshi.adapter<List<TestResultGroup>>(resultGroupListType)

    private val patientAdapter = moshi.adapter(Patient::class.java)

    val allReports: Flow<List<Report>> = reportDao.getAllReports().map { list ->
        list.map { it.toDomain() }
    }

    val reportCount: Flow<Int> = reportDao.getReportCount()

    fun searchReports(query: String): Flow<List<Report>> {
        return reportDao.searchReports(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getReportById(id: String): Report? {
        return reportDao.getReportById(id)?.toDomain()
    }

    /**
     * Full report generation workflow:
     * 1. Build placeholder map from patient + test results
     * 2. Get template paths (standard + colored) from TemplateManager
     * 3. Generate standard DOCX (placeholder replacement)
     * 4. Generate colored DOCX (placeholder replacement)
     * 5. Generate colored PDF via Canvas engine
     * 6. Save all file paths to Room
     * 7. Queue for backend sync
     */
    suspend fun createAndGenerateReport(
        patient: Patient,
        selectedTests: List<TestMaster>,
        testResults: List<TestResultGroup>
    ): Report {
        val reportId = "REP-" + UUID.randomUUID().toString().take(8).uppercase()
        val reportNumber = "PLP/${patient.labNumber}/${System.currentTimeMillis().toString().takeLast(4)}"

        val initialReport = Report(
            id = reportId,
            reportNumber = reportNumber,
            patient = patient,
            selectedTests = selectedTests,
            testResults = testResults,
            status = ReportStatus.DRAFT,
            generatedAt = System.currentTimeMillis()
        )

        // Generate standard DOCX
        val docxNormalFile = reportEngine.generateDocxReport(initialReport, isColored = false)

        // Generate colored DOCX
        val docxColorFile = reportEngine.generateDocxReport(initialReport, isColored = true)

        // Generate standard PDF
        val pdfNormalFile = reportEngine.generatePdfReport(initialReport, isColored = false)

        // Generate colored PDF
        val pdfColorFile = reportEngine.generatePdfReport(initialReport, isColored = true)

        val finalReport = initialReport.copy(
            docxFilePath = docxNormalFile.absolutePath,
            colorDocxFilePath = docxColorFile.absolutePath,
            pdfFilePath = pdfColorFile.absolutePath,
            colorPdfFilePath = pdfColorFile.absolutePath,
            status = ReportStatus.GENERATED
        )

        // Save to Room DB
        reportDao.insertReport(finalReport.toEntity())

        // Queue for background REST sync with FastAPI backend
        val syncPayload = moshi.adapter(Any::class.java).toJson(
            mapOf(
                "reportId" to reportId,
                "reportNumber" to reportNumber,
                "patientName" to patient.name,
                "doctor" to patient.doctor,
                "labNumber" to patient.labNumber,
                "testIds" to selectedTests.map { it.id }
            )
        )
        syncQueueDao.enqueueItem(
            SyncItemEntity(
                id = UUID.randomUUID().toString(),
                reportId = reportId,
                action = "UPLOAD_REPORT",
                payloadJson = syncPayload,
                attemptCount = 0,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
        )

        return finalReport
    }

    private fun ReportEntity.toDomain(): Report {
        val tests = try { testAdapter.fromJson(selectedTestIdsJson) ?: emptyList() } catch (e: Exception) { emptyList() }
        val results = try { resultAdapter.fromJson(testResultsJson) ?: emptyList() } catch (e: Exception) { emptyList() }

        // Reconstruct patient from JSON if available, otherwise from fields
        val patient = if (patientJson.isNotBlank()) {
            try {
                patientAdapter.fromJson(patientJson) ?: buildPatientFromFields()
            } catch (e: Exception) {
                buildPatientFromFields()
            }
        } else {
            buildPatientFromFields()
        }

        return Report(
            id = id,
            reportNumber = reportNumber,
            patient = patient,
            selectedTests = tests,
            testResults = results,
            pdfFilePath = pdfFilePath,
            docxFilePath = docxFilePath,
            colorDocxFilePath = colorDocxFilePath,
            colorPdfFilePath = colorPdfFilePath,
            status = try { ReportStatus.valueOf(status) } catch (e: Exception) { ReportStatus.GENERATED },
            generatedAt = generatedAt,
            syncedAt = syncedAt,
            syncError = syncError
        )
    }

    private fun ReportEntity.buildPatientFromFields(): Patient {
        return Patient(
            id = patientId,
            labNumber = reportNumber.split("/").getOrNull(1) ?: "",
            name = patientName,
            age = 0,
            ageUnit = "Years",
            gender = "",
            doctor = doctor,
            phone = "",
            email = "",
            collectionDate = "",
            collectionTime = ""
        )
    }

    private fun Report.toEntity() = ReportEntity(
        id = id,
        reportNumber = reportNumber,
        patientId = patient.id,
        patientName = patient.name,
        doctor = patient.doctor,
        patientJson = try { patientAdapter.toJson(patient) } catch (e: Exception) { "" },
        selectedTestIdsJson = testAdapter.toJson(selectedTests),
        testResultsJson = resultAdapter.toJson(testResults),
        pdfFilePath = pdfFilePath,
        docxFilePath = docxFilePath,
        colorDocxFilePath = colorDocxFilePath,
        colorPdfFilePath = colorPdfFilePath,
        status = status.name,
        generatedAt = generatedAt,
        syncedAt = syncedAt,
        syncError = syncError
    )
}
