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
    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            PhotoListScreen(
                onAddPhoto = { navController.navigate("edit") },
                onEditPhoto = { id -> navController.navigate("edit?id=$id") }
            )
        }
        composable(
            "edit?id={id}",
            arguments = listOf(navArgument("id") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null 
            })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            EditPhotoScreen(
                photoId = id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}