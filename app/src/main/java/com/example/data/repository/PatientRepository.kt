package com.example.data.repository

import com.example.data.local.PatientDao
import com.example.data.local.PatientEntity
import com.example.data.model.Patient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PatientRepository(private val patientDao: PatientDao) {

    val allPatients: Flow<List<Patient>> = patientDao.getAllPatients().map { entities ->
        entities.map { it.toDomain() }
    }

    fun searchPatients(query: String): Flow<List<Patient>> {
        return patientDao.searchPatients(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getPatientById(id: String): Patient? {
        return patientDao.getPatientById(id)?.toDomain()
    }

    suspend fun savePatient(patient: Patient): Patient {
        val patientId = if (patient.id.isBlank()) UUID.randomUUID().toString() else patient.id
        val entity = patient.copy(id = patientId).toEntity()
        patientDao.insertPatient(entity)
        return patient.copy(id = patientId)
    }

    private fun PatientEntity.toDomain() = Patient(
        id = id,
        labNumber = labNumber,
        name = name,
        age = age,
        ageUnit = ageUnit,
        gender = gender,
        doctor = doctor,
        phone = phone,
        email = email,
        collectionDate = collectionDate,
        collectionTime = collectionTime,
        paymentMode = paymentMode,
        amountPaid = amountPaid,
        totalAmount = totalAmount,
        remarks = remarks,
        createdAt = createdAt
    )

    private fun Patient.toEntity() = PatientEntity(
        id = id,
        labNumber = labNumber,
        name = name,
        age = age,
        ageUnit = ageUnit,
        gender = gender,
        doctor = doctor,
        phone = phone,
        email = email,
        collectionDate = collectionDate,
        collectionTime = collectionTime,
        paymentMode = paymentMode,
        amountPaid = amountPaid,
        totalAmount = totalAmount,
        remarks = remarks,
        createdAt = createdAt
    )
}
