package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// ─── Authentication DTOs ───────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username_or_email") val usernameOrEmail: String,
    @Json(name = "password") val password: String,
    @Json(name = "device_id") val deviceId: String = "android_client",
    @Json(name = "device_name") val deviceName: String = "Path Lab Pro Android",
    @Json(name = "platform") val platform: String = "android"
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "device_id") val deviceId: String = "android_client"
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String = "",
    @Json(name = "refresh_token") val refreshToken: String = "",
    @Json(name = "token_type") val tokenType: String = "bearer",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "username") val username: String = "",
    @Json(name = "role") val role: String = "",
    @Json(name = "lab_id") val labId: String = ""
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "id") val id: String = "",
    @Json(name = "username") val username: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "role") val role: String = "",
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "lab_id") val labId: String = ""
)

// ─── Lab Tests Catalog DTOs ────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class LabTestParameterDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "code") val code: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "unit") val unit: String = "",
    @Json(name = "min_normal") val minNormal: Double? = null,
    @Json(name = "max_normal") val maxNormal: Double? = null,
    @Json(name = "text_normal_range") val textNormalRange: String = "",
    @Json(name = "default_value") val defaultValue: String = "",
    @Json(name = "order") val order: Int = 0,
    @Json(name = "placeholder_code") val placeholderCode: String = ""
)

@JsonClass(generateAdapter = true)
data class LabTestResponse(
    @Json(name = "id") val id: String,
    @Json(name = "code") val code: String,
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String,
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "sample_type") val sampleType: String = "",
    @Json(name = "unit") val unit: String? = null,
    @Json(name = "normal_ranges") val normalRanges: Map<String, Any>? = null,
    @Json(name = "parameters") val parameters: List<Map<String, Any?>>? = null,
    @Json(name = "formula") val formula: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

// ─── Report Templates DTOs ─────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class TemplateResponse(
    @Json(name = "id") val id: String,
    @Json(name = "code") val code: String = "",
    @Json(name = "name") val name: String,
    @Json(name = "version") val version: Int = 1,
    @Json(name = "checksum") val checksum: String = "",
    @Json(name = "drive_file_id") val driveFileId: String = "",
    @Json(name = "colored_drive_file_id") val coloredDriveFileId: String = "",
    @Json(name = "template_type") val templateType: String = "DOCX",
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

// ─── Reports DTOs ──────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ReportCreateRequest(
    @Json(name = "patient_id") val patientId: String,
    @Json(name = "test_id") val testId: String,
    @Json(name = "doctor_id") val doctorId: String? = null,
    @Json(name = "test_values") val testValues: Map<String, Any> = emptyMap(),
    @Json(name = "impression") val impression: String? = null,
    @Json(name = "remarks") val remarks: String? = null,
    @Json(name = "status") val status: String = "DRAFT"
)

@JsonClass(generateAdapter = true)
data class ReportUpdateRequest(
    @Json(name = "doctor_id") val doctorId: String? = null,
    @Json(name = "test_values") val testValues: Map<String, Any>? = null,
    @Json(name = "impression") val impression: String? = null,
    @Json(name = "remarks") val remarks: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class ReportSignOffRequest(
    @Json(name = "remarks") val remarks: String? = null,
    @Json(name = "upload_to_drive") val uploadToDrive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ReportResponse(
    @Json(name = "id") val id: String,
    @Json(name = "report_number") val reportNumber: String = "",
    @Json(name = "patient_id") val patientId: String = "",
    @Json(name = "test_id") val testId: String = "",
    @Json(name = "doctor_id") val doctorId: String? = null,
    @Json(name = "status") val status: String = "DRAFT",
    @Json(name = "test_values") val testValues: Map<String, Any> = emptyMap(),
    @Json(name = "impression") val impression: String? = null,
    @Json(name = "remarks") val remarks: String? = null,
    @Json(name = "drive_pdf_id") val drivePdfId: String? = null,
    @Json(name = "drive_pdf_url") val drivePdfUrl: String? = null,
    @Json(name = "printed_count") val printedCount: Int = 0,
    @Json(name = "signed_by_id") val signedById: String? = null,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

// ─── Sync DTOs ─────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class SyncMutationItem(
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: String,
    @Json(name = "action") val action: String,
    @Json(name = "payload") val payload: Map<String, Any?> = emptyMap(),
    @Json(name = "client_timestamp") val clientTimestamp: Long = System.currentTimeMillis() / 1000
)

@JsonClass(generateAdapter = true)
data class SyncPushRequest(
    @Json(name = "device_id") val deviceId: String = "android_client",
    @Json(name = "mutations") val mutations: List<SyncMutationItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SyncPushResponse(
    @Json(name = "accepted") val accepted: Int = 0,
    @Json(name = "rejected") val rejected: Int = 0,
    @Json(name = "errors") val errors: List<String> = emptyList(),
    @Json(name = "server_timestamp") val serverTimestamp: Long = 0
)

@JsonClass(generateAdapter = true)
data class SyncPullResponse(
    @Json(name = "patients") val patients: List<Map<String, Any?>> = emptyList(),
    @Json(name = "doctors") val doctors: List<Map<String, Any?>> = emptyList(),
    @Json(name = "tests") val tests: List<Map<String, Any?>> = emptyList(),
    @Json(name = "reports") val reports: List<Map<String, Any?>> = emptyList(),
    @Json(name = "server_timestamp") val serverTimestamp: Long = 0
)

// ─── Dashboard DTOs ────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class DashboardSummaryResponse(
    @Json(name = "total_patients") val totalPatients: Int = 0,
    @Json(name = "today_patients") val todayPatients: Int = 0,
    @Json(name = "total_reports") val totalReports: Int = 0,
    @Json(name = "pending_reports") val pendingReports: Int = 0,
    @Json(name = "verified_reports") val verifiedReports: Int = 0,
    @Json(name = "total_doctors") val totalDoctors: Int = 0,
    @Json(name = "today_revenue") val todayRevenue: Double = 0.0,
    @Json(name = "monthly_revenue") val monthlyRevenue: Double = 0.0,
    @Json(name = "drive_sync_status") val driveSyncStatus: String = "",
    @Json(name = "drive_storage_used_mb") val driveStorageUsedMb: Double = 0.0
)

// ─── App Updates DTOs ──────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AppUpdateCheckRequest(
    @Json(name = "platform") val platform: String = "android",
    @Json(name = "current_version_code") val currentVersionCode: Int
)

@JsonClass(generateAdapter = true)
data class AppUpdateResponse(
    @Json(name = "id") val id: String = "",
    @Json(name = "target_platform") val targetPlatform: String = "",
    @Json(name = "version_name") val versionName: String = "",
    @Json(name = "version_code") val versionCode: Int = 0,
    @Json(name = "drive_file_id") val driveFileId: String = "",
    @Json(name = "download_url") val downloadUrl: String = "",
    @Json(name = "sha256_checksum") val sha256Checksum: String = "",
    @Json(name = "release_notes") val releaseNotes: String? = null,
    @Json(name = "is_mandatory") val isMandatory: Boolean = false,
    @Json(name = "update_available") val updateAvailable: Boolean = false,
    @Json(name = "created_at") val createdAt: String = ""
)

// ─── Patients DTOs ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class PatientCreateRequest(
    @Json(name = "name") val name: String,
    @Json(name = "age") val age: Int,
    @Json(name = "gender") val gender: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "blood_group") val bloodGroup: String? = null,
    @Json(name = "medical_history") val medicalHistory: String? = null,
    @Json(name = "uhid") val uhid: String? = null
)

