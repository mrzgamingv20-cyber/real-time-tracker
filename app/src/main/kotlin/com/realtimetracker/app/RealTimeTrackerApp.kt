package com.realtimetracker.app

import android.app.Application
import timber.log.Timber

class RealTimeTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("RealTimeTracker app initialized")
    }
}
