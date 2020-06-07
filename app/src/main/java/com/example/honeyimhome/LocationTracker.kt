package com.example.honeyimhome

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*


class LocationTracker (private val context: Context,
                       var fusedLocationClient: FusedLocationProviderClient, var newLocationStr: String
                       , var startTrackLocStr :String, var endTrackLocStr :String){
    var curlocation: LocationInfo?=null //todo
    private var isTrackingOn :Boolean =false
    private var TAG_PERMISSION_ERROR = "dont have runtime location permission"

    //start tracking the location and send a "started" boradcast intent
    public fun startTracking(){
        if(!checkPermissions()){
            Log.e("TAG_PERMISSION_ERROR",TAG_PERMISSION_ERROR) //todo
        }
        else{
            requestNewLocationData()
            sendBroadcast(startTrackLocStr)
            isTrackingOn=true
        }
    }
    fun getIsTrackingOn():Boolean{
        return isTrackingOn
    }
    fun getLatitude(): Double? {
        return curlocation?.latitude
    }

    fun getLongitude(): Double? {
        return curlocation?.longitude
    }

    fun getAaccuracy(): Float? {
        return curlocation?.accuracy
    }

    //stop tracking and send a "stopped" broadcast intent.
    public fun stopTracking(){
        stopLocationUpdates()
        sendBroadcast(endTrackLocStr)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 10000;
        mLocationRequest.fastestInterval = 5000;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        //todo
        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.myLooper())
    }

    //todo change fun name
    fun newLocation(location: Location){
        curlocation = LocationInfo(location.latitude,location.longitude,location.accuracy)
        sendBroadcast(newLocationStr)
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            newLocation(mLastLocation)
        }
    }

    fun sendBroadcast(broadcastMessage : String) {
        val intent = Intent()
        intent.action = broadcastMessage
        context.sendBroadcast(intent)
    }

    /**
     * This method will tell us whether or not the user grant us to access ACCESS_COARSE_LOCATION
     * and ACCESS_FINE_LOCATION.
     */
    fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(mLocationCallback)
        isTrackingOn=false
    }


}