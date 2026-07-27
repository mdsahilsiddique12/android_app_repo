package com.example.ui.screens.wizard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Patient
import com.example.data.model.Report
import com.example.data.model.ResultValue
import com.example.data.model.TestMaster
import com.example.data.model.TestResultGroup
import com.example.data.repository.PatientRepository
import com.example.data.repository.ReportRepository
import com.example.data.repository.TemplateManager
import com.example.data.repository.TestMasterRepository
import com.example.engine.report.NativeReportEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportWizardUiState(
    val currentStep: Int = 0, // 0: Patient Details, 1: Test Selection, 2: Result Entry, 3: Generation, 4: Preview
    // Patient Form — starts empty (no hardcoded data)
    val patientName: String = "",
    val age: String = "",
    val ageUnit: String = "Years",
    val gender: String = "",
    val doctor: String = "",
    val phone: String = "",
    val email: String = "",
    val collectionDate: String = "",
    val collectionTime: String = "",
    val paymentMode: String = "Cash",
    val amountPaid: String = "",
    val totalAmount: String = "",
    val labNumber: String = "",
    val remarks: String = "",
    // Test Selection — loaded dynamically from backend
    val availableTests: List<TestMaster> = emptyList(),
    val selectedTestIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val availableCategories: List<String> = listOf("All"),
    // Dynamic Results Entry: Map<TestId, Map<ParamId, Value>>
    val resultValues: Map<String, Map<String, String>> = emptyMap(),
    // Generated Report
    val generatedReport: Report? = null,
    val isGenerating: Boolean = false,
    val isLoadingTests: Boolean = false,
    val errorMessage: String? = null
)

class ReportWizardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val reportEngine = NativeReportEngine(application)
    private val templateManager = TemplateManager(application, database.templateDao())
    private val patientRepo = PatientRepository(database.patientDao())
    private val reportRepo = ReportRepository(
        database.reportDao(), database.syncQueueDao(), reportEngine, templateManager
    )
    private val testMasterRepo = TestMasterRepository(database.testMasterDao())

    private val _uiState = MutableStateFlow(ReportWizardUiState())
    val uiState: StateFlow<ReportWizardUiState> = _uiState.asStateFlow()

    init {
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val now = Date()

        _uiState.value = _uiState.value.copy(
            collectionDate = sdfDate.format(now),
            collectionTime = sdfTime.format(now),
            labNumber = "LAB-${System.currentTimeMillis().toString().takeLast(6)}"
        )

        // Load tests from backend (or cache)
        loadTests()
    }

    private fun loadTests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTests = true)

            // Try refreshing from backend
            testMasterRepo.refreshTestsFromBackend()

            // Load categories dynamically
            val categories = testMasterRepo.getCategories()
            val categoryNames = listOf("All") + categories.map { it.name }
            _uiState.value = _uiState.value.copy(availableCategories = categoryNames)

            // Observe tests from Room cache
            testMasterRepo.getTestsFlow().collect { tests ->
                _uiState.value = _uiState.value.copy(
                    availableTests = tests,
                    isLoadingTests = false
                )
            }
        }
    }

    private fun initDefaultResultValues(allTests: List<TestMaster>, selectedIds: Set<String>) {
        val map = mutableMapOf<String, MutableMap<String, String>>()
        allTests.filter { it.id in selectedIds }.forEach { test ->
            val paramMap = mutableMapOf<String, String>()
            test.parameters.forEach { param ->
                paramMap[param.id] = param.defaultValue
            }
            map[test.id] = paramMap
        }
        _uiState.value = _uiState.value.copy(resultValues = map)
    }

    fun onPatientFieldChanged(
        name: String? = null,
        age: String? = null,
        ageUnit: String? = null,
        gender: String? = null,
        doctor: String? = null,
        phone: String? = null,
        email: String? = null,
        paymentMode: String? = null,
        amountPaid: String? = null,
        remarks: String? = null
    ) {
        _uiState.value = _uiState.value.copy(
            patientName = name ?: _uiState.value.patientName,
            age = age ?: _uiState.value.age,
            ageUnit = ageUnit ?: _uiState.value.ageUnit,
            gender = gender ?: _uiState.value.gender,
            doctor = doctor ?: _uiState.value.doctor,
            phone = phone ?: _uiState.value.phone,
            email = email ?: _uiState.value.email,
            paymentMode = paymentMode ?: _uiState.value.paymentMode,
            amountPaid = amountPaid ?: _uiState.value.amountPaid,
            remarks = remarks ?: _uiState.value.remarks
        )
    }

    fun toggleTestSelection(testId: String) {
        val current = _uiState.value.selectedTestIds.toMutableSet()
        if (current.contains(testId)) {
            current.remove(testId)
        } else {
            current.add(testId)
        }
        _uiState.value = _uiState.value.copy(selectedTestIds = current)
        initDefaultResultValues(_uiState.value.availableTests, current)
    }

    fun onResultValueChanged(testId: String, paramId: String, newValue: String) {
        val currentMap = _uiState.value.resultValues.toMutableMap()
        val paramMap = (currentMap[testId] ?: emptyMap()).toMutableMap()
        paramMap[paramId] = newValue
        currentMap[testId] = paramMap
        _uiState.value = _uiState.value.copy(resultValues = currentMap)
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
    }

    fun onCategorySelected(cat: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = cat)
    }

    fun nextStep() {
        val next = (_uiState.value.currentStep + 1).coerceAtMost(4)
        _uiState.value = _uiState.value.copy(currentStep = next)
        if (next == 3) {
            generateReport()
        }
    }

    fun prevStep() {
        val prev = (_uiState.value.currentStep - 1).coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(currentStep = prev)
    }

    fun goToStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    fun generateReport() {
        _uiState.value = _uiState.value.copy(isGenerating = true, errorMessage = null)

        viewModelScope.launch {
            try {
                // Ensure templates are up to date
                templateManager.syncTemplatesFromBackend()

                val state = _uiState.value
                val patient = Patient(
                    labNumber = state.labNumber,
                    name = state.patientName,
                    age = state.age.toIntOrNull() ?: 0,
                    ageUnit = state.ageUnit,
                    gender = state.gender,
                    doctor = state.doctor,
                    phone = state.phone,
                    email = state.email,
                    collectionDate = state.collectionDate,
                    collectionTime = state.collectionTime,
                    paymentMode = state.paymentMode,
                    amountPaid = state.amountPaid.toDoubleOrNull() ?: 0.0,
                    totalAmount = state.totalAmount.toDoubleOrNull() ?: 0.0,
                    remarks = state.remarks
                )

                val savedPatient = patientRepo.savePatient(patient)

                val selectedTests = state.availableTests.filter { it.id in state.selectedTestIds }
                val testResultGroups = selectedTests.map { test ->
                    val paramValues = state.resultValues[test.id] ?: emptyMap()
                    val resultValuesList = test.parameters.map { param ->
                        val enterVal = paramValues[param.id] ?: param.defaultValue
                        val numVal = enterVal.replace(",", "").toDoubleOrNull()

                        val flag = when {
                            numVal != null && param.maxNormal != null && numVal > param.maxNormal -> "HIGH"
                            numVal != null && param.minNormal != null && numVal < param.minNormal -> "LOW"
                            else -> "NORMAL"
                        }

                        ResultValue(
                            parameterId = param.id,
                            parameterName = param.name,
                            value = enterVal,
                            unit = param.unit,
                            normalRange = param.textNormalRange,
                            statusFlag = flag,
                            placeholderCode = param.placeholderCode.ifBlank { param.code }
                        )
                    }
                    TestResultGroup(
                        testId = test.id,
                        testName = test.name,
                        results = resultValuesList
                    )
                }

                val report = reportRepo.createAndGenerateReport(savedPatient, selectedTests, testResultGroups)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generatedReport = report,
                    currentStep = 4
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = "Failed to generate report: ${e.message}"
                )
            }
        }
    }
}
