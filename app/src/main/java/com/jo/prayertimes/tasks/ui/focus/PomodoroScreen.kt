package com.jo.prayertimes.tasks.ui.focus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jo.prayertimes.R

@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel = viewModel()) {
    val secondsLeft by viewModel.secondsLeft.collectAsState()
    val phase by viewModel.phase.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60

    val phaseLabel = when (phase) {
        PomodoroPhase.WORK -> stringResource(R.string.focus_work)
        PomodoroPhase.BREAK -> stringResource(R.string.focus_break)
        PomodoroPhase.IDLE -> stringResource(R.string.focus_idle)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.focus_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = phaseLabel, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row {
            if (!isRunning) {
                Button(onClick = { viewModel.start() }) { Text(stringResource(R.string.focus_start)) }
            } else {
                Button(onClick = { viewModel.pause() }) { Text(stringResource(R.string.focus_pause)) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(onClick = { viewModel.reset() }) { Text(stringResource(R.string.focus_reset)) }
        }
    }
}
