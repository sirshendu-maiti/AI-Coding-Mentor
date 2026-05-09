package com.euphoria.aimentor.data.api

import com.euphoria.aimentor.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client configured to call the backend server.
 * - Automatically attaches Firebase ID token for auth
 * - Logging is BODY in debug, NONE in release
 * - Retry on transient failures
 */
object ApiClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // SECURITY: Only log bodies in debug builds to prevent API key/token leakage
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    /**
     * Auth interceptor: attaches Firebase ID token to every request.
     */
    private val authInterceptor = Interceptor { chain ->
        val token = try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // Get fresh ID token (cached, refreshes automatically if expired)
                runBlocking { user.getIdToken(false).await().token }
            } else null
        } catch (e: Exception) {
            null
        }

        val request = chain.request().newBuilder()
            .header("Content-Type", "application/json")
            .apply {
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        chain.proceed(request)
    }

    /**
     * Retry interceptor: retries on 503 and timeouts (up to 2 retries).
     */
    private val retryInterceptor = Interceptor { chain ->
        var response = chain.proceed(chain.request())
        var retryCount = 0

        while (!response.isSuccessful && retryCount < 2 &&
            (response.code == 503 || response.code == 504)
        ) {
            retryCount++
            response.close()
            Thread.sleep(1000L * retryCount) // Backoff: 1s, 2s
            response = chain.proceed(chain.request())
        }

        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(retryInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // AI calls can take up to 90s on server
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: BackendApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL.trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApiService::class.java)
    }
}
