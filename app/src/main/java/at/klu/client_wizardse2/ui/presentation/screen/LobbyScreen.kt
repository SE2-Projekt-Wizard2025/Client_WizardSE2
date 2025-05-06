package at.klu.client_wizardse2.ui.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import at.klu.client_wizardse2.ui.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.launch


@Composable
fun LobbyScreen(viewModel: MainViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    var nameInput by remember { mutableStateOf(TextFieldValue("")) }
    val players = viewModel.gameResponse?.players ?: emptyList()
    val hasStarted = viewModel.hasGameStarted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wizard Lobby", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Dein Name") },
            enabled = viewModel.gameResponse == null
        )

        Button(
            onClick = {
                scope.launch {
                    val gameId = "default"
                    val playerId = viewModel.playerId.ifBlank { java.util.UUID.randomUUID().toString() }
                    viewModel.connectAndJoin(gameId, playerId, nameInput.text)
                }
            },
            enabled = nameInput.text.isNotBlank() && viewModel.gameResponse == null
        ) {
            Text("Beitreten")
        }

        if (players.isNotEmpty()) {
            Text("Spieler in der Lobby:")
            players.forEach {
                Text("- ${it.playerName}")
            }

            Button(
                onClick = {
                    scope.launch { viewModel.startGame() }
                },
                enabled = viewModel.canStartGame() && !hasStarted
            ) {
                Text("Spiel starten")
            }
        }

        if (hasStarted) {
            Text("🎮 Spiel läuft!", style = MaterialTheme.typography.bodyLarge)
        }
    }
}