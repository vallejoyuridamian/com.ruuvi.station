package com.ruuvi.station.bluetooth
// Coolgreen development

import com.ruuvi.station.app.preferences.PreferencesRepository
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import android.widget.Toast
import android.content.Context


class MQTTDataForwarder (
    private val preferencesRepository: PreferencesRepository,
    private val context: Context
    ):IRuuviDataForwarder {

    private val DATA_LENGTH_OFFSET = 3
    private val SIZE_METADATA = 4 //
    private var mqttClient: MqttClient? = null
    private var mqttOptions: MqttConnectOptions = MqttConnectOptions()


    override fun forwardData(data: ByteArray, rssi: Int, id: String){

        var message : String
        var duration : Int // or Toast.LENGTH_LONG for a longer duration
        var toast : Toast

        if (preferencesRepository.getMQTTDataForwardingEnabled()){
            try {
                val persistence = MemoryPersistence()
                mqttOptions.setConnectionTimeout(10) // 10 seconds

                if (mqttClient == null) {
                    val mqtturl = preferencesRepository.getMQTTDataForwardingUrl()
                    val mqttport = preferencesRepository.getMQTTDataForwardingPort()
                    mqttClient = MqttClient("tcp://" + "$mqtturl" + ":" + "$mqttport",
                            MqttClient.generateClientId(), persistence)

                    message = "Connecting to MQTT broker, please wait"
                    duration = Toast.LENGTH_SHORT // or Toast.LENGTH_LONG for a longer duration
                    toast = Toast.makeText(context, message, duration)
                    toast.show()

                    mqttClient?.connect(mqttOptions)

                    message = "Connection successful"
                    duration = Toast.LENGTH_SHORT // or Toast.LENGTH_LONG for a longer duration
                    toast = Toast.makeText(context, message, duration)
                    toast.show()

                }
                val thisMac = preferencesRepository.getDeviceId()
                val timestamp = System.currentTimeMillis() / 1000L
                val lengthByte: Int = data.get(DATA_LENGTH_OFFSET) + SIZE_METADATA //
                val hexString = StringBuilder()
                for (i in 0 until lengthByte) {
                    hexString.append(String.format("%02X", data.get(i)))
                }
                val dataString = hexString.toString()
                // Create the message
                val mqttMessageStr = """{
    	            "gw_mac":	"$thisMac",
	                "rssi":	$rssi,
	                "aoa":	[],
	                "gwts":	"$timestamp",
	                "ts":	"$timestamp",
	                "data":	"$dataString",
	                "coords":	""
                }
                """
                val mqttMessage = MqttMessage(mqttMessageStr.toByteArray())

                // Publish the message to the topic "my/topic"
                // I have id (mac of the board and rssi)
                val topic = "ruuvi/$thisMac/$id"

                if (mqttClient?.isConnected == false){
                    message = "Connecting to MQTT broker, please wait"
                    duration = Toast.LENGTH_SHORT // or Toast.LENGTH_LONG for a longer duration
                    toast = Toast.makeText(context, message, duration)
                    toast.show()

                    mqttClient?.connect(mqttOptions)

                    message = "Connection successful"
                    duration = Toast.LENGTH_SHORT // or Toast.LENGTH_LONG for a longer duration
                    toast = Toast.makeText(context, message, duration)
                    toast.show()
                }

                mqttClient?.publish(topic, mqttMessage)

                // Disconnect from the broker
                // client.disconnect()
            } catch (e: MqttException) {
                System.err.println("An error occurred while creating the MqttClient:")
                System.err.println("Error message: " + e.message)
                System.err.println("Error code: " + e.reasonCode)
                e.printStackTrace()
                preferencesRepository.setMQTTDataForwardingEnabled(false)
                message = "Could not connect to broker, turning off MQTT Forwarding"
                duration = Toast.LENGTH_SHORT // or Toast.LENGTH_LONG for a longer duration
                toast = Toast.makeText(context, message, duration)
                toast.show()

            }
        } else {
            mqttClient?.let { client ->
                if (client.isConnected) {
                    client.disconnect()
                    mqttClient = null
                }
            }
        }
    }
}