package com.jo.prayertimes.tasks.ui.simple

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jo.prayertimes.R
import com.jo.prayertimes.tasks.data.DefaultCategories
import com.jo.prayertimes.tasks.data.DefaultDailyTemplates
import com.jo.prayertimes.tasks.data.SelectedTask
import java.util.*

@Composable
fun SelectionScreen(viewModel: SelectionViewModel = viewModel()) {
    val selected by viewModel.selected.collectAsState()
    var showAddFor by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    fun pickReminderTime(item: SelectedTask) {
        android.app.TimePickerDialog(
            android.view.ContextThemeWrapper(context, R.style.TasksTimePickerDialog),
            { _, hour, minute ->
                viewModel.setReminderTime(item, String.format("%02d:%02d", hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.selection_title), style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = { showResetConfirm = true }) {
                    Text(stringResource(R.string.reset_data_button))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.selection_hint), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        DefaultCategories.list.forEach { cat ->
            item {
                Text(
                    text = "${cat.icon} ${DefaultCategories.displayName(cat)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                )
            }
            val templates = DefaultDailyTemplates.list.filter { it.categoryId == cat.id }
            items(templates) { t ->
                val current = selected.find { it.title == t.title && it.categoryId == cat.id }
                val isOn = current != null
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = t.title, modifier = Modifier.weight(1f))
                        Switch(checked = isOn, onCheckedChange = { viewModel.toggleTemplate(t.title, cat.id) })
                    }
                    if (current != null) {
                        ReminderRow(item = current, onPickTime = { pickReminderTime(current) }, onToggle = { viewModel.toggleReminder(current, it) })
                    }
                }
            }
            val customs = selected.filter { it.isCustom && it.categoryId == cat.id }
            items(customs) { c ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${c.title}  ✏️", modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.removeCustom(c) }) { Text(stringResource(R.string.tasks_delete)) }
                    }
                    ReminderRow(item = c, onPickTime = { pickReminderTime(c) }, onToggle = { viewModel.toggleReminder(c, it) })
                }
            }
            item {
                TextButton(onClick = { showAddFor = cat.id }) {
                    Text(stringResource(R.string.selection_add_custom))
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_data_confirm_title)) },
            text = { Text(stringResource(R.string.reset_data_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllData()
                    showResetConfirm = false
                }) { Text(stringResource(R.string.reset_data_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.tl_cancel)) }
            }
        )
    }

    val targetCategoryId = showAddFor
    if (targetCategoryId != null) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFor = null },
            title = { Text(stringResource(R.string.selection_add_custom)) },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.tl_field_title)) }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) {
                        viewModel.addCustom(text, targetCategoryId)
                        text = ""
                        showAddFor = null
                    }
                }) { Text(stringResource(R.string.tasks_add)) }
            },
            dismissButton = { TextButton(onClick = { showAddFor = null }) { Text(stringResource(R.string.tl_cancel)) } }
        )
    }
}

@Composable
private fun ReminderRow(item: SelectedTask, onPickTime: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPickTime) {
            Text(text = "🔔 " + (item.reminderTime ?: stringResource(R.string.tasks_no_reminder)))
        }
        if (item.reminderTime != null) {
            Switch(checked = item.reminderEnabled, onCheckedChange = onToggle)
        }
    }
}
