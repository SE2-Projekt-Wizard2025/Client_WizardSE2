package at.klu.client_wizardse2.data.websocket

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
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

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        callback = mockk(relaxed = true)
        session = mockk(relaxed = true)
        stompClient = GameStompClient(callback)

        // Reflection fallback (optional)
        GameStompClient::class.java.getDeclaredField("session").apply {
            isAccessible = true
            set(stompClient, session)
        }

        GameStompClient::class.java.getDeclaredField("scope").apply {
            isAccessible = true
            set(stompClient, testScope)
        }
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

    // NOTE: sendJson() causes problems when using coverage

    @Test
    fun `callback method triggers StompCallback`() = runTest {
        stompClient.directCallback("Hello Test")

        verify(exactly = 1) {
            callback.onResponse("Hello Test")
        }
    }
}
