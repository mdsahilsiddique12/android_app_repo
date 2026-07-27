package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.remote.AuthTokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "path_lab_pro_prefs")

class UserPreferencesManager(private val context: Context) : AuthTokenProvider {

    companion object {
        val KEY_LAB_ID = stringPreferencesKey("lab_id")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_SERVER_URL = stringPreferencesKey("server_url")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_AUTO_PRINT = booleanPreferencesKey("auto_print")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val KEY_USER_ID = stringPreferencesKey("user_id")
    }

    val labIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAB_ID] ?: ""
    }

    val usernameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USERNAME] ?: ""
    }

    val authTokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN]
    }

    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_REFRESH_TOKEN]
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    val isBiometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: true
    }

    val serverUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: "https://android-backend-kang.onrender.com"
    }

    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: false
    }

    val lastSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIMESTAMP] ?: 0L
    }

    // ── AuthTokenProvider implementation (for OkHttp interceptor thread) ─────

    override fun getAccessToken(): String? {
        return runBlocking {
            context.dataStore.data.first()[KEY_AUTH_TOKEN]
        }
    }

    override fun getRefreshToken(): String? {
        return runBlocking {
            context.dataStore.data.first()[KEY_REFRESH_TOKEN]
        }
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[KEY_AUTH_TOKEN] = accessToken
                if (refreshToken.isNotBlank()) {
                    prefs[KEY_REFRESH_TOKEN] = refreshToken
                }
            }
        }
    }

    // ── Async save methods ──────────────────────────────────────────────────

    suspend fun saveAuth(labId: String, username: String, token: String, refreshToken: String, userId: String = "") {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAB_ID] = labId
            prefs[KEY_USERNAME] = username
            prefs[KEY_AUTH_TOKEN] = token
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_IS_LOGGED_IN] = true
            if (userId.isNotBlank()) {
                prefs[KEY_USER_ID] = userId
            }
        }
    }

    suspend fun saveTokensAsync(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = accessToken
            if (refreshToken.isNotBlank()) {
                prefs[KEY_REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            prefs[KEY_AUTH_TOKEN] = ""
            prefs[KEY_REFRESH_TOKEN] = ""
        }
    }

    suspend fun updateSettings(serverUrl: String, labId: String, biometricEnabled: Boolean, darkMode: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_LAB_ID] = labId
            prefs[KEY_BIOMETRIC_ENABLED] = biometricEnabled
            prefs[KEY_DARK_MODE] = darkMode
        }
    }
}
