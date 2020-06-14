package com.example.honeyimhome

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat


class LocalSendSmsBroadcastReceiver(private val context: Context) : BroadcastReceiver() {
    val PHONE_NUM = "0547228509"
    val SMS_MESSAGE_CONTENT = "hello to beautiful ex6"
    val EMPTY_SP_VALUE = ""

    //TODO("check implementation")
    override fun onReceive(context: Context?, intent: Intent?) {
        checkRunTimeSmsPermission()

        //have run time sms permission
        val phoneNum = intent?.getStringExtra(PHONE_NUM)
        val contentSmsMes = intent?.getStringExtra(SMS_MESSAGE_CONTENT)

        checkIntentValues(phoneNum, contentSmsMes)

        //phone number and content sms message have a values
        sendSMSMessage(phoneNum, contentSmsMes)
        createPushNotification(phoneNum, contentSmsMes)
    }

    private fun createPushNotification(phoneNum: String?, contentSmsMessage: String?) {
        //phone number and content sms message have a values
        //todo check
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: Notification = NotificationCompat.Builder(context)
            .setContentText("sending sms to $phoneNum:$contentSmsMessage")
            .setContentTitle("ex6")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        notificationManager.notify(0, notification)

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
    todo :return ? what is mean
     */
    private fun checkIntentValues(phone_num: String?, content_sms_message: String?) {
        if (phone_num == null || phone_num == EMPTY_SP_VALUE) { //safe check
            Log.e("TAG_SMS_INTENT_SEND", "phone number is null or empty")
        }
        if (content_sms_message == null || content_sms_message == EMPTY_SP_VALUE) { //safe check
            Log.e("TAG_SMS_INTENT_SEND", "content sms message is null or empty")
        }
    }

    private fun checkRunTimeSmsPermission() {
        //todo check that we have the send-sms runtime permission
        if (false) {//not have Run Time Sms Permission
            Log.e("TAG_SMS_Permission", "don't have SMS Permission")
            //todo return? (. If not, log an error to the logcat and return)
        }
    }

//    /**
//     * Check if we have SMS permission
//     */
//    fun isSmsPermissionGranted(): Boolean {
//        return ContextCompat.checkSelfPermission(
//            this,
//            Manifest.permission.READ_SMS
//        ) == PackageManager.PERMISSION_GRANTED
//    }

//    private checkSmsPermission(){
//
//        if(!requestReadAndSendSmsPermission()){
//            Log.e("TAG_SMS_Permission","don't have SMS Permission")
//            //todo return
//        }
//    }
//
//    /**
//     * Request runtime SMS permission
//     */
//    private fun requestReadAndSendSmsPermission() {
//        if (ActivityCompat.shouldShowRequestPermissionRationale(
//                this,
//                Manifest.permission.READ_SMS
//            )
//        ) {
//            // You may display a non-blocking explanation here, read more in the documentation:
//            // https://developer.android.com/training/permissions/requesting.html
//        }
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(Manifest.permission.READ_SMS),
//            SMS_PERMISSION_CODE
//        )
//    }
}