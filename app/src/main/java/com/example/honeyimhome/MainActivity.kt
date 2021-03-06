package com.example.honeyimhome

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import com.google.android.gms.location.LocationServices
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson

var KEY_USER_HOME_LOCATION_SP = "user_home_location"
var KEY_PHONE_NUMBER_SP = "phone_number"
val PHONE_NUM_INTENT = "phone_number"
val CONTEXT_INTENT = "context"
val SMS_MESSAGE = "Honey I'm Home!"
val SP_NAME = "mainSP"
val SEND_SMS_ACTION = "POST_PC.ACTION_SEND_SMS"


class MainActivity : AppCompatActivity() {

    /* identify user's action with which permission request.*/
    private val TAG = "MainActivity"
    private val PERMISSION_LOCATION_ID = 44
    private val PERMISSION_SMS_ID = 1546
    private val BUTTON_START_TEXT = "stop tracking"
    private val BUTTON_END_TEXT = "start tracking"
    private val NUM_TO_PRESENT_HOME_BUTTON = 50
    private val SMS_DIALOG_EXPLAINING ="App need sms permission, for send an SMS message "

    private lateinit var locationTracker: LocationTracker
    private lateinit var sp: SharedPreferences
    private lateinit var locationBroadCastReceiver: BroadcastReceiver

    //location
    private lateinit var buttonLocation: Button
    private lateinit var buttonClearHome: Button
    private lateinit var buttonSetHomeLocation: Button
    private lateinit var lastLocationTextView: TextView
    private lateinit var userHomeLocationTextView: TextView

    //sms
    private lateinit var setSmsPhoneNumButton: Button
    private lateinit var testSmsButton: Button
    private lateinit var deletePhoneNumButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

        //get users current position
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationTracker = LocationTracker(this, fusedLocationClient, sp)

        initView()
        initBroadcastReceiver()
        initButton()
        checkUserHomeLocation()
        checkPhoneNum()

