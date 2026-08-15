package com.jo.prayertimes.tasks.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jo.prayertimes.tasks.data.Category
import com.jo.prayertimes.tasks.data.DefaultCategories
import com.jo.prayertimes.tasks.data.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    viewModel: TimelineViewModel,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    val subtasks by viewModel.subtasksFor(task.id).collectAsState(initial = emptyList())
    var newSubtask by remember { mutableStateOf("") }
    val cat = categories.find { it.id == task.category }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${cat?.icon ?: ""} ${task.title}", style = MaterialTheme.typography.titleLarge)
                Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle(task) })
            }

            if (task.startTime != null) {
                Text(text = "${task.startTime} - ${task.endTime ?: ""}", style = MaterialTheme.typography.bodyMedium)
            }

            if (!task.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "📝 ${task.notes}")
            }
            if (!task.linkUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "🔗 ${task.linkUrl}")
            }
            if (task.isRecurring) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "🔁 مهمة متكررة", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "المهام الفرعية", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                OutlinedTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    label = { Text("خطوة جديدة") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newSubtask.isNotBlank()) {
                        viewModel.addSubtask(task.id, newSubtask, task.category)
                        newSubtask = ""
                    }
                }) { Text("إضافة") }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(subtasks) { sub ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = sub.isCompleted, onCheckedChange = { onToggle(sub) })
                        Text(text = sub.title)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onDelete(task) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("حذف المهمة") }
        }
    }
}
