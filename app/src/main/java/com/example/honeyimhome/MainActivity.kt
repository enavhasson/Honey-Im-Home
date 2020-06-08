package com.example.honeyimhome

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.gson.Gson


class MainActivity : AppCompatActivity() {

    /** REQUEST_CODE_PERMISSION_LOCATION
     * identify user's action with which permission request.
     */
    private val PERMISSION_LOCATION_ID = 44//todo
    val NEW_LOCTAION = "new_location"
    val START_RRACK_LOC = "start_location"
    val END_RRACK_LOC = "end_location"
    val BUTTON_START_TEXT = "stop tracking"
    val BUTTON_END_TEXT = "start tracking"
    var KEY_IS_TRACKING_ON_SP = "isTrackingOn"
    var KEY_CUR_LOCATION_SP = "current_location"
    private lateinit var locationTracker: LocationTracker
    private lateinit var sp: SharedPreferences
    private lateinit var buttonLocation: Button
    private lateinit var broadCastReceiver: BroadcastReceiver

    /*get users current position*/
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var latLocationTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sp= getSharedPreferences("main", Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_IS_TRACKING_ON_SP,false).apply()
        initView()
        initBroadcastReceiver()
        initButton()
    }

    fun initView() {
        latLocationTextView = findViewById(R.id.inf_tracking_location_TextView)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        buttonLocation = findViewById(R.id.tracking_location_button)
        buttonLocation.text = BUTTON_END_TEXT
        buttonLocation.setBackgroundColor(resources.getColor(R.color.colorGreen))//todo check
        locationTracker = LocationTracker(
            this,
            mFusedLocationClient,
            sp,
            NEW_LOCTAION,
            START_RRACK_LOC,
            END_RRACK_LOC
        )
    }

    fun initButton() {
        buttonLocation.setOnClickListener(View.OnClickListener {
            getLastLocation()
            val isTra=sp.getBoolean(KEY_IS_TRACKING_ON_SP, false)
            if (isTra) {
                locationTracker.stopTracking()
            } else {
                locationTracker.startTracking()

            }
        })
    }

    private fun initBroadcastReceiver() {
        this.broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                when (intent?.action) {
                    //todo
                    START_RRACK_LOC -> {
                        buttonLocation.text = BUTTON_START_TEXT
                        buttonLocation.setBackgroundColor(resources.getColor(R.color.colorRed))//todo check
                        Log.d("TAG_START", "Start Tracking") //todo
                    } //todo
                    NEW_LOCTAION -> {
                        val currentLocation = Gson().fromJson(
                            sp.getString(KEY_CUR_LOCATION_SP, ""),
                            LocationInfo::class.java
                        )
                        setLocation(currentLocation)
                        Log.d("TAG_NEW_LOCTAION", "NEW_LOCTAION Tracking") //todo
                    }
                    END_RRACK_LOC -> {
                        buttonLocation.text = BUTTON_END_TEXT
                        buttonLocation.setBackgroundColor(resources.getColor(R.color.colorGreen))//todo check
                    }
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(START_RRACK_LOC)
        intentFilter.addAction(END_RRACK_LOC)
        intentFilter.addAction(NEW_LOCTAION)
        this.registerReceiver(broadCastReceiver, intentFilter)
    }

    /**
     * This method will tell us whether or not the user grant us to access ACCESS_COARSE_LOCATION
     * and ACCESS_FINE_LOCATION.
     */
    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }


    /**
     * This method will request our necessary permissions to the user if they are not
     * already granted.
     */
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), PERMISSION_LOCATION_ID
        )
    }


    /**
     * This method is called when a user Allow or Deny our requested permissions.
     * So it will help us to move forward if the permissions are granted.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_LOCATION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted. Start getting the location information
                getLastLocation()
            }
        } else {
            //Manifest.permission.ACCESS_COARSE_LOCATION,
            //                Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_LOCATION_ID
            // the user has denied our request! =-O
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                var x = 1
                //todo
                // reached here? means we asked the user for this permission more than once,
                // and they still refuse. This would be a good time to open up a dialog
                // explaining why we need this permission
            }
        }
    }

    /**
     * This will check if the user has turned on location from the setting,
     * Cause user may grant the app to user location but if the location setting is
     * off then it'll be of no use.
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        val infLocation = LocationInfo(
                            location.altitude,
                            location.longitude,
                            location.accuracy
                        )
                        setLocation(infLocation)
                    }
                }
                locationTracker = LocationTracker(
                    this, mFusedLocationClient, sp,
                    NEW_LOCTAION, START_RRACK_LOC, END_RRACK_LOC
                )

            } else {
                //todo
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

//    @SuppressLint("MissingPermission")
//    private fun requestNewLocationData() {
//        val mLocationRequest = LocationRequest()
//        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        mLocationRequest.interval = 0
//        mLocationRequest.fastestInterval = 0
//        mLocationRequest.numUpdates = 1
//        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        //todo
//        mFusedLocationClient.requestLocationUpdates(
//            mLocationRequest, mLocationCallback,
//            Looper.myLooper()
//        )
//    }

//    private val mLocationCallback: LocationCallback = object : LocationCallback() {
//        override fun onLocationResult(locationResult: LocationResult) {
//            val mLastLocation: Location = locationResult.lastLocation
//            setLocation(mLastLocation)
//        }
//    }

    private fun setLocation(location: LocationInfo) {
        val newLocText =
            "latitude:${location.getLatitudeStr()}\nlongitude:${location.getLongitudeStr()
            }\naccuracy${location.getAccuracyStr()}"
        latLocationTextView.text = newLocText //todo
    }

}
