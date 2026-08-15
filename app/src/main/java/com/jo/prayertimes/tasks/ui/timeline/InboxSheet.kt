package com.jo.prayertimes.tasks.ui.timeline

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jo.prayertimes.R
import com.jo.prayertimes.tasks.data.Category
import com.jo.prayertimes.tasks.data.DefaultCategories
import com.jo.prayertimes.tasks.data.Task
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSheet(
    inbox: List<Task>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onSchedule: (Task, String, String) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    var taskBeingScheduled by remember { mutableStateOf<Task?>(null) }
    var pickedStart by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.tl_inbox_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Row {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text(stringResource(R.string.tl_inbox_placeholder)) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newTitle.isNotBlank()) {
                        onAdd(newTitle, categories.firstOrNull()?.id ?: "work")
                        newTitle = ""
                    }
                }) { Text(stringResource(R.string.tasks_add)) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (inbox.isEmpty()) {
                Text(stringResource(R.string.tl_inbox_empty))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(inbox) { task ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = task.title, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                android.app.TimePickerDialog(
                                    android.view.ContextThemeWrapper(context, R.style.TasksTimePickerDialog),
                                    { _, hour, minute ->
                                        val start = String.format("%02d:%02d", hour, minute)
                                        val endCal = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, hour)
                                            set(Calendar.MINUTE, minute)
                                            add(Calendar.MINUTE, 30)
                                        }
                                        val end = String.format(
                                            "%02d:%02d",
                                            endCal.get(Calendar.HOUR_OF_DAY),
                                            endCal.get(Calendar.MINUTE)
                                        )
                                        onSchedule(task, start, end)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            }) { Text(stringResource(R.string.tl_schedule_btn)) }
                        }
                    }
                }
            }
        }
    }
}
