package com.jo.prayertimes.tasks.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.jo.prayertimes.R
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel()) {
    val period by viewModel.period.collectAsState()
    val overall by viewModel.overall.collectAsState()
    val byCategory by viewModel.byCategory.collectAsState()
    val dailyBreakdown by viewModel.dailyBreakdown.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = stringResource(R.string.reports_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Row {
            PeriodChip(stringResource(R.string.reports_day), period == ReportPeriod.DAY) { viewModel.setPeriod(ReportPeriod.DAY) }
            PeriodChip(stringResource(R.string.reports_week), period == ReportPeriod.WEEK) { viewModel.setPeriod(ReportPeriod.WEEK) }
            PeriodChip(stringResource(R.string.reports_month), period == ReportPeriod.MONTH) { viewModel.setPeriod(ReportPeriod.MONTH) }
            PeriodChip(stringResource(R.string.reports_year), period == ReportPeriod.YEAR) { viewModel.setPeriod(ReportPeriod.YEAR) }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OverallSummaryCard(overall)

        if (dailyBreakdown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = stringResource(R.string.reports_daily_breakdown), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            DailyBarChart(dailyBreakdown)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(R.string.reports_by_category), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (byCategory.isEmpty()) {
            Text(text = stringResource(R.string.reports_empty))
        } else {
            byCategory.forEach { cat -> CategoryReportRow(cat) }
        }
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(end = 6.dp)
    )
}

@Composable
private fun OverallSummaryCard(summary: PeriodSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F3F3))
            .padding(16.dp)
    ) {
        Text(text = summary.label, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ratioColor(summary.ratio)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(summary.ratio * 100).roundToInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = stringResource(R.string.reports_completed_of, summary.completed, summary.total))
                Text(
                    text = stringResource(R.string.reports_overall_rate),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DailyBarChart(days: List<PeriodSummary>) {
    val maxTotal = (days.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val heightFraction = if (maxTotal == 0) 0f else day.total.toFloat() / maxTotal
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((80 * heightFraction.coerceAtLeast(0.05f)).dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ratioColor(day.ratio))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = day.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CategoryReportRow(cat: CategorySummary) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${cat.icon} ${cat.nameAr}")
            Text(text = "${cat.completed}/${cat.total} — ${(cat.ratio * 100).roundToInt()}%")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(cat.ratio)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ratioColor(cat.ratio))
            )
        }
    }
}

private fun ratioColor(ratio: Float): Color {
    return when {
        ratio == 0f -> Color(0xFFBDBDBD)
        ratio < 0.5f -> Color(0xFFEF5350)
        ratio < 0.8f -> Color(0xFFFFA726)
        ratio < 1f -> Color(0xFF42A5F5)
        else -> Color(0xFF66BB6A)
    }
}
