package at.klu.client_wizardse2.ui.presentation.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import dagger.hilt.android.lifecycle.HiltViewModel
//import javax.inject.Inject

import at.klu.client_wizardse2.model.response.GameResponse
import at.klu.client_wizardse2.network.GameStompClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainViewModel : ViewModel() {

    var gameResponse by mutableStateOf<GameResponse?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var gameId: String = ""
        private set

    var playerId: String = ""
        private set

    var playerName: String = ""
        private set

    fun connectAndJoin(gameId: String, playerId: String, playerName: String) {
        this.gameId = gameId
        this.playerId = playerId
        this.playerName = playerName

        viewModelScope.launch {
            val connected = GameStompClient.connect()
            if (connected) {
                GameStompClient.subscribeToGameUpdates(
                    onUpdate = { gameResponse = it },
                    scope = viewModelScope
                )
                delay(100)
                GameStompClient.sendJoinRequest(gameId, playerId, playerName)
            } else {
                error = "Connection to server failed"
            }
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
}
