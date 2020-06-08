package com.example.honeyimhome

class LocationInfo(var latitude:Double ,var longitude :Double,var accuracy:Float) {
    fun getLatitudeStr(): String {
        return latitude.toString()
    }

    fun getLongitudeStr(): String {
        return longitude.toString()
    }

    fun getAccuracyStr(): String {
        return accuracy.toString()
    }

    fun getAccuracy(): Float? {
        return accuracy
    }
}