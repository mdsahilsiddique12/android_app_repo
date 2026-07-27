package com.example.data.repository

import com.example.data.local.TestMasterDao
import com.example.data.local.TestMasterEntity
import com.example.data.model.TestCategory
import com.example.data.model.TestMaster
import com.example.data.model.TestParameter
import com.example.data.remote.ApiClient
import com.example.data.remote.LabTestResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for lab test definitions. All data comes from the backend API
 * and is cached locally in Room for offline use. No hardcoded tests.
 */
class TestMasterRepository(private val testMasterDao: TestMasterDao) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val paramListType = Types.newParameterizedType(List::class.java, TestParameter::class.java)
    private val paramAdapter = moshi.adapter<List<TestParameter>>(paramListType)

    /**
     * Returns a Flow of all tests from the local Room cache.
     * If the cache is empty, the UI layer should trigger [refreshTestsFromBackend].
     */
    fun getTestsFlow(): Flow<List<TestMaster>> {
        return testMasterDao.getAllTests().map { entities ->
            entities.map { entity ->
                val params = try {
                    paramAdapter.fromJson(entity.parametersJson) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                TestMaster(
                    id = entity.id,
                    code = entity.code,
                    name = entity.name,
                    category = entity.category,
                    price = entity.price,
                    sampleType = entity.sampleType,
                    parameters = params,
                    formula = entity.formula,
                    isActive = entity.isActive
                )
            }
        }
    }

    /**
     * Returns distinct categories derived dynamically from cached tests.
     */
    suspend fun getCategories(): List<TestCategory> {
        val categories = testMasterDao.getAllCategories()
        return categories.map { categoryName ->
            TestCategory(
                id = "CAT-${categoryName.uppercase().replace(" ", "_").take(10)}",
                name = categoryName
            )
        }
    }

    /**
     * Checks whether the local test cache has any entries.
     */
    suspend fun hasLocalCache(): Boolean {
        return testMasterDao.getTestCount() > 0
    }

    /**
     * Fetches the complete lab test catalog from the backend API and caches locally.
     * Returns the number of tests fetched, or -1 on failure (offline, etc.).
     */
    suspend fun refreshTestsFromBackend(): Int {
        return try {
            val response = ApiClient.apiService.listTests(offset = 0, limit = 500)
            if (response.isSuccessful && response.body() != null) {
                val remoteTests = response.body()!!
                val entities = remoteTests.map { it.toEntity() }
                // Replace all cached tests with fresh data
                testMasterDao.deleteAllTests()
                testMasterDao.insertTests(entities)
                entities.size
            } else {
                -1
            }
        } catch (e: Exception) {
            // Network error — offline mode, keep existing cache
            -1
        }
    }

    /**
     * Maps a backend [LabTestResponse] to a Room [TestMasterEntity].
     * Extracts parameters from the generic JSON list.
     */
    private fun LabTestResponse.toEntity(): TestMasterEntity {
        val testParams = extractParameters(this)
        return TestMasterEntity(
            id = id,
            code = code,
            name = name,
            category = category,
            price = price,
            sampleType = sampleType,
            parametersJson = paramAdapter.toJson(testParams),
            normalRangesJson = "{}",
            formula = formula,
            isActive = isActive,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Extracts typed [TestParameter] list from the backend's generic parameter objects.
     * Backend returns parameters as List<Map<String, Any?>> — we extract known fields.
     */
    private fun extractParameters(test: LabTestResponse): List<TestParameter> {
        val rawParams = test.parameters ?: return emptyList()
        return rawParams.mapIndexed { index, paramMap ->
            TestParameter(
                id = (paramMap["id"] as? String) ?: "p${index + 1}",
                code = (paramMap["code"] as? String) ?: (paramMap["placeholder_code"] as? String) ?: "",
                name = (paramMap["name"] as? String) ?: "Parameter ${index + 1}",
                unit = (paramMap["unit"] as? String) ?: "",
                minNormal = (paramMap["min_normal"] as? Number)?.toDouble(),
                maxNormal = (paramMap["max_normal"] as? Number)?.toDouble(),
                textNormalRange = (paramMap["text_normal_range"] as? String)
                    ?: buildTextRange(
                        (paramMap["min_normal"] as? Number)?.toDouble(),
                        (paramMap["max_normal"] as? Number)?.toDouble()
                    ),
                defaultValue = (paramMap["default_value"] as? String) ?: "",
                order = (paramMap["order"] as? Number)?.toInt() ?: index,
                placeholderCode = (paramMap["placeholder_code"] as? String)
                    ?: (paramMap["code"] as? String) ?: ""
            )
        }.sortedBy { it.order }
    }

    private fun buildTextRange(min: Double?, max: Double?): String {
        return when {
            min != null && max != null -> "$min - $max"
            max != null -> "< $max"
            min != null -> "> $min"
            else -> ""
        }
    }
}
