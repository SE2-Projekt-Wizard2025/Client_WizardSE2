package at.klu.client_wizardse2.ui.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
import at.klu.client_wizardse2.ui.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.launch


@Composable
fun LobbyScreen(viewModel: MainViewModel, onGameStart: () -> Unit) {
    val scope = rememberCoroutineScope()
    var nameInput by remember { mutableStateOf("") }
    val players = viewModel.gameResponse?.players ?: emptyList()
    val hasStarted = viewModel.hasGameStarted()

    // Sobald Spiel gestartet wurde: Bildschirm wechseln
    LaunchedEffect(hasStarted) {
        if (hasStarted) {
            onGameStart()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Dein Name") },
            enabled = viewModel.gameResponse == null
        )

        Button(
            onClick = {
                scope.launch {
                    val id = viewModel.playerId.ifBlank { java.util.UUID.randomUUID().toString() }
                    viewModel.connectAndJoin("default", id, nameInput)
                }
            },
            enabled = nameInput.isNotBlank() && viewModel.gameResponse == null
        ) {
            Text("Beitreten")
        }

        players.forEach { Text("- ${it.playerName}") }

        Button(
            onClick = { scope.launch { viewModel.startGame() } },
            enabled = true
        ) {
            Text("Spiel starten")
        }
    }
}