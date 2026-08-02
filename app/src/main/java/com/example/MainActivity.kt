package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.data.remote.ApiClient
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.PathLabProTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.navigation.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = com.example.data.local.UserPreferencesManager(applicationContext)
        ApiClient.init("https://android-backend-kang.onrender.com/", prefs)
        enableEdgeToEdge()
        setContent {
            PathLabProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isLoggedIn by prefs.isLoggedInFlow.collectAsState(initial = false)
                    val isRegistered by prefs.isRegisteredFlow.collectAsState(initial = false)

                    val startRoute = when {
                        isLoggedIn -> Screen.Dashboard.route
                        isRegistered -> Screen.Login.route
                        else -> Screen.Register.route
                    }

                    AppNavGraph(navController = navController, startDestination = startRoute)
                }
            }
        }
    }
}

