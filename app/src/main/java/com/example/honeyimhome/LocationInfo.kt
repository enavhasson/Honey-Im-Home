package com.example.honeyimhome

class LocationInfo(
    private var latitude: Double,
    private var longitude: Double,
    private var accuracy: Float
) {
    fun getLatitudeStr(): String {
        return latitude.toString()
    }

    fun getLongitudeStr(): String {
        return longitude.toString()
    }

    fun getAccuracyStr(): String {
        return accuracy.toString()
    }

    fun getAccuracy(): Float {
        return accuracy
    }

    fun getLatitude(): Double {
        return latitude
    }

    fun getLongitude(): Double {
        return longitude
    }

}