        if (savedInstanceState == null) {
            sp.edit().putBoolean(KEY_IS_TRACKING_ON_SP, false).apply()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (sp.getBoolean(KEY_IS_TRACKING_ON_SP, false)) {
            if (isLocationEnabled()) {
                setViewOnStartTracking()
                setViewOnNewLocation()
            } else {
                locationTracker.stopTracking()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(locationBroadCastReceiver)
        locationTracker.destroyTracking()
    }


    private fun initView() {
        lastLocationTextView = findViewById(R.id.inf_tracking_location_TextView)
        userHomeLocationTextView = findViewById(R.id.inf_home_location_TextView)

        buttonLocation = findViewById(R.id.tracking_location_button)
        setButtonEndTrackView()

        buttonClearHome = findViewById(R.id.clear_home_button)
        buttonSetHomeLocation = findViewById(R.id.set_home_location_button)

        setSmsPhoneNumButton = findViewById(R.id.set_SMS_phone_number_button)
        testSmsButton = findViewById(R.id.test_SMS_button)
        deletePhoneNumButton = findViewById(R.id.delete_phone_number_button)
    }

    private fun initButton() {
        initLocationTrackerButton()
        initClearHomeButton()
        initSetHomeLocationButton()
        initSetSmsPhoneNumButton()
        initTestSmsButton()
        initDelPhoneNumButton()
    }

    private fun initLocationTrackerButton() {
        buttonLocation.setOnClickListener(View.OnClickListener {
            checkLocationPermissions()
        })
        lastLocationTextView.visibility = View.INVISIBLE
    }

    private fun initClearHomeButton() {
        buttonClearHome.setOnClickListener(View.OnClickListener {
            if (sp.contains(KEY_USER_HOME_LOCATION_SP)) {
                sp.edit().remove(KEY_USER_HOME_LOCATION_SP).apply()
                userHomeLocationTextView.visibility = View.INVISIBLE
                buttonClearHome.visibility = View.INVISIBLE
            }
        })
        buttonClearHome.visibility = View.INVISIBLE
    }

    private fun initSetHomeLocationButton() {
        buttonSetHomeLocation.setOnClickListener(View.OnClickListener {
            val curLocationStr = sp.getString(KEY_CUR_LOCATION_SP, null)
            if (curLocationStr == null) {
                Log.e(
                    TAG, "current location is empty while set home location"
                )
            }
            sp.edit().putString(KEY_USER_HOME_LOCATION_SP, curLocationStr).apply()

            val homeLocation = Gson().fromJson(curLocationStr, LocationInfo::class.java)
            setUserHomeText(homeLocation)
            buttonClearHome.visibility = View.VISIBLE
            userHomeLocationTextView.visibility = View.VISIBLE
        })
        buttonSetHomeLocation.visibility = View.INVISIBLE
        userHomeLocationTextView.visibility = View.INVISIBLE
    }

    fun setViewOnStartTracking() {
        buttonLocation.text = BUTTON_START_TEXT
        buttonLocation.setBackgroundColor(ContextCompat.getColor(this,R.color.colorRed))
    }

    private fun setButtonEndTrackView() {
        buttonLocation.text = BUTTON_END_TEXT
        buttonLocation.setBackgroundColor(ContextCompat.getColor(this,R.color.colorGreen))
    }

    fun setViewOnNewLocation() {
        val currentLocationStr = sp.getString(KEY_CUR_LOCATION_SP, null)
        checkValidValueSP(currentLocationStr, KEY_CUR_LOCATION_SP)
        val currentLocation = Gson().fromJson(
            sp.getString(KEY_CUR_LOCATION_SP, null),
            LocationInfo::class.java
        )

        setLocation(currentLocation)
        lastLocationTextView.visibility = View.VISIBLE
        setVisibleHomeLocationButton(currentLocation)

    }

    private fun setLocation(location: LocationInfo) {
        val newLocText = "Your location:\n" +
                "latitude:${location.getLatitudeStr()}\nlongitude:${location.getLongitudeStr()
                }\naccuracy${location.getAccuracyStr()}"
        lastLocationTextView.text = newLocText
    }

    fun setViewOnEndTracking() {
        setButtonEndTrackView()
        lastLocationTextView.visibility = View.INVISIBLE
        buttonSetHomeLocation.visibility = View.INVISIBLE
    }

    private fun initBroadcastReceiver() {
        this.locationBroadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                when (intent?.action) {
                    START_TRACK_LOC -> {
                        setViewOnStartTracking()
                        Log.d(TAG, "Start Tracking")
                    }
                    NEW_LOCATION -> {
                        setViewOnNewLocation()
                        Log.d(TAG, "NEW_LOCATION Tracking")
                    }
                    END_TRACK_LOC -> {
                        setViewOnEndTracking()
                        Log.d(TAG, "End Tracking")
                    }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(START_TRACK_LOC)
        intentFilter.addAction(END_TRACK_LOC)
        intentFilter.addAction(NEW_LOCATION)
        this.registerReceiver(locationBroadCastReceiver, intentFilter)
    }


    /* Permissions*/

    /**
     * This method will tell us whether or not the user grant us to access ACCESS_COARSE_LOCATION
     * and ACCESS_FINE_LOCATION.
     */
    private fun checkManifestLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    /**
     * This method will request our necessary permissions to the user if they are not
     * already granted.
     */
    private fun requestLocationPermissions() {
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
     * So it will help us to move forward if the permissions are granted.*/
    @Override
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_LOCATION_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.
                    PERMISSION_GRANTED) {
                    // Granted. Start getting the location information
                    buttonLocation.performClick()
                } else {
                    // the user has denied our request! =-O
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) || ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        // reached here? means we asked the user for this permission more than once,
                        // and they still refuse. This would be a good time to open up a dialog
                        // explaining why we need this permission
                        Toast.makeText(
                            applicationContext,
                            "App can't operate without location permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            PERMISSION_SMS_ID -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    /*when the user accept our request for sms send permission
                    he need to clicked again on the button that open up a sms dialog explaining*/
                    return
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.SEND_SMS
                        )
                    ) {
                        Toast.makeText(
                            applicationContext,
                            SMS_DIALOG_EXPLAINING, Toast.LENGTH_LONG
                        ).show()
                    }
                }
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

