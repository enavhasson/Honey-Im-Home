package com.example.honeyimhome

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.*
import com.google.gson.Gson


class TrackUserLocationWork(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sp: SharedPreferences
    private val TAG ="TrackUserLocationWork"

    override fun doWork(): Result {
        if (!checkPermissions()) {
            return Result.success()
        }

        sp = applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

        if (!checkStoreParamsInPc()) {
            return Result.success()
        }

        requestNewLocationData()
        return Result.success()
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoreParamsInPc(): Boolean {
        val location = sp.getString(KEY_USER_HOME_LOCATION_SP, null)
        val phone = sp.getString(KEY_PHONE_NUMBER_SP, null)
        if (location == null || phone.isNullOrEmpty()) {
            return false
        }
        return true
    }


    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 10000;
        mLocationRequest.fastestInterval = 5000;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        fusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.getMainLooper()
        )
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val current: Location = locationResult.lastLocation
            if (current.accuracy >= 50) return
            fusedLocationClient.removeLocationUpdates(this)
            val currentLocation =
                LocationInfo(current.latitude, current.longitude, current.accuracy)

            val prevLocation = getLocationStoreSP(KEY_CUR_LOCATION_SP) ?: return
            if (current.distanceTo(convertToLocationClass(prevLocation, "prev")) < 50){
                sp.edit().putString(KEY_CUR_LOCATION_SP, Gson().toJson(currentLocation)).apply()
                return
            }
            val homeLocation = getLocationStoreSP(KEY_USER_HOME_LOCATION_SP)
            if(homeLocation==null){
                Log.e(TAG, "homeLocation is null")
                return
            }
            if (current.distanceTo(convertToLocationClass(homeLocation, "home")) < 50) {
                sendSms()
            }
        }
    }


    fun getLocationStoreSP(ket_SP: String): LocationInfo? {
        val currentLocationStr = sp.getString(ket_SP, null)
        if (currentLocationStr == null) {
            Log.e(TAG, "$ket_SP is null")
            return null
        }
        return Gson().fromJson(currentLocationStr, LocationInfo::class.java)
    }

    private fun sendSms() {
        val intent = Intent(SEND_SMS_ACTION)
        intent.putExtra(PHONE_NUM_INTENT, sp.getString(KEY_PHONE_NUMBER_SP, null))
        intent.putExtra(CONTEXT_INTENT, SMS_MESSAGE)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    fun convertToLocationClass(locationInfo: LocationInfo, provider: String): Location {
        val location = Location(provider)
        location.latitude = locationInfo.getLatitude()
        location.longitude = locationInfo.getLongitude()
        location.accuracy = locationInfo.getAccuracy()
        return location
    }
}