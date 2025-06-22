package at.klu.client_wizardse2.ui.presentation.components;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.layout.Column;
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme;
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.klu.client_wizardse2.model.response.dto.PlayerDto
import kotlin.collections.List
@Composable
fun ScoreboardView(scoreboard: List<PlayerDto>, currentPlayerName: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("📊 Scoreboard", style = MaterialTheme.typography.titleMedium)

        val sortedScoreboard = scoreboard.sortedByDescending { it.score }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Spieler", Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall)
            Text(text = "Runden", Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall)
            Text(text = "Gesamt", Modifier.weight(0.2f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End) // Hier auch TextAlign.End
        }
        Spacer(Modifier.height(4.dp))

        sortedScoreboard.forEachIndexed { index, player ->
            val isCurrentPlayer = player.playerName == currentPlayerName
            val rank = index + 1

            val nameStyle = if (isCurrentPlayer) {
                MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
            } else {
                MaterialTheme.typography.bodyLarge
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = player.playerName,
                    style = nameStyle,
                    modifier = Modifier.weight(0.4f)
                )
                // Zeigt die Punkte jeder Runde an
                val roundScoresString =
                    player.roundScores?.joinToString(", ") ?: "N/A"
                Text(
                    text = roundScoresString,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.4f)
                )

                // Zeigt die Gesamtpunktzahl an
                Text(
                    text = "${player.score}",
                    style = nameStyle,
                    modifier = Modifier.weight(0.2f),
                    textAlign = TextAlign.End
                )
            }

        }
    }
}