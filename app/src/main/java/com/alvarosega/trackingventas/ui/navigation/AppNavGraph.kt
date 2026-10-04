package com.alvarosega.trackingventas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alvarosega.trackingventas.data.local.SessionManager
import com.alvarosega.trackingventas.ui.home.HomeScreen
import com.alvarosega.trackingventas.ui.login.LoginScreen
import com.alvarosega.trackingventas.ui.saneamiento.SaneamientoFormScreen
import com.alvarosega.trackingventas.ui.saneamiento.SaneamientoViewModel
import com.alvarosega.trackingventas.ui.tracking.MainMapScreen
import com.alvarosega.trackingventas.ui.visita.VisitaRegistrationScreen
import com.alvarosega.trackingventas.ui.visita.VisitaViewModel

object Screen {
    const val LOGIN = "login"
    const val HOME = "home"
    const val MAP = "map"
    const val VISITA_REGISTRATION = "visita_registration"
    const val SANEAMIENTO_FORM = "saneamiento_form?clientId={clientId}"
}

@Composable
fun AppNavGraph(
    sessionManager: SessionManager
) {
    val navController = rememberNavController()

    val startDest = if (!sessionManager.getToken().isNullOrBlank()) {
        Screen.HOME
    } else {
        Screen.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(Screen.LOGIN) {
            LoginScreen(
                onLoginSuccess = { _ ->
                    navController.navigate(Screen.HOME) {
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.HOME) {
            HomeScreen(
                onNavigateToMap = {
                    navController.navigate(Screen.MAP)
                },
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(Screen.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MAP) { backStackEntry ->
            val visitaViewModel: VisitaViewModel = hiltViewModel(backStackEntry)

            MainMapScreen(
                visitaViewModel = visitaViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToVisita = {
                    navController.navigate(Screen.VISITA_REGISTRATION)
                },
                onNavigateToAltaCliente = {
                    navController.navigate("saneamiento_form")
                },
                onNavigateToEditarCliente = { client ->
                    navController.navigate("saneamiento_form?clientId=${client.clientId}")
                }
            )
        }
        composable(Screen.VISITA_REGISTRATION) {
            val mapBackStackEntry = remember(it) {
                navController.getBackStackEntry(Screen.MAP)
            }
            val sharedVisitaViewModel: VisitaViewModel = hiltViewModel(mapBackStackEntry)

            VisitaRegistrationScreen(
                viewModel = sharedVisitaViewModel,
                sellerCode = "VENDEDOR",
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisteredSuccessfully = {
                    navController.popBackStack()
                },
                onNavigateToSaneamiento = { clientId ->
                    if (clientId != null) {
                        navController.navigate("saneamiento_form?clientId=$clientId")
                    } else {
                        navController.navigate("saneamiento_form")
                    }
                }
            )
        }

        composable(
            route = Screen.SANEAMIENTO_FORM,
            arguments = listOf(
                navArgument("clientId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("clientId")
            val clientId = rawId?.toLongOrNull()
            val saneamientoViewModel: SaneamientoViewModel = hiltViewModel()

            SaneamientoFormScreen(
                viewModel = saneamientoViewModel,
                clientId = clientId,
                sellerCode = "VENDEDOR",
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSavedSuccessfully = {
                    navController.popBackStack()
                }
            )
        }
    }
}