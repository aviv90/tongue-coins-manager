package com.krumin.tonguecoinsmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.krumin.tonguecoinsmanager.ui.navigation.Screen
import com.krumin.tonguecoinsmanager.ui.screens.EditPhotoScreen
import com.krumin.tonguecoinsmanager.ui.screens.PhotoListScreen
import com.krumin.tonguecoinsmanager.ui.theme.TongueCoinsManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TongueCoinsManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TongueCoinsApp()
                }
            }
        }
    }
}

@Composable
fun TongueCoinsApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.List.route) {
        composable(Screen.List.route) {
            PhotoListScreen(
                onAddPhoto = { navController.navigate(Screen.Edit.createRoute(null)) },
                onEditPhoto = { id -> navController.navigate(Screen.Edit.createRoute(id)) },
                navController = navController
            )
        }
        composable(
            route = Screen.Edit.route,
            arguments = listOf(navArgument(Screen.ARG_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(Screen.ARG_ID)
            EditPhotoScreen(
                photoId = id,
                onBack = { navController.popBackStack() },
                onResult = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        Screen.RESULT_KEY,
                        result
                    )
                }
            )
        }
        composable(Screen.DailyRiddle.route) {
            com.krumin.tonguecoinsmanager.ui.screens.DailyRiddleScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}