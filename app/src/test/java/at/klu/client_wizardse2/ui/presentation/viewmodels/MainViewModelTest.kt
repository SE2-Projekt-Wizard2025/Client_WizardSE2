package at.klu.client_wizardse2.ui.presentation.viewmodels

import android.util.Log
import at.klu.client_wizardse2.model.response.GameResponse
import at.klu.client_wizardse2.model.response.GameStatus
import at.klu.client_wizardse2.model.response.dto.PlayerDto
import at.klu.client_wizardse2.network.GameStompClient
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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
        every { Log.e(any<String>(), any<String>()) } answers { 0 }
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } answers { 0 }
    }

    @Test
    fun `given successful connection when connectAndJoin called then gameResponse is updated`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        @Suppress("UNCHECKED_CAST")
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
            GameStompClient.subscribeToGameUpdates(onUpdate = any(), scope = any(), playerId = any())
        } throws RuntimeException("Subscription failed")

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertNull(viewModel.gameResponse)
        assertEquals("Error: Subscription failed", viewModel.error)
    }

    @Test
    fun `given sendJoinRequest throws exception when connectAndJoin called then error is set`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        @Suppress("UNCHECKED_CAST")
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
            lastPlayedCard = null,
            lastTrickWinnerId="p1"
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
            lastPlayedCard = null,
            lastTrickWinnerId=null
        )

        assertFalse(viewModel.hasGameStarted())
    }

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
        @Suppress("UNCHECKED_CAST")
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
            lastPlayedCard = null,
            lastTrickWinnerId= TEST_PLAYER_ID
        )
    }

    @Test
    fun `scoreboard should be updated correctly`() = runTest {
        val viewModel = MainViewModel()

        val sampleScoreboard = listOf(
            PlayerDto(
                playerId = "p1",
                playerName = "Alice",
                score = 30,
                ready = true,
                tricksWon = 2,
                prediction = 2
            ),
            PlayerDto(
                playerId = "p2",
                playerName = "Bob",
                score = 40,
                ready = true,
                tricksWon = 1,
                prediction = 1
            )
        )
        viewModel.scoreboard = sampleScoreboard

        assertEquals(2, viewModel.scoreboard.size)
        assertEquals("Alice", viewModel.scoreboard[0].playerName)
        assertEquals(40, viewModel.scoreboard[1].score)
    }

    @Test
    fun `playCard should call GameStompClient with correct data`() = runTest {
        val testGameId = "game-test-123"
        val testPlayerId = "player-test-abc"
        val testCardString = "RED_10"

        viewModel.gameId = testGameId
        viewModel.playerId = testPlayerId

        coEvery { GameStompClient.sendPlayCardRequest(any(), any(), any()) } just Runs

        viewModel.playCard(testCardString)
        advanceUntilIdle()

        coVerify {
            GameStompClient.sendPlayCardRequest(
                eq(testGameId),
                eq(testPlayerId),
                eq(testCardString)
            )
        }
        assertNull(viewModel.error)
    }

    @Test
    fun `playCard should set error if gameId is not set`() = runTest {
        viewModel.gameId = ""
        viewModel.playerId = TEST_PLAYER_ID

        coJustRun { GameStompClient.sendPlayCardRequest(any(), any(), any()) }

        viewModel.playCard("RED_5")
        advanceUntilIdle()

        assertNotNull(viewModel.error)
        assertEquals("Game ID oder Player ID nicht gesetzt. Karte kann nicht gelegt werden.", viewModel.error)
        coVerify(exactly = 0) { GameStompClient.sendPlayCardRequest(any(), any(), any()) }
    }

    @Test
    fun `playCard should set error if playerId is not set`() = runTest {
        viewModel.gameId = TEST_GAME_ID
        viewModel.playerId = ""

        coJustRun { GameStompClient.sendPlayCardRequest(any(), any(), any()) }

        viewModel.playCard("BLUE_7")
        advanceUntilIdle()

        assertNotNull(viewModel.error)
        assertEquals("Game ID oder Player ID nicht gesetzt. Karte kann nicht gelegt werden.", viewModel.error)
        coVerify(exactly = 0) { GameStompClient.sendPlayCardRequest(any(), any(), any()) }
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

        viewModel.gameId = ""
        viewModel.startGame()
        advanceUntilIdle()

        coVerify { GameStompClient.sendStartGameRequest(eq("")) }
    }

    @Test
    fun `connectAndJoin does not crash when session is null`() = runTest {

        coEvery { GameStompClient.connect() } returns true
        coEvery {
            GameStompClient.subscribeToGameUpdates(playerId = any(), onUpdate = any(), scope = any())
        } just Runs
        coEvery {
            GameStompClient.sendJoinRequest(any(), any(), any())
        } just Runs

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertNull(viewModel.error)
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

    @Test
    fun `connectAndJoin should set showRoundSummaryScreen true if status is ROUND_END_SUMMARY`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        @Suppress("UNCHECKED_CAST")
        coEvery {
            GameStompClient.subscribeToGameUpdates(playerId = any(), onUpdate = any(), scope = any())
        } answers {
            val callback = args[1] as (GameResponse) -> Unit

            val roundSummaryResponse = fakeResponse.copy(status = GameStatus.ROUND_END_SUMMARY)
            callback(roundSummaryResponse)
        }
        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } just Runs
        coEvery { GameStompClient.subscribeToErrors(any(), any()) } just Runs
        coEvery { GameStompClient.subscribeToScoreboard(any(), any(), any()) } just Runs

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertEquals(GameStatus.ROUND_END_SUMMARY, viewModel.gameResponse?.status)
        assertTrue(viewModel.showRoundSummaryScreen)
    }

    @Test
    fun `connectAndJoin should set showRoundSummaryScreen false if status is not ROUND_END_SUMMARY and it was previously true`() = runTest {
        viewModel.showRoundSummaryScreen = true

        coEvery { GameStompClient.connect() } returns true
        @Suppress("UNCHECKED_CAST")
        coEvery {
            GameStompClient.subscribeToGameUpdates(playerId = any(), onUpdate = any(), scope = any())
        } answers {
            val callback = args[1] as (GameResponse) -> Unit

            val playingResponse = fakeResponse.copy(status = GameStatus.PLAYING)
            callback(playingResponse)
        }
        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } just Runs
        coEvery { GameStompClient.subscribeToErrors(any(), any()) } just Runs
        coEvery { GameStompClient.subscribeToScoreboard(any(), any(), any()) } just Runs

        // When
        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        // Then
        assertEquals(GameStatus.PLAYING, viewModel.gameResponse?.status)
        assertFalse(viewModel.showRoundSummaryScreen)
    }

    @Test
    fun `sendPrediction should set specific error message if prediction is not allowed`() = runTest {
        val gameId = "game-1"
        val playerId = "player-1"
        val prediction = 3
        val specificErrorMessage = "Diese Vorhersage ergibt exakt die Anzahl der Stiche und ist damit verboten."

        coEvery { GameStompClient.sendPrediction(any(), any(), any()) } throws RuntimeException(specificErrorMessage)

        val success = viewModel.sendPrediction(gameId, playerId, prediction)
        advanceUntilIdle()

        assertFalse(success)
        assertEquals("❌ Vorhersage nicht erlaubt! Die Summe der Stiche stimmt. Bitte wählen Sie einen anderen Wert ein.", viewModel.error)
        coVerify { GameStompClient.sendPrediction(eq(gameId), eq(playerId), eq(prediction)) }
    }

    @Test
    fun `sendPrediction should set generic error message if exception occurs without specific message`() = runTest {
        val gameId = "game-1"
        val playerId = "player-1"
        val prediction = 3

        coEvery { GameStompClient.sendPrediction(any(), any(), any()) } throws RuntimeException(null as String?)

        val success = viewModel.sendPrediction(gameId, playerId, prediction)
        advanceUntilIdle()

        assertFalse(success)
        assertEquals("Error: Unbekannter Fehler bei der Vorhersage.", viewModel.error)
    }

    @Test
    fun `playCard should set error if IllegalStateException occurs`() = runTest {
        val testCardString = "RED_10"
        viewModel.gameId = TEST_GAME_ID
        viewModel.playerId = TEST_PLAYER_ID

        coEvery { GameStompClient.sendPlayCardRequest(any(), any(), any()) } throws IllegalStateException("Das Spiel ist nicht aktiv.")

        val success = viewModel.playCard(testCardString)
        advanceUntilIdle()

        assertFalse(success)
        assertEquals("Das Spiel ist nicht aktiv.", viewModel.error)
        coVerify { GameStompClient.sendPlayCardRequest(eq(TEST_GAME_ID), eq(TEST_PLAYER_ID), eq(testCardString)) }
    }

    @Test
    fun `playCard should set error if IllegalArgumentException occurs`() = runTest {
        val testCardString = "INVALID_CARD"
        viewModel.gameId = TEST_GAME_ID
        viewModel.playerId = TEST_PLAYER_ID

        coEvery { GameStompClient.sendPlayCardRequest(any(), any(), any()) } throws IllegalArgumentException("Ungültige Karte zum Legen.")

        val success = viewModel.playCard(testCardString)
        advanceUntilIdle()

        assertFalse(success)
        assertEquals("Ungültige Karte zum Legen.", viewModel.error)
        coVerify { GameStompClient.sendPlayCardRequest(eq(TEST_GAME_ID), eq(TEST_PLAYER_ID), eq(testCardString)) }
    }

    @Test
    fun `playCard should set generic error if unexpected exception occurs`() = runTest {
        val testCardString = "RED_10"
        viewModel.gameId = TEST_GAME_ID
        viewModel.playerId = TEST_PLAYER_ID

        coEvery { GameStompClient.sendPlayCardRequest(any(), any(), any()) } throws RuntimeException("DB error")

        val success = viewModel.playCard(testCardString)
        advanceUntilIdle()

        assertFalse(success)
        assertEquals("Unerwarteter Fehler beim Kartenlegen: DB error", viewModel.error)
        coVerify { GameStompClient.sendPlayCardRequest(eq(TEST_GAME_ID), eq(TEST_PLAYER_ID), eq(testCardString)) }
    }

    @Test
    fun `clearLastTrickWinner should not crash if gameResponse is null`() {
        viewModel.gameResponse = null
        viewModel.clearLastTrickWinner()
        assertNull(viewModel.gameResponse)
    }

    @Test
    fun `proceedToNextRound should not call sendProceedToNextRound if gameId is empty`() = runTest {
        viewModel.gameId = ""

        coJustRun { GameStompClient.sendProceedToNextRound(any()) }

        viewModel.proceedToNextRound()
        advanceUntilIdle()

        coVerify(exactly = 0) { GameStompClient.sendProceedToNextRound(any()) }
    }

}