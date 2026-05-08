package com.yourapp.timemanagement

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yourapp.timemanagement.core.TimeManagementViewModel
import com.yourapp.timemanagement.core.TimeManagementViewModelFactory
import com.yourapp.timemanagement.ui.TimeManagementApp
import com.yourapp.timemanagement.ui.theme.TimeManagementTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotifications()
        setContent {
            val viewModel: TimeManagementViewModel = viewModel(
                factory = TimeManagementViewModelFactory(appContainer()),
            )
            val uiState by viewModel.uiState.collectAsState()
            TimeManagementTheme(settings = uiState.settings) {
                TimeManagementApp(uiState = uiState, viewModel = viewModel)
            }
        }
    }

    private fun maybeRequestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }
}