    private fun checkLocationPermissions() {
        if (checkManifestLocationPermissions()) {
            if (isLocationEnabled()) {
                val isTra = sp.getBoolean(KEY_IS_TRACKING_ON_SP, false)
                if (isTra) {
                    locationTracker.stopTracking()
                    //button invisibility handle in BroadcastReceiver
                } else {
                    locationTracker.startTracking()
                    //button visibility handle in BroadcastReceiver
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestLocationPermissions()
        }
    }


    /* HOME LOCATION*/

    private fun checkUserHomeLocation() {
        if (sp.contains(KEY_USER_HOME_LOCATION_SP)) {
            val userHomeStr = sp.getString(KEY_USER_HOME_LOCATION_SP, null)
            checkValidValueSP(userHomeStr, "userHomeStr")
            val userHomeLoc = Gson().fromJson(userHomeStr, LocationInfo::class.java)
            setUserHomeText(userHomeLoc)
            buttonClearHome.visibility = View.VISIBLE
        } else {
            userHomeLocationTextView.visibility = View.INVISIBLE
        }
    }

    private fun checkValidValueSP(str: String?, name_val: String) {
        if (str == null || str == "") { //safe check
            Log.e(TAG, "$name_val is null or empty")
            return
        }
    }

    private fun checkPhoneNum() {
        if (sp.getString(KEY_PHONE_NUMBER_SP, null) != null
        ) {
            testSmsButton.visibility = View.VISIBLE
            deletePhoneNumButton.visibility = View.VISIBLE
        } else {
            testSmsButton.visibility = View.INVISIBLE
            deletePhoneNumButton.visibility = View.INVISIBLE
        }
    }

    private fun setUserHomeText(userHomeLoc: LocationInfo) {
        val userHomeText = "your home location is defined as:\n" + "<latitude:" +
                "${userHomeLoc.getLatitudeStr()}, longitude:${userHomeLoc.getLongitudeStr()}>"
        userHomeLocationTextView.text = userHomeText
        userHomeLocationTextView.visibility = View.VISIBLE
    }

    private fun setVisibleHomeLocationButton(currentLocation: LocationInfo) {
        if (currentLocation.getAccuracy() < NUM_TO_PRESENT_HOME_BUTTON) {
            buttonSetHomeLocation.visibility = View.VISIBLE
        } else {
            buttonSetHomeLocation.visibility = View.INVISIBLE
        }
    }

    //sms functions
    private fun initSetSmsPhoneNumButton() {
        setSmsPhoneNumButton.setOnClickListener(View.OnClickListener {
            if (checkSendSmsPermission()) showAddPhoneNumDialog(this)
        })
    }

    private fun checkValidPhoneNum(phoneNum: String?): Boolean {
        return phoneNum != null && phoneNum != "" && phoneNum.isNotEmpty() && phoneNum.length > 1
                && (phoneNum[0].isDigit() || phoneNum[0] == '+')
                && phoneNum.substring(1).isDigitsOnly()
    }

    private fun addPhoneNumToSp(phoneNum: String) {
        // check for invalid value(not number ...)
        if (checkValidPhoneNum(phoneNum)
        ) {
            sp.edit().putString(KEY_PHONE_NUMBER_SP, phoneNum).apply()
            testSmsButton.visibility = View.VISIBLE
            deletePhoneNumButton.visibility = View.VISIBLE
        } else {
            testSmsButton.visibility = View.INVISIBLE
            deletePhoneNumButton.visibility = View.INVISIBLE
        }
    }

    private fun checkSendSmsPermission(): Boolean {
        val hasSmsPermission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) ==
                    PackageManager.PERMISSION_GRANTED
        if (hasSmsPermission) {
            return true
        }
        //requestSMSPermissions
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.SEND_SMS),
            PERMISSION_SMS_ID
        );
        return false
    }



    private fun showAddPhoneNumDialog(c: Context) {
        val inputText = EditText(c)
        inputText.setRawInputType(InputType.TYPE_CLASS_PHONE)
        val dialog = AlertDialog.Builder(c)
            .setView(inputText)
            .setPositiveButton("Add") { _, _ ->
                val input = inputText.text.toString()
                addPhoneNumToSp(input)
            }
            .setNegativeButton("Cancel", null)
            .setTitle("Add a phone number")
        val phoneNum = sp.getString(KEY_PHONE_NUMBER_SP, null)
        if (phoneNum != null) { //phone num clear or never insert
            dialog.setMessage("Store Phone number is: $phoneNum")
        }
        dialog.create()
        dialog.show()
    }


    private fun initTestSmsButton() {
        testSmsButton.setOnClickListener(View.OnClickListener {
            if(checkSendSmsPermission()) {
                val intent = Intent(SEND_SMS_ACTION)
                intent.putExtra(PHONE_NUM_INTENT, sp.getString(KEY_PHONE_NUMBER_SP, null))
                intent.putExtra(CONTEXT_INTENT, SMS_MESSAGE)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
        })
        testSmsButton.visibility = View.INVISIBLE
    }

    private fun initDelPhoneNumButton() {
        deletePhoneNumButton.setOnClickListener(View.OnClickListener {
            if (sp.contains(KEY_PHONE_NUMBER_SP)) {
                sp.edit().remove(KEY_PHONE_NUMBER_SP).apply()
                deletePhoneNumButton.visibility = View.INVISIBLE
                testSmsButton.visibility = View.INVISIBLE
            }
        })
        deletePhoneNumButton.visibility = View.INVISIBLE
    }
}
