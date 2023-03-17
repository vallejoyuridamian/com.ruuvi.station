package com.ruuvi.station.network.domain

import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.model.NetworkRequestType
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.NetworkRequest
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import com.ruuvi.station.network.data.NetworkTokenInfo
import com.ruuvi.station.network.data.request.*
import com.ruuvi.station.network.data.requestWrappers.UploadImageRequestWrapper
import com.ruuvi.station.network.data.response.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception
import java.util.*

class RuuviNetworkInteractor (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkRequestExecutor: NetworkRequestExecutor,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val firebaseInteractor: FirebaseInteractor,
    private val networkResponseLocalizer: NetworkResponseLocalizer
) {
    val signedIn: Boolean
        get() = getToken() != null

    fun getEmail() = getToken()?.email

    private fun getToken() = tokenRepository.getTokenInfo()

    private var userInfo: UserInfoResponse? = null

    val mainScope = CoroutineScope(Dispatchers.Main)
    val ioScope = CoroutineScope(Dispatchers.IO)

    fun registerUser(user: UserRegisterRequest, onResult: (UserRegisterResponse?) -> Unit) {
        networkRepository.registerUser(user) {
            networkResponseLocalizer.localizeResponse(it)
            onResult(it)
        }
    }

    fun verifyUser(token: String, onResult: (UserVerifyResponse?) -> Unit) {
        networkRepository.verifyUser(token) { response ->
            networkResponseLocalizer.localizeResponse(response)
            response?.let {
                if (response.error.isNullOrEmpty() && response.data != null) {
                    tokenRepository.saveTokenInfo(
                        NetworkTokenInfo(response.data.email, response.data.accessToken))
                    firebaseInteractor.logSignIn()
                }
            }
            onResult(response)
        }
    }

    fun shouldSendDataToNetwork() = getToken() != null

    fun shouldSendSensorDataToNetwork(sensorId: String): Boolean {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        return shouldSendDataToNetwork() && sensorSettings?.networkSensor == true
    }

    fun shouldSendSensorDataToNetworkForOwner(sensorId: String): Boolean {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        return shouldSendDataToNetwork() && sensorSettings?.owner == getToken()?.email
    }

    fun getUserInfo(onResult: (UserInfoResponse?) -> Unit) {
        ioScope.launch {
            val result = getUserInfo()
            networkResponseLocalizer.localizeResponse(result)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    suspend fun getUserInfo(): UserInfoResponse? {
        val token = getToken()
        if (token != null) {
            val benchUpdate1 = Date()
            userInfo = networkRepository.getUserInfo(token.token)
            val benchUpdate2 = Date()
            Timber.d("benchmark-getUserInfo-finish ${benchUpdate2.time - benchUpdate1.time} ms")
            return userInfo
        } else {
            return null
        }
    }

    fun getSensorNetworkStatus(mac: String): SensorDataResponse? {
        return userInfo?.data?.sensors?.firstOrNull {it.sensor == mac}
    }

    suspend fun claimSensor(sensorSettings: SensorSettings, onResult: (ClaimSensorResponse?) -> Unit) {
        val token = getToken()?.token
        token?.let {
            val request = ClaimSensorRequest(sensorSettings.id, sensorSettings.displayName)
            try {
                networkRepository.claimSensor(request, token) { claimResponse ->
                    networkResponseLocalizer.localizeResponse(claimResponse)
                    if (claimResponse?.isSuccess() == true) {
                        sensorSettingsRepository.setSensorOwner(
                            sensorSettings.id,
                            getEmail() ?: "",
                            true
                        )
                    } else {
                        val maskedEmail =
                            Regex("\\b\\S*@\\S*\\.\\S*\\b").find(claimResponse?.error ?: "")?.value
                        if (maskedEmail?.isNotEmpty() == true) {
                            sensorSettingsRepository.setSensorOwner(
                                sensorSettings.id,
                                maskedEmail,
                                false
                            )
                        }
                    }
                    getUserInfo {
                        onResult(claimResponse)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(
                        ClaimSensorResponse(
                            RuuviNetworkResponse.errorResult,
                            e.message.toString(),
                            null,
                            null
                        )
                    )
                }
            }
        }
    }

    fun checkSensorOwner(sensorId: String) {
        val token = getToken()?.token
        token?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val response = networkRepository.checkSensorOwner(sensorId, token)
                if (response?.isSuccess() == true && response.data?.email?.isNotEmpty() == true) {
                    sensorSettingsRepository.setSensorOwner(
                        sensorId,
                        response.data.email,
                        false
                    )
                }
            }
        }
    }

    fun unclaimSensor(sensorId: String) {
        val networkRequest = NetworkRequest(NetworkRequestType.UNCLAIM, sensorId, UnclaimSensorRequest(sensorId))
        Timber.d("unclaimSensor $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest)
    }

    fun unshareSensor(recipientEmail: String, sensorId: String) {
        val networkRequest = NetworkRequest(NetworkRequestType.UNSHARE, sensorId, UnshareSensorRequest(recipientEmail, sensorId))
        Timber.d("unshareSensor $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest)
    }

    fun shareSensor(recipientEmail: String, tagId: String, handler: CoroutineExceptionHandler, onResult: (ShareSensorResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = ShareSensorRequest(recipientEmail, tagId)
                val response = networkRepository.shareSensor(request, token)
                networkResponseLocalizer.localizeResponse(response)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    fun unshareSensor(recipientEmail: String, tagId: String, handler: CoroutineExceptionHandler, onResult: (ShareSensorResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = UnshareSensorRequest(recipientEmail, tagId)
                val response = networkRepository.unshareSensor(request, token)
                networkResponseLocalizer.localizeResponse(response)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    fun getSharedInfo(sensorId: String?, handler: CoroutineExceptionHandler, onResult: (SensorInfo?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val response = networkRepository.getSensors(sensorId, it)
                networkResponseLocalizer.localizeResponse(response)
                if (response?.isSuccess() == true && response.data != null) {
                    val result = response.data.sensors.firstOrNull { it.sensor == sensorId }
                    withContext(Dispatchers.Main) {
                        onResult(result)
                    }
                }
            }
        }
    }

    fun updateSensorCalibration(sensorId: String) {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        if (shouldSendSensorDataToNetworkForOwner(sensorId) && sensorSettings != null) {
            val networkRequest = NetworkRequest(NetworkRequestType.UPDATE_SENSOR, sensorId,
                UpdateSensorRequest(
                    sensor = sensorId,
                    offsetTemperature = sensorSettings.temperatureOffset ?: 0.0,
                    offsetHumidity = sensorSettings.humidityOffset ?: 0.0,
                    offsetPressure = sensorSettings.pressureOffset ?: 0.0
                ))
            Timber.d("updateSensor $networkRequest")
            networkRequestExecutor.registerRequest(networkRequest)
        }
    }

    fun updateSensorName(sensorId: String) {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        if (shouldSendSensorDataToNetwork(sensorId) && sensorSettings != null) {
            val networkRequest = NetworkRequest(NetworkRequestType.UPDATE_SENSOR, sensorId,
                UpdateSensorRequest(
                    sensor = sensorId,
                    name = sensorSettings.displayName
                ))
            Timber.d("updateSensor $networkRequest")
            networkRequestExecutor.registerRequest(networkRequest)
        }
    }

    fun uploadImage(sensorId: String, filename: String) {
        if (shouldSendSensorDataToNetwork(sensorId)) {
            val networkRequest = NetworkRequest(NetworkRequestType.UPLOAD_IMAGE, sensorId, UploadImageRequestWrapper(filename, UploadImageRequest(sensorId)))
            Timber.d("uploadImage $networkRequest")
            networkRequestExecutor.registerRequest(networkRequest)
        }
    }

    fun uploadImage(tagId: String, filename: String, handler: CoroutineExceptionHandler, onResult: (UploadImageResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = UploadImageRequest(tagId, "image/jpeg")
                val response = networkRepository.uploadImage(filename, request, token)
                networkResponseLocalizer.localizeResponse(response)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    fun resetImage(sensorId: String) {
        if (shouldSendSensorDataToNetwork(sensorId)) {
            val networkRequest = NetworkRequest(NetworkRequestType.RESET_IMAGE, sensorId, UploadImageRequest.getResetImageRequest(sensorId))
            Timber.d("resetImage $networkRequest")
            networkRequestExecutor.registerRequest(networkRequest)
        }
    }

    suspend fun getSensorData(request: GetSensorDataRequest):GetSensorDataResponse? = withContext(Dispatchers.IO) {
        val token = getToken()?.token
        token?.let {
            return@withContext networkRepository.getSensorData(token, request)
        }
    }

    suspend fun getSensorLastData(sensorId: String):GetSensorDataResponse? {
        val request = GetSensorDataRequest(
            sensor = sensorId,
            since = null,
            until = Date(),
            sort = SortMode.DESCENDING,
            limit = 1,
            mode = SensorDataMode.MIXED
        )
        return getSensorData(request)
    }

    suspend fun getSensorDenseLastData(): SensorDenseResponse? = withContext(Dispatchers.IO) {
        val token = getToken()?.token

        token?.let {
            val request = SensorDenseRequest(
                sensor = null,
                measurements = true,
                alerts = false,
                sharedToOthers = false,
                sharedToMe = true
            )
            return@withContext networkRepository.getSensorDenseData(token, request)
        }
    }

    fun updateUserSetting(name: String, value: String) {
        val networkRequest = NetworkRequest(
            NetworkRequestType.SETTINGS,
            name,
            UpdateUserSettingRequest(name, value)
        )
        Timber.d("updateUserSetting $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest, true)
    }

    fun setAlert(alarm: Alarm) {
        if (shouldSendSensorDataToNetwork(alarm.ruuviTagId) && alarm.alarmType.networkCode != null) {
            val networkRequest = NetworkRequest(
                NetworkRequestType.SET_ALERT,
                alarm.ruuviTagId + alarm.alarmType.networkCode,
                SetAlertRequest.getAlarmRequest(alarm)
            )
            Timber.d("setAlert $networkRequest")
            networkRequestExecutor.registerRequest(networkRequest)
        }
    }
}