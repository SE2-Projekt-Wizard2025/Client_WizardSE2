package at.klu.client_wizardse2.data.websocket

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import org.json.JSONObject

private const val WEBSOCKET_URI = "ws://10.0.2.2:8080/ws-broker"

/**
 * Manages the WebSocket STOMP connection to the game server using Krossbow.
 *
 * This class connects to the server, subscribes to STOMP topics, sends messages,
 * and forwards received messages to the provided [StompCallback].
 *
 * @param callbacks Callback interface for handling received messages.
 */
class GameStompClient(val callbacks: StompCallback) {

    private lateinit var topicFlow: Flow<String>
    private lateinit var collector: Job

    private lateinit var jsonFlow: Flow<String>
    private lateinit var jsonCollector: Job

    private lateinit var client: StompClient
    private lateinit var session: StompSession

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Establishes a STOMP connection and subscribes to the predefined topics.
     */
    fun connect() {
        client = StompClient(OkHttpWebSocketClient()) // other config can be passed in here

        scope.launch {
            try {
                session = client.connect(WEBSOCKET_URI)
                subscribeToText()
                subscribeToJson()
                callback("connected")
            } catch (e: Exception) {
                callback("Connection failed: ${e.message}")
                Log.e("GameStompClient", "Connection failed", e)
            }
        }
    }

    /**
     * Posts the message to the main thread and triggers the callback.
     *
     * @param msg The message to send back to the UI.
     */
    private fun callback(msg: String){
        Handler(Looper.getMainLooper()).post{
            callbacks.onResponse(msg)
        }
    }

    /**
     * Sends a basic text message to the /app/hello STOMP endpoint.
     */
    fun sendHello(){
        scope.launch {
            session.sendText("/app/hello","message from client")
        }
    }

    /**
     * Sends a JSON-formatted message to the /app/object STOMP endpoint.
     */
    fun sendJson(){
        val json = JSONObject();

        json.put("from","client")
        json.put("text","from client")

        val o = json.toString()

        scope.launch {
            session.sendText("/app/object",o);
        }
    }

    /**
     * Subscribes to the /topic/hello-response topic and forwards messages to the callback.
     */
    private suspend fun subscribeToText() {
        topicFlow = session.subscribeText("/topic/hello-response")
        collector = scope.launch {
            topicFlow.collect { msg -> callback(msg) }
        }
    }

    /**
     * Subscribes to the /topic/rcv-object topic, parses JSON, and sends a formatted message to the callback.
     */
    private suspend fun subscribeToJson() {
        jsonFlow = session.subscribeText("/topic/rcv-object")
        jsonCollector = scope.launch {
            jsonFlow.collect { msg ->
                try {
                    val json = JSONObject(msg)
                    val from = json.optString("from", "???")
                    val text = json.optString("text", "")
                    callback("[$from]: $text")
                } catch (e: Exception) {
                    callback("Invalid JSON received")
                    Log.e("GameStompClient", "JSON parsing error", e)
                }
            }
        }
    }
}