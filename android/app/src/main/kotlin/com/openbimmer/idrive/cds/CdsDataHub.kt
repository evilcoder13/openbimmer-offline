package com.openbimmer.idrive.cds

import android.util.Log
import org.json.JSONObject

data class VehicleTelemetry(
    val vin: String = "",
    val model: String = "",
    val speedKmh: Double = 0.0,
    val fuelLiters: Double = 0.0,
    val fuelPercent: Int = 0,
    val batteryPercent: Int = 0,
    val odometerKm: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val parkingBrake: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "vin" to vin,
            "model" to model,
            "speedKmh" to speedKmh,
            "fuelLiters" to fuelLiters,
            "fuelPercent" to fuelPercent,
            "batteryPercent" to batteryPercent,
            "odometerKm" to odometerKm,
            "latitude" to latitude,
            "longitude" to longitude,
            "parkingBrake" to parkingBrake,
            "timestamp" to timestamp
        )
    }
}

class CdsDataHub(
    private val onTelemetryUpdated: (VehicleTelemetry) -> Unit
) {
    companion object {
        private const val TAG = "CdsDataHub"
    }

    private var currentTelemetry = VehicleTelemetry()

    fun parseCdsPayload(payload: ByteArray) {
        try {
            val jsonString = String(payload, Charsets.UTF_8).trim()
            if (!jsonString.startsWith("{")) return

            val json = JSONObject(jsonString)
            Log.d(TAG, "Received CDS payload: $jsonString")

            var updated = currentTelemetry

            if (json.has("driving.speed")) {
                val speed = json.getJSONObject("driving.speed").optDouble("speed", updated.speedKmh)
                updated = updated.copy(speedKmh = speed)
            }
            if (json.has("driving.odometer")) {
                val odo = json.getJSONObject("driving.odometer").optLong("odometer", updated.odometerKm)
                updated = updated.copy(odometerKm = odo)
            }
            if (json.has("fuel.level")) {
                val fuel = json.getJSONObject("fuel.level")
                val liters = fuel.optDouble("liters", updated.fuelLiters)
                val percent = fuel.optInt("percent", updated.fuelPercent)
                updated = updated.copy(fuelLiters = liters, fuelPercent = percent)
            }
            if (json.has("battery.level")) {
                val battery = json.getJSONObject("battery.level").optInt("percent", updated.batteryPercent)
                updated = updated.copy(batteryPercent = battery)
            }
            if (json.has("navigation.gpsPosition")) {
                val gps = json.getJSONObject("navigation.gpsPosition")
                val lat = gps.optDouble("latitude", updated.latitude)
                val lng = gps.optDouble("longitude", updated.longitude)
                updated = updated.copy(latitude = lat, longitude = lng)
            }
            if (json.has("parkingBrakeStatus")) {
                val pb = json.optBoolean("parkingBrakeStatus", updated.parkingBrake)
                updated = updated.copy(parkingBrake = pb)
            }
            if (json.has("vehicle.identification")) {
                val idObj = json.getJSONObject("vehicle.identification")
                val vin = idObj.optString("vin", updated.vin)
                val model = idObj.optString("model", updated.model)
                updated = updated.copy(vin = vin, model = model)
            }

            currentTelemetry = updated
            onTelemetryUpdated(currentTelemetry)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse CDS payload: ${e.message}")
        }
    }
}
