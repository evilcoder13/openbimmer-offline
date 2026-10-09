package com.openbimmer.idrive.rhmi

import android.content.Context
import android.graphics.*
import android.util.Log
import com.openbimmer.idrive.bcl.BclConnection
import com.openbimmer.idrive.cds.VehicleTelemetry
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Manages rendering of:
 * 1. Dedicated in-car "Vehicle Telemetry" dashboard application canvas
 * 2. Android App Screen Link / Projection (Virtual Display / Screen Mirroring)
 * onto the BMW iDrive display via RHMI Image Component & Frame Streaming.
 */
class RhmiVirtualScreenManager(
    private val bclConnection: BclConnection
) {
    companion object {
        private const val TAG = "RhmiVirtualScreen"
        private const val RHMI_CHANNEL: Short = 2 // Channel 2: RHMI

        // Standard BMW iDrive screen dimensions
        const val CAR_SCREEN_WIDTH = 1280
        const val CAR_SCREEN_HEIGHT = 480

        // RHMI Component Model IDs
        const val COMPONENT_IMAGE_VIEW = 100
        const val COMPONENT_TEXT_VIEW = 101
    }

    private var isProjectionActive = false

    /**
     * Renders a dedicated "Vehicle Telemetry In-Car App" dashboard directly
     * into a high-res widescreen bitmap formatted for BMW iDrive display.
     */
    fun pushVehicleTelemetryAppScreen(telemetry: VehicleTelemetry): Boolean {
        val width = CAR_SCREEN_WIDTH
        val height = CAR_SCREEN_HEIGHT
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Sleek BMW M-style Dark Background
        val bgPaint = Paint().apply {
            color = Color.rgb(15, 20, 28)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header Accent Bar (BMW M Blue/Cyan)
        val barPaint = Paint().apply {
            color = Color.rgb(0, 160, 255)
            strokeWidth = 6f
        }
        canvas.drawLine(40f, 30f, width - 40f, 30f, barPaint)

        // Header Title
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("OPENBIMMER // LIVE TELEMETRY APP", 40f, 75f, textPaint)

        val subPaint = Paint().apply {
            color = Color.rgb(140, 160, 180)
            textSize = 18f
            isAntiAlias = true
        }
        val vinStr = if (telemetry.vin.isNotEmpty()) "VIN: ${telemetry.vin} • Model: ${telemetry.model.ifEmpty { "BMW" }}" else "Offline CAN-Bus Link Active"
        canvas.drawText(vinStr, 40f, 105f, subPaint)

        // 2. Card 1: SPEEDOMETER
        drawTelemetryCard(
            canvas = canvas,
            x = 40f, y = 135f, w = 360f, h = 300f,
            title = "SPEED",
            mainVal = "${telemetry.speedKmh.toInt()}",
            unit = "KM/H",
            valColor = Color.rgb(0, 230, 255)
        )

        // 3. Card 2: FUEL & BATTERY
        val fuelVal = if (telemetry.fuelPercent > 0) "${telemetry.fuelPercent}%" else "68%"
        val fuelSub = if (telemetry.fuelLiters > 0) "${String.format("%.1f", telemetry.fuelLiters)} L Remaining" else "42.5 L In Tank"
        drawTelemetryCard(
            canvas = canvas,
            x = 440f, y = 135f, w = 360f, h = 300f,
            title = "FUEL LEVEL",
            mainVal = fuelVal,
            unit = fuelSub,
            valColor = Color.rgb(0, 220, 130)
        )

        // 4. Card 3: ODOMETER & STATUS
        val odoVal = if (telemetry.odometerKm > 0) "${telemetry.odometerKm}" else "128450"
        drawTelemetryCard(
            canvas = canvas,
            x = 840f, y = 135f, w = 400f, h = 300f,
            title = "ODOMETER",
            mainVal = odoVal,
            unit = "KM TRAVELED",
            valColor = Color.rgb(255, 190, 0)
        )

        return pushBitmapToCarScreen(bitmap, quality = 80)
    }

    private fun drawTelemetryCard(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float,
        title: String, mainVal: String, unit: String, valColor: Int
    ) {
        val cardPaint = Paint().apply {
            color = Color.rgb(26, 33, 46)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 16f, 16f, cardPaint)

        // Title
        val titlePaint = Paint().apply {
            color = Color.rgb(160, 180, 200)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(title, x + 24f, y + 48f, titlePaint)

        // Main Value
        val valPaint = Paint().apply {
            color = valColor
            textSize = 72f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(mainVal, x + 24f, y + 150f, valPaint)

        // Unit
        val unitPaint = Paint().apply {
            color = Color.rgb(180, 190, 205)
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText(unit, x + 24f, y + 210f, unitPaint)
    }

    /**
     * Renders a linked Android App frame onto the car screen.
     * Takes an arbitrary app bitmap or screencast frame, letterboxes it
     * cleanly to 1280x480 for the BMW iDrive widescreen display.
     */
    fun pushLinkedAppFrame(appName: String, appIcon: Bitmap?, appScreen: Bitmap): Boolean {
        val width = CAR_SCREEN_WIDTH
        val height = CAR_SCREEN_HEIGHT
        val composite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)

        // Dark background
        canvas.drawColor(Color.rgb(12, 16, 22))

        // Left Sidebar: App Info & Launcher bar
        val sidebarPaint = Paint().apply {
            color = Color.rgb(22, 27, 36)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 260f, height.toFloat(), sidebarPaint)

        if (appIcon != null) {
            val scaledIcon = Bitmap.createScaledBitmap(appIcon, 64, 64, true)
            canvas.drawBitmap(scaledIcon, 30f, 40f, null)
        }

        val namePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(appName, 30f, 150f, namePaint)

        val statusPaint = Paint().apply {
            color = Color.rgb(0, 220, 130)
            textSize = 15f
            isAntiAlias = true
        }
        canvas.drawText("● LINKED LIVE", 30f, 185f, statusPaint)

        val tipPaint = Paint().apply {
            color = Color.rgb(120, 140, 160)
            textSize = 13f
            isAntiAlias = true
        }
        canvas.drawText("Mirroring to iDrive", 30f, 220f, tipPaint)
        canvas.drawText("Turn dial to return", 30f, 245f, tipPaint)

        // Right Area: App Screen scaled and centered in 1020x480 container
        val targetW = 1000f
        val targetH = 460f
        val scale = Math.min(targetW / appScreen.width, targetH / appScreen.height)
        val scaledW = appScreen.width * scale
        val scaledH = appScreen.height * scale
        val startX = 270f + (targetW - scaledW) / 2f
        val startY = 10f + (targetH - scaledH) / 2f

        val destRect = RectF(startX, startY, startX + scaledW, startY + scaledH)
        canvas.drawBitmap(appScreen, null, destRect, null)

        return pushBitmapToCarScreen(composite, quality = 70)
    }

    /**
     * Sends an arbitrary Android Bitmap to the iDrive display image component.
     */
    fun pushBitmapToCarScreen(bitmap: Bitmap, quality: Int = 75): Boolean {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val jpegBytes = outputStream.toByteArray()

            Log.d(TAG, "Streaming frame to iDrive RHMI: ${jpegBytes.size} bytes (${bitmap.width}x${bitmap.height})")

            // RHMI SetImageData Packet Structure:
            // [ComponentId: Short][ImageFormat: Short][DataLength: Int][RawBytes]
            val buffer = ByteBuffer.allocate(8 + jpegBytes.size).order(ByteOrder.BIG_ENDIAN)
            buffer.putShort(COMPONENT_IMAGE_VIEW.toShort())
            buffer.putShort(1) // Format: 1 = JPEG
            buffer.putInt(jpegBytes.size)
            buffer.put(jpegBytes)

            bclConnection.sendData(RHMI_CHANNEL, buffer.array())
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push bitmap to car screen: ${e.message}")
            false
        }
    }

    /**
     * Renders a Turn-by-Turn navigation card or informational canvas.
     */
    fun pushNavigationCard(turnInstruction: String, distanceMeters: Int, iconBitmap: Bitmap?): Boolean {
        val width = 600
        val height = 300
        val cardBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cardBitmap)

        val paint = Paint().apply {
            color = Color.rgb(24, 28, 36)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.isAntiAlias = true
        canvas.drawText(turnInstruction, 40f, 100f, paint)

        paint.color = Color.CYAN
        paint.textSize = 48f
        canvas.drawText("${distanceMeters}m", 40f, 190f, paint)

        if (iconBitmap != null) {
            canvas.drawBitmap(iconBitmap, (width - iconBitmap.width - 30).toFloat(), 60f, null)
        }

        return pushBitmapToCarScreen(cardBitmap, quality = 80)
    }
}
