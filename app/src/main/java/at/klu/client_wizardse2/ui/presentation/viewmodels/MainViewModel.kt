package at.klu.client_wizardse2.ui.presentation.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import dagger.hilt.android.lifecycle.HiltViewModel
//import javax.inject.Inject

import kotlinx.coroutines.launch



import at.klu.client_wizardse2.model.response.GameResponse
import at.klu.client_wizardse2.model.response.dto.PlayerDto
import at.klu.client_wizardse2.network.GameStompClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import at.klu.client_wizardse2.model.response.GameStatus

class MainViewModel : ViewModel() {

    var gameResponse by mutableStateOf<GameResponse?>(null)
        @VisibleForTesting set

    var error by mutableStateOf<String?>(null)
        private set

    var gameId: String = ""
        @VisibleForTesting set

    var playerId: String = ""

    var playerName: String = ""
        private set

    var scoreboard by mutableStateOf<List<PlayerDto>>(emptyList())
        @VisibleForTesting
        internal set

    var showRoundSummaryScreen by mutableStateOf(false)
        private set

    var hasSubmittedPrediction by mutableStateOf(false)

    private var lastKnownRound by mutableStateOf(-1)


    fun connectAndJoin(gameId: String, playerId: String, playerName: String) {
        this.gameId = gameId
        this.playerId = playerId
        this.playerName = playerName

        viewModelScope.launch {
            try {
                val connected = GameStompClient.connect()
                if (connected) {
                    Log.d("StompDebug", "Verbindung erfolgreich, starte Abo für $playerId")
                    GameStompClient.subscribeToGameUpdates(
                        playerId = playerId,
                        onUpdate = { response ->
                            // 🔁 Runde hat sich geändert → Vorhersage-Zustände zurücksetzen
                            if (response.currentRound != lastKnownRound) {
                                hasSubmittedPrediction = false
                                error = null
                                lastKnownRound = response.currentRound
                            }
                            if (response.status == GameStatus.ROUND_END_SUMMARY) { // NEU: Prüfung auf ROUND_END_SUMMARY
                                showRoundSummaryScreen = true // Signalisiert der UI, den Zusammenfassungsbildschirm anzuzeigen
                                Log.d("MainViewModel", "GameStatus ist ROUND_END_SUMMARY. Zeige Runden-Zusammenfassung an.")
                            } else {
                               if (showRoundSummaryScreen) {
                                    showRoundSummaryScreen = false
                                    Log.d("MainViewModel", "GameStatus ist nicht mehr ROUND_END_SUMMARY. Verberge Runden-Zusammenfassung.")
                                }
                            }

                            gameResponse = response // ⬅️ Das MUSS erhalten bleiben!
                        },
                        scope = viewModelScope
                    )
                    GameStompClient.subscribeToErrors(
                        playerId = playerId,
                        onError = { errorMessage ->
                            error = errorMessage
                            hasSubmittedPrediction = false
                        },
                        scope = viewModelScope
                    )
                    delay(100)
                    GameStompClient.sendJoinRequest(gameId, playerId, playerName)
                    subscribeToScoreboard(gameId)
                } else {
                    error = "Connection to server failed"
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            }
        }

    }

    fun subscribeToScoreboard(gameId: String) {
        viewModelScope.launch {
            GameStompClient.subscribeToScoreboard(
                gameId = gameId,
                onScoreboardReceived = { newBoard ->
                    scoreboard = newBoard
                    Log.d("MainViewModel", "Scoreboard aktualisiert: $newBoard")
                },
                scope = viewModelScope
            )
        }
    }


    fun startGame() {
        viewModelScope.launch {
            GameStompClient.sendStartGameRequest(gameId)
        }
    }

    fun hasGameStarted(): Boolean {
        return gameResponse?.status == GameStatus.PLAYING && !showRoundSummaryScreen
    }

    suspend fun sendPrediction(gameId: String, playerId: String, prediction: Int): Boolean {
        return try {
            GameStompClient.sendPrediction(gameId, playerId, prediction)
            error = null
            true
        } catch (e: Exception) {
            val msg = e.message ?: e.cause?.message ?: "Unbekannter Fehler"
            error = if (msg.contains("exakt die Anzahl der Stiche")) {
                "! Vorhersage nicht erlaubt – die Summe entspricht der Rundenzahl. Gib bitte einen anderen Wert ein."
            } else {
                "! Fehler: $msg"
            }
            false
        }
    }

    fun playCard(cardString: String) {
        viewModelScope.launch {

            if (gameId.isNotEmpty() && playerId.isNotEmpty()) {
                GameStompClient.sendPlayCardRequest(gameId, playerId, cardString)
            } else {
                error = "Game ID or Player ID not set. Cannot play card."
            }
        }
    }

    fun clearLastTrickWinner() {
        gameResponse = gameResponse?.copy(lastTrickWinnerId = null)
    }

    fun proceedToNextRound() { // NEU: Methode
        viewModelScope.launch {
            if (gameId.isNotEmpty()) {
                GameStompClient.sendProceedToNextRound(gameId) // NEU: Aufruf an GameStompClient
                // Nach dem Senden der Anfrage erwarten wir eine neue GameResponse,
                // die den Status auf PREDICTION setzen sollte und den Zusammenfassungsbildschirm ausblendet.
            } else {
                Log.e("MainViewModel", "Cannot proceed to next round: gameId is empty.")
            }
        }
    }

}
