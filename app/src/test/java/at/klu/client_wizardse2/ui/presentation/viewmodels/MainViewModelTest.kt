package at.klu.client_wizardse2.ui.presentation.viewmodels

import android.util.Log
import at.klu.client_wizardse2.model.response.GameResponse
import at.klu.client_wizardse2.model.response.GameStatus
import at.klu.client_wizardse2.network.GameStompClient
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel()
        mockkObject(GameStompClient)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @Test
    fun `given successful connection when connectAndJoin called then gameResponse is updated`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        coEvery {
            GameStompClient.subscribeToGameUpdates(
                playerId = any(),
                onUpdate = any(),
                scope = any()
            )
        } answers {
            val callback = args[1] as (GameResponse) -> Unit
            callback(fakeResponse)
        }
        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } just Runs

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertEquals(fakeResponse, viewModel.gameResponse)
        assertNull(viewModel.error)
    }

    @Test
    fun `given failed connection when connectAndJoin called then error is set`() = runTest {
        coEvery { GameStompClient.connect() } returns false

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertNull(viewModel.gameResponse)
        assertEquals("Connection to server failed", viewModel.error)
    }

    @Test
    fun `given connect throws exception when connectAndJoin called then error is set`() = runTest {
        coEvery { GameStompClient.connect() } throws RuntimeException("Unexpected crash")

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertNull(viewModel.gameResponse)
        assertEquals("Error: Unexpected crash", viewModel.error)
    }

    @Test
    fun `given subscribe throws exception when connectAndJoin called then error is set`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        coEvery {
            GameStompClient.subscribeToGameUpdates(
                playerId = any(),
                onUpdate = any(),
                scope = any()
            )
        } throws RuntimeException("Subscription failed")

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertNull(viewModel.gameResponse)
        assertEquals("Error: Subscription failed", viewModel.error)
    }

    @Test
    fun `given sendJoinRequest throws exception when connectAndJoin called then error is set`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        coEvery {
            GameStompClient.subscribeToGameUpdates(
                playerId = any(),
                onUpdate = any(),
                scope = any()
            )
        } answers {
            val callback = args[1] as (GameResponse) -> Unit
            callback(fakeResponse)
        }
        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } throws RuntimeException("Join failed")

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        // Es wurde vorher eine gültige GameResponse durch den Callback gesetzt
        assertEquals(fakeResponse, viewModel.gameResponse)
        assertEquals("Error: Join failed", viewModel.error)
    }

    @Test
    fun `sendPrediction should call GameStompClient with correct data`() = runTest {

        val gameId = "game-1"
        val playerId = "player-1"
        val prediction = 3

        coEvery { GameStompClient.sendPrediction(any(), any(), any()) } just Runs

        viewModel.sendPrediction(gameId, playerId, prediction)
        advanceUntilIdle()

        coVerify {
            GameStompClient.sendPrediction(
                eq(gameId),
                eq(playerId),
                eq(prediction)
            )
        }
    }
    @Test
    fun `hasGameStarted should return true when game status is PLAYING`() {
        val viewModel = MainViewModel()
        viewModel.gameResponse = GameResponse(
            gameId = "test-id",
            status = GameStatus.PLAYING,
            currentPlayerId = null,
            players = emptyList(),
            handCards = emptyList(),
            lastPlayedCard = null
        )

        assertTrue(viewModel.hasGameStarted())
    }

    @Test
    fun `hasGameStarted should return false when game status is not PLAYING`() {
        val viewModel = MainViewModel()
        viewModel.gameResponse = GameResponse(
            gameId = "test-id",
            status = GameStatus.LOBBY,
            currentPlayerId = null,
            players = emptyList(),
            handCards = emptyList(),
            lastPlayedCard = null
        )

        assertFalse(viewModel.hasGameStarted())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `startGame should call GameStompClient with correct gameId`() = runTest {
        val viewModel = MainViewModel()
        val testGameId = "test-game-123"
        viewModel.gameId = testGameId

        coEvery { GameStompClient.sendStartGameRequest(any()) } just Runs

        viewModel.startGame()
        advanceUntilIdle()

        coVerify { GameStompClient.sendStartGameRequest(eq(testGameId)) }
    }


    private fun setupMockSuccess() {
        coEvery { GameStompClient.connect() } returns true
        coEvery {
            GameStompClient.subscribeToGameUpdates(playerId = any(), onUpdate = any(), scope = any())
        } answers {
            val callback = args[1] as (GameResponse) -> Unit
            callback(fakeResponse)
        }
        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } returns Unit
    }

    companion object {
        private const val TEST_GAME_ID = "game1"
        private const val TEST_PLAYER_ID = "p1"
        private const val TEST_PLAYER_NAME = "TestPlayer"

        private val fakeResponse = GameResponse(
            gameId = TEST_GAME_ID,
            status = GameStatus.PLAYING,
            currentPlayerId = TEST_PLAYER_ID,
            players = emptyList(),
            handCards = emptyList(),
            lastPlayedCard = null
        )
    }

    @Test
    fun `gameId, playerId, playerName are correctly assigned after connectAndJoin`() = runTest {
        coEvery { GameStompClient.connect() } returns false

        viewModel.connectAndJoin("myGame", "player42", "Lina")
        advanceUntilIdle()

        assertEquals("myGame", viewModel.gameId)
        assertEquals("player42", viewModel.playerId)
        assertEquals("Lina", viewModel.playerName)
    }

    @Test
    fun `startGame should not crash when gameId is empty`() = runTest {
        coEvery { GameStompClient.sendStartGameRequest(any()) } just Runs

        viewModel.gameId = "" // explizit leer
        viewModel.startGame()
        advanceUntilIdle()

        coVerify { GameStompClient.sendStartGameRequest(eq("")) }
    }

    @Test
    fun `connectAndJoin does not crash when session is null`() = runTest {
        // Simuliere connect() als erfolgreich, aber ohne Session gesetzt
        coEvery { GameStompClient.connect() } returns true
        coEvery {
            GameStompClient.subscribeToGameUpdates(playerId = any(), onUpdate = any(), scope = any())
        } just Runs
        coEvery {
            GameStompClient.sendJoinRequest(any(), any(), any())
        } just Runs

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertNull(viewModel.error) // Kein Fehler, aber keine Response erwartet
    }

    @Test
    fun `connectAndJoin handles empty playerId gracefully`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        coEvery {
            GameStompClient.subscribeToGameUpdates(playerId = any(), onUpdate = any(), scope = any())
        } just Runs
        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } just Runs

        viewModel.connectAndJoin(TEST_GAME_ID, "", TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertEquals(TEST_GAME_ID, viewModel.gameId)
        assertEquals("", viewModel.playerId)
        assertEquals(TEST_PLAYER_NAME, viewModel.playerName)
    }


}
