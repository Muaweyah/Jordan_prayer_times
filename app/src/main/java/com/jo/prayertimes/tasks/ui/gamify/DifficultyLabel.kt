package com.jo.prayertimes.tasks.ui.gamify

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jo.prayertimes.R
import com.jo.prayertimes.tasks.data.Difficulty

@Composable
fun difficultyLabel(d: Difficulty): String = when (d) {
    Difficulty.EASY -> stringResource(R.string.diff_easy)
    Difficulty.MEDIUM -> stringResource(R.string.diff_medium)
    Difficulty.HARD -> stringResource(R.string.diff_hard)
}
