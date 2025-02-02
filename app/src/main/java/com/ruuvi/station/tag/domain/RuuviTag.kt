package com.ruuvi.station.tag.domain

import com.ruuvi.station.alarm.domain.AlarmStatus
import java.util.Date

data class RuuviTag(
    val id: String,
    val name: String,
    val displayName: String,
    val rssi: Int,
    val temperature: Double,
    val humidity: Double?,
    val pressure: Double?,
    val movementCounter: Int?,
    val temperatureString: String,
    val humidityString: String,
    val pressureString: String,
    var temperatureOffset: Double?,
    var humidityOffset: Double?,
    var pressureOffset: Double?,
    val voltage: Double,
    val accelerationX: Double?,
    val accelerationY: Double?,
    val accelerationZ: Double?,
    val txPower: Double,
    val measurementSequenceNumber: Int,
    val movementCounterString: String,
    val defaultBackground: Int,
    val dataFormat: Int,
    val updatedAt: Date?,
    val userBackground: String?,
    val networkBackground: String?,
    val status: AlarmStatus = AlarmStatus.NO_ALARM,
    val connectable: Boolean?,
    val lastSync: Date?,
    val networkLastSync: Date?,
    val networkSensor: Boolean,
    val owner: String?,
    var firmware: String?
)