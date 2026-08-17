package com.jo.prayertimes.tasks.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jo.prayertimes.R
import com.jo.prayertimes.tasks.data.Category
import com.jo.prayertimes.tasks.data.DefaultCategories
import com.jo.prayertimes.tasks.data.Task

@Composable
fun TaskOverviewScreen(viewModel: TaskOverviewViewModel = viewModel()) {
    val period by viewModel.period.collectAsState()
    val items by viewModel.items.collectAsState()
    val completedIds by viewModel.completedIds.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.overview_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Row {
            FilterChip(selected = period == OverviewPeriod.DAY, onClick = { viewModel.setPeriod(OverviewPeriod.DAY) }, label = { Text(stringResource(R.string.reports_day)) }, modifier = Modifier.padding(end = 4.dp))
            FilterChip(selected = period == OverviewPeriod.WEEK, onClick = { viewModel.setPeriod(OverviewPeriod.WEEK) }, label = { Text(stringResource(R.string.reports_week)) }, modifier = Modifier.padding(end = 4.dp))
            FilterChip(selected = period == OverviewPeriod.MONTH, onClick = { viewModel.setPeriod(OverviewPeriod.MONTH) }, label = { Text(stringResource(R.string.reports_month)) }, modifier = Modifier.padding(end = 4.dp))
            FilterChip(selected = period == OverviewPeriod.YEAR, onClick = { viewModel.setPeriod(OverviewPeriod.YEAR) }, label = { Text(stringResource(R.string.reports_year)) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Text(stringResource(R.string.overview_empty))
        } else {
            val grouped = items.groupBy { it.category }
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
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
                        OverviewRow(task = task, done = completedIds.contains(task.id), onToggle = { viewModel.toggle(task) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewRow(task: Task, done: Boolean, onToggle: () -> Unit) {
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
        Checkbox(checked = done, onCheckedChange = { onToggle() })
    }
}
