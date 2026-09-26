package com.sagar.babystatus

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.ui.BabyStatusApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainActivityUiState.Loading }

        // Every screen's top is forest green, so status bar icons are always light.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            BabyStatusTheme {
                (uiState as? MainActivityUiState.Ready)?.let { ready ->
                    BabyStatusApp(
                        hasBaby = ready.hasBaby,
                        deletedIds = viewModel.deletedIds,
                        onUndoDelete = viewModel::undoDelete,
                    )
                }
            }
        }
    }
}
