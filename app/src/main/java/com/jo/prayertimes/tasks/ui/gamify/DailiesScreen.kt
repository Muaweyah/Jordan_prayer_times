package com.jo.prayertimes.tasks.ui.gamify

import com.jo.prayertimes.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jo.prayertimes.tasks.data.Category
import com.jo.prayertimes.tasks.data.DefaultCategories
import com.jo.prayertimes.tasks.data.Difficulty
import com.jo.prayertimes.tasks.data.Task

@Composable
fun DailiesScreen(viewModel: DailiesViewModel = viewModel()) {
    val stats by viewModel.stats.collectAsState()
    val dailies by viewModel.dailies.collectAsState()
    val completedToday by viewModel.completedToday.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = "إضافة") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.tl_nav_dailies), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            StatsBar(stats)
            Spacer(modifier = Modifier.height(16.dp))

            if (dailies.isEmpty()) {
                Text(stringResource(R.string.dailies_empty))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(dailies) { task ->
                        val done = completedToday.contains(task.id)
                        DailyRow(task, done, onToggle = { viewModel.toggle(task) }, onDelete = { viewModel.deleteDaily(task) })
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddDailyDialog(categories = categories, onDismiss = { showAdd = false }, onConfirm = { t, c, d, days ->
            viewModel.addDaily(t, c, d, days)
            showAdd = false
        })
    }
}

@Composable
private fun DailyRow(task: Task, done: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    val cat = DefaultCategories.list.find { it.id == task.category }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Checkbox(checked = done, onCheckedChange = { onToggle() })
            Text(text = "${cat?.icon ?: ""} ${task.title}")
        }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.tasks_delete)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDailyDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, List<Int>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    val selectedDays = remember { mutableStateListOf(0, 1, 2, 3, 4, 5, 6) }

    LaunchedEffect(categories) { if (selectedCategory == null) selectedCategory = categories.firstOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_daily_title)) },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.tl_field_title)) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCategory?.let { "${it.icon} ${DefaultCategories.displayName(it)}" } ?: stringResource(R.string.tasks_choose_category))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text("${cat.icon} ${DefaultCategories.displayName(cat)}") }, onClick = { selectedCategory = cat; expanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Difficulty.values().forEach { d ->
                        FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(difficultyLabel(d)) }, modifier = Modifier.padding(end = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.recurrence_days_label), style = MaterialTheme.typography.labelMedium)
                val weekDayLabels = androidx.compose.ui.res.stringArrayResource(id = R.array.week_day_labels)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    weekDayLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedDays.contains(index),
                            onClick = { if (selectedDays.contains(index)) selectedDays.remove(index) else selectedDays.add(index) },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onConfirm(title, selectedCategory?.id ?: "work", difficulty.name, selectedDays.sorted())
            }) { Text(stringResource(R.string.tasks_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tl_cancel)) } }
    )
}
