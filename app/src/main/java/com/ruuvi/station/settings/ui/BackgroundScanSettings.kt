package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun BackgroundScanSettings(
    scaffoldState: ScaffoldState,
    viewModel: BackgroundScanSettingsViewModel
) {

    val backgroundInterval = viewModel.intervalFlow.collectAsState()
    val intervalOptions = remember {
        viewModel.getIntervalOptions()
    }

    val initialValue = intervalOptions.firstOrNull { it.value == backgroundInterval.value }

    val backgroundScanEnabled = viewModel.backgroundScanEnabled.collectAsState()

    val showOptimizationTips = viewModel.showOptimizationTips.collectAsState()

    PageSurfaceWithPadding {
        Column {
            SwitchRuuvi(
                text = stringResource(id = R.string.background_scanning),
                checked = backgroundScanEnabled.value,
                onCheckedChange = viewModel::setBackgroundScanEnabled
            )
            SubtitleWithPadding(text = stringResource(id = R.string.settings_background_scan_interval))
            TwoButtonsSelector(
                values = intervalOptions,
                initialValue = initialValue ?: intervalOptions.first(),
                onValueChanged = viewModel::setBackgroundScanInterval
            )

            if (showOptimizationTips.value) {
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
                SubtitleWithPadding(text = stringResource(id = R.string.settings_background_battery_optimization_title))
                ParagraphWithPadding(text = stringResource(id = R.string.settings_background_battery_optimization))
                ParagraphWithPadding(text = stringResource(id = viewModel.getBatteryOptimizationMessageId()))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RuuviButton(text = stringResource(id = R.string.open_settings)) {
                        viewModel.openOptimizationSettings()
                    }
                }
            }
        }
    }
}