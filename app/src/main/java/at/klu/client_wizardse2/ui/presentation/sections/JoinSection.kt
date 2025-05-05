package at.klu.client_wizardse2.ui.presentation.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.klu.client_wizardse2.ui.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.launch

// NOTE: Only for testing connection with backend right now, will be improved later
@Composable
fun JoinSection(viewModel: MainViewModel) {
    var gameId by remember { mutableStateOf("game-123") }
    var playerId by remember { mutableStateOf("player-abc") }
    var playerName by remember { mutableStateOf("Max") }

    val gameResponse = viewModel.gameResponse
    val error = viewModel.error
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wizard – Join Game", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(value = gameId, onValueChange = { gameId = it }, label = { Text("Game ID") })
        OutlinedTextField(value = playerId, onValueChange = { playerId = it }, label = { Text("Player ID") })
        OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Name") })

        Button(onClick = {
            scope.launch {
                viewModel.connectAndJoin(gameId, playerId, playerName)
            }
        }) {
            Text("Join")
        }

        gameResponse?.let {
            Text("Game-ID: ${it.gameId}")
            Text("Player:")
            it.players.forEach { p -> Text("- ${p.playerName}") }
        }

        error?.let { Text(it, color = Color.Red) }
    }
}

