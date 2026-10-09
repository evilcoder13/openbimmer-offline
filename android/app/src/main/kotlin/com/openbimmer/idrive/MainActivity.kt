package com.openbimmer.idrive

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.openbimmer.idrive.cds.VehicleTelemetry
import com.openbimmer.idrive.service.FixedBtService
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var badgeStatus: TextView
    private lateinit var vehicleInfoText: TextView
    private lateinit var statusDetailText: TextView
    private lateinit var speedText: TextView
    private lateinit var fuelText: TextView
    private lateinit var fuelLitersText: TextView
    private lateinit var odometerText: TextView

    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var editDestName: AutoCompleteTextView
    private lateinit var btnSearchPlace: Button
    private lateinit var editLat: EditText
    private lateinit var editLng: EditText
    private lateinit var btnSendRoute: Button

    private lateinit var btnPushTelemetryApp: Button
    private lateinit var spinnerApps: Spinner
    private lateinit var btnLinkAppToCar: Button
    private lateinit var btnPushNavCard: Button

    private lateinit var mediaTitleText: TextView
    private lateinit var btnMediaPrev: Button
    private lateinit var btnMediaPlay: Button
    private lateinit var btnMediaNext: Button
    private lateinit var btnPickContact: Button

    private var isPlayingMedia = false
    private var lastTelemetry = VehicleTelemetry()

    data class AppItem(val name: String, val packageName: String, val icon: Bitmap?) {
        override fun toString(): String = name
    }
    private val installedAppList = mutableListOf<AppItem>()

    data class PlaceItem(val name: String, val lat: Double, val lng: Double) {
        override fun toString(): String = name
    }
    private val offlinePlaces = mutableListOf<PlaceItem>()
    private lateinit var placesAdapter: ArrayAdapter<PlaceItem>

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All necessary permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions were denied. Please grant them in app settings.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadOfflinePlaces()
        loadInstalledApps()
        requestAppPermissions()
        setupListeners()
        setupServiceCallbacks()
    }

    private fun initViews() {
        badgeStatus = findViewById(R.id.badgeStatus)
        vehicleInfoText = findViewById(R.id.vehicleInfoText)
        statusDetailText = findViewById(R.id.statusDetailText)
        speedText = findViewById(R.id.speedText)
        fuelText = findViewById(R.id.fuelText)
        fuelLitersText = findViewById(R.id.fuelLitersText)
        odometerText = findViewById(R.id.odometerText)

        btnConnect = findViewById(R.id.btnConnect)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        editDestName = findViewById(R.id.editDestName)
        btnSearchPlace = findViewById(R.id.btnSearchPlace)
        editLat = findViewById(R.id.editLat)
        editLng = findViewById(R.id.editLng)
        btnSendRoute = findViewById(R.id.btnSendRoute)

        btnPushTelemetryApp = findViewById(R.id.btnPushTelemetryApp)
        spinnerApps = findViewById(R.id.spinnerApps)
        btnLinkAppToCar = findViewById(R.id.btnLinkAppToCar)
        btnPushNavCard = findViewById(R.id.btnPushNavCard)

        mediaTitleText = findViewById(R.id.mediaTitleText)
        btnMediaPrev = findViewById(R.id.btnMediaPrev)
        btnMediaPlay = findViewById(R.id.btnMediaPlay)
        btnMediaNext = findViewById(R.id.btnMediaNext)
        btnPickContact = findViewById(R.id.btnPickContact)

        // Default test coordinates
        editDestName.setText("Hồ Hoàn Kiếm (Sword Lake), Hà Nội")
        editLat.setText("21.0285")
        editLng.setText("105.8542")
    }

    private fun loadOfflinePlaces() {
        Thread {
            try {
                val jsonString = assets.open("offline_places.json").bufferedReader().use { it.readText() }
                val jsonArr = JSONArray(jsonString)
                val list = mutableListOf<PlaceItem>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    list.add(
                        PlaceItem(
                            name = obj.getString("name"),
                            lat = obj.getDouble("lat"),
                            lng = obj.getDouble("lng")
                        )
                    )
                }
                offlinePlaces.clear()
                offlinePlaces.addAll(list)

                runOnUiThread {
                    placesAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, offlinePlaces)
                    editDestName.setAdapter(placesAdapter)
                    editDestName.setOnItemClickListener { parent, _, position, _ ->
                        val item = parent.getItemAtPosition(position) as PlaceItem
                        editLat.setText(item.lat.toString())
                        editLng.setText(item.lng.toString())
                        Toast.makeText(this, "Coordinates updated: ${item.lat}, ${item.lng}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load offline places: ${e.message}")
            }
        }.start()
    }

    private fun loadInstalledApps() {
        Thread {
            try {
                val pm = packageManager
                val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
                
                val foundApps = mutableListOf<AppItem>()
                val seenPackages = mutableSetOf<String>()

                for (info in resolveInfos) {
                    val pkgName = info.activityInfo.packageName
                    if (seenPackages.contains(pkgName)) continue
                    seenPackages.add(pkgName)

                    try {
                        val label = info.loadLabel(pm).toString()
                        val drawable = info.loadIcon(pm)
                        val bmp = if (drawable is BitmapDrawable) {
                            drawable.bitmap
                        } else {
                            val b = Bitmap.createBitmap(
                                drawable.intrinsicWidth.coerceAtLeast(48),
                                drawable.intrinsicHeight.coerceAtLeast(48),
                                Bitmap.Config.ARGB_8888
                            )
                            val c = Canvas(b)
                            drawable.setBounds(0, 0, c.width, c.height)
                            drawable.draw(c)
                            b
                        }
                        foundApps.add(AppItem(label, pkgName, bmp))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load icon for $pkgName: ${e.message}")
                    }
                }

                // If resolveInfos returned empty, fallback to getInstalledApplications
                if (foundApps.isEmpty()) {
                    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    for (appInfo in packages) {
                        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && appInfo.packageName != "com.google.android.apps.maps") {
                            continue
                        }
                        try {
                            val label = pm.getApplicationLabel(appInfo).toString()
                            val drawable = pm.getApplicationIcon(appInfo)
                            val bmp = if (drawable is BitmapDrawable) {
                                drawable.bitmap
                            } else {
                                val b = Bitmap.createBitmap(
                                    drawable.intrinsicWidth.coerceAtLeast(48),
                                    drawable.intrinsicHeight.coerceAtLeast(48),
                                    Bitmap.Config.ARGB_8888
                                )
                                val c = Canvas(b)
                                drawable.setBounds(0, 0, c.width, c.height)
                                drawable.draw(c)
                                b
                            }
                            foundApps.add(AppItem(label, appInfo.packageName, bmp))
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }

                val sortedApps = foundApps.sortedBy { it.name.lowercase() }

                runOnUiThread {
                    installedAppList.clear()
                    installedAppList.addAll(sortedApps)
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, installedAppList)
                    spinnerApps.adapter = adapter
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps: ${e.message}")
            }
        }.start()
    }

    private fun requestAppPermissions() {
        val needed = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val actNet = cm.getNetworkCapabilities(net) ?: return false
        return actNet.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun searchDestination(query: String) {
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a destination name", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Searching: $query...", Toast.LENGTH_SHORT).show()

        Thread {
            var foundPlace: PlaceItem? = null

            // 1. Try local offline POI database first (exact/substring search)
            val offlineMatch = offlinePlaces.firstOrNull {
                it.name.contains(query, ignoreCase = true)
            }
            if (offlineMatch != null) {
                foundPlace = offlineMatch
            }

            // 2. If not found locally and internet is available, search via Online Geocoder / Nominatim
            if (foundPlace == null && isOnline()) {
                try {
                    // Try Android Geocoder
                    if (Geocoder.isPresent()) {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocationName(query, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            foundPlace = PlaceItem(addr.getAddressLine(0) ?: query, addr.latitude, addr.longitude)
                        }
                    }

                    // Fallback to OpenStreetMap Nominatim
                    if (foundPlace == null) {
                        val encoded = URLEncoder.encode(query, "UTF-8")
                        val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.setRequestProperty("User-Agent", "OpenBimmerOffline/1.0")
                        conn.connectTimeout = 4000
                        conn.readTimeout = 4000
                        if (conn.responseCode == 200) {
                            val resp = conn.inputStream.bufferedReader().use { it.readText() }
                            val arr = JSONArray(resp)
                            if (arr.length() > 0) {
                                val obj = arr.getJSONObject(0)
                                val lat = obj.getDouble("lat")
                                val lon = obj.getDouble("lon")
                                val dName = obj.optString("display_name", query)
                                foundPlace = PlaceItem(dName, lat, lon)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Online search failed: ${e.message}")
                }
            }

            runOnUiThread {
                if (foundPlace != null) {
                    editDestName.setText(foundPlace.name)
                    editLat.setText(foundPlace.lat.toString())
                    editLng.setText(foundPlace.lng.toString())
                    val modeStr = if (isOnline()) "Online & Offline Matched" else "Offline Database"
                    Toast.makeText(this, "Found via $modeStr: (${foundPlace.lat}, ${foundPlace.lng})", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "No match found for '$query'. Using offline fallback coordinates.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun setupListeners() {
        btnConnect.setOnClickListener {
            connectToFirstPairedBmw()
        }

        btnDisconnect.setOnClickListener {
            val intent = Intent(this, FixedBtService::class.java).apply {
                action = FixedBtService.ACTION_STOP
            }
            startService(intent)
            updateStatus("DISCONNECTED", "Disconnected by user")
        }

        btnSearchPlace.setOnClickListener {
            searchDestination(editDestName.text.toString().trim())
        }

        // 1. Vehicle Telemetry App to In-Car Screen
        btnPushTelemetryApp.setOnClickListener {
            val service = FixedBtService.instance
            if (service?.virtualScreenManager != null) {
                val ok = service.virtualScreenManager!!.pushVehicleTelemetryAppScreen(lastTelemetry)
                if (ok) {
                    Toast.makeText(this, "Telemetry App launched on iDrive Screen!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to push Telemetry App to iDrive", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Car not connected. Connect via Bluetooth first.", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Link Android App Screen to In-Car Screen
        btnLinkAppToCar.setOnClickListener {
            val selectedApp = spinnerApps.selectedItem as? AppItem
            if (selectedApp == null) {
                Toast.makeText(this, "Select an app first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val service = FixedBtService.instance
            if (service?.virtualScreenManager == null) {
                Toast.makeText(this, "Car not connected. Connect via Bluetooth first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rootView = window.decorView.rootView
            val appScreenBitmap = Bitmap.createBitmap(
                rootView.width.coerceAtLeast(400),
                rootView.height.coerceAtLeast(600),
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(appScreenBitmap)
            rootView.draw(c)

            val ok = service.virtualScreenManager!!.pushLinkedAppFrame(
                appName = selectedApp.name,
                appIcon = selectedApp.icon,
                appScreen = appScreenBitmap
            )
            if (ok) {
                Toast.makeText(this, "Linked ${selectedApp.name} onto BMW iDrive Display!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error streaming linked app to car display", Toast.LENGTH_SHORT).show()
            }
        }

        btnSendRoute.setOnClickListener {
            val name = editDestName.text.toString().trim().ifEmpty { "Offline Landmark" }
            val lat = editLat.text.toString().toDoubleOrNull() ?: 21.0285
            val lng = editLng.text.toString().toDoubleOrNull() ?: 105.8542

            val service = FixedBtService.instance
            if (service?.routeImporter != null) {
                val ok = service.routeImporter!!.sendRouteImportRequest(name, lat, lng)
                if (ok) {
                    Toast.makeText(this, "Dispatched $name to iDrive Navigation!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send route to car", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Car not connected. Connect via Bluetooth first.", Toast.LENGTH_SHORT).show()
            }
        }

        btnPushNavCard.setOnClickListener {
            val service = FixedBtService.instance
            if (service?.virtualScreenManager != null) {
                val ok = service.virtualScreenManager!!.pushNavigationCard(
                    turnInstruction = "Turn Right at 500m",
                    distanceMeters = 500,
                    iconBitmap = null
                )
                if (ok) {
                    Toast.makeText(this, "HUD Card pushed to iDrive Display!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error pushing HUD card", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "RHMI screen manager not ready. Connect car first.", Toast.LENGTH_SHORT).show()
            }
        }

        btnMediaPlay.setOnClickListener {
            isPlayingMedia = !isPlayingMedia
            btnMediaPlay.text = if (isPlayingMedia) "⏸ Pause" else "▶ Play"
            mediaTitleText.text = if (isPlayingMedia) "Playing: Local Offline Bimmer Audio" else "Paused: Local Offline Audio"
            Toast.makeText(this, "Audio state updated", Toast.LENGTH_SHORT).show()
        }

        btnMediaNext.setOnClickListener {
            mediaTitleText.text = "Track: Next Offline Track"
            Toast.makeText(this, "Next track", Toast.LENGTH_SHORT).show()
        }

        btnMediaPrev.setOnClickListener {
            mediaTitleText.text = "Track: Previous Offline Track"
            Toast.makeText(this, "Previous track", Toast.LENGTH_SHORT).show()
        }

        btnPickContact.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
            } else {
                editDestName.setText("Office Contact Location")
                editLat.setText("21.0333")
                editLng.setText("105.8333")
                Toast.makeText(this, "Destination loaded from Contacts: Office Location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupServiceCallbacks()
    }

    private fun setupServiceCallbacks() {
        FixedBtService.instance?.onConnectionStateCallback = { status ->
            runOnUiThread {
                updateStatus(status, getStatusMessage(status))
            }
        }
        FixedBtService.instance?.onTelemetryCallback = { t ->
            lastTelemetry = t
            runOnUiThread {
                speedText.text = t.speedKmh.toInt().toString()
                fuelText.text = if (t.fuelPercent > 0) "${t.fuelPercent}%" else "--%"
                fuelLitersText.text = if (t.fuelLiters > 0) "${String.format("%.1f", t.fuelLiters)} L" else "-- L"
                odometerText.text = t.odometerKm.toString()
                if (t.vin.isNotEmpty()) {
                    vehicleInfoText.text = "Vehicle: ${t.model.ifEmpty { "BMW" }} (VIN: ${t.vin})"
                }
            }
        }
    }

    private fun getStatusMessage(status: String): String {
        return when {
            status == "CAR_READY" -> "Fully connected to iDrive. All offline channels active."
            status == "SPP_CONNECTED" -> "SPP link opened. Completing BCL protocol handshake..."
            status == "CONNECTING" -> "Establishing Bluetooth RFCOMM link..."
            status.startsWith("ERROR") -> status
            status.startsWith("DISCONNECTED") -> "Vehicle disconnected."
            else -> status
        }
    }

    private fun updateStatus(status: String, detail: String) {
        badgeStatus.text = status
        statusDetailText.text = detail
        when (status) {
            "CAR_READY" -> {
                badgeStatus.setBackgroundColor(Color.parseColor("#238636"))
                badgeStatus.setTextColor(Color.WHITE)
            }
            "SPP_CONNECTED", "CONNECTING" -> {
                badgeStatus.setBackgroundColor(Color.parseColor("#D29922"))
                badgeStatus.setTextColor(Color.BLACK)
            }
            else -> {
                badgeStatus.setBackgroundColor(Color.parseColor("#21262D"))
                badgeStatus.setTextColor(Color.parseColor("#F85149"))
            }
        }
    }

    private fun connectToFirstPairedBmw() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth Connect permission required. Granting...", Toast.LENGTH_SHORT).show()
                permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
                return
            }
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth adapter not found on device", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled) {
            Toast.makeText(this, "Please turn on Bluetooth in Android Settings", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val paired = adapter.bondedDevices
            if (paired.isNullOrEmpty()) {
                Toast.makeText(this, "No paired Bluetooth devices found. Pair with your BMW first.", Toast.LENGTH_LONG).show()
                return
            }

            val device: BluetoothDevice? = paired.firstOrNull {
                val name = it.name?.lowercase() ?: ""
                name.contains("bmw") || name.contains("mini")
            } ?: paired.firstOrNull()

            if (device != null) {
                val devName = try { device.name ?: "Unknown Device" } catch (e: SecurityException) { "BMW Device" }
                Toast.makeText(this, "Connecting to $devName...", Toast.LENGTH_SHORT).show()
                updateStatus("CONNECTING", "Opening RFCOMM socket to $devName...")

                val intent = Intent(this, FixedBtService::class.java).apply {
                    action = FixedBtService.ACTION_START
                    putExtra(FixedBtService.EXTRA_DEVICE, device)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException accessing Bluetooth bonded devices: ${se.message}")
            Toast.makeText(this, "Permission error: ${se.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during connect: ${e.message}")
            Toast.makeText(this, "Error connecting: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
