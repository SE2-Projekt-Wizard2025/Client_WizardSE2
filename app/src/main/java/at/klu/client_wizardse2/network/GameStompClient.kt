package at.klu.client_wizardse2.network

import android.util.Log
import at.klu.client_wizardse2.model.request.GameRequest
import at.klu.client_wizardse2.model.request.PredictionRequest
import at.klu.client_wizardse2.model.response.GameResponse
import at.klu.client_wizardse2.model.response.dto.PlayerDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

private const val WEBSOCKET_URI = "ws://10.0.2.2:8080/ws"
private const val TAG = "GameStompClient"

object GameStompClient {
    private var stompClient = StompClient(OkHttpWebSocketClient())
    private var session: StompSession? = null
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Establishes a WebSocket connection to the configured STOMP endpoint.
     *
     * This method initializes the [session] with a new STOMP connection using the underlying
     * [OkHttpWebSocketClient]. It logs the connection status to Logcat using [TAG].
     *
     * @return `true` if the connection was established successfully, `false` otherwise.
     *
     * @throws Exception only internally caught – any exception is logged and results in `false`.
     *
     * Usage example:
     * ```
     * val connected = GameStompClient.connect()
     * if (connected) {
     *     // Proceed with sending or subscribing
     * }
     * ```
     */
    suspend fun connect(): Boolean {
        Log.d(TAG, "Try connecting to $WEBSOCKET_URI.")
        return try {
            session = stompClient.connect(WEBSOCKET_URI)
            Log.d(TAG, "Connection successfully established.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            false
        }
    }

    /**
     * Sends a join request to the game server via STOMP over WebSocket.
     *
     * This method creates a [GameRequest] containing the provided `gameId`, `playerId`,
     * and `playerName`, serializes it to JSON, and sends it to the STOMP destination `/app/game/join`.
     *
     * Note: This function assumes an active WebSocket [session]. If the session is null,
     * the request will silently fail (no exception thrown).
     *
     * @param gameId The unique identifier of the game to join.
     * @param playerId The unique identifier of the player attempting to join.
     * @param playerName The name of the player (used for display purposes in the game).
     *
     * Example:
     * ```
     * GameStompClient.sendJoinRequest("game-123", "player-abc", "Elias")
     * ```
     */
    suspend fun sendJoinRequest(gameId: String, playerId: String, playerName: String) {
        val request = GameRequest(
            gameId = gameId,
            playerId = playerId,
            playerName = playerName
        )
        val jsonBody = json.encodeToString(GameRequest.serializer(), request)
        session?.sendText("/app/game/join", jsonBody)
    }
    suspend fun sendPrediction(gameId: String, playerId: String, prediction: Int) {
        try {
            val request = PredictionRequest(gameId, playerId, prediction)
            val jsonBody = json.encodeToString(PredictionRequest.serializer(), request)
            session?.sendText("/app/game/predict", jsonBody)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Senden der Vorhersage: ${e.message}", e)
            throw e
        }
    }


    /**
     * Subscribes to game state updates from the server via the STOMP topic `/topic/game`.
     *
     * This function listens to incoming WebSocket messages containing [GameResponse] data.
     * Each received message is deserialized from JSON and passed to the provided [onUpdate] callback.
     *
     * The subscription runs on the main thread via a [CoroutineScope] with [Dispatchers.Main].
     *
     * Note: If the current [session] is `null`, the subscription will not be established.
     * Any deserialization errors are caught and printed to `stderr`.
     *
     * @param onUpdate Callback function that receives each decoded [GameResponse].
     *
     * Example:
     * ```
     * GameStompClient.subscribeToGameUpdates { response ->
     *     println("New game state: ${response.status}")
     * }
     * ```
     */
    suspend fun subscribeToGameUpdates(
        playerId: String,
        onUpdate: (GameResponse) -> Unit,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        val topic = "/topic/game/$playerId"
        Log.d("StompDebug", "Subscribing to topic: $topic")

        session?.subscribeText(topic)?.onEach { message ->
            try {
                val response = json.decodeFromString(GameResponse.serializer(), message)
                Log.d("StompDebug", "GameResponse empfangen: $response")
                onUpdate(response)
            } catch (e: Exception) {
                Log.e("StompDebug", "Fehler beim Parsen: ${e.message}")
                e.printStackTrace()
            }
        }?.launchIn(scope)
    }
    suspend fun sendStartGameRequest(gameId: String) {
        val jsonGameId = "\"$gameId\""
        session?.sendText("/app/game/start", jsonGameId)
    }

    suspend fun sendPlayCardRequest(gameId: String, playerId: String, card: String) {
        val request = GameRequest(
            gameId = gameId,
            playerId = playerId,
            card = card
        )
        val jsonBody = json.encodeToString(GameRequest.serializer(), request)
        session?.sendText("/app/game/play", jsonBody)
    }

    // Only for Testing:
    fun setSessionForTesting(mock: StompSession?) {
        session = mock
    }

    suspend fun subscribeToScoreboard(
        gameId: String,
        onScoreboardReceived: (List<PlayerDto>) -> Unit,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        val topic = "/topic/game/$gameId/scoreboard"
        session?.subscribeText(topic)?.onEach { message ->
            try {
                val scoreboard = json.decodeFromString(
                    ListSerializer(PlayerDto.serializer()),
                    message
                )
                onScoreboardReceived(scoreboard)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Parsen des Scoreboards: ${e.message}", e)
            }
        }?.launchIn(scope)
    }

    suspend fun subscribeToErrors(
        playerId: String,
        onError: (String) -> Unit,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        val topic = "/topic/errors/$playerId"
        session?.subscribeText(topic)?.onEach { message ->
            Log.e(TAG, "Fehler empfangen: $message")
            onError(message)
        }?.launchIn(scope)
    }

    suspend fun sendProceedToNextRound(gameId: String) {
        val jsonGameId = "\"$gameId\""
        withContext(Dispatchers.IO) {
            session?.sendText("/app/game/proceedToNextRound", jsonGameId)
        }
        Log.d(TAG, "Proceed to next round request sent for gameId: $gameId")
    }


 }
