package com.alvarosega.trackingventas.data.location

import android.location.Location

interface LocationClient {
    suspend fun getCurrentLocation(): Location?
    fun isGpsEnabled(): Boolean
}