package at.klu.client_wizardse2.ui.presentation.screen


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import at.klu.client_wizardse2.model.response.dto.CardDto


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current //Lösung da jetzt MainViewModel einen Context erwartet...
    val viewModel = remember { MainViewModel(context) }
    var currentScreen by remember { mutableStateOf(Screen.Lobby) }
    var previousRound by remember { mutableIntStateOf(0)  }

    val gameResponse = viewModel.gameResponse
    LaunchedEffect(gameResponse?.status?.name, gameResponse?.currentRound) {
        val status = gameResponse?.status?.name
        val newRound = gameResponse?.currentRound ?: return@LaunchedEffect

        if (status == "PREDICTION" && newRound != null) {
            //Nur wenn bereits eine Runde gespielt wurde
            if (newRound > previousRound && previousRound > 0) {
                viewModel.roundJustEnded = true
            } else {
                currentScreen = Screen.Deal
            }
        } else if (status == "PLAYING") {
            currentScreen = Screen.Game
        } else if (status == "ENDED") {
            currentScreen = Screen.GameEnd
        }

        previousRound = newRound
    }

    if (viewModel.roundJustEnded) {
        RoundSummaryScreen(
            viewModel = viewModel,
            onContinue = {
                viewModel.roundJustEnded = false
                currentScreen = Screen.Deal
            }
        )
    } else {
        when (currentScreen) {
            Screen.Lobby -> LobbyScreen(
                viewModel = viewModel,
                onGameStart = {
                    previousRound = 0
                    viewModel.roundJustEnded = false
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
fun CardView(card: CardDto, onCardClick: ((String) -> Unit)? = null) {
    val backgroundColor = when (card.color.uppercase()) {
        "RED" -> Color.Red
        "BLUE" -> Color.Blue
        "GREEN" -> Color.Green
        "YELLOW" -> Color.Yellow
        else -> Color.LightGray
    }

    val textColor = if (card.color.uppercase() in listOf("YELLOW", "GREEN")) Color.Black else Color.White

    val actualCardString = when (card.type) {
        "WIZARD" -> "WIZARD"
        "JESTER" -> "JESTER"
        else -> "${card.color}_${card.value}"
    }

    val cardModifier = Modifier
        .width(80.dp)
        .height(120.dp)
        .then(
            if (onCardClick != null) Modifier.clickable { onCardClick(actualCardString) }
            else Modifier
        )

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (card.type) {
                    "WIZARD" -> "Wizard"
                    "JESTER" -> "Narr"
                    else -> card.color
                },
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
            Text(
                text = when (card.type) {
                    "WIZARD", "JESTER" -> ""
                    else -> card.value
                },
                style = MaterialTheme.typography.titleLarge,
                color = textColor
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
    val currentRound = gameResponse?.currentRound ?: 0
    var lastKnownRound by remember { mutableStateOf(currentRound) }
    var showNextRoundDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentRound) {
        if (currentRound > lastKnownRound) {
            showNextRoundDialog = true
            lastKnownRound = currentRound
        }
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) { // Container,Last Played Card
            Text("Zuletzt gespielt:")
            gameResponse?.lastPlayedCard?.let { lastCardString ->
                 val lastPlayedCardDto = lastCardString.toCardDto()
                lastPlayedCardDto?.let { CardView(it) } ?: Text(lastCardString)
            } ?: Text("-")
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
        if (showNextRoundDialog) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("🕓 Neue Runde beginnt!", style = MaterialTheme.typography.titleMedium)

            Button(onClick = {
                showNextRoundDialog = false
            }) {
                Text("OK – weiter")
            }
        }
        val showWinnerOkButton = gameResponse?.handCards?.isEmpty() == true &&
                gameResponse.lastTrickWinnerId == viewModel.playerId &&
                gameResponse.status.name == "PLAYING"

        if (showWinnerOkButton) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                viewModel.clearLastTrickWinner()
            }) {
                Text("OK – Weiter zur nächsten Runde")
            }
        }
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

fun String.toCardDto(): CardDto? {
    return when {
        equals("WIZARD", ignoreCase = true) -> CardDto(color = "SPECIAL", value = "0", type = "WIZARD")
        equals("JESTER", ignoreCase = true) -> CardDto(color = "SPECIAL", value = "0", type = "JESTER")
        contains("_") -> {
            val parts = this.split("_")
            if (parts.size == 2) {
                CardDto(color = parts[0], value = parts[1], type = "NUMBER")
            } else null
        }
        else -> null
    }
}

@Composable
fun RoundSummaryScreen(viewModel: MainViewModel, onContinue: () -> Unit) {
    val winner = viewModel.scoreboard.maxByOrNull { it.tricksWon }
    val round = viewModel.gameResponse?.currentRound ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔁 Runde $round beendet!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (winner != null) {
            Text("${winner.playerName} hat die Runde gewonnen mit ${winner.tricksWon} Stichen.")
        } else {
            Text("Runde beendet.")
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onContinue) {
            Text("Weiter zur Vorhersage")
        }
    }
}


