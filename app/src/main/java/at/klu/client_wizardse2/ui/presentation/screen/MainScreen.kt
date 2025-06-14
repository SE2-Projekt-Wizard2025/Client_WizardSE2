package at.klu.client_wizardse2.ui.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import at.klu.client_wizardse2.model.response.GameResponse
import at.klu.client_wizardse2.model.response.dto.PlayerDto
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.viewmodel.compose.viewModel
import at.klu.client_wizardse2.ui.presentation.sections.JoinSection
import at.klu.client_wizardse2.ui.presentation.viewmodels.MainViewModel
import at.klu.client_wizardse2.ui.presentation.screen.Screen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel = remember { MainViewModel() }
    var currentScreen by remember { mutableStateOf(Screen.Lobby) }
    var previousRound by remember { mutableStateOf(0) }

    val gameResponse = viewModel.gameResponse
    LaunchedEffect(gameResponse) {
        val status = gameResponse?.status?.name
        val round = gameResponse?.currentRound ?: 0

        if (status == "ENDED" && currentScreen != Screen.GameEnd) {
           //spiel vorbei EndBildschirm
            currentScreen = Screen.GameEnd

        } else if (status == "PLAYING" && round > previousRound) {
            //neue Runde, stichvorhersage wieder
            currentScreen = Screen.Deal
        }
        previousRound = round
    }

    when (currentScreen) {
        Screen.Lobby -> LobbyScreen(
            viewModel = viewModel,
            onGameStart = {
                previousRound=1
                currentScreen = Screen.Deal }
        )
        Screen.Deal -> CardDealScreen(
            viewModel = viewModel,
            onPredictionComplete = { currentScreen = Screen.Game }
        )
        Screen.Game -> {
            SimpleGameScreen(viewModel = viewModel)
        }
        Screen.GameEnd -> {
            GameEndScreen(viewModel = viewModel)
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

@Composable
fun SimpleGameScreen(viewModel: MainViewModel) {
    val gameResponse = viewModel.gameResponse
    val players = gameResponse?.players ?: emptyList()
    val currentPlayer = remember(gameResponse?.currentPlayerId, players) {
        players.find { it.playerId == gameResponse?.currentPlayerId }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text(text = "Runde ${gameResponse?.currentRound ?: 1}", style = MaterialTheme.typography.headlineSmall)
        Text(text = "${currentPlayer?.playerName ?: "..."} ist an der Reihe", style = MaterialTheme.typography.titleMedium)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Trumpfkarte:")
            gameResponse?.trumpCard?.let { CardView(it) } ?: Text("- Keiner -")
        }

        Text("Hier werden die gespielten Karten angezeigt")

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Deine Hand:", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(gameResponse?.handCards ?: emptyList()) { card -> CardView(card) }
            }
        }

        ScoreboardView(scoreboard = viewModel.scoreboard, currentPlayerName = viewModel.playerName)
    }
}


@Composable
fun GameEndScreen(viewModel: MainViewModel) {
    val winner = remember(viewModel.scoreboard) {
        viewModel.scoreboard.maxByOrNull { it.score }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏆 Spiel beendet! 🏆", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        winner?.let {
            Text("Gewinner: ${it.playerName} mit ${it.score} Punkten!", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(32.dp))
        ScoreboardView(scoreboard = viewModel.scoreboard, currentPlayerName = viewModel.playerName)
    }
}

