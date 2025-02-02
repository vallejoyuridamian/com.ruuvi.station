package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.app.ui.components.*

@Composable
fun AppearanceSettings(
    scaffoldState: ScaffoldState,
    viewModel: AppearanceSettingsViewModel
) {
    val dashboardEnabled = viewModel.dashboardEnabled.observeAsState(initial = false)
    val darkMode = viewModel.darkMode.observeAsState(DarkModeState.SYSTEM_THEME)
    val themeOptions = viewModel.getThemeOptions()

    PageSurfaceWithPadding {
        Column {
            DashboardSettings(
                dashboardEnabled = dashboardEnabled,
                dashboardEnabledChanged = viewModel::setIsDashboardEnabled
            )

            AppThemeSettings(
                themeOptions = themeOptions,
                currentTheme = darkMode,
                onThemeChange = viewModel::setDarkMode
            )
        }
    }
}

@Composable
fun DashboardSettings(
    dashboardEnabled: State<Boolean>,
    dashboardEnabledChanged: ((Boolean) -> Unit)
) {
    SwitchRuuvi(
        text = stringResource(id = R.string.dashboard),
        checked = dashboardEnabled.value,
        onCheckedChange = dashboardEnabledChanged
    )
    Paragraph(text = stringResource(id = R.string.dashboard_description))
}

@Composable
fun AppThemeSettings(
    themeOptions: Array<DarkModeState>,
    currentTheme: State<DarkModeState>,
    onThemeChange: (DarkModeState) -> Unit
) {
    SubtitleWithPadding(text = stringResource(id = R.string.app_theme))

    for (themeOption in themeOptions) {
        AppThemeOption(
            themeOption = themeOption,
            onThemeChange = onThemeChange,
            isSelected = currentTheme.value == themeOption
        )
    }
}

@Composable
fun AppThemeOption(
    themeOption: DarkModeState,
    isSelected: Boolean,
    onThemeChange: (DarkModeState) -> Unit
) {
    RadioButtonRuuvi(
        text = stringResource(id = themeOption.title),
        isSelected = isSelected
    ) {
        onThemeChange(themeOption)
    }
}