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
import com.google.android.gms.location.*
import com.google.gson.Gson

const val NEW_LOCATION = "new_location"
const val END_TRACK_LOC = "end_location"
const val START_TRACK_LOC = "start_location"
const val KEY_IS_TRACKING_ON_SP = "isTrackingOn"
const val KEY_CUR_LOCATION_SP = "current_location"


class LocationTracker(
    private val context: Context,
    private var fusedLocationClient: FusedLocationProviderClient, private var sp: SharedPreferences
) {

    private var isTrackingOn: Boolean = false
    private var TAG_PERMISSION_ERROR = "don't have runtime location permission"

    /**start tracking the location and send a "started" broadcast intent*/
    fun startTracking() {
        if (!checkPermissions()) {
            Log.e("TAG_PERMISSION_ERROR", TAG_PERMISSION_ERROR)
        } else {
            requestNewLocationData()
            sendBroadcast(START_TRACK_LOC)
            isTrackingOn = true
            sp.edit().putBoolean(KEY_IS_TRACKING_ON_SP, isTrackingOn).apply()
        }
    }

    /**stop tracking and send a "stopped" broadcast intent.*/
    fun stopTracking() {
        stopLocationUpdates()
        sendBroadcast(END_TRACK_LOC)
    }

    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 10000;
        mLocationRequest.fastestInterval = 5000;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    fun setNewLocation(location: Location) {
        val curLocation = LocationInfo(location.latitude, location.longitude, location.accuracy)
        sp.edit().putString(KEY_CUR_LOCATION_SP, Gson().toJson(curLocation)).apply()
        sendBroadcast(NEW_LOCATION)
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            setNewLocation(mLastLocation)
        }
    }

    private fun sendBroadcast(broadcastMessage: String) {
        val intent = Intent()
        intent.action = broadcastMessage
        context.sendBroadcast(intent)
    }

    /**
     * This method will tell us whether or not the user grant us to access ACCESS_COARSE_LOCATION
     * and ACCESS_FINE_LOCATION.
     */
    private fun checkPermissions(): Boolean {
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

    fun destroyTracking() {
        fusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

}