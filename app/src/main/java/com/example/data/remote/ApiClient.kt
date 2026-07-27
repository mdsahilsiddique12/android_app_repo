package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Provides access to stored auth tokens for network interceptors.
 * Implemented by [UserPreferencesManager] or similar.
 */
interface AuthTokenProvider {
    /** Returns the current JWT access token, or null if not logged in. */
    fun getAccessToken(): String?
    /** Returns the current refresh token, or null. */
    fun getRefreshToken(): String?
    /** Persists new tokens after a successful refresh. */
    fun saveTokens(accessToken: String, refreshToken: String)
}

object ApiClient {

    private const val DEFAULT_BASE_URL = "https://android-backend-kang.onrender.com/"
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
    private const val MAX_RETRIES = 3

    private var currentBaseUrl = DEFAULT_BASE_URL
    private var tokenProvider: AuthTokenProvider? = null

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // ── Logging (debug builds only) ─────────────────────────────────────────
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // ── Auth Interceptor: injects Bearer token into every request ───────────
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        val token = tokenProvider?.getAccessToken()
        if (!token.isNullOrBlank()) {
            val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
            builder.header("Authorization", bearer)
        }

        builder.header("Accept", "application/json")
        chain.proceed(builder.build())
    }

    // ── Retry Interceptor: retries on transient failures ────────────────────
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null

        for (attempt in 0 until MAX_RETRIES) {
            try {
                response?.close()
                response = chain.proceed(request)
                // Don't retry on client errors (4xx) or success
                if (response.isSuccessful || (response.code in 400..499)) {
                    return@Interceptor response
                }
                // Retry on server errors (5xx)
                if (attempt < MAX_RETRIES - 1) {
                    response.close()
                    Thread.sleep(1000L * (attempt + 1)) // Exponential-ish backoff
                }
            } catch (e: IOException) {
                exception = e
                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(1000L * (attempt + 1))
                }
            }
        }

        response ?: throw (exception ?: IOException("Request failed after $MAX_RETRIES retries"))
    }

    // ── Token Refresh Authenticator: auto-refreshes on 401 ──────────────────
    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // Avoid infinite refresh loops
            if (response.request.header("X-Token-Refreshed") != null) {
                return null
            }

            val refreshToken = tokenProvider?.getRefreshToken() ?: return null

            // Build a synchronous refresh call
            val refreshRequest = RefreshTokenRequest(
                refreshToken = refreshToken,
                deviceId = "android_client"
            )

            val refreshBody = moshi.adapter(RefreshTokenRequest::class.java)
                .toJson(refreshRequest)

            val refreshHttpRequest = Request.Builder()
                .url("${currentBaseUrl}api/v1/auth/refresh")
                .post(
                    okhttp3.RequestBody.create(
                        okhttp3.MediaType.parse("application/json; charset=utf-8"),
                        refreshBody
                    )
                )
                .build()

            return try {
                val refreshClient = OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()

                val refreshResponse = refreshClient.newCall(refreshHttpRequest).execute()

                if (refreshResponse.isSuccessful) {
                    val responseBody = refreshResponse.body?.string()
                    val tokenResponse = responseBody?.let {
                        moshi.adapter(TokenResponse::class.java).fromJson(it)
                    }

                    if (tokenResponse != null && tokenResponse.accessToken.isNotBlank()) {
                        // Save new tokens
                        tokenProvider?.saveTokens(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken.ifBlank { refreshToken }
                        )

                        // Retry original request with new token
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${tokenResponse.accessToken}")
                            .header("X-Token-Refreshed", "true")
                            .build()
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun buildOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private var okHttpClient = buildOkHttpClient()

    private var retrofit: Retrofit = buildRetrofit(DEFAULT_BASE_URL)

    var apiService: PathLabApiService = retrofit.create(PathLabApiService::class.java)
        private set

    /**
     * Initialize the API client. Call from Application.onCreate() or MainActivity.
     * @param baseUrl Optional custom backend URL.
     * @param authProvider Token provider for JWT auth and refresh.
     */
    fun init(baseUrl: String? = null, authProvider: AuthTokenProvider? = null) {
        if (!baseUrl.isNullOrBlank()) {
            val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            currentBaseUrl = formattedUrl
        }
        this.tokenProvider = authProvider
        rebuildClient()
    }

    fun updateBaseUrl(newUrl: String) {
        if (newUrl.isNotBlank()) {
            val formattedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
            if (formattedUrl != currentBaseUrl) {
                currentBaseUrl = formattedUrl
                rebuildClient()
            }
        }
    }

    fun setTokenProvider(provider: AuthTokenProvider) {
        this.tokenProvider = provider
    }

    private fun rebuildClient() {
        okHttpClient = buildOkHttpClient()
        retrofit = buildRetrofit(currentBaseUrl)
        apiService = retrofit.create(PathLabApiService::class.java)
    }

    private fun buildRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
