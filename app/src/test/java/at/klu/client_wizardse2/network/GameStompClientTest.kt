package at.klu.client_wizardse2.network

import android.util.Log
import at.klu.client_wizardse2.model.response.GameResponse
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameStompClientTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var mockClient: StompClient
    private lateinit var mockSession: StompSession

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockClient = mockk()
        mockSession = mockk(relaxed = true)
        mockStaticHelpers()
        resetGameStompClient()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
        unmockkStatic(Log::class)
    }

    private fun mockStaticHelpers() {
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    private fun resetGameStompClient() {
        GameStompClient.apply {
            javaClass.getDeclaredField("stompClient").apply {
                isAccessible = true
                set(this@apply, mockClient)
            }
            javaClass.getDeclaredField("session").apply {
                isAccessible = true
                set(this@apply, null)
            }
        }
    }

    @Test
    fun `connect should return true on successful connection`() = testScope.runTest {
        coEvery { mockClient.connect(any()) } returns mockSession

        val result = GameStompClient.connect()

        assertTrue(result)
        coVerify { mockClient.connect(any()) }
    }

    @Test
    fun `connect should return false on failure`() = testScope.runTest {
        coEvery { mockClient.connect(any()) } throws RuntimeException("Connection error")

        val result = GameStompClient.connect()

        assertFalse(result)
    }

    @Test
    fun `sendJoinRequest should send correct JSON to destination`() = testScope.runTest {
        GameStompClient.setSessionForTesting(mockSession)
        coEvery {
            mockSession.sendText(eq("/app/game/join"), any())
        } returns null

        GameStompClient.sendJoinRequest("game1", "player1", "TestPlayer")

        coVerify {
            mockSession.sendText(eq("/app/game/join"), match { it.contains("TestPlayer") })
        }
    }

    @Test
    fun `subscribeToGameUpdates should invoke callback with deserialized GameResponse`() = testScope.runTest {
        val testJson = """{"gameId":"g1","status":"PLAYING","currentPlayerId":"p1","players":[],"handCards":[],"lastPlayedCard":null}"""
        val flow = flowOf(testJson)

        coEvery { mockSession.subscribeText("/topic/game") } returns flow
        GameStompClient.setSessionForTesting(mockSession)

        val responses = mutableListOf<GameResponse>()

        GameStompClient.subscribeToGameUpdates(
            onUpdate = { responses.add(it) },
            scope = this
        )

        advanceUntilIdle()

        assertEquals(1, responses.size)
        assertEquals("g1", responses.first().gameId)
    }

    @Test
    fun `subscribeToGameUpdates should trigger catch block on invalid JSON`() = testScope.runTest {
        val invalidJson = """{ invalid json """
        val flow = flowOf(invalidJson)

        coEvery { mockSession.subscribeText("/topic/game") } returns flow
        GameStompClient.setSessionForTesting(mockSession)

        val responses = mutableListOf<GameResponse>()

        GameStompClient.subscribeToGameUpdates(
            onUpdate = { responses.add(it) },
            scope = this
        )

        advanceUntilIdle()

        assertTrue("onUpdate should not be called", responses.isEmpty())
    }

    @Test
    fun `setSessionForTesting should set internal session field correctly`() = runBlocking {
        // Arrange: Session is replaced with mock
        GameStompClient.setSessionForTesting(mockSession)

        coEvery { mockSession.sendText(any(), any()) } returns null

        // Act: Trigger code that uses session
        GameStompClient.sendJoinRequest("game1", "player1", "testName")

        // Assert: Verify session was used
        coVerify { mockSession.sendText("/app/game/join", any()) }
    }

    @Test
    fun `sendPrediction should send correct JSON to prediction endpoint`() = testScope.runTest {
        GameStompClient.setSessionForTesting(mockSession)

        coEvery {
            mockSession.sendText(eq("/app/game/predict"), any())
        } returns null

        GameStompClient.sendPrediction("game-1", "player-1", 2)

        coVerify {
            mockSession.sendText(eq("/app/game/predict"), match { it.contains("2") && it.contains("player-1") })
        }
    }

}