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
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

class MQTTDataForwarder (
    private val preferencesRepository: PreferencesRepository,
    private val context: Context
    ):IRuuviDataForwarder {

    private val DATA_LENGTH_OFFSET = 3
    private val RETRY_TIME_MIN: Long = 60 // 5
    private val MAX_SAVE_DATA_HOURS = 168 // 1
    private val MAX_SAVE_DATA_MB = 30 // 30
    private val SIZE_METADATA = 4 //
    private val folderName = "MQTTData"
    private var mqttClient: MqttClient? = null
    private var mqttOptions: MqttConnectOptions = MqttConnectOptions()
    private var pausedDueToDisconnect = false
    private val timer = Timer()
    private val rootDir: File = context.getFilesDir()
    private val mqttDir: File = File(rootDir.absolutePath + "/" + folderName)
    private val thisMac = preferencesRepository.getDeviceId()

    override fun forwardData(data: ByteArray, rssi: Int, macid: String) {

        // OK so I have to prepare the message first
        if (preferencesRepository.getMQTTDataForwardingEnabled()) {

            val mqttMessageStr = getMQTTMessageStr(data, rssi, thisMac)

            // If it is not paused due to disconnect, we send it out
            if (!pausedDueToDisconnect) {
                try {
                    val persistence = MemoryPersistence()
                    mqttOptions.setConnectionTimeout(10) // 10 seconds

                    // First initialization
                    if (mqttClient == null) {
                        val mqtturl = preferencesRepository.getMQTTDataForwardingUrl()
                        val mqttport = preferencesRepository.getMQTTDataForwardingPort()
                        mqttClient = MqttClient("tcp://" + "$mqtturl" + ":" + "$mqttport",
                                MqttClient.generateClientId(), persistence)

                        showMessage("Connecting to MQTT broker, please wait")
                        mqttClient?.connect(mqttOptions)
                        showMessage("Connection successful")
                    }

                    val mqttMessage = MqttMessage(mqttMessageStr.toByteArray())

                    // Publish the message to the topic "my/topic"
                    // I have id (mac of the board and rssi)

                    val topic = "ruuvi/$thisMac/$macid"

                    // Try to connect if it is not connected
                    if (mqttClient?.isConnected == false) {
                        showMessage("Connecting to MQTT broker, please wait")
                        mqttClient?.connect(mqttOptions)
                        showMessage("Connection successful")
                    }

                    mqttClient?.publish(topic, mqttMessage)
                    checkCachedMessages()

                } catch (e: MqttException) {
                    // If it failed we go here, print the error and pause
                    System.err.println("An error occurred while creating the MqttClient:")
                    System.err.println("Error message: " + e.message)
                    System.err.println("Error code: " + e.reasonCode)
                    e.printStackTrace()
                    //preferencesRepository.setMQTTDataForwardingEnabled(false)
                    pausedDueToDisconnect = true
                    mqttClient = null
                    showMessage("Could not connect to broker, trying again in one hour")

                    cacheMessage(mqttMessageStr, macid)
                    // Create a timer to try again in one hour
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            // code to execute after 1 hour (now it will be 5 minutes) //
                            // OK so here we want to try to reconnect //
                            pausedDueToDisconnect = false
                            // check if there are messages to send //
                            checkCachedMessages()
                        }
                    }, 1000 * 60 * RETRY_TIME_MIN)  // 5 minutes in milliseconds  //3600000) // 1 hour in milliseconds
                }
            } else {
                // we will disconnect if connected if it is paused ( also later if it is turned off)
                mqttClient?.let { client ->
                    if (client.isConnected) {
                        client.disconnect()
                        mqttClient = null
                    }
                }
                // if it is paused but on, we just cache it
                cacheMessage(mqttMessageStr, macid)
            }
        } else {
            // we will disconnect if connected if it is paused or turned off
            mqttClient?.let { client ->
                if (client.isConnected) {
                    client.disconnect()
                    mqttClient = null
                }
            }
        }
    }

    fun getMQTTMessageStr(data: ByteArray, rssi: Int, thisMac: String): String {
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
        return mqttMessageStr
    }

    fun showMessage(message: String) {
        val duration = Toast.LENGTH_SHORT // or Toast.LENGTH_LONG for a longer duration
        val toast = Toast.makeText(context, message, duration)
        toast.show()
    }

    fun fileAgeFromName(fileName: String): Long {
        val year = fileName.substring(0, 4).toInt()
        val month = fileName.substring(4, 6).toInt()
        val day = fileName.substring(6, 8).toInt()
        val hour = fileName.substring(8, 10).toInt()
        // I will remove the minutes
        val minute = fileName.substring(10, 12).toInt()

        val fileCalendar = Calendar.getInstance()
        fileCalendar.set(year, month - 1, day, hour, minute, 0)
        //fileCalendar.set(year, month - 1, day, hour, 0, 0)
        fileCalendar.set(Calendar.MILLISECOND, 0)

        val age = System.currentTimeMillis() - fileCalendar.timeInMillis
        return age
    }

    // Returns fileName
    fun cacheMessage(mqttMessageStr: String, macid: String) :String {

        // This is just to check the folder of the app, I'll remove this later
        var dir = context.getFilesDir()
        // We make a MQTT folder to save the data there
        val folder = File(context.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }

        // This is just to check what is in the folder, I may need it later to delete old files
        dir = File(dir.absolutePath + "/" + folderName)
        var files = dir.list()

        // Get the current date and time in the format yyyymmddhh

        //val currentDate = SimpleDateFormat("yyyyMMddHH", Locale.getDefault()).format(Date())

        // This is just for testing, the files will be hourly
        val currentDate = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())
        // Get the MAC address of the device
        val macAddress = macid.replace(":", "")
        // Create the file name by concatenating the date, MAC address, and file extension
        val fileName = "$currentDate$macAddress.txt"

        // Create or open the file for writing
        val file = File(mqttDir, fileName)
        val fileWriter = FileWriter(file, true)

        // Check if file exists and append comma if necessary
        if (file.exists() && file.length() > 0) {
            fileWriter.write(",")
        }


        // Write message to the file
        fileWriter.write(mqttMessageStr)

        // Close the file writer
        fileWriter.close()

        return fileName

    }


    fun checkCachedMessages() {
        // OK so here we want to see if there are old messages and if we can send them out //
        // This is just to check what is in the folder, I may need it later to delete old files
        var file: File
        var fileReader: FileReader
        var bufferedReader: BufferedReader
        var stringBuilder: StringBuilder
        var line: String?
        var fileContent: String
        var macid: String
        // First we delete old files or if we exceeded 30 MB
        deleteOldFiles(mqttDir)
        // So we check all existing files in the directory
        var mqttFileNames = mqttDir.list()

        // if the list is empty, there is nothing to do
        if (!mqttFileNames.isNullOrEmpty()) {
            // try to connect again, but this time no cache of messages since there is no message
            try {
                val persistence = MemoryPersistence()
                mqttOptions.setConnectionTimeout(10) // 10 seconds
                // Just in case mqtt client is null
                if (mqttClient == null) {
                    val mqtturl = preferencesRepository.getMQTTDataForwardingUrl()
                    val mqttport = preferencesRepository.getMQTTDataForwardingPort()
                    mqttClient = MqttClient("tcp://" + "$mqtturl" + ":" + "$mqttport",
                            MqttClient.generateClientId(), persistence)

                    mqttClient?.connect(mqttOptions)
                }

                // Try to connect if it is not connected
                if (mqttClient?.isConnected == false) {
                    mqttClient?.connect(mqttOptions)
                }

            } catch (e: MqttException) {
                // If it failed we go here, print the error and pause
                System.err.println("An error occurred while creating the MqttClient:")
                System.err.println("Error message: " + e.message)
                System.err.println("Error code: " + e.reasonCode)
                e.printStackTrace()
                //preferencesRepository.setMQTTDataForwardingEnabled(false)
                pausedDueToDisconnect = true
                mqttClient = null

               // Create a timer to try again in one hour
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        // code to execute after 1 hour (now it will be 5 minutes) //
                        // OK so here we want to try to reconnect //
                        pausedDueToDisconnect = false
                        // check if there are messages to send //
                        checkCachedMessages()
                    }
                }, 1000 * 60 * RETRY_TIME_MIN)  // 5 minutes in milliseconds  //3600000) // 1 hour in milliseconds
            }

            // we do it again in case it was deleted
            mqttFileNames = mqttDir.list()

            for (fileName in mqttFileNames) {
                file = File(mqttDir, fileName)
                macid = macIdFromName(fileName)

                // If the file is too old we delete it
                // If not, we read it and send all data to the broker
                fileReader = FileReader(file)
                bufferedReader = BufferedReader(fileReader)
                stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                fileContent = stringBuilder.toString()
                bufferedReader.close()

                val jsonArray = JSONArray("[" + fileContent + "]")

                val jsonStrings = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    jsonStrings.add(jsonArray.getString(i))
                }
                for (jsonString in jsonStrings) {
                    val mqttMessage = MqttMessage(jsonString.toByteArray())
                    val topic = "ruuvi/$thisMac/$macid"
                    mqttClient?.publish(topic, mqttMessage)
                }
                // finally, delete the file //
                file.delete()
            }
        }
        // if we got to this point, we can unpause
        pausedDueToDisconnect = false
    }

    fun macIdFromName(fileName:String): String{
        val macid = fileName.substring(12, 24).toString()
        val formattedMacid = macid.chunked(2).joinToString(":")
        return formattedMacid
    }
    fun deleteOldFiles (folder: File) {

        var fileNames = folder.list()
        for (fileName in fileNames) {
            var fileAge = fileAgeFromName(fileName)
            var file = File(folder, fileName)
            //if (fileAge > 60 * 10 * 1000) { // files older than 10 minutes we delete
            if (fileAge > MAX_SAVE_DATA_HOURS * 60 * 60 * 1000) { // files older than 7 days we delete
                file.delete()
            }
        }
        // We also check that the whole folder doesn't pass the 30 MB threshold
        var folderSize = getFolderSize(folder)
        while (folderSize > MAX_SAVE_DATA_MB * 1024 * 1024) { // 30 MB in bytes
            val mqttFileNames = folder.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
            // If there are no files, exit the loop
            if (mqttFileNames.isEmpty()) {
                break
            }
            // Delete the oldest file
            val oldestFile = mqttFileNames.first()
                oldestFile.delete()
        }
    }

    fun getFolderSize(folder: File): Long {
        var length: Long = 0
        for (file in folder.listFiles()) {
            if (file.isFile()) {
                length += file.length()
            } else {
                length += getFolderSize(file)
            }
        }
        return length
    }
}
