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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jo.prayertimes.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jo.prayertimes.tasks.ui.TaskViewModel
import com.jo.prayertimes.tasks.ui.stats.ReportsScreen
import java.util.*

class TasksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.jo.prayertimes.tasks.ui.theme.TasksAppTheme(darkTheme = isSystemInDarkTheme()) {
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
                    selected = currentRoute == "daily",
                    onClick = { navController.navigate("daily") },
                    icon = { Text("✅") },
                    label = { Text(stringResource(R.string.tasks_nav_today)) }
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
            startDestination = "daily",
            modifier = Modifier.padding(padding)
        ) {
            composable("daily") { DailyTasksScreen() }
            composable("reports") { ReportsScreen() }
        }
    }
}

@Composable
fun DailyTasksScreen(viewModel: TaskViewModel = viewModel()) {
    val tasks by viewModel.tasksForCurrentDate().collectAsState(initial = emptyList())
    val visibleCategories by viewModel.visibleCategories.collectAsState()
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<com.jo.prayertimes.tasks.data.Category?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visibleCategories) {
        if (selectedCategory == null && visibleCategories.isNotEmpty()) {
            selectedCategory = visibleCategories.first()
        }
    }

    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = stringResource(R.string.tasks_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = newTaskTitle,
            onValueChange = { newTaskTitle = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.tasks_new_task)) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(text = selectedCategory?.let { "${it.icon} ${com.jo.prayertimes.tasks.data.DefaultCategories.displayName(it)}" } ?: stringResource(R.string.tasks_choose_category))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    visibleCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.icon} ${com.jo.prayertimes.tasks.data.DefaultCategories.displayName(cat)}") },
                            onClick = { selectedCategory = cat; expanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = {
                android.app.TimePickerDialog(
                    android.view.ContextThemeWrapper(context, com.jo.prayertimes.R.style.TasksTimePickerDialog),
                    { _, hour, minute -> reminderTime = String.format("%02d:%02d", hour, minute) },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            }) { Text(text = reminderTime ?: stringResource(R.string.tasks_no_reminder)) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val cat = selectedCategory
                if (newTaskTitle.isNotBlank() && cat != null) {
                    viewModel.addTask(newTaskTitle, cat.id, reminderTime)
                    newTaskTitle = ""
                    reminderTime = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.tasks_add)) }

        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Text(text = stringResource(R.string.tasks_empty))
        } else {
            LazyColumn {
                items(tasks) { task ->
                    val cat = visibleCategories.find { it.id == task.category }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = task.isCompleted, onCheckedChange = { viewModel.toggleTask(task) })
                            Text(text = "${cat?.icon ?: ""} ${task.title}")
                        }
                        TextButton(onClick = { viewModel.deleteTask(task) }) { Text(stringResource(R.string.tasks_delete)) }
                    }
                }
            }
        }
    }
}
