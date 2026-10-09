package com.openbimmer.idrive.accessory

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BtAccessorySocket(
    private val device: BluetoothDevice,
    private val onConnected: (InputStream, OutputStream) -> Unit,
    private val onDisconnected: (String) -> Unit
) {
    companion object {
        private const val TAG = "BtAccessorySocket"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 1000L
    }

    private var socket: BluetoothSocket? = null
    private var isRunning = false
    private var workerThread: Thread? = null

    fun connectAsync() {
        if (isRunning) return
        isRunning = true
        workerThread = Thread {
            var attempts = 0
            var connected = false

            while (isRunning && attempts < MAX_RETRIES && !connected) {
                attempts++
                try {
                    Log.d(TAG, "Attempting SPP RFCOMM connection to ${device.address} (Attempt $attempts/$MAX_RETRIES)")
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    socket?.connect()

                    if (socket?.isConnected == true) {
                        Log.i(TAG, "Connection established and SPP data link opened with BMW iDrive!")
                        connected = true
                        val inputStream = socket!!.inputStream
                        val outputStream = socket!!.outputStream
                        onConnected(inputStream, outputStream)
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "RFCOMM connect attempt $attempts failed: ${e.message}")
                    closeSocket()
                    if (attempts < MAX_RETRIES && isRunning) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS)
                        } catch (ie: InterruptedException) {
                            break
                        }
                    }
                }
            }

            if (!connected && isRunning) {
                Log.e(TAG, "Failed to connect to car after $MAX_RETRIES attempts")
                onDisconnected("Failed to connect via Bluetooth SPP after $MAX_RETRIES attempts")
            }
        }.apply { start() }
    }

    fun disconnect() {
        isRunning = false
        closeSocket()
        workerThread?.interrupt()
        workerThread = null
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        } finally {
            socket = null
        }
    }
}
