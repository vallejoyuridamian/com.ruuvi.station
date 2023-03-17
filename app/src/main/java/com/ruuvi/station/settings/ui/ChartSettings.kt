package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
fun ChartSettings(
    scaffoldState: ScaffoldState,
    viewModel: ChartSettingsViewModel
) {
    val displayAllPoint = viewModel.showAllPoints.collectAsState()
    val drawDots = viewModel.drawDots.collectAsState()
    val numberOfDays = viewModel.viewPeriod.collectAsState()

    val numberOfDaysOptions = remember {
        viewModel.getViewPeriodOptions()
    }
    val initialValue = numberOfDaysOptions.firstOrNull { it.value == numberOfDays.value }

    PageSurfaceWithPadding {
        Column {
            SwitchRuuvi(
                text = stringResource(id = R.string.settings_chart_all_points),
                checked = displayAllPoint.value,
                onCheckedChange = viewModel::setShowAllPoints
            )
            Paragraph(text = stringResource(id = R.string.settings_chart_all_points_description))

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

            SwitchRuuvi(
                text = stringResource(id = R.string.settings_chart_draw_dots),
                checked = drawDots.value,
                onCheckedChange = viewModel::setDrawDots
            )
            Paragraph(text = stringResource(id = R.string.settings_chart_draw_dots_description))

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

            SubtitleWithPadding(text = stringResource(id = R.string.settings_chart_view_period))
            Paragraph(text = stringResource(id = R.string.settings_chart_view_period_description))
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
            TwoButtonsSelector(
                values = numberOfDaysOptions,
                initialValue = initialValue ?: numberOfDaysOptions.first(),
                onValueChanged = viewModel::setViewPeriod
            )
        }
    }
}