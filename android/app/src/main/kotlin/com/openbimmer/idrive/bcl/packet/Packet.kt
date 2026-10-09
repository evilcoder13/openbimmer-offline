package com.openbimmer.idrive.bcl.packet

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Packet(
    val command: Command,
    val src: Short,
    val dest: Short,
    val length: Short,
    val payload: ByteArray = ByteArray(0)
) {
    companion object {
        const val HEADER_SIZE = 8

        fun encode(command: Command, src: Short, dest: Short, length: Short, payload: ByteArray?): ByteArray {
            val payloadBytes = payload ?: ByteArray(0)
            val buffer = ByteBuffer.allocate(HEADER_SIZE + payloadBytes.size).order(ByteOrder.BIG_ENDIAN)
            buffer.putShort(command.id)
            buffer.putShort(src)
            buffer.putShort(dest)
            buffer.putShort(payloadBytes.size.toShort())
            if (payloadBytes.isNotEmpty()) {
                buffer.put(payloadBytes)
            }
            return buffer.array()
        }

        fun decode(bytes: ByteArray): Packet? {
            if (bytes.size < HEADER_SIZE) return null
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val cmdId = buffer.short
            val command = Command.fromId(cmdId) ?: return null
            val src = buffer.short
            val dest = buffer.short
            val length = buffer.short
            val payload = ByteArray(length.toInt().coerceAtLeast(0))
            if (bytes.size >= HEADER_SIZE + payload.size) {
                buffer.get(payload)
            }
            return Packet(command, src, dest, length, payload)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Packet
        return command == other.command && src == other.src && dest == other.dest && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = command.hashCode()
        result = 31 * result + src
        result = 31 * result + dest
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
