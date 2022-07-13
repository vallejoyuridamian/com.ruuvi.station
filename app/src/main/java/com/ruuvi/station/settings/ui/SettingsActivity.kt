package com.ruuvi.station.settings.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dfu.ui.MyTopAppBar
import com.ruuvi.station.util.extensions.navigate
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.coroutines.flow.collect
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class SettingsActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val appSettingsListViewModel: AppSettingsListViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RuuviTheme() {
                val navController = rememberNavController()
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val activity = LocalContext.current as Activity
                var title: String by rememberSaveable { mutableStateOf("") }

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        title = SettingsRoutes.getTitleByRoute(
                            activity,
                            backStackEntry.destination.route ?: ""
                        )
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.background,
                    topBar = { MyTopAppBar(title = title) },
                    scaffoldState = scaffoldState
                ) {

                    NavHost(navController = navController, startDestination = SettingsRoutes.LIST) {
                        composable(SettingsRoutes.LIST) {
                            SettingsList(scaffoldState, navController::navigate, appSettingsListViewModel)
                        }
                        composable(SettingsRoutes.TEMPERATURE) {
                            TemperatureSettings(scaffoldState = scaffoldState)
                        }
                        composable(SettingsRoutes.HUMIDITY) {
                            HumiditySettings(scaffoldState = scaffoldState)
                        }
                        composable(SettingsRoutes.PRESSURE) {
                            PressureSettings(scaffoldState = scaffoldState)
                        }
                    }
                }

                val systemBarsColor = RuuviStationTheme.colors.systemBars
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = systemBarsColor,
                        darkIcons = false
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val settingsIntent = Intent(context, SettingsActivity::class.java)
            context.startActivity(settingsIntent)
        }
    }
}

@Composable
fun TemperatureSettings(
    scaffoldState: ScaffoldState
) {
    Text(text = "TemperatureSettings")
}

@Composable
fun PressureSettings(
    scaffoldState: ScaffoldState
) {
    Text(text = "PressureSettings")
}


@Composable
fun HumiditySettings(
    scaffoldState: ScaffoldState
) {
    Text(text = "HumiditySettings")
}