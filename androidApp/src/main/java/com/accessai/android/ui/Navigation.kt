package com.accessai.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.accessai.android.ui.screens.home.HomeScreen
import com.accessai.android.ui.screens.vision.VisionScreen
import com.accessai.android.ui.screens.audio.AudioScreen
import com.accessai.android.ui.screens.navigation.NavigationScreen
import com.accessai.android.ui.screens.communication.CommunicationScreen

/**
 * Navigation route definitions.
 */
object Routes {
    const val HOME = "home"
    const val VISION = "vision"
    const val AUDIO = "audio"
    const val NAVIGATION = "navigation"
    const val COMMUNICATION = "communication"
}

/**
 * Main navigation host for AccessAI.
 * Each accessibility module has its own screen/route.
 */
@Composable
fun AccessAINavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToVision = { navController.navigate(Routes.VISION) },
                onNavigateToAudio = { navController.navigate(Routes.AUDIO) },
                onNavigateToNavigation = { navController.navigate(Routes.NAVIGATION) },
                onNavigateToCommunication = { navController.navigate(Routes.COMMUNICATION) }
            )
        }

        composable(Routes.VISION) {
            VisionScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.AUDIO) {
            AudioScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.NAVIGATION) {
            NavigationScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.COMMUNICATION) {
            CommunicationScreen(onBack = { navController.popBackStack() })
        }
    }
}
