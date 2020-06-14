package com.example.honeyimhome

import android.app.Application
import android.content.IntentFilter

class SmsApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        val intentFilter = IntentFilter()
        val localSendSmsBroadcastReceiver=LocalSendSmsBroadcastReceiver()
        intentFilter.addAction("POST_PC.ACTION_SEND_SMS")
        this.registerReceiver(localSendSmsBroadcastReceiver, intentFilter)
    }
}