package com.jo.prayertimes.tasks.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jo.prayertimes.R
import com.jo.prayertimes.tasks.data.DefaultCategories
import com.jo.prayertimes.tasks.data.SelectedTask
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ArchiveScreen(viewModel: ArchiveViewModel = viewModel()) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val items by viewModel.items.collectAsState()
    val completedIds by viewModel.completedIds.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.archive_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.goToPreviousDay() }) { Text("◀") }
            TextButton(onClick = {
                val cal = selectedDate
                android.app.DatePickerDialog(
                    android.view.ContextThemeWrapper(context, R.style.TasksTimePickerDialog),
                    { _, y, m, d -> viewModel.setDate(y, m, d) },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Text(SimpleDateFormat("EEEE، d MMMM yyyy", Locale.getDefault()).format(selectedDate.time))
            }
            IconButton(onClick = { viewModel.goToNextDay() }) { Text("▶") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val total = items.size
        val done = items.count { completedIds.contains(it.id) }
        val ratio = if (total == 0) 0f else done.toFloat() / total

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${(ratio * 100).roundToInt()}%", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = stringResource(R.string.today_summary, done, total))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Text(stringResource(R.string.today_empty))
        } else {
            val grouped = items.groupBy { it.categoryId }
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                DefaultCategories.list.forEach { cat ->
                    val catItems = grouped[cat.id] ?: return@forEach
                    item {
                        Text(
                            text = "${cat.icon} ${DefaultCategories.displayName(cat)}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                    }
                    items(catItems) { task ->
                        ArchiveRow(task = task, done = completedIds.contains(task.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveRow(task: SelectedTask, done: Boolean) {
    val bg = if (done) Color(0xFF2E7D32).copy(alpha = 0.25f) else Color(0xFFC62828).copy(alpha = 0.18f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = task.title, modifier = Modifier.weight(1f))
        Text(text = if (done) "✅" else "❌")
    }
}
