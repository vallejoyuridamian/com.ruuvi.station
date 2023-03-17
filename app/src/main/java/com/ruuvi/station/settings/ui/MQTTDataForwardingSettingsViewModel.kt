// Coolgreen development
package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.koushikdutta.async.future.FutureCallback
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.domain.GatewayTestResult
import com.ruuvi.station.settings.domain.GatewayTestResultType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MQTTDataForwardingSettingsViewModel(
        private val interactor: AppSettingsInteractor
) : ViewModel() {
    private val _mqttDataForwardingUrl = MutableStateFlow(interactor.getMQTTDataForwardingUrl())
    val mqttDataForwardingUrl: StateFlow<String> = _mqttDataForwardingUrl

    private val _mqttDataForwardingPort = MutableStateFlow(interactor.getMQTTDataForwardingPort())
    val mqttDataForwardingPort: StateFlow<String> = _mqttDataForwardingPort

    private val _mqttDataForwardingEnabled = MutableStateFlow(interactor.getMQTTDataForwardingEnabled())
    val mqttDataForwardingEnabled: StateFlow<Boolean> = _mqttDataForwardingEnabled


    fun setMQTTDataForwardingUrl(url: String) {
        interactor.setMQTTDataForwardingUrl(url)
        _mqttDataForwardingUrl.value = interactor.getMQTTDataForwardingUrl()
    }

    fun setMQTTDataForwardingPort(port: String) {
        interactor.setMQTTDataForwardingPort(port)
        _mqttDataForwardingPort.value = interactor.getMQTTDataForwardingPort()
    }

    fun setMQTTDataForwardingEnabled(mqttDataForwardingEnabled: Boolean) {
        interactor.setMQTTDataForwardingEnabled(mqttDataForwardingEnabled)
        _mqttDataForwardingEnabled.value = interactor.getMQTTDataForwardingEnabled()
    }

}