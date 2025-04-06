package at.klu.client_wizardse2.data.websocket

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.junit.After
import org.junit.Before
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class GameStompClientTest {

    private lateinit var stompClient: GameStompClient
    private lateinit var session: StompSession
    private lateinit var callback: StompCallback
    private lateinit var client: StompClient

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        callback = mockk(relaxed = true)
        session = mockk(relaxed = true)
        client = mockk(relaxed = true)
        stompClient = GameStompClient(callback)

        injectPrivate("session", session)
        injectPrivate("client", client)
        injectPrivate("scope", testScope)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendHello triggers STOMP sendText`() = runTest {
        stompClient.sendHello()
        testScheduler.advanceUntilIdle()

        coVerify { session.sendText("/app/hello", "message from client") }
    }

    @Test
    fun `callback method triggers StompCallback`() = runTest {
        stompClient.directCallback("Hello Test")
        verify { callback.onResponse("Hello Test") }
    }

    /**
     * Helper to inject private field via reflection.
     */
    private fun injectPrivate(fieldName: String, value: Any) {
        GameStompClient::class.java.getDeclaredField(fieldName).apply {
            isAccessible = true
            set(stompClient, value)
        }
    }
}
