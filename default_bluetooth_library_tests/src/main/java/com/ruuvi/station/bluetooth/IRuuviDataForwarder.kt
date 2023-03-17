package com.ruuvi.station.bluetooth
import java.util.*

interface IRuuviDataForwarder {
    fun forwardData(data: ByteArray, rssi: Int, id: String)
}