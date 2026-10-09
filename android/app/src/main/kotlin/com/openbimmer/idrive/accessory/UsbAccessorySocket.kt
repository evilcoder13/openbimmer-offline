package com.openbimmer.idrive.accessory

import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class UsbAccessorySocket(
    private val usbManager: UsbManager,
    private val accessory: UsbAccessory,
    private val onConnected: (InputStream, OutputStream) -> Unit,
    private val onDisconnected: (String) -> Unit
) {
    companion object {
        private const val TAG = "UsbAccessorySocket"
    }

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    private var isRunning = false
    private var monitorThread: Thread? = null

    val isConnected: Boolean
        get() = isRunning && fileDescriptor != null

    fun connect() {
        if (isRunning) return
        try {
            Log.d(TAG, "Opening USB Accessory descriptor: ${accessory.manufacturer} - ${accessory.model}")
            fileDescriptor = usbManager.openAccessory(accessory)
            if (fileDescriptor == null) {
                val err = "Failed to open UsbAccessory ParcelFileDescriptor (permission or busy)"
                Log.e(TAG, err)
                onDisconnected(err)
                return
            }

            val fd: FileDescriptor = fileDescriptor!!.fileDescriptor
            inputStream = FileInputStream(fd)
            outputStream = FileOutputStream(fd)
            isRunning = true

            Log.i(TAG, "BMW USB Accessory connection established successfully via AOA!")
            onConnected(inputStream!!, outputStream!!)

        } catch (e: Exception) {
            Log.e(TAG, "Exception opening USB accessory: ${e.message}", e)
            close()
            onDisconnected("USB Accessory error: ${e.message}")
        }
    }

    fun disconnect() {
        close()
    }

    private fun close() {
        isRunning = false
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}

        inputStream = null
        outputStream = null
        fileDescriptor = null
        monitorThread = null
    }
}
