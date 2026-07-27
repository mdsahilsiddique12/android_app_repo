package com.example.ui.screens.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TestMaster
import com.example.ui.components.PdfCanvasViewer
import com.example.ui.components.StepProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportWizardScreen(
    viewModel: ReportWizardViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val steps = listOf("Patient", "Tests", "Results", "Generate", "Preview")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Report Generation Wizard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Desktop Identical Workflow", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStep > 0) viewModel.prevStep() else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Step Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                StepProgressBar(
                    currentStep = uiState.currentStep,
                    steps = steps,
                    onStepClick = viewModel::goToStep
                )
            }

            // Body Area based on Current Step
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.currentStep) {
                    0 -> StepPatientDetails(uiState, viewModel)
                    1 -> StepTestSelection(uiState, viewModel)
                    2 -> StepDynamicResultEntry(uiState, viewModel)
                    3 -> StepGeneratingReport(uiState)
                    4 -> {
                        uiState.generatedReport?.let { report ->
                            PdfCanvasViewer(
                                report = report,
                                onBack = { viewModel.goToStep(2) }
                            )
                        }
                    }
                }
            }

            // Navigation Bottom Action Bar (Steps 0, 1, 2)
            if (uiState.currentStep < 3) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.currentStep > 0) {
                            OutlinedButton(
                                onClick = viewModel::prevStep,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Button(
                            onClick = viewModel::nextStep,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (uiState.currentStep == 2) "Generate DOCX & PDF" else "Next Step")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepPatientDetails(
    uiState: ReportWizardUiState,
    viewModel: ReportWizardViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Patient Registration & Demographics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Enter patient details as registered in laboratory reception", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

        // Lab Number Display
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Assigned Lab Number:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(uiState.labNumber, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        OutlinedTextField(
            value = uiState.patientName,
            onValueChange = { viewModel.onPatientFieldChanged(name = it) },
            label = { Text("Patient Full Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.age,
                onValueChange = { viewModel.onPatientFieldChanged(age = it) },
                label = { Text("Age *") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = uiState.ageUnit,
                onValueChange = { viewModel.onPatientFieldChanged(ageUnit = it) },
                label = { Text("Unit") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = uiState.gender,
                onValueChange = { viewModel.onPatientFieldChanged(gender = it) },
                label = { Text("Gender *") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.doctor,
            onValueChange = { viewModel.onPatientFieldChanged(doctor = it) },
            label = { Text("Referred Doctor *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { viewModel.onPatientFieldChanged(phone = it) },
                label = { Text("Phone Number") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onPatientFieldChanged(email = it) },
                label = { Text("Email Address") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.paymentMode,
                onValueChange = { viewModel.onPatientFieldChanged(paymentMode = it) },
                label = { Text("Payment Mode") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = uiState.amountPaid,
                onValueChange = { viewModel.onPatientFieldChanged(amountPaid = it) },
                label = { Text("Amount Paid ($)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.remarks,
            onValueChange = { viewModel.onPatientFieldChanged(remarks = it) },
            label = { Text("Clinical Notes / Remarks") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
    }
}

@Composable
private fun StepTestSelection(
    uiState: ReportWizardUiState,
    viewModel: ReportWizardViewModel
) {
    val categories = uiState.availableCategories

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Select Laboratory Tests & Packages", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Selected tests will automatically populate result parameters in Step 3", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text("Search CBC, LFT, KFT, Thyroid, Glucose...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Categories Filter Bar
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = uiState.selectedCategory == cat,
                    onClick = { viewModel.onCategorySelected(cat) },
                    label = { Text(cat, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filtered Test List
        val filtered = uiState.availableTests.filter { test ->
            val matchQuery = test.name.contains(uiState.searchQuery, ignoreCase = true) ||
                    test.category.contains(uiState.searchQuery, ignoreCase = true)
            val matchCategory = uiState.selectedCategory == "All" || test.category == uiState.selectedCategory
            matchQuery && matchCategory
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filtered) { test ->
                val isSelected = test.id in uiState.selectedTestIds
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleTestSelection(test.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleTestSelection(test.id) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(test.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${test.category} • ${test.parameters.size} Parameters", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("$${test.price.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDynamicResultEntry(
    uiState: ReportWizardUiState,
    viewModel: ReportWizardViewModel
) {
    val selectedTests = uiState.availableTests.filter { it.id in uiState.selectedTestIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Dynamic Result Value Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Parameter fields are generated dynamically based on selected desktop test masters", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

        if (selectedTests.isEmpty()) {
            Text("No tests selected. Please return to Step 2.", color = Color.Red, fontSize = 13.sp)
        }

        selectedTests.forEach { test ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(test.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(test.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    test.parameters.forEach { param ->
                        val currentVal = uiState.resultValues[test.id]?.get(param.id) ?: param.defaultValue

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(param.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Ref: ${param.textNormalRange} ${param.unit}", fontSize = 10.sp, color = Color.Gray)
                            }

                            OutlinedTextField(
                                value = currentVal,
                                onValueChange = { newValue ->
                                    viewModel.onResultValueChanged(test.id, param.id, newValue)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepGeneratingReport(uiState: ReportWizardUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp), strokeWidth = 4.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Native Engine Rendering Report...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Building DOCX file & rendering A4 Coloured PDF Canvas", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))

            uiState.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(err, color = Color.Red, fontSize = 12.sp)
            }
        }
    }
}
