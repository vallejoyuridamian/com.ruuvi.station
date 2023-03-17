package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.settings.domain.GatewayTestResultType

@Composable
fun MQTTDataForwardingSettings(
        scaffoldState: ScaffoldState,
        viewModel: MQTTDataForwardingSettingsViewModel
) {
    val context = LocalContext.current
    val mqttDataForwardingUrl = viewModel.mqttDataForwardingUrl.collectAsState()
    val mqttPort = viewModel.mqttDataForwardingPort.collectAsState()
    val mqttEnabled = viewModel.mqttDataForwardingEnabled.collectAsState()

    PageSurfaceWithPadding {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

            Subtitle(text = stringResource(id = R.string.mqtt_data_forwarding_url))

            TextFieldRuuvi(
                    value = mqttDataForwardingUrl.value,
                    hint = stringResource(id = R.string.mqtt_data_forwarding_url_hint),
                    onValueChange = viewModel::setMQTTDataForwardingUrl,
                    modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

            Subtitle(text = stringResource(id = R.string.mqtt_data_forwarding_port))

            TextFieldRuuvi(
                    value = mqttPort.value,
                    hint = stringResource(id = R.string.mqtt_data_forwarding_port_hint),
                    onValueChange = viewModel::setMQTTDataForwardingPort,
                    modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

            SwitchRuuvi(
                    text = stringResource(id = R.string.data_forwarding_mqtt_enable),
                    checked = mqttEnabled.value,
                    onCheckedChange = viewModel::setMQTTDataForwardingEnabled
            )

        }
    }
}