package com.currencyconverter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.currencyconverter.app.data.ConnectivityObserver
import com.currencyconverter.app.data.PreferencesRepository
import com.currencyconverter.app.data.RatesRepository
import com.currencyconverter.app.ui.ConverterScreen
import com.currencyconverter.app.ui.theme.CurrencyConverterTheme
import com.currencyconverter.app.viewmodel.ConverterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = RatesRepository(applicationContext)
        val preferences = PreferencesRepository(applicationContext)
        val connectivity = ConnectivityObserver(applicationContext)

        setContent {
            val viewModel: ConverterViewModel = viewModel(factory = ConverterViewModel.Factory(repository, preferences, connectivity, applicationContext))
            val state by viewModel.uiState.collectAsState()

            LaunchedEffect(state.darkTheme) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !state.darkTheme
                insetsController.isAppearanceLightNavigationBars = !state.darkTheme
            }

            CurrencyConverterTheme(darkTheme = state.darkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CurrencyConverterTheme.colors.bg)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    ConverterScreen(state = state, viewModel = viewModel, assets = assets)
                }
            }
        }
    }
}
