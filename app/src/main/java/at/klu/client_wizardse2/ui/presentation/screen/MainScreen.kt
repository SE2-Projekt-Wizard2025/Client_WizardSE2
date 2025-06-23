package at.klu.client_wizardse2.ui.presentation.screen


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.klu.client_wizardse2.model.response.dto.PlayerDto
import at.klu.client_wizardse2.ui.presentation.viewmodels.MainViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import at.klu.client_wizardse2.model.response.GameStatus
import at.klu.client_wizardse2.model.response.dto.CardDto
import at.klu.client_wizardse2.ui.presentation.components.ScoreboardView
import at.klu.client_wizardse2.ui.presentation.components.CardView
import at.klu.client_wizardse2.ui.presentation.components.toCardDto




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current //Lösung da jetzt MainViewModel einen Context erwartet...
    val viewModel = remember { MainViewModel(context) }
    var currentScreen by remember { mutableStateOf(Screen.Lobby) }
    var previousRound by remember { mutableIntStateOf(0)  }
    val showRoundSummaryScreen = viewModel.showRoundSummaryScreen


    LaunchedEffect(viewModel.gameResponse?.status) {
        val status = viewModel.gameResponse?.status

        if (status == GameStatus.ROUND_END_SUMMARY) {
            Log.d("MainScreen", "Status ist ROUND_END_SUMMARY. RoundSummaryScreen wird angezeigt.")

        }else if (status == GameStatus.PREDICTION) {
            Log.d("MainScreen", "Status ist PREDICTION. Wechsle zu Deal Screen.")
            currentScreen = Screen.Deal

        } else if (status == GameStatus.PLAYING) {
            Log.d("MainScreen", "Status ist PLAYING. Wechsle zu Game Screen.")
            currentScreen = Screen.Game
        } else if (status == GameStatus.ENDED) {
            Log.d("MainScreen", "Status ist ENDED. Wechsle zu GameEnd Screen.")
            currentScreen = Screen.GameEnd
        }
    }

    if (showRoundSummaryScreen) {
        RoundSummaryScreen(
            viewModel = viewModel,
            onContinue = {
                viewModel.proceedToNextRound()
            }
        )
    } else {
        when (currentScreen) {
            Screen.Lobby -> LobbyScreen(
                viewModel = viewModel,
                onGameStart = {
                    currentScreen = Screen.Deal
                }
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
}


@Composable
fun SimpleGameScreen(viewModel: MainViewModel) {
    val gameResponse = viewModel.gameResponse
    val players = gameResponse?.players ?: emptyList()
    val currentPlayer = remember(gameResponse?.currentPlayerId, players) {
        players.find { it.playerId == gameResponse?.currentPlayerId }
    }
    val currentRound = gameResponse?.currentRound ?: 0

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            text = "Runde ${gameResponse?.currentRound ?: 1}",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "${currentPlayer?.playerName ?: "..."} ist an der Reihe",
            style = MaterialTheme.typography.titleMedium
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Trumpfkarte:")
            gameResponse?.trumpCard?.let { CardView(it) } ?: Text("- Keiner -")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Zuletzt gespielt:")
            val lastPlayedCardString = gameResponse?.lastPlayedCard

            if (lastPlayedCardString != null) {
                val lastPlayedCardDto = lastPlayedCardString.toCardDto()

                if (lastPlayedCardDto != null) {
                    CardView(card = lastPlayedCardDto)
                } else {
                    Text(text = lastPlayedCardString)
                }
            } else {
                Text(text = "-")
            }
        }

        gameResponse?.lastTrickWinnerId?.let { winnerId ->
            val winnerName = players.find { it.playerId == winnerId }?.playerName
            if (!winnerName.isNullOrEmpty()) {
                Text(
                    text = "🎉 $winnerName hat den letzten Stich gewonnen!",
                    color = Color(0xFF4CAF50), // Grün
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Deine Hand:", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(gameResponse?.handCards ?: emptyList()) { card ->
                    val isMyTurn = viewModel.playerId == gameResponse?.currentPlayerId
                    CardView(
                        card = card,
                        onCardClick = { cardString: String -> //not sure ob des so passt, oda explizit mit onClick gehört
                            if (isMyTurn) {
                                viewModel.playCard(cardString)
                            } else null
                        }
                    )
                }
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
                    Text(
                        "Gewinner: ${it.playerName} mit ${it.score} Punkten!",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.height(32.dp))
                ScoreboardView(
                    scoreboard = viewModel.scoreboard,
                    currentPlayerName = viewModel.playerName
                )
            }
        }

        //val showWinnerOkButton = gameResponse?.handCards?.isEmpty() == true &&
        //        gameResponse.lastTrickWinnerId == viewModel.playerId &&
        //        gameResponse.status.name == "PLAYING"


        @Composable
        fun RoundSummaryScreen(viewModel: MainViewModel, onContinue: () -> Unit) {
            val currentRoundNumber = viewModel.gameResponse?.currentRound ?: 0
            val roundEnded = if (currentRoundNumber > 0) currentRoundNumber else 0

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔁 Runde $roundEnded beendet!", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))

                ScoreboardView(
                    scoreboard = viewModel.scoreboard,
                    currentPlayerName = viewModel.playerName
                )

                Spacer(Modifier.height(24.dp))
                Button(onClick = onContinue) {
                    Text("Weiter zur nächsten Runde")
                }
            }
        }





