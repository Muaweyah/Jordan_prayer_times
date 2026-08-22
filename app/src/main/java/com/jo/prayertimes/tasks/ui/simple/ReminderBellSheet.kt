package com.jo.prayertimes.tasks.ui.simple

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jo.prayertimes.R
import com.jo.prayertimes.tasks.data.DefaultCategories

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBellSheet(onDismiss: () -> Unit, viewModel: ReminderBellViewModel = viewModel()) {
    val reminders by viewModel.reminders.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.reminder_bell_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            if (reminders.isEmpty()) {
                Text(stringResource(R.string.reminder_bell_empty))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(reminders) { item ->
                        val cat = DefaultCategories.list.find { it.id == item.categoryId }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${cat?.icon ?: ""} ${item.title}")
                            Text(text = "🔔 ${item.reminderTime}")
                        }
                    }
                }
            }
        }
    }
}
