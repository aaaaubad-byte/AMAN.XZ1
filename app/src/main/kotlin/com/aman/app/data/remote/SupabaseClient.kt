package com.aman.app.data.remote

import android.content.Context
import android.util.Log
import com.aman.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Singleton managing connection to Supabase backend on Android.
 * Connects directly using the official Supabase Kotlin SDK.
 * Respects credential security: Never hardcodes secrets.
 */
object AmanSupabase {
    private const val TAG = "AmanSupabase"

    private val configuredUrl: String = BuildConfig.SUPABASE_URL
    private val configuredAnonKey: String = BuildConfig.SUPABASE_ANON_KEY

    lateinit var client: SupabaseClient
        private set

    var isInitialized: Boolean = false
        private set

    /**
     * Factual verification whether real Supabase configuration was provided.
     * Prevents treating placeholder values as a working connection.
     */
    fun isConfigured(): Boolean {
        return configuredUrl.isNotBlank() &&
                !configuredUrl.contains("placeholder") &&
                !configuredUrl.contains("your-supabase-project") &&
                configuredAnonKey.isNotBlank() &&
                !configuredAnonKey.contains("placeholder") &&
                !configuredAnonKey.contains("your-anon-key")
    }

    fun initialize(
        context: Context,
        customUrl: String? = null,
        customKey: String? = null
    ) {
        val url = customUrl ?: configuredUrl
        val key = customKey ?: configuredAnonKey

        if (!isConfigured() && customUrl == null) {
            Log.w(TAG, "Supabase credentials are not configured. Backend operations will be marked as unconfigured.")
            return
        }

        try {
            client = createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = key
            ) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
                install(Storage)
            }
            isInitialized = true
            Log.i(TAG, "Supabase Android client initialized successfully with URL: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client: ${e.message}", e)
        }
    }

    val auth: Auth
        get() {
            check(isInitialized) { "Supabase Client is not initialized or configured" }
            return client.auth
        }

    val postgrest: Postgrest
        get() {
            check(isInitialized) { "Supabase Client is not initialized or configured" }
            return client.postgrest
        }
}
