package com.openbimmer.idrive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.openbimmer.idrive.accessory.BtAccessorySocket
import com.openbimmer.idrive.bcl.BclConnection
import com.openbimmer.idrive.carcloud.CarCloudRouteImporter
import com.openbimmer.idrive.cds.CdsDataHub
import com.openbimmer.idrive.cds.VehicleTelemetry

class FixedBtService : Service() {
    companion object {
        private const val TAG = "FixedBtService"
        private const val NOTIFICATION_ID = 4040
        private const val CHANNEL_ID = "bmw_bt_service_channel"

        const val EXTRA_DEVICE = "extra_device"
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"

        var instance: FixedBtService? = null
            private set
    }

    private var btSocket: BtAccessorySocket? = null
    private var bclConnection: BclConnection? = null
    private var cdsDataHub: CdsDataHub? = null
    var routeImporter: CarCloudRouteImporter? = null
        private set
    var virtualScreenManager: com.openbimmer.idrive.rhmi.RhmiVirtualScreenManager? = null
        private set

    val isConnected: Boolean
        get() = btSocket?.isConnected == true

    var onTelemetryCallback: ((VehicleTelemetry) -> Unit)? = null
    var onConnectionStateCallback: ((String) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        cdsDataHub = CdsDataHub { telemetry ->
            onTelemetryCallback?.invoke(telemetry)
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification("Connecting to BMW iDrive..."))

        val device: BluetoothDevice? = intent?.getParcelableExtra(EXTRA_DEVICE)
        if (device != null) {
            connectToDevice(device)
        }

        return START_STICKY
    }

    fun connectToDevice(device: BluetoothDevice) {
        onConnectionStateCallback?.invoke("CONNECTING")
        btSocket?.disconnect()
        btSocket = BtAccessorySocket(
            device = device,
            onConnected = { inputStream, outputStream ->
                onConnectionStateCallback?.invoke("SPP_CONNECTED")
                bclConnection = BclConnection(
                    inputStream = inputStream,
                    outputStream = outputStream,
                    onPacketReceived = { packet ->
                        if (packet.src.toInt() == 1) { // Channel 1: CDS
                            cdsDataHub?.parseCdsPayload(packet.payload)
                        }
                    },
                    onHandshakeComplete = {
                        routeImporter = CarCloudRouteImporter(bclConnection!!)
                        virtualScreenManager = com.openbimmer.idrive.rhmi.RhmiVirtualScreenManager(bclConnection!!)
                        onConnectionStateCallback?.invoke("CAR_READY")
                        updateNotification("Connected to BMW iDrive (Offline Active)")
                    },
                    onError = { err ->
                        onConnectionStateCallback?.invoke("ERROR: $err")
                    }
                )
                bclConnection?.startHandshake()
            },
            onDisconnected = { err ->
                onConnectionStateCallback?.invoke("DISCONNECTED: $err")
                updateNotification("Disconnected from vehicle")
            }
        )
        btSocket?.connectAsync()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BMW In-Car Link",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenBimmer Offline Link")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onDestroy() {
        bclConnection?.stop()
        btSocket?.disconnect()
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
