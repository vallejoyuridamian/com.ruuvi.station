package com.ruuvi.station.settings.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.startup.ui.StartupActivity
import com.ruuvi.station.util.BackgroundScanModes

@Composable
fun SettingsList(
    scaffoldState: ScaffoldState,
    onNavigate: (UiEvent.Navigate) -> Unit,
    viewModel: AppSettingsListViewModel
) {
    val context = LocalContext.current
    BackHandler {
        if (viewModel.shouldRestartApp()) {
            StartupActivity.start(context, false)
        }
        (context as Activity).finish()
    }

    var intervalText = ""
    if (viewModel.getBackgroundScanMode() != BackgroundScanModes.DISABLED) {
        val bgScanInterval = viewModel.getBackgroundScanInterval()
        val min = bgScanInterval / 60
        val sec = bgScanInterval - min * 60
        if (min > 0) intervalText += min.toString() + " " + stringResource(R.string.min) + " "
        if (sec > 0) intervalText += sec.toString() + " " + stringResource(R.string.sec)
    } else {
        intervalText = stringResource(id = R.string.alert_subtitle_off)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_appearance),
                description = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.APPEARANCE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_background_scan),
                description = intervalText,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.BACKGROUNDSCAN)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_temperature_unit),
                description = stringResource(id = viewModel.getTemperatureUnit().unit),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.TEMPERATURE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_humidity_unit),
                description = stringResource(id = viewModel.getHumidityUnit().unit),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.HUMIDITY)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_pressure_unit),
                description = stringResource(id = viewModel.getPressureUnit().unit),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.PRESSURE)) }
            )
        }

        if (viewModel.shouldShowCloudMode()) {
            item {
                SettingsElement(
                    name = stringResource(id = R.string.ruuvi_cloud),
                    description = null,
                    onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.CLOUD)) }
                )
            }
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_chart),
                description = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.CHARTS)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_data_forwarding),
                description = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.DATAFORWARDING)) }
            )
        }

        // Coolgreen modification
        item {
            SettingsElement(
                    name = stringResource(id = R.string.settings_mqtt_data_forwarding),
                    description = null,
                    onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.MQTT_DATAFORWARDING)) }
            )
        }
    }
}

@Composable
fun SettingsElement(
    name: String,
    description: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        ConstraintLayout(modifier = Modifier
            .fillMaxWidth()
            .height(RuuviStationTheme.dimensions.settingsListHeight)
            .padding(RuuviStationTheme.dimensions.medium)
            .clickable(role = Role.Button) { onClick.invoke() }
        ) {
            val (caption, descElement, arrow) = createRefs()

            Text(
                modifier = Modifier
                    .constrainAs(caption) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.absoluteLeft)
                        end.linkTo(descElement.start)
                        width = Dimension.fillToConstraints
                    },
                style = RuuviStationTheme.typography.subtitle,
                text = name,
                textAlign = TextAlign.Left,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (description != null) {
                Text(
                    modifier = Modifier
                        .padding(end = RuuviStationTheme.dimensions.mediumPlus)
                        .constrainAs(descElement) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            //start.linkTo(caption.end)
                            end.linkTo(arrow.start)
                            //width = Dimension.fillToConstraints

                        },
                    style = RuuviStationTheme.typography.paragraph,
                    textAlign = TextAlign.End,
                    text = description,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }

            Image(
                painter = painterResource(id = R.drawable.arrow_forward_16),
                contentDescription = "",
                modifier = Modifier
                    .constrainAs(arrow) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }
            )
        }
        DividerRuuvi()
    }
}