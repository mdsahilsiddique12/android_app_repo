package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR labNumber LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchPatients(query: String): Flow<List<PatientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Update
    suspend fun updatePatient(patient: PatientEntity)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY generatedAt DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: String): ReportEntity?

    @Query("SELECT * FROM reports WHERE patientName LIKE '%' || :query || '%' OR reportNumber LIKE '%' || :query || '%' ORDER BY generatedAt DESC")
    fun searchReports(query: String): Flow<List<ReportEntity>>

    @Query("SELECT COUNT(*) FROM reports")
    fun getReportCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status, syncedAt = :syncedAt, syncError = :syncError WHERE id = :id")
    suspend fun updateReportSyncStatus(id: String, status: String, syncedAt: Long?, syncError: String?)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE isDownloaded = 1")
    suspend fun getDownloadedTemplates(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE code = :code LIMIT 1")
    suspend fun getTemplateByCode(code: String): TemplateEntity?

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: String): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity)

    @Query("UPDATE templates SET isDownloaded = :isDownloaded, localDocxPath = :localPath, version = :version, checksum = :checksum, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun updateTemplateDownloadState(id: String, isDownloaded: Boolean, localPath: String, version: Int, checksum: String, lastUpdated: Long)

    @Query("UPDATE templates SET isColoredDownloaded = :isDownloaded, coloredLocalDocxPath = :localPath, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun updateColoredTemplateState(id: String, isDownloaded: Boolean, localPath: String, lastUpdated: Long)

    @Query("DELETE FROM templates")
    suspend fun clearAll()
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingQueue(): Flow<List<SyncItemEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingQueueSnapshot(): List<SyncItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueItem(item: SyncItemEntity)

    @Query("UPDATE sync_queue SET status = :status, attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun updateItemStatus(id: String, status: String)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}

@Dao
interface TestMasterDao {
    @Query("SELECT * FROM test_masters WHERE isActive = 1 ORDER BY category ASC, name ASC")
    fun getAllTests(): Flow<List<TestMasterEntity>>

    @Query("SELECT * FROM test_masters WHERE id = :id LIMIT 1")
    suspend fun getTestById(id: String): TestMasterEntity?

    @Query("SELECT * FROM test_masters WHERE code = :code LIMIT 1")
    suspend fun getTestByCode(code: String): TestMasterEntity?

    @Query("SELECT DISTINCT category FROM test_masters WHERE isActive = 1 ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT COUNT(*) FROM test_masters")
    suspend fun getTestCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTests(tests: List<TestMasterEntity>)

    @Query("DELETE FROM test_masters")
    suspend fun deleteAllTests()
}
