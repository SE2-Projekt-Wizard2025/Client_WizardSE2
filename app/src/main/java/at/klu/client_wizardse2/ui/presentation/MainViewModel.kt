package at.klu.client_wizardse2.ui.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import at.klu.client_wizardse2.data.websocket.StompCallback
import at.klu.client_wizardse2.data.websocket.GameStompClient

/**
 * ViewModel for managing the WebSocket STOMP connection and message state.
 *
 * This ViewModel connects to the backend using [GameStompClient] and handles sending and receiving
 * messages via the STOMP protocol. Incoming messages are exposed through [messages] as a
 * reactive list for UI display.
 */
class MainViewModel : ViewModel(), StompCallback {
    /**
     * A state-backed list of messages received from the STOMP server.
     */
    val messages = mutableStateListOf<String>()

    private val stompClient = GameStompClient(this)

    init {
        stompClient.connect()
    }

    /**
     * Sends a simple text message to the server via the STOMP endpoint.
     */
    fun sendHello() {
        stompClient.sendHello()
    }

    /**
     * Sends a JSON-formatted message to the server via the STOMP endpoint.
     */
    fun sendJson() {
        stompClient.sendJson()
    }

    /**
     * Callback triggered when a new message is received from the STOMP server.
     *
     * @param res The message body as a string.
     */
    override fun onResponse(res: String) {
        messages.add(res)
    }
}