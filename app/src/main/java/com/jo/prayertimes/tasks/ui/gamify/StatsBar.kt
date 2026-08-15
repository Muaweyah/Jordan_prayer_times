package com.jo.prayertimes.tasks.ui.gamify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jo.prayertimes.tasks.data.RewardEngine
import com.jo.prayertimes.tasks.data.UserStats

@Composable
fun StatsBar(stats: UserStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("❤️ ${stats.health}/${stats.maxHealth}", style = MaterialTheme.typography.labelLarge)
            Text("المستوى ${stats.level}", style = MaterialTheme.typography.labelSmall)
        }
        Column {
            val needed = RewardEngine.xpForLevel(stats.level)
            Text("⭐ ${stats.xp}/$needed", style = MaterialTheme.typography.labelLarge)
            Text("خبرة", style = MaterialTheme.typography.labelSmall)
        }
        Column {
            Text("🪙 ${stats.gold}", style = MaterialTheme.typography.labelLarge)
            Text("عملة", style = MaterialTheme.typography.labelSmall)
        }
    }
}
