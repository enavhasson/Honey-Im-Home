package com.example.honeyimhome

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.gson.Gson



class LocationTracker(
    private val context: Context,
    var fusedLocationClient: FusedLocationProviderClient, var sp: SharedPreferences
) {
    public val NEW_LOCTAION = "new_location"
    public val END_TRACK_LOC = "end_location"
    public val START_TRACK_LOC = "start_location"
    public val KEY_IS_TRACKING_ON_SP = "isTrackingOn"
    public val KEY_CUR_LOCATION_SP = "current_location"
    private var isTrackingOn: Boolean = false
    private var TAG_PERMISSION_ERROR = "dont have runtime location permission"

    //start tracking the location and send a "started" boradcast intent
    fun startTracking() {
        if (!checkPermissions()) {
            Log.e("TAG_PERMISSION_ERROR", TAG_PERMISSION_ERROR) //todo
        } else {
            requestNewLocationData()
            sendBroadcast(START_TRACK_LOC)
            isTrackingOn = true
            sp.edit().putBoolean(KEY_IS_TRACKING_ON_SP,isTrackingOn).apply()
        }
    }

    fun getIsTrackingOn(): Boolean {
        return isTrackingOn
    }

    //stop tracking and send a "stopped" broadcast intent.
    fun stopTracking() {
        stopLocationUpdates()
        sendBroadcast(END_TRACK_LOC)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 10000;
        mLocationRequest.fastestInterval = 5000;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        //todo
        fusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    //todo change fun name
    fun newLocation(location: Location) {
        val curLocation = LocationInfo(location.latitude, location.longitude, location.accuracy)
        sp.edit().putString(KEY_CUR_LOCATION_SP, Gson().toJson(curLocation)).apply()
        sendBroadcast(NEW_LOCTAION)
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            newLocation(mLastLocation)
        }
    }

    fun sendBroadcast(broadcastMessage: String) {
        val intent = Intent()
        intent.action = broadcastMessage
        context.sendBroadcast(intent)
    }

    /**
     * This method will tell us whether or not the user grant us to access ACCESS_COARSE_LOCATION
     * and ACCESS_FINE_LOCATION.
     */
    fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun stopLocationUpdates() {
        destroyTracking()
        isTrackingOn = false
        sp.edit().putBoolean(KEY_IS_TRACKING_ON_SP, isTrackingOn).apply()
    }

    fun destroyTracking(){
        fusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

}