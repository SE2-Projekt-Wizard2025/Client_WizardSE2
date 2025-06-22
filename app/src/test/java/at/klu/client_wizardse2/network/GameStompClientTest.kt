package at.klu.client_wizardse2.network

import android.util.Log
import at.klu.client_wizardse2.model.response.GameResponse
import at.klu.client_wizardse2.model.response.dto.PlayerDto
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coJustRun
import kotlinx.coroutines.flow.MutableSharedFlow
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle



@OptIn(ExperimentalCoroutinesApi::class)
class GameStompClientTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var mockClient: StompClient
    private lateinit var mockSession: StompSession
    private val badJsonFlow = MutableSharedFlow<String>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockClient = mockk()
        mockSession = mockk(relaxed = true)
        mockStaticHelpers()
        resetGameStompClient()
    }
    private fun mockStaticHelpers() {
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
        mockkStatic(Log::class)

        // Mocke alle Varianten von Log.d und Log.e
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
        unmockkStatic(Log::class)
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
        val testJson = """{"gameId":"g1","status":"PLAYING","currentPlayerId":"p1","players":[],"handCards":[],"lastPlayedCard":null,
            "lastTrickWinnerId": "p1",
            "trumpCard": null,
            "currentRound": 1,
            "currentPredictionPlayerId": null}"""
        val flow = flowOf(testJson)
        val testPlayerId = "p1"

        coEvery { mockSession.subscribeText("/topic/game/$testPlayerId") } returns flow
        GameStompClient.setSessionForTesting(mockSession)

        val responses = mutableListOf<GameResponse>()

        GameStompClient.subscribeToGameUpdates(
            playerId = testPlayerId,
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
            playerId = "p1",
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
    fun `sendStartGameRequest should send quoted gameId to server`() = testScope.runTest {
        GameStompClient.setSessionForTesting(mockSession)

        coEvery {
            mockSession.sendText("/app/game/start", "\"myGameId\"")
        } returns null

        GameStompClient.sendStartGameRequest("myGameId")

        coVerify {
            mockSession.sendText(eq("/app/game/start"), eq("\"myGameId\""))
        }
    }

    @Test
    fun `connect should log error and return false if exception is thrown`() = testScope.runTest {
        coEvery { mockClient.connect(any()) } throws RuntimeException("Boom")

        val result = GameStompClient.connect()

        assertFalse(result)
        coVerify { Log.e(match { it == "GameStompClient" }, any(), any()) }
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

    @Test
    fun `sendPlayCardRequest should send correct JSON to destination`() = testScope.runTest {
        GameStompClient.setSessionForTesting(mockSession)
        val gameId = "game-xyz"
        val playerId = "player-abc"
        val card = "WIZARD"

        coJustRun {
            mockSession.sendText(eq("/app/game/play"), any())
        }

        GameStompClient.sendPlayCardRequest(gameId, playerId, card)
        advanceUntilIdle()


        coVerify {
            mockSession.sendText(
                eq("/app/game/play"),
                match { jsonString ->
                    jsonString.contains("\"gameId\":\"$gameId\"") &&
                            jsonString.contains("\"playerId\":\"$playerId\"") &&
                            jsonString.contains("\"card\":\"$card\"")
                }
            )
        }

    fun `sendJoinRequest should do nothing if session is null`() = runTest {
        GameStompClient.setSessionForTesting(null)

        GameStompClient.sendJoinRequest("game-id", "player-id", "test-name")
    }

    @Test
    fun `sendPrediction should do nothing if session is null`() = runTest {

        GameStompClient.setSessionForTesting(null)

        GameStompClient.sendPrediction("game-id", "player-id", 1)
    }

    @Test
    fun `sendStartGameRequest should do nothing if session is null`() = runTest {

        GameStompClient.setSessionForTesting(null)

        GameStompClient.sendStartGameRequest("game-id")
    }

    @Test
    fun `subscribeToGameUpdates should not crash on null session`() = runTest {

        GameStompClient.setSessionForTesting(null)

        val receivedResponses = mutableListOf<GameResponse>()


        GameStompClient.subscribeToGameUpdates(
            playerId = "player-id",
            onUpdate = { receivedResponses.add(it) },
            scope = this
        )

        advanceUntilIdle()

        assertTrue("No updates should be received", receivedResponses.isEmpty())
    }

    @Test
    fun `subscribeToGameUpdates should catch JSON parsing exception`() = runTest {
        val invalidJson = """{ invalid json """
        val flow = flowOf(invalidJson)

        coEvery { mockSession.subscribeText("/topic/game/p1") } returns flow
        GameStompClient.setSessionForTesting(mockSession)

        val received = mutableListOf<GameResponse>()

        GameStompClient.subscribeToGameUpdates(
            playerId = "p1",
            onUpdate = { received.add(it) },
            scope = this
        )

        advanceUntilIdle()

        assertTrue(received.isEmpty())
    }

    @Test
    fun `subscribeToGameUpdates should use default CoroutineScope`() = runTest {
        val json = """{"gameId":"g1","status":"PLAYING","currentPlayerId":"p1","players":[],"handCards":[],"lastPlayedCard":null}"""
        val flow = flowOf(json)

        coEvery { mockSession.subscribeText("/topic/game/p1") } returns flow
        GameStompClient.setSessionForTesting(mockSession)

        val received = mutableListOf<GameResponse>()

        GameStompClient.subscribeToGameUpdates(
            playerId = "p1",
            onUpdate = { received.add(it) }
        )

        advanceUntilIdle()
        assertEquals(1, received.size)

    }

}
    @Test
    fun `sendPlayCardRequest should do nothing if session is null`() = runTest {
        GameStompClient.setSessionForTesting(null)
        GameStompClient.sendPlayCardRequest("game-id", "player-id", "RED_5")
    }

    @Test
    fun `subscribeToScoreboard should invoke callback on successful message`() = testScope.runTest {
        val testJson = """[{"playerId":"p1","playerName":"Alice","score":100,"ready":true,"tricksWon":1,"prediction":1}]"""
        val flow = flowOf(testJson)
        val gameId = "game1"

        coEvery { mockSession.subscribeText("/topic/game/$gameId/scoreboard") } returns flow
        GameStompClient.setSessionForTesting(mockSession)

        var receivedScoreboard: List<PlayerDto>? = null
        GameStompClient.subscribeToScoreboard(
            gameId = gameId,
            onScoreboardReceived = { receivedScoreboard = it },
            scope = this
        )

        advanceUntilIdle()

        assertNotNull(receivedScoreboard)
        assertEquals(1, receivedScoreboard?.size)
        assertEquals("Alice", receivedScoreboard?.first()?.playerName)
    }


    @Test
    fun `subscribeToScoreboard should log error on json parsing exception`() = testScope.runTest {
        val invalidJson = "this is not valid json"
        val flow = flowOf(invalidJson)
        val gameId = "game1"

        coEvery { mockSession.subscribeText("/topic/game/game1/scoreboard") } returns flow
        GameStompClient.setSessionForTesting(mockSession)

        var receivedScoreboard: List<PlayerDto>? = null
        GameStompClient.subscribeToScoreboard(
            gameId = gameId,
            onScoreboardReceived = { receivedScoreboard = it },
            scope = this
        )

        advanceUntilIdle()

        assertNull(receivedScoreboard)
        coVerify { Log.e(eq("GameStompClient"), any(), any()) }
    }

    @Test
    fun `sendJoinRequest should not send if session is null`() = runTest {
        // Given
        GameStompClient.setSessionForTesting(null)

        // When
        GameStompClient.sendJoinRequest("game1", "player1", "Alice")
        advanceUntilIdle()

        coVerify(exactly = 0) {
            any<StompSession>().sendText(any(), any())
        }
    }

    @Test
    fun `sendPrediction should not send if session is null`() = runTest {
        GameStompClient.setSessionForTesting(null)
        GameStompClient.sendPrediction("game1", "player1", 1)
        advanceUntilIdle()
        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `subscribeToGameUpdates should not subscribe if session is null`() = runTest {
        GameStompClient.setSessionForTesting(null)
        GameStompClient.subscribeToGameUpdates(playerId = "player1", onUpdate = {}, scope = this)
        advanceUntilIdle()
        coVerify(exactly = 0) { any<StompSession>().subscribeText(any()) }
    }

    @Test
    fun `sendStartGameRequest should not send if session is null`() = runTest {
        GameStompClient.setSessionForTesting(null)
        GameStompClient.sendStartGameRequest("game1")
        advanceUntilIdle()
        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `sendPlayCardRequest should not send if session is null`() = runTest {
        GameStompClient.setSessionForTesting(null)
        GameStompClient.sendPlayCardRequest("game1", "player1", "RED_5")
        advanceUntilIdle()
        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }

    @Test
    fun `subscribeToScoreboard should not subscribe if session is null`() = runTest {
        GameStompClient.setSessionForTesting(null)
        GameStompClient.subscribeToScoreboard("game1", onScoreboardReceived = {}, scope = this)
        advanceUntilIdle()
        coVerify(exactly = 0) { any<StompSession>().subscribeText(any()) }
    }

    @Test
    fun `subscribeToErrors should not subscribe if session is null`() = runTest {
        GameStompClient.setSessionForTesting(null)
        GameStompClient.subscribeToErrors("player1", onError = {}, scope = this)
        advanceUntilIdle()
        coVerify(exactly = 0) { any<StompSession>().subscribeText(any()) }
    }

    @Test
    fun `sendProceedToNextRound should not send if session is null`() = runTest {
        GameStompClient.setSessionForTesting(null)
        GameStompClient.sendProceedToNextRound("game1")
        advanceUntilIdle()
        coVerify(exactly = 0) { any<StompSession>().sendText(any(), any()) }
    }
}
