package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.CartFinderTheme
import com.example.ui.viewmodel.CartFinderViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CartFinderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CartFinderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Only track when the app is actively in the foreground
        viewModel.startTracking()
    }

    override fun onPause() {
        super.onPause()
        // Immediately release GPS chip and hardware compass sensors when minimized or screen turned off
        viewModel.stopTracking()
    }
}
