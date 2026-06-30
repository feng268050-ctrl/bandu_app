package com.bandu.tiji

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = statusBarStyleForAppBackground())
        setContent {
            BanduTijiApp()
        }
    }

    private fun statusBarStyleForAppBackground(): SystemBarStyle {
        val isDarkApp = (
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            ) == Configuration.UI_MODE_NIGHT_YES
        return if (isDarkApp) {
            SystemBarStyle.light(Color.WHITE, Color.WHITE)
        } else {
            SystemBarStyle.dark(Color.BLACK)
        }
    }
}