@JsonClass(generateAdapter = true)
data class PatientApiResponse(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "age") val age: Int,
    @Json(name = "gender") val gender: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "blood_group") val bloodGroup: String? = null,
    @Json(name = "medical_history") val medicalHistory: String? = null,
    @Json(name = "uhid") val uhid: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

// ═══════════════════════════════════════════════════════════════════════════════
// Retrofit Service Interface
// ═══════════════════════════════════════════════════════════════════════════════

interface PathLabApiService {

    // ── Authentication ──────────────────────────────────────────────────────
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<UserResponse>

    // ── Lab Tests Catalog ───────────────────────────────────────────────────
    @GET("api/v1/tests")
    suspend fun listTests(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 200
    ): Response<List<LabTestResponse>>

    @GET("api/v1/tests/search")
    suspend fun searchTests(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50
    ): Response<List<LabTestResponse>>

    @GET("api/v1/tests/{test_id}")
    suspend fun getTest(@Path("test_id") testId: String): Response<LabTestResponse>

    // ── Report Templates ────────────────────────────────────────────────────
    @GET("api/v1/templates")
    suspend fun listTemplates(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<List<TemplateResponse>>

    @POST("api/v1/templates/sync/{template_id}")
    suspend fun syncTemplate(@Path("template_id") templateId: String): Response<TemplateResponse>

    @Streaming
    @GET("api/v1/templates/{template_id}/download")
    suspend fun downloadTemplate(
        @Path("template_id") templateId: String,
        @Query("type") type: String = "standard"
    ): Response<ResponseBody>

    // ── Reports ─────────────────────────────────────────────────────────────
    @POST("api/v1/reports")
    suspend fun createReport(@Body request: ReportCreateRequest): Response<ReportResponse>

    @GET("api/v1/reports")
    suspend fun listReports(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<List<ReportResponse>>

    @GET("api/v1/reports/{report_id}")
    suspend fun getReport(@Path("report_id") reportId: String): Response<ReportResponse>

    @PUT("api/v1/reports/{report_id}")
    suspend fun updateReport(
        @Path("report_id") reportId: String,
        @Body request: ReportUpdateRequest
    ): Response<ReportResponse>

    @POST("api/v1/reports/{report_id}/sign-off")
    suspend fun signOffReport(
        @Path("report_id") reportId: String,
        @Body request: ReportSignOffRequest
    ): Response<ReportResponse>

    // ── Patients ────────────────────────────────────────────────────────────
    @POST("api/v1/patients")
    suspend fun createPatient(@Body request: PatientCreateRequest): Response<PatientApiResponse>

    @GET("api/v1/patients/search")
    suspend fun searchPatients(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<List<PatientApiResponse>>

    // ── Sync ────────────────────────────────────────────────────────────────
    @POST("api/v1/sync/push")
    suspend fun syncPush(@Body request: SyncPushRequest): Response<SyncPushResponse>

    @GET("api/v1/sync/pull")
    suspend fun syncPull(@Query("since") since: Long = 0): Response<SyncPullResponse>

    // ── Dashboard ───────────────────────────────────────────────────────────
    @GET("api/v1/dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>

    // ── App Updates ─────────────────────────────────────────────────────────
    @POST("api/v1/updates/check")
    suspend fun checkAppUpdate(@Body request: AppUpdateCheckRequest): Response<AppUpdateResponse>
}
