package com.example.data.repository

import android.content.Context
import com.example.data.local.TemplateDao
import com.example.data.local.TemplateEntity
import com.example.data.model.ReportTemplate
import com.example.data.remote.ApiClient
import com.example.data.remote.TemplateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Manages report template lifecycle: fetching manifest from backend,
 * version checking, downloading standard + colored DOCX templates,
 * and caching locally for offline use.
 *
 * Templates flow: Android → Backend → Google Drive → Backend → Android
 * Android never accesses Google Drive directly.
 */
class TemplateManager(
    private val context: Context,
    private val templateDao: TemplateDao
) {

    private val templatesDir: File
        get() = File(context.filesDir, "templates").apply { if (!exists()) mkdirs() }

    fun getTemplatesDir(): File = templatesDir

    fun getTemplatesFlow(): Flow<List<ReportTemplate>> {
        return templateDao.getAllTemplates().map { list ->
            list.map { entity ->
                ReportTemplate(
                    id = entity.id,
                    code = entity.code,
                    name = entity.name,
                    version = entity.version,
                    checksum = entity.checksum,
                    driveFileId = entity.driveFileId,
                    localDocxPath = entity.localDocxPath,
                    isDownloaded = entity.isDownloaded,
                    coloredDriveFileId = entity.coloredDriveFileId,
                    coloredLocalDocxPath = entity.coloredLocalDocxPath,
                    isColoredDownloaded = entity.isColoredDownloaded,
                    templateType = entity.templateType,
                    lastUpdated = entity.lastUpdated
                )
            }
        }
    }

    /**
     * Synchronizes template manifest from backend.
     * Downloads templates that are missing or have a newer version.
     * Returns number of templates updated.
     */
    suspend fun syncTemplatesFromBackend(): Int {
        var updatedCount = 0
        try {
            val response = ApiClient.apiService.listTemplates(offset = 0, limit = 100)
            if (response.isSuccessful && response.body() != null) {
                val remoteTemplates = response.body()!!

                for (remote in remoteTemplates) {
                    val local = templateDao.getTemplateById(remote.id)

                    val needsStandardDownload = local == null
                            || !local.isDownloaded
                            || local.version < remote.version
                            || !File(local.localDocxPath).exists()

                    val needsColoredDownload = local == null
                            || !local.isColoredDownloaded
                            || local.version < remote.version
                            || local.coloredLocalDocxPath.isBlank()
                            || !File(local.coloredLocalDocxPath).exists()

                    var standardPath = local?.localDocxPath ?: ""
                    var coloredPath = local?.coloredLocalDocxPath ?: ""
                    var standardDownloaded = local?.isDownloaded ?: false
                    var coloredDownloaded = local?.isColoredDownloaded ?: false

                    // Download standard template if needed
                    if (needsStandardDownload) {
                        val file = downloadTemplateFile(remote.id, "standard", "${remote.code}.docx")
                        if (file != null) {
                            standardPath = file.absolutePath
                            standardDownloaded = true
                        }
                    }

                    // Download colored template if needed
                    if (needsColoredDownload) {
                        val file = downloadTemplateFile(remote.id, "colored", "${remote.code}_COLOR.docx")
                        if (file != null) {
                            coloredPath = file.absolutePath
                            coloredDownloaded = true
                        }
                    }

                    val checksum = if (standardPath.isNotBlank() && File(standardPath).exists()) {
                        calculateFileMD5(File(standardPath))
                    } else {
                        remote.checksum
                    }

                    // Upsert template record
                    templateDao.insertTemplate(
                        TemplateEntity(
                            id = remote.id,
                            code = remote.code,
                            name = remote.name,
                            version = remote.version,
                            checksum = checksum,
                            driveFileId = remote.driveFileId,
                            localDocxPath = standardPath,
                            isDownloaded = standardDownloaded,
                            coloredDriveFileId = remote.coloredDriveFileId,
                            coloredLocalDocxPath = coloredPath,
                            isColoredDownloaded = coloredDownloaded,
                            templateType = remote.templateType,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )

                    if (needsStandardDownload || needsColoredDownload) {
                        updatedCount++
                    }
                }
            }
        } catch (e: Exception) {
            // Offline — keep existing cached templates
        }
        return updatedCount
    }

    /**
     * Checks if a specific template needs updating by comparing version with backend.
     * Returns true if the local version is outdated or missing.
     */
    suspend fun isTemplateOutdated(templateId: String): Boolean {
        return try {
            val local = templateDao.getTemplateById(templateId) ?: return true
            val response = ApiClient.apiService.listTemplates()
            if (response.isSuccessful && response.body() != null) {
                val remote = response.body()!!.find { it.id == templateId }
                if (remote != null) {
                    remote.version > local.version || !local.isDownloaded
                } else {
                    false
                }
            } else {
                false // Can't check — assume cached is fine
            }
        } catch (e: Exception) {
            false // Offline — use cached
        }
    }

    /**
     * Gets cached template paths for a test code.
     * Returns Pair(standardDocxPath, coloredDocxPath), or null if not cached.
     */
    suspend fun getTemplatePathsForTest(testCode: String): Pair<String, String>? {
        // Try exact code match first
        var template = templateDao.getTemplateByCode(testCode)

        // Try generic/fallback template
        if (template == null) {
            template = templateDao.getTemplateByCode("MASTER-ALL")
        }

        // Try first available template
        if (template == null) {
            val downloaded = templateDao.getDownloadedTemplates()
            template = downloaded.firstOrNull()
        }

        if (template != null && template.isDownloaded) {
            val standardPath = template.localDocxPath
            val coloredPath = template.coloredLocalDocxPath.ifBlank { standardPath }
            return Pair(standardPath, coloredPath)
        }

        return null
    }

    /**
     * Downloads a template file from the backend.
     * @param templateId Backend template UUID
     * @param type "standard" or "colored"
     * @param fileName Local file name to save as
     * @return Downloaded file, or null on failure
     */
    private suspend fun downloadTemplateFile(templateId: String, type: String, fileName: String): File? {
        return try {
            withContext(Dispatchers.IO) {
                val response = ApiClient.apiService.downloadTemplate(templateId, type)
                if (response.isSuccessful && response.body() != null) {
                    val file = File(templatesDir, fileName)
                    response.body()!!.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    file
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Initializes templates on first launch — syncs manifest from backend.
     */
    suspend fun initializeLocalTemplates() {
        syncTemplatesFromBackend()
    }

    private fun calculateFileMD5(file: File): String {
        return try {
            if (!file.exists()) return ""
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
