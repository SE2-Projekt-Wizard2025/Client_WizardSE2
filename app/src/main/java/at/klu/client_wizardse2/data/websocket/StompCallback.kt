package at.klu.client_wizardse2.data.websocket

/**
 * Callback interface for receiving messages from a STOMP connection.
 *
 * Implement this interface to handle messages received through a WebSocket STOMP client.
 */
interface StompCallback {
    /**
     * Called when a new message is received from the server.
     *
     * @param res The received message body as a string.
     */
    fun onResponse(res:String);
}