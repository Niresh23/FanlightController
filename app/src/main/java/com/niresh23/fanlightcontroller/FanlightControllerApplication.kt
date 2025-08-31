package com.niresh23.fanlightcontroller

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.niresh23.fanlightcontroller.di.AppModule
import com.niresh23.fanlightcontroller.ui.BluetoothControlService.Companion.NOTIFICATION_CHANNEL_ID
import com.niresh23.fanlightcontroller.ui.BluetoothControlService.Companion.NOTIFICATION_CHANNEL_NAME
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FanlightControllerApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        startKoin {
            androidContext(this@FanlightControllerApplication)
            modules(AppModule)
        }
    }
}

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
