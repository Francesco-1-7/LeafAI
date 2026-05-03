package com.example.leafai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.leafai.model.LeafResult
import com.example.leafai.ui.screen.CameraScreen
import com.example.leafai.ui.screen.PlantInputScreen
import com.example.leafai.ui.screen.ResultScreen
import com.example.leafai.ui.screen.UploadScreen
import com.example.leafai.viewmodel.LeafViewModel

/**
 * Navigation destinations for the app.
 */
sealed class Screen(val route: String) {
    object Upload : Screen("upload")
    object PlantInput : Screen("plant_input")
    object Camera : Screen("camera")
    object Result : Screen("result")
}

/**
 * Main navigation graph for the app.
 * Flow: Upload -> PlantInput -> Camera -> Result
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: LeafViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Upload.route
    ) {
        uploadScreen(
            viewModel = viewModel,
            onNavigateToPlantInput = { 
                navController.navigate(Screen.PlantInput.route) {
                    popUpTo(Screen.Upload.route) { inclusive = true }
                }
            }
        )
        plantInputScreen(
            viewModel = viewModel,
            onNavigateToCamera = {
                navController.navigate(Screen.Camera.route)
            }
        )
        cameraScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onNavigateToResult = { result ->
                navController.navigate(Screen.Result.route)
            }
        )
        resultScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onRetake = { navController.navigate(Screen.PlantInput.route) }
        )
    }
}

fun NavGraphBuilder.uploadScreen(
    viewModel: LeafViewModel,
    onNavigateToPlantInput: () -> Unit
) {
    composable(route = Screen.Upload.route) {
        UploadScreen(
            viewModel = viewModel,
            onModelLoaded = onNavigateToPlantInput
        )
    }
}

fun NavGraphBuilder.plantInputScreen(
    viewModel: LeafViewModel,
    onNavigateToCamera: () -> Unit
) {
    composable(route = Screen.PlantInput.route) {
        PlantInputScreen(
            viewModel = viewModel,
            onNext = onNavigateToCamera
        )
    }
}

fun NavGraphBuilder.cameraScreen(
    viewModel: LeafViewModel,
    onBack: () -> Unit,
    onNavigateToResult: (LeafResult) -> Unit
) {
    composable(route = Screen.Camera.route) {
        CameraScreen(
            viewModel = viewModel,
            onBack = onBack,
            onResult = onNavigateToResult
        )
    }
}

fun NavGraphBuilder.resultScreen(
    viewModel: LeafViewModel,
    onBack: () -> Unit,
    onRetake: () -> Unit
) {
    composable(route = Screen.Result.route) {
        ResultScreen(
            viewModel = viewModel,
            onBack = onBack,
            onRetake = onRetake
        )
    }
}
