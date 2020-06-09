package com.example.honeyimhome

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.gson.Gson


class MainActivity : AppCompatActivity() {

    /** REQUEST_CODE_PERMISSION_LOCATION
     * identify user's action with which permission request.
     */
    private val PERMISSION_LOCATION_ID = 44//todo
    val BUTTON_START_TEXT = "stop tracking"
    val BUTTON_END_TEXT = "start tracking"
    var KEY_USER_HOME_LOCATION_SP = "user_home_location"
    val NUM_TO_PRESENT_HOME_BUTTON = 50
    private lateinit var locationTracker: LocationTracker
    private lateinit var sp: SharedPreferences
    private lateinit var buttonLocation: Button
    private lateinit var buttonClearHome: Button
    private lateinit var buttonSetHomeLocation: Button
    private lateinit var broadCastReceiver: BroadcastReceiver

    /*get users current position*/
//    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocationTextView: TextView
    private lateinit var userHomeLocationTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sp = getSharedPreferences("main", Context.MODE_PRIVATE)
        initView()
        initBroadcastReceiver()
        initButton()

        checkUserHomeLocation()

        if (savedInstanceState!=null) {
            if(sp.getBoolean(locationTracker.KEY_IS_TRACKING_ON_SP,false))
            {
                setViewOnStartTracking()
                setViewOnNewLocation()
            }
        }
        else{
            sp.edit().putBoolean(locationTracker.KEY_IS_TRACKING_ON_SP,false).apply()
        }
//        if (!sp.contains(KEY_CUR_LOCATION_SP)) {//todo
//            lastLocationTextView.visibility = View.INVISIBLE
//        }
    }


    fun initView() {
        lastLocationTextView = findViewById(R.id.inf_tracking_location_TextView)
        userHomeLocationTextView = findViewById(R.id.inf_home_location_TextView)

        buttonLocation = findViewById(R.id.tracking_location_button)
        buttonLocation.text = BUTTON_END_TEXT
        buttonLocation.setBackgroundColor(resources.getColor(R.color.colorGreen))//todo check
        buttonClearHome = findViewById(R.id.clear_home_button)
        buttonSetHomeLocation = findViewById(R.id.set_home_location_button)


        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationTracker = LocationTracker(this, fusedLocationClient, sp)
    }

    fun initButton() {
        initLocationTrackerButton()
        initClearHomeButton()
        initSetHomeLocationButton()
    }

    fun initLocationTrackerButton() {
        buttonLocation.setOnClickListener(View.OnClickListener {
            checkLocationPremissions()

        })
        lastLocationTextView.visibility=View.INVISIBLE
    }

    fun initClearHomeButton() {
        buttonClearHome.setOnClickListener(View.OnClickListener {
            if (sp.contains(KEY_USER_HOME_LOCATION_SP)) {
                sp.edit().remove(KEY_USER_HOME_LOCATION_SP).apply()
                userHomeLocationTextView.visibility = View.INVISIBLE
                buttonClearHome.visibility = View.INVISIBLE
            }
        })
        buttonClearHome.visibility = View.INVISIBLE

    }

    fun initSetHomeLocationButton() {
        buttonSetHomeLocation.setOnClickListener(View.OnClickListener {
            val curLocationStr =sp.getString(locationTracker.KEY_CUR_LOCATION_SP,"")
            if (curLocationStr.equals("")){
                throw Throwable("current location is empty while set home location")//todo
            }
            sp.edit().putString(KEY_USER_HOME_LOCATION_SP,curLocationStr).apply()

            val homeLocation = Gson().fromJson(curLocationStr ,LocationInfo::class.java)
            setUserHomeText(homeLocation)
            buttonClearHome.visibility= View.VISIBLE
            userHomeLocationTextView.visibility= View.VISIBLE
        })
        buttonSetHomeLocation.visibility = View.INVISIBLE
        userHomeLocationTextView.visibility = View.INVISIBLE
    }

    fun setViewOnStartTracking(){
        buttonLocation.text = BUTTON_START_TEXT
        buttonLocation.setBackgroundColor(resources.getColor(R.color.colorRed))//todo check
//        lastLocationTextView.visibility=View.VISIBLE //todo
        Log.d("TAG_START", "Start Tracking") //todo
    }

    fun setViewOnNewLocation(){
        val currentLocation = Gson().fromJson(
            sp.getString(locationTracker.KEY_CUR_LOCATION_SP, ""),
            LocationInfo::class.java)
        setLocation(currentLocation)
        lastLocationTextView.visibility=View.VISIBLE
        setVisibleHomeLocationButton(currentLocation)

    }

    fun setViewOnEndTracking(){
        buttonLocation.text = BUTTON_END_TEXT
        buttonLocation.setBackgroundColor(resources.getColor(R.color.colorGreen))//todo check
        lastLocationTextView.visibility=View.INVISIBLE
        buttonSetHomeLocation.visibility= View.INVISIBLE
    }

    private fun initBroadcastReceiver() {
        this.broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                when (intent?.action) {
                    locationTracker.START_TRACK_LOC -> {
                        setViewOnStartTracking()
                    }
                    locationTracker.NEW_LOCTAION -> {
                        setViewOnNewLocation()
                        Log.d("TAG_NEW_LOCTAION", "NEW_LOCTAION Tracking") //todo
                    }
                    locationTracker.END_TRACK_LOC -> {
                        setViewOnEndTracking()
                    }
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(locationTracker.START_TRACK_LOC)
        intentFilter.addAction(locationTracker.END_TRACK_LOC)
        intentFilter.addAction(locationTracker.NEW_LOCTAION)
        this.registerReceiver(broadCastReceiver, intentFilter)
    }

    /**
     * This method will tell us whether or not the user grant us to access ACCESS_COARSE_LOCATION
     * and ACCESS_FINE_LOCATION.
     */
    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

//                &&
//                ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) == PackageManager.PERMISSION_GRANTED
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
                Manifest.permission.ACCESS_FINE_LOCATION //todo
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
//                checkLocationPremissions() //todo
                buttonLocation.performClick()
            }
        } else {
            //Manifest.permission.ACCESS_COARSE_LOCATION,
            //                Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_LOCATION_ID
            // the user has denied our request! =-O
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION)
//                || ActivityCompat.shouldShowRequestPermissionRationale(
//                    this, Manifest.permission.ACCESS_FINE_LOCATION
//                )
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

