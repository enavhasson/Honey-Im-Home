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

    override fun doWork(): Result {
        if (!checkPermissions()) {
            return Result.success()
        }

        sp = applicationContext.getSharedPreferences("mainSP", Context.MODE_PRIVATE)

        if (!checkStoreParmsInPc()) {
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

    private fun checkStoreParmsInPc(): Boolean {

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
            val location: Location = locationResult.lastLocation
            if (location.accuracy >= 50) return
            val currentLocation =
                LocationInfo(location.latitude, location.longitude, location.accuracy)
            val prevLocation = getLocationStoreSP(KEY_CUR_LOCATION_SP)
            if (prevLocation == null) {
                Log.e("value_null", "prev Location is null")
            }
            fusedLocationClient.removeLocationUpdates(this)
            sp.edit().putString("current", Gson().toJson(currentLocation)).apply()
            val homeLocation = getLocationStoreSP(KEY_USER_HOME_LOCATION_SP)
            if(prevLocation==null||homeLocation==null){
                return
            }
            if (location.distanceTo(convertToLocationClass(prevLocation, "prev")) > 50 &&
                location.distanceTo(convertToLocationClass(homeLocation, "prev")) < 50) {
                sendSms()
            }
        }
    }


    fun getLocationStoreSP(ket_SP: String): LocationInfo? {
        val currentLocationStr = sp.getString(ket_SP, null)
        if (currentLocationStr == null) {
            Log.e("value_null", "$ket_SP is null")
        }
        return Gson().fromJson(currentLocationStr, LocationInfo::class.java)

    }

    private fun sendSms() {
        val intent = Intent("POST_PC.ACTION_SEND_SMS")
        intent.putExtra(PHONE_NUM_INTENT, sp.getString(KEY_PHONE_NUMBER_SP, null))
        intent.putExtra(CONTEXT_INTENT, SMS_MESSAGE)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    fun convertToLocationClass(locationInfo: LocationInfo, provider: String): Location {
        val location = Location(provider)
        location.latitude = locationInfo.latitude
        location.longitude = locationInfo.longitude
        location.accuracy = locationInfo.accuracy
        return location
    }
}