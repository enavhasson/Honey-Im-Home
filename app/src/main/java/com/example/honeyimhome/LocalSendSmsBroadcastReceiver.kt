package com.example.honeyimhome

import android.Manifest
import android.app.Notification
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
    private val PHONE_NUM = "phone_number"
    private val SMS_MESSAGE_CONTENT = "context"
    private val EMPTY_SP_VALUE = ""
    private val channelId = "SMS_CHANNEL_ID"

    override fun onReceive(context: Context?, intent: Intent?) {
        //safe check
        //no date ,or not interesting intent action
        if (intent == null || intent.action != "POST_PC.ACTION_SEND_SMS"|| context == null) {
            return
        }

        checkRunTimeSmsPermission(context)

        //have run time sms permission
        val phoneNum = intent.getStringExtra(PHONE_NUM)
        val contentSmsMes = intent.getStringExtra(SMS_MESSAGE_CONTENT)

        checkIntentValues(phoneNum, contentSmsMes)

        //phone number and content sms message have a values
        sendSMSMessage(phoneNum, contentSmsMes)
        createSmsChannelIfNotExists(context)
        createPushNotification(phoneNum, contentSmsMes,context)
    }

    private fun createSmsChannelIfNotExists(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notificationChannels.forEach { channel ->
                if (channel.id == channelId) {
                    return
                }
            }

            // Create the NotificationChannel
            val name = "Sms Notification"
            val descriptionText = "channel for Sms Notification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            channel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun createPushNotification(phoneNum: String?, contentSmsMessage: String?,context: Context) {
        //phone number and content sms message have a values
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentText("sending sms to $phoneNum:$contentSmsMessage")
            .setContentTitle("ex6 - Honey Im Home!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // notificationId is a unique int for each notification that you must define
        val notificationId = 6
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun sendSMSMessage(phoneNum: String?, contentSmsMessage: String?) {
        //phone number and content sms message have a values
        val smsManager: SmsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNum, null, contentSmsMessage, null, null)

        //todo ,check if is needed
//        Toast.makeText(
//            ApplicationProvider.getApplicationContext(), "SMS sent.",
//            Toast.LENGTH_LONG
//        ).show()
    }

    /**
    check if one of the intent values is null or empty, log an error and return.
     */
    private fun checkIntentValues(phone_num: String?, content_sms_message: String?) {
        if (phone_num == null || phone_num == "" || phone_num == EMPTY_SP_VALUE) { //safe check
            Log.e("TAG_SMS_INTENT_SEND", "phone number is null or empty")
            return
        }
        if (content_sms_message == null || content_sms_message == "" || content_sms_message == EMPTY_SP_VALUE) { //safe check
            Log.e("TAG_SMS_INTENT_SEND", "content sms message is null or empty")
            return
        }
    }

    private fun checkRunTimeSmsPermission(context: Context) {
        val runTimePerm = isSmsPermissionGranted(context)
        if (!runTimePerm) {//not have Run Time Sms Permission
            Log.e("TAG_SMS_Permission", "don't have SMS Permission")
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