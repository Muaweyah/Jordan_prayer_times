package com.jo.prayertimes.tasks.ui.timeline

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jo.prayertimes.R
import com.jo.prayertimes.tasks.data.Category
import com.jo.prayertimes.tasks.data.DefaultCategories
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimedTaskDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, categoryId: String, start: String, end: String, notes: String?, link: String?, recurring: Boolean, days: String?) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateListOf<Int>() }

    LaunchedEffect(categories) {
        if (selectedCategory == null) selectedCategory = categories.firstOrNull()
    }

    fun pickTime(onPicked: (String) -> Unit) {
        android.app.TimePickerDialog(
            android.view.ContextThemeWrapper(context, R.style.TasksTimePickerDialog),
            { _, hour, minute -> onPicked(String.format("%02d:%02d", hour, minute)) },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tl_add_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.tl_field_title)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCategory?.let { "${it.icon} ${DefaultCategories.displayName(it)}" } ?: stringResource(R.string.tasks_choose_category))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.icon} ${DefaultCategories.displayName(cat)}") },
                                onClick = { selectedCategory = cat; expanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = { pickTime { startTime = it } }, modifier = Modifier.weight(1f)) {
                        Text(if (startTime.isBlank()) stringResource(R.string.tl_start_time) else startTime)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { pickTime { endTime = it } }, modifier = Modifier.weight(1f)) {
                        Text(if (endTime.isBlank()) stringResource(R.string.tl_end_time) else endTime)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.tl_notes)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text(stringResource(R.string.tl_link)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Text(stringResource(R.string.tl_recurring))
                }

                if (isRecurring) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    val weekDayLabels = androidx.compose.ui.res.stringArrayResource(id = R.array.week_day_labels)
                        weekDayLabels.forEachIndexed { index, label ->
                            FilterChip(
                                selected = selectedDays.contains(index),
                                onClick = {
                                    if (selectedDays.contains(index)) selectedDays.remove(index)
                                    else selectedDays.add(index)
                                },
                                label = { Text(label) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && startTime.isNotBlank()) {
                    onConfirm(
                        title,
                        selectedCategory?.id ?: "work",
                        startTime,
                        endTime.ifBlank { startTime },
                        notes.ifBlank { null },
                        link.ifBlank { null },
                        isRecurring,
                        if (isRecurring) selectedDays.sorted().joinToString(",") else null
                    )
                }
            }) { Text(stringResource(R.string.tasks_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tl_cancel)) }
        }
    )
}
