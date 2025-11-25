package com.appautomation

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AutomationApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
