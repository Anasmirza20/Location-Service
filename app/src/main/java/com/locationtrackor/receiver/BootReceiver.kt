package com.locationtrackor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.locationtrackor.service.LocationTrackingService
import com.locationtrackor.util.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (preferenceManager.isTracking()) {
                val serviceIntent = Intent(context, LocationTrackingService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
