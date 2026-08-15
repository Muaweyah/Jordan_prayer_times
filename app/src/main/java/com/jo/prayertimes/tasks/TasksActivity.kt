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
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
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
                    label = { Text("اليوم") }
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
        Text(text = "مهام اليوم", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = newTaskTitle,
            onValueChange = { newTaskTitle = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("مهمة جديدة") }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(text = selectedCategory?.let { "${it.icon} ${it.nameAr}" } ?: "اختر تصنيف")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    visibleCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.icon} ${cat.nameAr}") },
                            onClick = { selectedCategory = cat; expanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = {
                android.app.TimePickerDialog(
                    context,
                    { _, hour, minute -> reminderTime = String.format("%02d:%02d", hour, minute) },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            }) { Text(text = reminderTime ?: "بدون تنبيه") }
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
        ) { Text("إضافة") }

        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Text(text = "لا توجد مهام بعد — أضف أول مهمة لك")
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
                        TextButton(onClick = { viewModel.deleteTask(task) }) { Text("حذف") }
                    }
                }
            }
        }
    }
}
