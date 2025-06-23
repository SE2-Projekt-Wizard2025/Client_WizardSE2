package at.klu.client_wizardse2.ui.presentation.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.klu.client_wizardse2.ui.presentation.components.CardView
import at.klu.client_wizardse2.ui.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.launch


@Composable
fun CardDealScreen(viewModel: MainViewModel, onPredictionComplete: () -> Unit) {
    val handCards = viewModel.gameResponse?.handCards ?: emptyList()
    val response = viewModel.gameResponse
    val trumpCard = response?.trumpCard
    val currentPredictionPlayerId = response?.currentPredictionPlayerId
    val isMyTurn = currentPredictionPlayerId == viewModel.playerId
    var predictionInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val hasSubmittedPrediction = viewModel.hasSubmittedPrediction

    val currentPredictionPlayerName = viewModel.gameResponse?.players
        ?.find { it.playerId == currentPredictionPlayerId }
        ?.playerName ?: "Unbekannt"

    val error = viewModel.error


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    )

    {
        if (viewModel.gameResponse?.currentPlayerId == viewModel.playerId) { //@Elias
            Button(
                onClick = { viewModel.toggleCheatState(viewModel.playerId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isCheating(viewModel.playerId))
                        Color.Red.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (viewModel.isCheating(viewModel.playerId))
                        "Farbregel ignorieren (AKTIV)"
                    else "Farbregel ignorieren"
                )
            }
        }

        if (trumpCard != null) {
            Text("🃏 Trumpfkarte", style = MaterialTheme.typography.headlineSmall)
            CardView(trumpCard)
        } else {
            Text("Noch keine Trumpfkarte bekannt.", style = MaterialTheme.typography.bodyMedium)
        }

        // Handkarten
        Text("🎴 Deine Handkarte${if (handCards.size > 1) "n" else ""}", style = MaterialTheme.typography.headlineSmall)

        if (handCards.isEmpty()) {
            Text("Keine Karten erhalten oder noch nicht verteilt.")
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(handCards) { card ->
                    CardView(card)
                }
            }

        }
        // 📝 Vorhersage-Eingabe
        OutlinedTextField(
            value = predictionInput,
            onValueChange = { predictionInput = it.filter { c -> c.isDigit() } },
            label = { Text("Stich-Vorhersage") },
            singleLine = true,
            enabled = isMyTurn && !viewModel.hasSubmittedPrediction
        )



        if (!isMyTurn) {
            Text("🔄 $currentPredictionPlayerName ist gerade an der Reihe mit der Vorhersage", color = Color.Gray)
        }
        if (isMyTurn && viewModel.hasSubmittedPrediction) {
            Text("✅ Vorhersage gesendet! Warte auf andere Spieler …", color = Color.Gray)
        }


        Button(
            onClick = {
                val prediction = predictionInput.toIntOrNull()
                if (prediction != null) {
                    scope.launch {
                        val success = viewModel.sendPrediction(
                            gameId = viewModel.gameId,
                            playerId = viewModel.playerId,
                            prediction = prediction
                        )
                        if (success) {
                            viewModel.hasSubmittedPrediction = true
                        } else {
                            viewModel.hasSubmittedPrediction= false
                            predictionInput = ""
                        }
                    }
                }
            },
            enabled = predictionInput.isNotBlank() && isMyTurn && !viewModel.hasSubmittedPrediction
        ) {
            Text("Weiter")
        }

        viewModel.error?.let {
            Text(it, color = Color.Red)
        }

    }
}

