package com.jo.prayertimes.tasks.ui.timeline

import com.jo.prayertimes.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jo.prayertimes.tasks.data.Category
import com.jo.prayertimes.tasks.data.DefaultCategories
import com.jo.prayertimes.tasks.data.Task
import java.util.*

private fun categoryColor(catId: String): Color {
    val cat = DefaultCategories.list.find { it.id == catId }
    return try {
        Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#9E9E9E"))
    } catch (e: Exception) {
        Color(0xFF9E9E9E)
    }
}

@Composable
fun TimelineScreen(viewModel: TimelineViewModel = viewModel()) {
    val tasks by viewModel.tasksForDate().collectAsState(initial = emptyList())
    val inbox by viewModel.inboxTasks().collectAsState(initial = emptyList())
    val visibleCategories by viewModel.visibleCategories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showInbox by remember { mutableStateOf(false) }
    var detailTask by remember { mutableStateOf<Task?>(null) }

    val blocks = remember(tasks) { viewModel.buildBlocks(tasks) }
    val blocksByHour = remember(blocks) { blocks.groupBy { it.startMinutes / 60 } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tasks_add))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.tl_header), style = MaterialTheme.typography.headlineMedium)
                BadgedBox(badge = {
                    if (inbox.isNotEmpty()) Badge { Text("${inbox.size}") }
                }) {
                    IconButton(onClick = { showInbox = true }) {
                        Text("📥")
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items((6..23).toList()) { hour ->
                    val hourBlocks = blocksByHour[hour] ?: emptyList()
                    HourRow(hour = hour, blocks = hourBlocks, onTaskClick = { detailTask = it })
                }
            }
        }
    }

    if (showAddDialog) {
        AddTimedTaskDialog(
            categories = visibleCategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, cat, start, end, notes, link, recurring, days ->
                viewModel.addTimedTask(title, cat, start, end, notes, link, recurring, days)
                showAddDialog = false
            }
        )
    }

    if (showInbox) {
        InboxSheet(
            inbox = inbox,
            categories = visibleCategories,
            onDismiss = { showInbox = false },
            onAdd = { title, cat -> viewModel.addInboxTask(title, cat) },
            onSchedule = { task, start, end -> viewModel.scheduleFromInbox(task, start, end) }
        )
    }

    detailTask?.let { task ->
        TaskDetailSheet(
            task = task,
            viewModel = viewModel,
            categories = visibleCategories,
            onDismiss = { detailTask = null },
            onToggle = { viewModel.toggleTask(it) },
            onDelete = { viewModel.deleteTask(it); detailTask = null }
        )
    }
}

@Composable
private fun HourRow(hour: Int, blocks: List<TimelineBlock>, onTaskClick: (Task) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = String.format("%02d:00", hour),
            modifier = Modifier.width(56.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            if (blocks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                )
            } else {
                blocks.forEach { block ->
                    val h = ((block.endMinutes - block.startMinutes).coerceAtLeast(20) / 2).dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor(block.task.category).copy(alpha = 0.85f))
                            .padding(8.dp)
                            .then(Modifier),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).clickable { onTaskClick(block.task) }) {
                            Text(
                                text = block.task.title,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${block.task.startTime} - ${block.task.endTime ?: ""}",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (block.task.isCompleted) {
                            Text(text = "✓", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
