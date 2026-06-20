package com.realtimetracker.app

import android.app.Application
import timber.log.Timber

class RealTimeTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
