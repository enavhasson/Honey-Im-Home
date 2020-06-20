package com.example.honeyimhome
import android.app.Application
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val receiver = LocalSendSmsBroadcastReceiver()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(SEND_SMS_ACTION))

        val work: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<TrackUserLocationWork>(15, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(this).enqueue(work)
    }
}