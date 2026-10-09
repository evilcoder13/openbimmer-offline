package com.openbimmer.idrive.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.openbimmer.idrive.service.FixedBtService

class A4aBluetoothBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "A4aBtReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Bluetooth broadcast received: $action")

        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        val deviceName = device?.name?.lowercase() ?: ""
        val isBmwOrMini = deviceName.contains("bmw") || deviceName.contains("mini")

        if (device != null && (isBmwOrMini || deviceName.isEmpty())) {
            when (action) {
                "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED",
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.i(TAG, "BMW/MINI vehicle detected: ${device.name} (${device.address}). Starting FixedBtService...")
                    val serviceIntent = Intent(context, FixedBtService::class.java).apply {
                        this.action = FixedBtService.ACTION_START
                        putExtra(FixedBtService.EXTRA_DEVICE, device)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Log.i(TAG, "BMW/MINI vehicle disconnected: ${device.name}")
                    val serviceIntent = Intent(context, FixedBtService::class.java).apply {
                        this.action = FixedBtService.ACTION_STOP
                    }
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
