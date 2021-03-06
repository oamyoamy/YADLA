
package com.kludgenics.cgmlogger.extension
/*
import android.location.Location
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest

/**
 * Calculates the error function assuming a normal distribution with mean 0 and standard deviation 1
 */
private fun errorFunction(x: Double) : Double {
    if (x < 0)
        return -errorFunction(-x)
    else
        return 1.0 - Math.pow((1.0 / (1.0 + .278393 * x + 0.230389 * x * x + .000972 * x * x * x + .078108 * x * x * x * x)), 4.0)
}

private fun complimentaryErrorFunction(x: Double): Double {
    return 1 - errorFunction(x)
}

private fun cumulativeDensityFunction(x: Double, mean: Double, sigma: Double): Double {
    return .5 * (complimentaryErrorFunction((mean-x)/(sigma * Math.sqrt(2.0))))
}

public fun Location.probabilityWithin(neighbor: Location) : Double {
    val sigma = this.accuracy.toDouble()
    val distance = this.distanceTo(neighbor).toDouble()
    return 1 - (cumulativeDensityFunction(distance, 0.0, sigma) - cumulativeDensityFunction(-distance, 0.0, sigma))
}

private inline fun Geofence(init: Geofence.Builder.() -> Geofence.Builder?): Geofence? {
    val builder = Geofence.Builder()
    return builder.init()?.build()
}

public fun Geofence.Builder.setCircularRegion(location: Location, radius: Float): Geofence.Builder? {
    return setCircularRegion(location.latitude, location.longitude, radius)
}

public inline fun GeofencingRequest (init: GeofencingRequest.Builder.() -> GeofencingRequest.Builder?): GeofencingRequest? {
    val builder = GeofencingRequest.Builder()
    return builder.init()?.build()
}

public inline fun GeofencingRequest.Builder.Geofence(init: Geofence.Builder.() -> Geofence.Builder?): GeofencingRequest.Builder? {
    val builder = Geofence.Builder()
    this.addGeofence(builder.init()?.build())
    return this
}

public fun GeofencingRequest.Builder.Geofences(geofences: List<Geofence.Builder.() -> Geofence.Builder>): GeofencingRequest.Builder? {
    geofences.forEach {
        val builder = Geofence.Builder()
        this.addGeofence(builder.it().build())
    }
    return this
}

 */
