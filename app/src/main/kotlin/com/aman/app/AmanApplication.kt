package com.aman.app

import android.app.Application
import android.util.Log
import com.aman.app.data.remote.AmanSupabase

/**
 * AMAN Application entry point.
 * Initializes Supabase Client and Android app-level services.
 */
class AmanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing AMAN Native Android Application...")
        
        // Initialize Supabase client
        AmanSupabase.initialize(this)
    }

    companion object {
        private const val TAG = "AmanApp"
    }
}
