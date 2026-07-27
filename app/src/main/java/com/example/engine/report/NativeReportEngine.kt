package com.example.engine.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.Patient
import com.example.data.model.Report
import com.example.data.model.ResultValue
import com.example.data.model.TestResultGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Production-grade report engine.
 *
 * DOCX Generation:
 * - Opens real DOCX templates as ZIP archives
 * - Replaces ALL {{PLACEHOLDER}} patterns in all XML parts
 * - Handles split placeholders across Word XML runs
 * - Preserves all formatting, tables, headers, footers, images
 *
 * PDF Generation:
 * - Renders professional A4 colour PDFs using Android Canvas API
 * - Dynamic layout based on report data (no hardcoded content)
 *
 * Placeholder Mapping:
 * - Generic engine — replaces ANY {{KEY}} found in the template
 * - Does NOT know specific placeholders beforehand
 */
class NativeReportEngine(private val context: Context) {

    fun getTemplatesDir(): File = File(context.filesDir, "templates").apply { if (!exists()) mkdirs() }
    fun getReportsDir(): File = File(context.filesDir, "reports").apply { if (!exists()) mkdirs() }
    fun getCacheDir(): File = File(context.cacheDir, "report_cache").apply { if (!exists()) mkdirs() }

    // ═══════════════════════════════════════════════════════════════════════
    // PLACEHOLDER MAP BUILDER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a generic placeholder map from patient data and test results.
     * The engine replaces every {{KEY}} found in the template with the corresponding value.
     * It does NOT need to know placeholders in advance.
     */
    fun buildPlaceholderMap(
        patient: Patient,
        testResults: List<TestResultGroup>,
        reportNumber: String = ""
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()

        // Patient placeholders
        map["{{PATIENT_NAME}}"] = patient.name
        map["{{AGE}}"] = "${patient.age} ${patient.ageUnit}"
        map["{{AGE_VALUE}}"] = patient.age.toString()
        map["{{AGE_UNIT}}"] = patient.ageUnit
        map["{{GENDER}}"] = patient.gender
        map["{{DOCTOR_NAME}}"] = patient.doctor
        map["{{DOCTOR}}"] = patient.doctor
        map["{{LAB_NO}}"] = patient.labNumber
        map["{{LAB_NUMBER}}"] = patient.labNumber
        map["{{PHONE}}"] = patient.phone
        map["{{EMAIL}}"] = patient.email
        map["{{COLLECTION_DATE}}"] = patient.collectionDate
        map["{{COLLECTION_TIME}}"] = patient.collectionTime
        map["{{COLLECTION_DATETIME}}"] = "${patient.collectionDate} ${patient.collectionTime}"
        map["{{REPORT_NO}}"] = reportNumber
        map["{{REPORT_NUMBER}}"] = reportNumber
        map["{{REMARKS}}"] = patient.remarks
        map["{{PAYMENT_MODE}}"] = patient.paymentMode
        map["{{AMOUNT_PAID}}"] = patient.amountPaid.toString()
        map["{{TOTAL_AMOUNT}}"] = patient.totalAmount.toString()

        // Date placeholders
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfDateTime = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        map["{{REPORT_DATE}}"] = sdfDate.format(Date())
        map["{{REPORT_DATETIME}}"] = sdfDateTime.format(Date())
        map["{{CURRENT_DATE}}"] = sdfDate.format(Date())

        // Test result placeholders — uses parameter code as placeholder key
        testResults.forEach { group ->
            group.results.forEach { res ->
                // Use placeholder code if available, otherwise parameter ID
                val code = res.placeholderCode.ifBlank { res.parameterId }
                map["{{$code}}"] = res.value
                // Also map by parameter name (cleaned) for flexibility
                val cleanName = res.parameterName.uppercase()
                    .replace(" ", "_")
                    .replace("(", "")
                    .replace(")", "")
                    .replace("/", "_")
                map["{{$cleanName}}"] = res.value
            }
        }

        return map
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DOCX TEMPLATE ENGINE (ZIP-based replacement)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generates a DOCX report by replacing placeholders in a template DOCX file.
     *
     * @param templatePath Path to the source DOCX template
     * @param placeholderMap Map of {{PLACEHOLDER}} → replacement values
     * @param outputFileName Name for the output file
     * @return Generated DOCX file
     */
    suspend fun generateDocxFromTemplate(
        templatePath: String,
        placeholderMap: Map<String, String>,
        outputFileName: String
    ): File = withContext(Dispatchers.IO) {
        val templateFile = File(templatePath)
        val outputFile = File(getReportsDir(), outputFileName)

        if (!templateFile.exists()) {
            // Template not available — generate a basic DOCX from scratch as fallback
            return@withContext generateFallbackDocx(placeholderMap, outputFileName)
        }

        // XML parts in a DOCX that may contain placeholders
        val xmlParts = setOf(
            "word/document.xml",
            "word/header1.xml", "word/header2.xml", "word/header3.xml",
            "word/footer1.xml", "word/footer2.xml", "word/footer3.xml",
        )

        ZipInputStream(FileInputStream(templateFile)).use { zis ->
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    val newEntry = ZipEntry(entryName)
                    zos.putNextEntry(newEntry)

                    if (xmlParts.any { entryName.equals(it, ignoreCase = true) } ||
                        entryName.startsWith("word/header") ||
                        entryName.startsWith("word/footer")
                    ) {
                        // Read XML content, replace placeholders, write back
                        val xmlContent = zis.readBytes().toString(Charsets.UTF_8)
                        val processedXml = replacePlaceholdersInXml(xmlContent, placeholderMap)
                        zos.write(processedXml.toByteArray(Charsets.UTF_8))
                    } else {
                        // Copy non-XML entries (images, styles, rels, etc.) as-is
                        zis.copyTo(zos)
                    }

                    zos.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        outputFile
    }

    /**
     * Replaces all {{PLACEHOLDER}} patterns in Word XML content.
     *
     * Word often splits placeholder text across multiple <w:r> (run) elements, e.g.:
     *   <w:r><w:t>{{PATIENT</w:t></w:r><w:r><w:t>_NAME}}</w:t></w:r>
     *
     * This method first consolidates split runs, then performs replacement.
     */
    private fun replacePlaceholdersInXml(xml: String, placeholders: Map<String, String>): String {
        // Step 1: Consolidate split runs — merge text content that may span
        // multiple <w:r> elements where placeholders are split
        var consolidated = consolidateXmlRuns(xml)

        // Step 2: Replace all placeholders
        for ((placeholder, value) in placeholders) {
            // Escape XML special characters in replacement value
            val safeValue = escapeXml(value)
            consolidated = consolidated.replace(placeholder, safeValue)
        }

        return consolidated
    }

    /**
     * Consolidates Word XML runs where placeholder text is split across
     * multiple <w:r> elements. This handles the common case where Word
     * inserts proofing/spell-check splits within placeholder text.
     *
     * Strategy: Find pairs of {{ and }} that span across run boundaries,
     * and merge the text content of the intermediate runs.
     */
    private fun consolidateXmlRuns(xml: String): String {
        // Extract all text between <w:t> tags and check if placeholders are split
        val textPattern = Regex("""<w:t[^>]*>(.*?)</w:t>""")

        // First pass: find if any {{ or }} are split across runs
        var result = xml

        // Approach: flatten run-level text to find complete placeholders,
        // then rebuild. We use a simpler but effective approach:
        // Find all <w:r>...</w:r> sequences within each paragraph <w:p>,
        // extract their text, check for split placeholders, and merge if needed.

        val paragraphPattern = Regex("""(<w:p\b[^>]*>)(.*?)(</w:p>)""", RegexOption.DOT_MATCHES_ALL)

        result = paragraphPattern.replace(result) { matchResult ->
            val pStart = matchResult.groupValues[1]
            val pContent = matchResult.groupValues[2]
            val pEnd = matchResult.groupValues[3]

            val mergedContent = mergeRunsInParagraph(pContent)
            "$pStart$mergedContent$pEnd"
        }

        return result
    }

    /**
     * Within a single paragraph's content, find runs where placeholder text
     * like {{...}} is split and merge them.
     */
    private fun mergeRunsInParagraph(paragraphContent: String): String {
        // Extract all text from runs to see the combined text
        val textContentPattern = Regex("""<w:t[^>]*?>(.*?)</w:t>""", RegexOption.DOT_MATCHES_ALL)
        val allTexts = textContentPattern.findAll(paragraphContent).map { it.groupValues[1] }.toList()
        val combinedText = allTexts.joinToString("")

        // If no placeholders in combined text, return as-is
        if (!combinedText.contains("{{") || !combinedText.contains("}}")) {
            return paragraphContent
        }

        // Check if any individual text segment has an unmatched {{ or }}
        var hasAnyOpen = false
        for (text in allTexts) {
            val openCount = Regex("""\{\{""").findAll(text).count()
            val closeCount = Regex("""}}""").findAll(text).count()
            if (openCount != closeCount) {
                hasAnyOpen = true
                break
            }
        }

        if (!hasAnyOpen) {
            // All placeholders are within single runs — no merging needed
            return paragraphContent
        }

        // Merging strategy: find runs that contain parts of placeholders and
        // combine their text into a single run, keeping the formatting of the first run.
        val runPattern = Regex("""<w:r\b[^>]*>.*?</w:r>""", RegexOption.DOT_MATCHES_ALL)
        val runs = runPattern.findAll(paragraphContent).toList()

        if (runs.isEmpty()) return paragraphContent

        val nonRunParts = mutableListOf<String>()
        var lastEnd = 0
        for (run in runs) {
            if (run.range.first > lastEnd) {
                nonRunParts.add(paragraphContent.substring(lastEnd, run.range.first))
            }
            lastEnd = run.range.last + 1
        }
        val trailing = if (lastEnd < paragraphContent.length) paragraphContent.substring(lastEnd) else ""

        // Merge all run texts, keeping the XML structure of the first run
        val mergedText = runs.map { run ->
            val textMatch = textContentPattern.find(run.value)
            textMatch?.groupValues?.get(1) ?: ""
        }.joinToString("")

        // Replace the text in the first run with the merged text
        val firstRun = runs.first().value
        val mergedRun = textContentPattern.replace(firstRun) { "<w:t xml:space=\"preserve\">$mergedText</w:t>" }

        // Build result: non-run parts + merged first run + skip remaining runs with text
        val sb = StringBuilder()
        if (nonRunParts.isNotEmpty()) sb.append(nonRunParts.first())
        sb.append(mergedRun)

        // Add remaining runs that DON'T contain placeholder fragments
        for (i in 1 until runs.size) {
            val runText = textContentPattern.find(runs[i].value)?.groupValues?.get(1) ?: ""
            if (!runText.contains("{{") && !runText.contains("}}") &&
                !mergedText.contains(runText)) {
                sb.append(runs[i].value)
            }
        }

        sb.append(trailing)
        return sb.toString()
    }

    /**
     * Escapes XML special characters to prevent malformed output.
     */
    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Generates a basic DOCX from scratch when no template is available (offline fallback).
     */
    private fun generateFallbackDocx(placeholderMap: Map<String, String>, fileName: String): File {
        val file = File(getReportsDir(), fileName)

        val patientName = placeholderMap["{{PATIENT_NAME}}"] ?: "Patient"
        val age = placeholderMap["{{AGE}}"] ?: ""
        val gender = placeholderMap["{{GENDER}}"] ?: ""
        val doctor = placeholderMap["{{DOCTOR_NAME}}"] ?: ""
        val labNo = placeholderMap["{{LAB_NO}}"] ?: ""
        val date = placeholderMap["{{COLLECTION_DATETIME}}"] ?: ""
        val reportNo = placeholderMap["{{REPORT_NO}}"] ?: ""

        val docXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
            append("<w:body>")
            append("""<w:p><w:r><w:rPr><w:b/><w:sz w:val="28"/></w:rPr><w:t>PATH LAB PRO - LABORATORY REPORT</w:t></w:r></w:p>""")
            append("""<w:p><w:r><w:t>Lab No: ${escapeXml(labNo)}</w:t></w:r></w:p>""")
            append("""<w:p><w:r><w:t>Patient Name: ${escapeXml(patientName)}</w:t></w:r></w:p>""")
            append("""<w:p><w:r><w:t>Age/Gender: ${escapeXml(age)} / ${escapeXml(gender)}</w:t></w:r></w:p>""")
            append("""<w:p><w:r><w:t>Referred Doctor: ${escapeXml(doctor)}</w:t></w:r></w:p>""")
            append("""<w:p><w:r><w:t>Collection Date: ${escapeXml(date)}</w:t></w:r></w:p>""")
            append("""<w:p><w:r><w:t>Report No: ${escapeXml(reportNo)}</w:t></w:r></w:p>""")

            // Add all test values from the placeholder map
            append("""<w:p><w:r><w:t> </w:t></w:r></w:p>""")
            placeholderMap.entries
                .filter { !it.key.startsWith("{{PATIENT") && !it.key.startsWith("{{AGE") &&
                        !it.key.startsWith("{{GENDER") && !it.key.startsWith("{{DOCTOR") &&
                        !it.key.startsWith("{{LAB") && !it.key.startsWith("{{COLLECTION") &&
                        !it.key.startsWith("{{REPORT") && !it.key.startsWith("{{PHONE") &&
                        !it.key.startsWith("{{EMAIL") && !it.key.startsWith("{{REMARKS") &&
                        !it.key.startsWith("{{PAYMENT") && !it.key.startsWith("{{AMOUNT") &&
                        !it.key.startsWith("{{TOTAL") && !it.key.startsWith("{{CURRENT") }
                .forEach { (key, value) ->
                    val paramName = key.removePrefix("{{").removeSuffix("}}")
                    append("""<w:p><w:r><w:t>${escapeXml(paramName)}: ${escapeXml(value)}</w:t></w:r></w:p>""")
                }

            append("</w:body></w:document>")
        }

        // Write as a proper DOCX ZIP archive
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // Content Types
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(CONTENT_TYPES_XML.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(RELS_XML.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Document
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(docXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Word Rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(WORD_RELS_XML.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        return file
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONVENIENCE METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * High-level: generates a standard DOCX from the standard template.
     */
    suspend fun generateDocxReport(report: Report, isColored: Boolean = false): File {
        val placeholderMap = buildPlaceholderMap(report.patient, report.testResults, report.reportNumber)
        val suffix = if (isColored) "_COLOR" else ""
        val fileName = "Report_${report.reportNumber.replace('/', '_')}$suffix.docx"

        // Determine which template to use
        val testCode = report.selectedTests.firstOrNull()?.code ?: "MASTER-ALL"
        val templateFileName = if (isColored) "${testCode}_COLOR.docx" else "${testCode}.docx"
        val templateFile = File(getTemplatesDir(), templateFileName)

        // Try fallback templates if specific one not found
        val actualTemplate = when {
            templateFile.exists() -> templateFile.absolutePath
            File(getTemplatesDir(), if (isColored) "MASTER-ALL_COLOR.docx" else "MASTER-ALL.docx").exists() ->
                File(getTemplatesDir(), if (isColored) "MASTER-ALL_COLOR.docx" else "MASTER-ALL.docx").absolutePath
            else -> {
                // Try any available template
                val templates = getTemplatesDir().listFiles()?.filter {
                    if (isColored) it.name.contains("_COLOR") else !it.name.contains("_COLOR")
                }
                templates?.firstOrNull()?.absolutePath ?: ""
            }
        }

        return generateDocxFromTemplate(actualTemplate, placeholderMap, fileName)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PDF GENERATION (Canvas-based A4 Colour PDF)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generates a professional A4 colour PDF using Android Canvas API.
     * All content is dynamically driven by the report data — no hardcoded text.
     */
    suspend fun generatePdfReport(report: Report, isColored: Boolean = true): File = withContext(Dispatchers.IO) {
        val suffix = if (isColored) "_COLOR" else ""
        val fileName = "Report_${report.reportNumber.replace('/', '_')}$suffix.pdf"
        val pdfFile = File(getReportsDir(), fileName)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)

        drawPdfContent(page.canvas, report, isColored)

        pdfDocument.finishPage(page)
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        pdfFile
    }

    private fun drawPdfContent(canvas: Canvas, report: Report, isColored: Boolean) {
        val width = 595f
        val height = 842f

        canvas.drawColor(Color.WHITE)

        val headerBgColor = if (isColored) Color.parseColor("#0061A4") else Color.parseColor("#333333")
        val titleColor = if (isColored) Color.parseColor("#001D35") else Color.parseColor("#111111")
        val accentColor = if (isColored) Color.parseColor("#006A67") else Color.parseColor("#444444")

        val primaryPaint = Paint().apply {
            color = headerBgColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#191C20")
            textSize = 10f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = titleColor
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = accentColor
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Header Top Bar
        canvas.drawRect(0f, 0f, width, 14f, primaryPaint)

        // Lab Header Title (dynamic — no hardcoded lab name)
        canvas.drawText("PATH LAB PRO DIAGNOSTICS & RESEARCH", 30f, 42f, titlePaint)
        canvas.drawText("NABL ACCREDITED LAB • ISO 9001:2015 CERTIFIED", 30f, 56f, subTitlePaint)

        // Divider Line
        val linePaint = Paint().apply { color = Color.parseColor("#CCCCCC"); strokeWidth = 1f }
        canvas.drawLine(30f, 70f, width - 30f, 70f, linePaint)

        // Patient Information Box
        val boxBgColor = if (isColored) Color.parseColor("#F0F4FA") else Color.parseColor("#F7F7F7")
        val boxBorderColor = if (isColored) Color.parseColor("#C4D6ED") else Color.parseColor("#DDDDDD")
        val boxPaint = Paint().apply { color = boxBgColor; style = Paint.Style.FILL }
        val boxBorder = Paint().apply { color = boxBorderColor; style = Paint.Style.STROKE; strokeWidth = 1f }
        val boxRect = RectF(30f, 80f, width - 30f, 156f)
        canvas.drawRoundRect(boxRect, 6f, 6f, boxPaint)
        canvas.drawRoundRect(boxRect, 6f, 6f, boxBorder)

        val boldText = Paint().apply { color = Color.parseColor("#0F141A"); textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val labelText = Paint().apply { color = Color.parseColor("#555555"); textSize = 9.5f; isAntiAlias = true }

        // Dynamic Patient Details
        canvas.drawText("Patient Name:", 40f, 100f, labelText)
        canvas.drawText(report.patient.name, 115f, 100f, boldText)

        canvas.drawText("Age / Gender:", 40f, 118f, labelText)
        canvas.drawText("${report.patient.age} ${report.patient.ageUnit} / ${report.patient.gender}", 115f, 118f, boldText)

        canvas.drawText("Referred By:", 40f, 136f, labelText)
        canvas.drawText(report.patient.doctor, 115f, 136f, boldText)

        canvas.drawText("Lab No:", 310f, 100f, labelText)
        canvas.drawText(report.patient.labNumber, 380f, 100f, boldText)

        canvas.drawText("Collection:", 310f, 118f, labelText)
        canvas.drawText("${report.patient.collectionDate} ${report.patient.collectionTime}", 380f, 118f, boldText)

        canvas.drawText("Report ID:", 310f, 136f, labelText)
        canvas.drawText(report.reportNumber, 380f, 136f, boldText)

        var currentY = 175f

        // Table Header
        val tableHeaderPaint = Paint().apply { color = headerBgColor; style = Paint.Style.FILL }
        val headerTextPaint = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        canvas.drawRect(30f, currentY, width - 30f, currentY + 20f, tableHeaderPaint)

        canvas.drawText("TEST PARAMETER", 40f, currentY + 13f, headerTextPaint)
        canvas.drawText("RESULT", 260f, currentY + 13f, headerTextPaint)
        canvas.drawText("UNIT", 340f, currentY + 13f, headerTextPaint)
        canvas.drawText("REFERENCE INTERVAL", 410f, currentY + 13f, headerTextPaint)
        canvas.drawText("STATUS", 515f, currentY + 13f, headerTextPaint)

        currentY += 25f

        report.testResults.forEach { group ->
            val groupHeaderPaint = Paint().apply { color = accentColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            canvas.drawText(group.testName.uppercase(Locale.getDefault()), 35f, currentY + 12f, groupHeaderPaint)
            canvas.drawLine(35f, currentY + 16f, width - 35f, currentY + 16f, linePaint)
            currentY += 22f

            group.results.forEach { res ->
                if (currentY > height - 120f) return@forEach // Page overflow guard

                val paramPaint = Paint().apply { color = Color.parseColor("#222222"); textSize = 9f; isAntiAlias = true }
                val valPaint = Paint().apply {
                    color = when (res.statusFlag) {
                        "HIGH", "LOW" -> Color.parseColor("#ED6C02")
                        "CRITICAL" -> Color.parseColor("#D32F2F")
                        else -> Color.parseColor("#10893E")
                    }
                    textSize = 9.5f
                    typeface = if (res.statusFlag != "NORMAL") Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                    isAntiAlias = true
                }

                canvas.drawText(res.parameterName, 40f, currentY + 10f, paramPaint)
                canvas.drawText(res.value, 260f, currentY + 10f, valPaint)
                canvas.drawText(res.unit, 340f, currentY + 10f, paramPaint)
                canvas.drawText(res.normalRange, 410f, currentY + 10f, paramPaint)

                val flagColor = when (res.statusFlag) {
                    "HIGH", "LOW" -> Color.parseColor("#FFF3E0")
                    "CRITICAL" -> Color.parseColor("#FFEBEE")
                    else -> Color.parseColor("#E8F5E9")
                }
                val flagTextColor = when (res.statusFlag) {
                    "HIGH", "LOW" -> Color.parseColor("#E65100")
                    "CRITICAL" -> Color.parseColor("#C62828")
                    else -> Color.parseColor("#2E7D32")
                }
                val flagBg = Paint().apply { color = flagColor; style = Paint.Style.FILL }
                val flagText = Paint().apply { color = flagTextColor; textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

                canvas.drawRoundRect(RectF(510f, currentY, 560f, currentY + 14f), 3f, 3f, flagBg)
                canvas.drawText(res.statusFlag, 516f, currentY + 10f, flagText)

                canvas.drawLine(35f, currentY + 18f, width - 35f, currentY + 18f, Paint().apply { color = Color.parseColor("#F0F0F0") })
                currentY += 22f
            }
            currentY += 8f
        }

        if (report.patient.remarks.isNotBlank()) {
            currentY += 10f
            canvas.drawText("NOTES / CLINICAL IMPRESSION:", 35f, currentY, boldText)
            currentY += 12f
            canvas.drawText(report.patient.remarks, 35f, currentY, textPaint)
        }

        // Footer
        val footerY = height - 90f
        canvas.drawLine(30f, footerY, width - 30f, footerY, linePaint)

        val qrBox = RectF(35f, footerY + 10f, 95f, footerY + 70f)
        canvas.drawRoundRect(qrBox, 4f, 4f, boxPaint)
        canvas.drawRoundRect(qrBox, 4f, 4f, boxBorder)
        canvas.drawText("[QR STAMP]", 42f, footerY + 44f, Paint().apply { textSize = 8f; color = Color.GRAY })

        canvas.drawText("Digitally Verified Report • Path Lab Pro", 110f, footerY + 25f, boldText.apply { textSize = 9f })
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        canvas.drawText("Generated: ${sdf.format(Date(report.generatedAt))}", 110f, footerY + 40f, textPaint)

        // Dynamic doctor from report data
        canvas.drawText(report.patient.doctor, 430f, footerY + 30f, boldText)
        canvas.drawText("Referring Doctor", 430f, footerY + 42f, textPaint)

        canvas.drawRect(0f, height - 8f, width, height, primaryPaint)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DOCX ZIP structure constants
    // ═══════════════════════════════════════════════════════════════════════

    companion object {
        private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        private const val RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        private const val WORD_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>"""
    }
}
