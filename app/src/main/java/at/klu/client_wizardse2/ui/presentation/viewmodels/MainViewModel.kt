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
import kotlinx.coroutines.async

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
                   GameStompClient.subscribeToGameUpdates(
                        playerId = playerId,
                        onUpdate = { response ->
                            // 🔁 Runde hat sich geändert → Vorhersage-Zustände zurücksetzen
                            if (response.currentRound != lastKnownRound) {
                                hasSubmittedPrediction = false
                                error = null
                                lastKnownRound = response.currentRound
                            }
                            if (response.status == GameStatus.ROUND_END_SUMMARY) {
                                showRoundSummaryScreen = true
                            } else {
                               if (showRoundSummaryScreen) {
                                    showRoundSummaryScreen = false
                                }
                            }

                            gameResponse = response
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

    suspend fun playCard(cardString: String): Boolean {

        return viewModelScope.async {
            if (gameId.isNotEmpty() && playerId.isNotEmpty()) {
                try {
                    GameStompClient.sendPlayCardRequest(gameId, playerId, cardString)
                    error = null
                    true
                } catch (e: IllegalStateException) {
                    error = e.message ?: "Unbekannter Fehler beim Kartenlegen."
                    false
                } catch (e: IllegalArgumentException) {
                    error = e.message ?: "Ungültige Karte zum Legen ausgewählt."
                    false
                } catch (e: Exception) {
                    error = "Unerwarteter Fehler beim Kartenlegen: ${e.message}"
                    false
                }
            } else {
                error = "Game ID oder Player ID nicht gesetzt. Karte kann nicht gelegt werden."
                false
            }
        }.await()
    }

    fun clearLastTrickWinner() {
        gameResponse = gameResponse?.copy(lastTrickWinnerId = null)
    }

    fun proceedToNextRound() {
        viewModelScope.launch {
            if (gameId.isNotEmpty()) {
                GameStompClient.sendProceedToNextRound(gameId)
            } else {
              }
        }
    }

}