//    @SuppressLint("MissingPermission")
//    private fun getLastLocation() {
//        if (checkPermissions()) {
//            if (isLocationEnabled()) {
//                mFusedLocationClient.lastLocation.addOnCompleteListener { task ->
//                    val location: Location? = task.result
//                    if (location != null) {
//                        val infLocation = LocationInfo(
//                            location.altitude,
//                            location.longitude,
//                            location.accuracy
//                        )
//                        setLocation(infLocation)
//                    }
//                }
//                locationTracker = LocationTracker(
//                    this, mFusedLocationClient, sp,
//                    NEW_LOCTAION, START_RRACK_LOC, END_RRACK_LOC
//                )
//
//            } else {
//                //todo
//                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
//                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                startActivity(intent)
//            }
//        } else {
//            requestPermissions()
//        }
//    }

    private fun checkLocationPremissions() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                val isTra = sp.getBoolean(locationTracker.KEY_IS_TRACKING_ON_SP, false)
                if (isTra) {
                    locationTracker.stopTracking()
                    //button invisibility handle in BroadcastReceiver
                } else {
                    locationTracker.startTracking()
                    //button visibility handle in BroadcastReceiver
                }
            }else{
                //todo ask
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadCastReceiver)
        locationTracker.destroyTracking()
    }

    fun checkUserHomeLocation() { //TODO
        if (sp.contains(KEY_USER_HOME_LOCATION_SP)) {
            val userHomeStr = sp.getString(KEY_USER_HOME_LOCATION_SP, "")
            val userHomeLoc = Gson().fromJson(userHomeStr, LocationInfo::class.java)
            setUserHomeText(userHomeLoc)
            buttonClearHome.visibility=View.VISIBLE
        }
        else{
            userHomeLocationTextView.visibility=View.INVISIBLE
        }
    }

    fun setUserHomeText(userHomeLoc: LocationInfo){
        val userHomeText = "your home location is defined as:\n" + "<latitude:" +
                "${userHomeLoc.getLatitudeStr()}, longitude:${userHomeLoc.getLongitudeStr()}>"
        userHomeLocationTextView.text = userHomeText
        userHomeLocationTextView.visibility=View.VISIBLE
    }

    private fun setLocation(location: LocationInfo) {
        val newLocText = "Your location:\n" +
            "latitude:${location.getLatitudeStr()}\nlongitude:${location.getLongitudeStr()
            }\naccuracy${location.getAccuracyStr()}"
        lastLocationTextView.text = newLocText
    }

    fun setVisibleHomeLocationButton(currentLocation: LocationInfo) {
        if (currentLocation.getAccuracy()!! < NUM_TO_PRESENT_HOME_BUTTON) {
            buttonSetHomeLocation.visibility = View.VISIBLE
        }
        else{
            buttonSetHomeLocation.visibility = View.INVISIBLE
        }
    }
}
