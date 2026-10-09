package com.openbimmer.idrive.bcl

import android.os.Build
import android.util.Log
import com.openbimmer.idrive.bcl.packet.Command
import com.openbimmer.idrive.bcl.packet.Packet
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BclConnection(
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    private val onPacketReceived: (Packet) -> Unit,
    private val onHandshakeComplete: () -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "BclConnection"
        private val SESSION_INIT_BYTES = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        private const val PROTOCOL_VERSION: Short = 0x02
    }

    enum class State {
        DISCONNECTED,
        INIT,
        SELECT_PROTOCOL,
        KNOCKING,
        REGISTERING,
        LAUNCHING,
        CONNECTED,
        ERROR
    }

    var state: State = State.DISCONNECTED
        private set

    private var isRunning = false
    private var readThread: Thread? = null

    fun startHandshake() {
        if (isRunning) return
        isRunning = true
        state = State.INIT
        Log.i(TAG, "Starting BCL Handshake sequence...")

        readThread = Thread {
            readLoop()
        }.apply { start() }

        try {
            // Step 1: Send Init Session Bytes
            outputStream.write(SESSION_INIT_BYTES)
            outputStream.flush()
            Log.d(TAG, "Sent SESSION_INIT_BYTES")

            // Step 2: Send SELECTPROTO
            state = State.SELECT_PROTOCOL
            val protoPayload = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
                .putShort(PROTOCOL_VERSION)
                .array()
            sendPacket(Command.SELECTPROTO, src = 0, dest = 0, payload = protoPayload)
            Log.d(TAG, "Sent Command.SELECTPROTO (mVersion = $PROTOCOL_VERSION)")

            // Step 3: Send KNOCK
            state = State.KNOCKING
            val knockBuffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN)
            knockBuffer.putShort(Build.VERSION.SDK_INT.toShort()) // Android SDK level
            knockBuffer.putShort(1) // AppType: Media/Companion
            knockBuffer.put("OpenBimmer".toByteArray(Charsets.UTF_8).copyOf(16))
            val knockPayload = knockBuffer.array()
            sendPacket(Command.KNOCK, src = 0, dest = 0, payload = knockPayload)
            Log.d(TAG, "Sent Command.KNOCK")

            // Step 4: Send REGISTER
            state = State.REGISTERING
            val regBuffer = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
            regBuffer.put(0x01) // SubCommand
            regBuffer.putShort(1)
            regBuffer.put("com.bmw.app".toByteArray(Charsets.UTF_8).copyOf(16))
            sendPacket(Command.REGISTER, src = 0, dest = 0, payload = regBuffer.array())
            Log.d(TAG, "Sent Command.REGISTER")

            // Step 5: Send LAUNCH
            state = State.LAUNCHING
            sendPacket(Command.LAUNCH, src = 0, dest = 0, payload = null)
            Log.d(TAG, "Sent Command.LAUNCH")

            // Step 6: Open Multiplex Logical Channels (CDS, RHMI, CarCloud)
            // Channel 1: CDS (Car Data Server)
            openChannel(src = 1, dest = 1)
            // Channel 2: RHMI (In-Car UI)
            openChannel(src = 2, dest = 2)
            // Channel 3: CarCloud (Route Import RPC)
            openChannel(src = 3, dest = 3)

            state = State.CONNECTED
            Log.i(TAG, "BCL Handshake completed successfully! Multiplex channels opened.")
            onHandshakeComplete()
        } catch (e: Exception) {
            state = State.ERROR
            Log.e(TAG, "Error during BCL handshake: ${e.message}")
            onError("BCL Handshake failed: ${e.message}")
        }
    }

    fun openChannel(src: Short, dest: Short) {
        Log.d(TAG, "BCL OPEN src=$src dest=$dest")
        sendPacket(Command.OPEN, src = src, dest = dest, payload = null)
    }

    @Synchronized
    fun sendPacket(command: Command, src: Short, dest: Short, payload: ByteArray?) {
        val bytes = Packet.encode(command, src, dest, (payload?.size ?: 0).toShort(), payload)
        outputStream.write(bytes)
        outputStream.flush()
    }

    fun sendData(destChannel: Short, data: ByteArray) {
        sendPacket(Command.DATA, src = destChannel, dest = destChannel, payload = data)
    }

    private fun readLoop() {
        val headerBuf = ByteArray(Packet.HEADER_SIZE)
        while (isRunning) {
            try {
                var bytesRead = 0
                while (bytesRead < Packet.HEADER_SIZE && isRunning) {
                    val count = inputStream.read(headerBuf, bytesRead, Packet.HEADER_SIZE - bytesRead)
                    if (count == -1) throw Exception("Stream closed by car")
                    bytesRead += count
                }

                if (!isRunning) break
                val packetHeader = Packet.decode(headerBuf) ?: continue
                val payloadSize = packetHeader.length.toInt()
                val payload = if (payloadSize > 0) {
                    val pBuf = ByteArray(payloadSize)
                    var pRead = 0
                    while (pRead < payloadSize && isRunning) {
                        val count = inputStream.read(pBuf, pRead, payloadSize - pRead)
                        if (count == -1) break
                        pRead += count
                    }
                    pBuf
                } else {
                    ByteArray(0)
                }

                val fullPacket = packetHeader.copy(payload = payload)
                onPacketReceived(fullPacket)
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "BCL read loop exception: ${e.message}")
                    onError("BCL Read error: ${e.message}")
                    break
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        state = State.DISCONNECTED
        readThread?.interrupt()
        readThread = null
    }
}
