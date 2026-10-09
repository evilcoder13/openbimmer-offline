package com.openbimmer.idrive.bcl.packet

enum class Command(val id: Short) {
    INIT(0x00),
    SELECTPROTO(0x01),
    KNOCK(0x02),
    REGISTER(0x03),
    LAUNCH(0x04),
    OPEN(0x05),
    CLOSE(0x06),
    DATA(0x07),
    DATA_ACK(0x08),
    RESET(0x09),
    PING(0x0A),
    PONG(0x0B);

    companion object {
        fun fromId(id: Short): Command? = entries.find { it.id == id }
    }
}
