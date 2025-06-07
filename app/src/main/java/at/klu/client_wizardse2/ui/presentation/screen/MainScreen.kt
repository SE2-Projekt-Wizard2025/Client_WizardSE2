package at.klu.client_wizardse2.ui.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.klu.client_wizardse2.model.response.dto.PlayerDto
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.viewmodel.compose.viewModel
import at.klu.client_wizardse2.ui.presentation.sections.JoinSection
import at.klu.client_wizardse2.ui.presentation.viewmodels.MainViewModel
import at.klu.client_wizardse2.ui.presentation.screen.Screen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel = remember { MainViewModel() }
    var currentScreen by remember { mutableStateOf(Screen.Lobby) }

    when (currentScreen) {
        Screen.Lobby -> LobbyScreen(
            viewModel = viewModel,
            onGameStart = { currentScreen = Screen.Deal }
        )
        Screen.Deal -> CardDealScreen(
            viewModel = viewModel,
            onPredictionComplete = { currentScreen = Screen.Game }
        )
        Screen.Game -> {
            //später finaler Spielscreen
            Text("Spiel läuft...")
            val scoreboard = viewModel.scoreboard

            Box(modifier = Modifier.fillMaxSize()) {
                ScoreboardView(scoreboard = viewModel.scoreboard, currentPlayerName = viewModel.playerName)
            }

        }
    }
}

@Composable
fun ScoreboardView(scoreboard: List<PlayerDto>, currentPlayerName: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("📊 Scoreboard", style = MaterialTheme.typography.titleMedium)

        val sortedScoreboard = scoreboard.sortedByDescending { it.score }

        sortedScoreboard.forEachIndexed { index, player ->
            val isCurrentPlayer = player.playerName == currentPlayerName
            val rank = index + 1

            val nameStyle = if (isCurrentPlayer) {
                MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
            } else {
                MaterialTheme.typography.bodyLarge
            }

            Text(
                text = "$rank. ${player.playerName} – Geboten: ${player.prediction}, Gewonnen: ${player.tricksWon}, Punkte: ${player.score}",
                style = nameStyle
            )
        }
    }
}

