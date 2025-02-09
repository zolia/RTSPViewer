package com.cameraviewer.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cameraviewer.ui.screens.CameraListScreen
import com.cameraviewer.ui.screens.LiveViewScreen

@Composable
fun CameraApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "cameras") {
        composable("cameras") {
            CameraListScreen(
                onCameraClick = { cameraId ->
                    navController.navigate("live/$cameraId")
                }
            )
        }
        composable("live/{cameraId}") { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getString("cameraId")?.toLongOrNull() ?: return@composable
            LiveViewScreen(
                cameraId = cameraId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}