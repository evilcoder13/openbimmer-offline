package com.openbimmer.idrive.carcloud

import android.util.Log
import com.openbimmer.idrive.bcl.BclConnection
import org.json.JSONObject

class CarCloudRouteImporter(
    private val bclConnection: BclConnection
) {
    companion object {
        private const val TAG = "CarCloudRouteImporter"
        private const val CARCLOUD_CHANNEL: Short = 3
    }

    fun sendRouteImportRequest(destinationName: String, latitude: Double, longitude: Double): Boolean {
        return try {
            val messageId = System.currentTimeMillis().toString()
            val json = JSONObject().apply {
                put("type", "route_import_request")
                put("messageId", messageId)
                put("finalDestination", JSONObject().apply {
                    put("id", messageId)
                    put("name", destinationName)
                    put("latitude", latitude)
                    put("longitude", longitude)
                })
            }

            val payloadBytes = json.toString().toByteArray(Charsets.UTF_8)
            Log.i(TAG, "Direct Route Import dispatched over Bluetooth to iDrive: $destinationName ($latitude, $longitude)")
            bclConnection.sendData(CARCLOUD_CHANNEL, payloadBytes)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send route import request: ${e.message}")
            false
        }
    }
}
