package com.jo.prayertimes.tasks

import android.Manifest
import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jo.prayertimes.R
import com.jo.prayertimes.tasks.ui.focus.PomodoroScreen
import com.jo.prayertimes.tasks.ui.archive.ArchiveScreen
import com.jo.prayertimes.tasks.ui.simple.SelectionScreen
import com.jo.prayertimes.tasks.ui.simple.TodayScreen
import com.jo.prayertimes.tasks.ui.stats.ReportsScreen
import com.jo.prayertimes.tasks.ui.theme.TasksAppTheme

class TasksActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.jo.prayertimes.LocaleHelper.wrap(newBase))
    }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksRoot() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tl_app_title)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Text("🏠")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                NavigationBarItem(
                    selected = currentRoute == "selection",
                    onClick = { navController.navigate("selection") },
                    icon = { Text("📋") },
                    label = { Text(stringResource(R.string.tl_nav_selection)) }
                )
                NavigationBarItem(
                    selected = currentRoute == "today",
                    onClick = { navController.navigate("today") },
                    icon = { Text("✅") },
                    label = { Text(stringResource(R.string.tl_nav_today_simple)) }
                )
                NavigationBarItem(
                    selected = currentRoute == "archive",
                    onClick = { navController.navigate("archive") },
                    icon = { Text("🗄️") },
                    label = { Text(stringResource(R.string.tl_nav_archive)) }
                )
                NavigationBarItem(
                    selected = currentRoute == "focus",
                    onClick = { navController.navigate("focus") },
                    icon = { Text("⏱️") },
                    label = { Text(stringResource(R.string.tl_nav_focus)) }
                )
                NavigationBarItem(
                    selected = currentRoute == "reports",
                    onClick = { navController.navigate("reports") },
                    icon = { Text("📊") },
                    label = { Text(stringResource(R.string.tasks_nav_reports)) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "selection",
            modifier = Modifier.padding(padding)
        ) {
            composable("selection") { SelectionScreen() }
            composable("today") { TodayScreen() }
            composable("archive") { ArchiveScreen() }
            composable("focus") { PomodoroScreen() }
            composable("reports") { ReportsScreen() }
        }
    }
}
