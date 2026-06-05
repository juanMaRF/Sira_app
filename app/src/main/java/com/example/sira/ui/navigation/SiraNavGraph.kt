package com.example.sira.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sira.data.model.SensorType
import com.example.sira.ui.auth.AuthViewModel
import com.example.sira.ui.auth.LoginScreen
import com.example.sira.ui.home.HomeScreen
import com.example.sira.ui.plantlist.PlantListScreen
import com.example.sira.ui.plantlist.RegisterPlantScreen
import com.example.sira.ui.profile.ProfileScreen
import com.example.sira.ui.sensordetail.SensorDetailScreen

/** Rutas y nombres de argumentos de navegación. */
object SiraRoutes {
    const val LOGIN = "login"
    const val PLANT_LIST = "plant_list"
    const val REGISTER = "register"
    const val PROFILE = "profile"
    const val HOME = "home"
    const val SENSOR_DETAIL = "sensor_detail"

    const val PLANT_ARG = "plantId"
    const val SENSOR_ARG = "sensor"

    fun home(plantId: String) = "$HOME/$plantId"
    fun sensorDetail(plantId: String, type: SensorType) = "$SENSOR_DETAIL/$plantId/${type.name}"
}

/**
 * Grafo de navegación. Comparte un único [AuthViewModel] para que el estado de
 * sesión sea coherente en toda la app.
 *
 * Flujo: login → mis plantas → (registro) / home(plantId) → detalle de sensor.
 */
@Composable
fun SiraNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val startDestination = if (currentUser != null) SiraRoutes.PLANT_LIST else SiraRoutes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(SiraRoutes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoggedIn = {
                    navController.navigate(SiraRoutes.PLANT_LIST) {
                        popUpTo(SiraRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(SiraRoutes.PLANT_LIST) {
            PlantListScreen(
                onPlantClick = { plantId ->
                    navController.navigate(SiraRoutes.home(plantId))
                },
                onAddPlant = { navController.navigate(SiraRoutes.REGISTER) },
                onProfile = { navController.navigate(SiraRoutes.PROFILE) }
            )
        }

        composable(SiraRoutes.REGISTER) {
            RegisterPlantScreen(
                onRegistered = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(SiraRoutes.PROFILE) {
            ProfileScreen(
                viewModel = authViewModel,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(SiraRoutes.LOGIN) {
                        popUpTo(SiraRoutes.PLANT_LIST) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${SiraRoutes.HOME}/{${SiraRoutes.PLANT_ARG}}",
            arguments = listOf(navArgument(SiraRoutes.PLANT_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString(SiraRoutes.PLANT_ARG).orEmpty()
            HomeScreen(
                onSensorClick = { sensor ->
                    navController.navigate(SiraRoutes.sensorDetail(plantId, sensor))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${SiraRoutes.SENSOR_DETAIL}/{${SiraRoutes.PLANT_ARG}}/{${SiraRoutes.SENSOR_ARG}}",
            arguments = listOf(
                navArgument(SiraRoutes.PLANT_ARG) { type = NavType.StringType },
                navArgument(SiraRoutes.SENSOR_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sensorName = backStackEntry.arguments?.getString(SiraRoutes.SENSOR_ARG)
            val sensor = runCatching { SensorType.valueOf(sensorName ?: "") }
                .getOrDefault(SensorType.SOIL_MOISTURE)
            SensorDetailScreen(
                sensorType = sensor,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
