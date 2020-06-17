package com.example.honeyimhome

import android.app.Application
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class SmsApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        val receiver=LocalSendSmsBroadcastReceiver(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter("POST_PC.ACTION_SEND_SMS"))
    }
}