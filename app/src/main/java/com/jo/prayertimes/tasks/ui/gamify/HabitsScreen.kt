package com.jo.prayertimes.tasks.ui.gamify

import com.jo.prayertimes.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun HabitsScreen(viewModel: HabitsViewModel = viewModel()) {
    val stats by viewModel.stats.collectAsState()
    val habits by viewModel.habitsList.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = "إضافة") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.tl_nav_habits), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            StatsBar(stats)
            Spacer(modifier = Modifier.height(16.dp))

            if (habits.isEmpty()) {
                Text(stringResource(R.string.habits_empty))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(habits) { habit ->
                        HabitRow(habit, onPositive = { viewModel.tapPositive(habit) }, onNegative = { viewModel.tapNegative(habit) }, onDelete = { viewModel.deleteHabit(habit) })
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddHabitDialog(categories = categories, onDismiss = { showAdd = false }, onConfirm = { t, c, d, p, n ->
            viewModel.addHabit(t, c, d, p, n)
            showAdd = false
        })
    }
}

@Composable
private fun HabitRow(task: Task, onPositive: () -> Unit, onNegative: () -> Unit, onDelete: () -> Unit) {
    val cat = DefaultCategories.list.find { it.id == task.category }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "${cat?.icon ?: ""} ${task.title}", modifier = Modifier.weight(1f))
        if (task.isNegativeHabit) {
            OutlinedButton(onClick = onNegative) { Text("−") }
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (task.isPositiveHabit) {
            Button(onClick = onPositive) { Text("+") }
        }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.tasks_delete)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHabitDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var positive by remember { mutableStateOf(true) }
    var negative by remember { mutableStateOf(false) }

    LaunchedEffect(categories) { if (selectedCategory == null) selectedCategory = categories.firstOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_habit_title)) },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.habit_name_label)) }, modifier = Modifier.fillMaxWidth())
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = positive, onCheckedChange = { positive = it })
                    Text(stringResource(R.string.positive_label))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = negative, onCheckedChange = { negative = it })
                    Text(stringResource(R.string.negative_label))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onConfirm(title, selectedCategory?.id ?: "work", difficulty.name, positive, negative)
            }) { Text(stringResource(R.string.tasks_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tl_cancel)) } }
    )
}
