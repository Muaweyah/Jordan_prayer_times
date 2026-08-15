package com.jo.prayertimes.tasks

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jo.prayertimes.tasks.ui.stats.ReportsScreen
import com.jo.prayertimes.tasks.ui.theme.TasksAppTheme
import com.jo.prayertimes.tasks.ui.timeline.TimelineScreen

class TasksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TasksAppTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TasksRoot()
                }
            }
        }
    }
}

@Composable
fun TasksRoot() {
    val navController = rememberNavController()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                NavigationBarItem(
                    selected = currentRoute == "timeline",
                    onClick = { navController.navigate("timeline") },
                    icon = { Text("🗓️") },
                    label = { Text("الجدول") }
                )
                NavigationBarItem(
                    selected = currentRoute == "reports",
                    onClick = { navController.navigate("reports") },
                    icon = { Text("📊") },
                    label = { Text("التقارير") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "timeline",
            modifier = Modifier.padding(padding)
        ) {
            composable("timeline") { TimelineScreen() }
            composable("reports") { ReportsScreen() }
        }
    }
}
