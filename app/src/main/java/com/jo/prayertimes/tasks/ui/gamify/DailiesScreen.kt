package com.jo.prayertimes.tasks.ui.gamify

import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jo.prayertimes.R
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
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tasks_add)) }
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
        AddDailyDialog(categories = categories, onDismiss = { showAdd = false }, onConfirm = { t, c, d, rt, days, md, ym, yd ->
            viewModel.addDaily(t, c, d, rt, days, md, ym, yd)
            showAdd = false
        })
    }
}

@Composable
private fun DailyRow(task: Task, done: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    val cat = DefaultCategories.list.find { it.id == task.category }
    val freqLabel = when (task.recurrenceType) {
        "MONTHLY" -> "📅 ${task.monthDay}"
        "YEARLY" -> "📅 ${task.yearDay}/${task.yearMonth}"
        else -> null
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Checkbox(checked = done, onCheckedChange = { onToggle() })
            Column {
                Text(text = "${cat?.icon ?: ""} ${task.title}")
                if (freqLabel != null) {
                    Text(text = freqLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.tasks_delete)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDailyDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, List<Int>?, Int?, Int?, Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var recurrenceType by remember { mutableStateOf("WEEKLY") }
    val selectedDays = remember { mutableStateListOf(0, 1, 2, 3, 4, 5, 6) }
    var monthDayText by remember { mutableStateOf("1") }
    var yearMonthText by remember { mutableStateOf("1") }
    var yearDayText by remember { mutableStateOf("1") }

    LaunchedEffect(categories) { if (selectedCategory == null) selectedCategory = categories.firstOrNull() }
    val weekDayLabels = androidx.compose.ui.res.stringArrayResource(id = R.array.week_day_labels)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_daily_title)) },
        text = {
            var titleSuggestionsExpanded by remember { mutableStateOf(false) }
            var isCustomEntry by remember { mutableStateOf(false) }
            val titleFocusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.tl_field_title)) },
                        readOnly = !isCustomEntry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocusRequester)
                            .then(if (!isCustomEntry) Modifier.clickable { titleSuggestionsExpanded = true } else Modifier),
                        trailingIcon = { IconButton(onClick = { titleSuggestionsExpanded = true }) { Text("▾") } }
                    )
                    val suggestions = selectedCategory?.let { cat ->
                        com.jo.prayertimes.tasks.data.DefaultDailyTemplates.list.filter { it.categoryId == cat.id }
                    } ?: emptyList()
                    DropdownMenu(expanded = titleSuggestionsExpanded && suggestions.isNotEmpty(), onDismissRequest = { titleSuggestionsExpanded = false }) {
                        suggestions.forEach { s ->
                            DropdownMenuItem(text = { Text(s.title) }, onClick = { title = s.title; isCustomEntry = false; titleSuggestionsExpanded = false })
                        }
                        DropdownMenuItem(text = { Text(stringResource(R.string.add_new_custom)) }, onClick = {
                            title = ""
                            isCustomEntry = true
                            titleSuggestionsExpanded = false
                            titleFocusRequester.requestFocus()
                            keyboardController?.show()
                        })
                    }
                }
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
                Text(stringResource(R.string.recurrence_type_label), style = MaterialTheme.typography.labelMedium)
                Row {
                    FilterChip(selected = recurrenceType == "WEEKLY", onClick = { recurrenceType = "WEEKLY" }, label = { Text(stringResource(R.string.recurrence_weekly)) }, modifier = Modifier.padding(end = 4.dp))
                    FilterChip(selected = recurrenceType == "MONTHLY", onClick = { recurrenceType = "MONTHLY" }, label = { Text(stringResource(R.string.recurrence_monthly)) }, modifier = Modifier.padding(end = 4.dp))
                    FilterChip(selected = recurrenceType == "YEARLY", onClick = { recurrenceType = "YEARLY" }, label = { Text(stringResource(R.string.recurrence_yearly)) })
                }

                Spacer(modifier = Modifier.height(8.dp))
                when (recurrenceType) {
                    "WEEKLY" -> {
                        Text(stringResource(R.string.recurrence_days_label), style = MaterialTheme.typography.labelMedium)
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
                    "MONTHLY" -> {
                        OutlinedTextField(
                            value = monthDayText,
                            onValueChange = { if (it.length <= 2) monthDayText = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.recurrence_day_of_month)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "YEARLY" -> {
                        Row {
                            OutlinedTextField(
                                value = yearDayText,
                                onValueChange = { if (it.length <= 2) yearDayText = it.filter { c -> c.isDigit() } },
                                label = { Text(stringResource(R.string.recurrence_day)) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = yearMonthText,
                                onValueChange = { if (it.length <= 2) yearMonthText = it.filter { c -> c.isDigit() } },
                                label = { Text(stringResource(R.string.recurrence_month)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    when (recurrenceType) {
                        "WEEKLY" -> onConfirm(title, selectedCategory?.id ?: "work", difficulty.name, "WEEKLY", selectedDays.sorted(), null, null, null)
                        "MONTHLY" -> onConfirm(title, selectedCategory?.id ?: "work", difficulty.name, "MONTHLY", null, monthDayText.toIntOrNull()?.coerceIn(1, 31) ?: 1, null, null)
                        else -> onConfirm(title, selectedCategory?.id ?: "work", difficulty.name, "YEARLY", null, null, yearMonthText.toIntOrNull()?.coerceIn(1, 12) ?: 1, yearDayText.toIntOrNull()?.coerceIn(1, 31) ?: 1)
                    }
                }
            }) { Text(stringResource(R.string.tasks_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tl_cancel)) } }
    )
}
