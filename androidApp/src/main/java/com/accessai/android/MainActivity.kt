package com.accessai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.accessai.android.ui.AccessAINavHost
import com.accessai.android.ui.theme.AccessAITheme

/**
 * Main entry point for the AccessAI Android app.
 * Sets up edge-to-edge display and Compose navigation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AccessAITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AccessAINavHost(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
