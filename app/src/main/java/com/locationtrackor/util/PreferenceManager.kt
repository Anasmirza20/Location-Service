package com.locationtrackor.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferenceManager(context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)

    fun isTracking(): Boolean = sharedPrefs.getBoolean("is_tracking", false)

    fun setTracking(isTracking: Boolean) {
        sharedPrefs.edit { putBoolean("is_tracking", isTracking) }
    }
}
