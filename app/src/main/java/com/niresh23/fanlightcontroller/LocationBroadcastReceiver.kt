package com.niresh23.fanlightcontroller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager

class LocationBroadcastReceiver(
    private val locationStateChanged: (Boolean) -> Unit
): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        if (intent?.action?.equals("android.location.MODE_CHANGED") == true) {
            locationManager?.isLocationEnabled?.let {
                locationStateChanged.invoke(it)
            }
        }
    }
}