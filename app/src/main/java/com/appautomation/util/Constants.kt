package com.appautomation.util

object Constants {
    // Default duration
    const val DEFAULT_DURATION_MINUTES = 7
    const val DEFAULT_DURATION_MILLIS = DEFAULT_DURATION_MINUTES * 60 * 1000L
    
    // Duration limits
    const val MIN_DURATION_MINUTES = 1
    const val MAX_DURATION_MINUTES = 60
    
    // Interaction settings
    const val DEFAULT_INTERACTION_INTERVAL_SECONDS = 1
    const val MIN_INTERACTION_INTERVAL_SECONDS = 1
    const val MAX_INTERACTION_INTERVAL_SECONDS = 60
    
    // Shared preferences
    const val PREFS_NAME = "automation_prefs"
    const val PREF_DEFAULT_DURATION = "default_duration_minutes"
    const val PREF_GLOBAL_DURATION = "global_duration_minutes"
    const val PREF_INTERACTION_INTERVAL = "interaction_interval_seconds"
    const val PREF_INCLUDE_SYSTEM_APPS = "include_system_apps"
    const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
}
