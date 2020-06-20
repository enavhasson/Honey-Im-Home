package com.example.honeyimhome

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class LocalSendSmsBroadcastReceiver() : BroadcastReceiver() {
    private val CHANNEL_ID = "6"
    private val TAG = "LocalSendSmsBroadcastReceiver"
    private val SMS_NOTIFICATION_TITLE = "ex6 - Honey Im Home!"

    //a unique int for each notification
    private val SMS_NOTIFICATION_ID =6

    override fun onReceive(context: Context?, intent: Intent?) {
        //safe check
        //no date ,or not interesting intent action
        if (intent == null || intent.action != SEND_SMS_ACTION || context == null) {
            return
        }

        checkRunTimeSmsPermission(context)

        //have run time sms permission
        val phoneNum = intent.getStringExtra(PHONE_NUM_INTENT)
        val contentSmsMes = intent.getStringExtra(CONTEXT_INTENT)

        checkIntentValues(phoneNum, contentSmsMes)

        //phone number and content sms message have a values
        sendSMSMessage(phoneNum, contentSmsMes)
        createSmsChannelIfNotExists(context)
        createPushNotification(phoneNum, contentSmsMes, context)
    }

    private fun createSmsChannelIfNotExists(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notificationChannels.forEach { channel ->
                if (channel.id == CHANNEL_ID) {
                    return
                }
            }

            // Create the NotificationChannel
            val name = "Sms Notification"
            val descriptionText = "channel for Sms Notification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun createPushNotification(
        phoneNum: String?,
        contentSmsMessage: String?,
        context: Context
    ) {
        //phone number and content sms message have a values
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentText("sending sms to $phoneNum:$contentSmsMessage")
            .setContentTitle(SMS_NOTIFICATION_TITLE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(SMS_NOTIFICATION_ID, notification)
    }

    private fun sendSMSMessage(phoneNum: String?, contentSmsMessage: String?) {
        //phone number and content sms message have a values
        val smsManager: SmsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(
            phoneNum, null, contentSmsMessage,
            null, null
        )
    }

    /**
    check if one of the intent values is null or empty, log an error and return.
     */
    @SuppressLint("LongLogTag")
    private fun checkIntentValues(phone_num: String?, content_sms_message: String?) {
        if (phone_num == null || phone_num == "") { //safe check
            Log.e(TAG, "phone number is null or empty")
            return
        }
        if (content_sms_message == null || content_sms_message == "") { //safe check
            Log.e(TAG, "content sms message is null or empty")
            return
        }
    }

    @SuppressLint("LongLogTag")
    private fun checkRunTimeSmsPermission(context: Context) {
        val runTimePerm = isSmsPermissionGranted(context)
        if (!runTimePerm) {//not have Run Time Sms Permission
            Log.e(TAG, "don't have SMS Permission")
            return
        }
    }

    /**
     * Check if we have SMS permission
     */
    private fun isSmsPermissionGranted(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }


}