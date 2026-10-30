package com.example.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LocationHelper {
    /**
     * Calculates distance in meters between two GPS coordinates using the Haversine formula.
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Formats distance into human-readable string (feet/miles).
     */
    fun formatDistance(meters: Double): String {
        val feet = meters * 3.28084
        return when {
            feet < 1000 -> "${feet.toInt()} ft away"
            else -> {
                val miles = meters / 1609.34
                String.format(java.util.Locale.US, "%.1f mi away", miles)
            }
        }
    }

    /**
     * Calculates bearing in degrees (0..360) from point 1 to point 2.
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    /**
     * Launches external GPS navigation (Google Maps / system navigator) to the vehicle location.
     */
    fun openNavigationDirections(context: android.content.Context, lat: Double, lng: Double, label: String = "Vehicle Location") {
        try {
            val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(${android.net.Uri.encode(label)})")
            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // Fallback to web browser Google Maps
            val webUri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
            val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, webUri).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Fetches current real hardware GPS coordinates using Google Play Services Fused Location.
     */
    fun fetchDeviceLocation(
        context: android.content.Context,
        onSuccess: (Double, Double) -> Unit,
        onFailure: () -> Unit = {}
    ) {
        try {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        onSuccess(loc.latitude, loc.longitude)
                    } else {
                        onFailure()
                    }
                }
                .addOnFailureListener {
                    onFailure()
                }
        } catch (_: SecurityException) {
            onFailure()
        } catch (_: Exception) {
            onFailure()
        }
    }
}
