package com.fittrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fittrack.app.ui.navigation.FitTrackNavGraph
import com.fittrack.app.ui.theme.FitTrackTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Navigation between screens is handled by
 * FitTrackNavGraph (Jetpack Navigation Compose).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitTrackTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FitTrackNavGraph()
                }
            }
        }
    }
}
