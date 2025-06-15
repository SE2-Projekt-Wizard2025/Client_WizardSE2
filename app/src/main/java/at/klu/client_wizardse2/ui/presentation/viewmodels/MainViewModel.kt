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
        private set


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
                        onUpdate = { gameResponse = it },
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
                onScoreboardReceived = { newBoard -> scoreboard = newBoard },
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
        return gameResponse?.status?.name == "PLAYING"
    }

    fun sendPrediction(gameId: String, playerId: String, prediction: Int) {
        viewModelScope.launch {
            GameStompClient.sendPrediction(gameId, playerId, prediction)

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

}
