package at.klu.client_wizardse2.ui.presentation.viewmodels

import android.util.Log
import android.content.Context
import android.hardware.camera2.CameraManager
import at.klu.client_wizardse2.helper.AndroidTorchController
import at.klu.client_wizardse2.helper.FlashlightHelper
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
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        val mockFlashlight = mockk<FlashlightHelper>(relaxed = true)
        viewModel = MainViewModel(context)
        viewModel.flashlightHelper = mockFlashlight
        mockkObject(GameStompClient)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
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
        val testGameId = "test-game-123"
        viewModel.gameId = testGameId

        coEvery { GameStompClient.sendStartGameRequest(any()) } just Runs

        viewModel.startGame()
        advanceUntilIdle()

        coVerify { GameStompClient.sendStartGameRequest(eq(testGameId)) }
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
        assertEquals("Game ID or Player ID not set. Cannot play card.", viewModel.error)
        coVerify(exactly = 0) { GameStompClient.sendPlayCardRequest(any(), any(), any()) }
    }

    @Test
    fun `playCard should set error if playerId is not set`() = runTest {
        viewModel.gameId = TEST_GAME_ID
        viewModel.playerId = "" // PlayerId ist nicht gesetzt

        coJustRun { GameStompClient.sendPlayCardRequest(any(), any(), any()) }

        viewModel.playCard("BLUE_7")
        advanceUntilIdle()

        assertNotNull(viewModel.error)
        assertEquals("Game ID or Player ID not set. Cannot play card.", viewModel.error)
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

    @Test
    fun `clearLastTrickWinner should nullify lastTrickWinnerId`() {
        viewModel.gameResponse = GameResponse(
            gameId = "game1",
            status = GameStatus.PLAYING,
            currentPlayerId = "p1",
            players = emptyList(),
            handCards = emptyList(),
            lastPlayedCard = null,
            lastTrickWinnerId = "p2"
        )

        viewModel.clearLastTrickWinner()
        assertNull(viewModel.gameResponse?.lastTrickWinnerId)
    }

    @Test
    fun `clearLastTrickWinner should set lastTrickWinnerId to null`() {
        val responseWithWinner = fakeResponse.copy(lastTrickWinnerId = "p1")
        viewModel.gameResponse = responseWithWinner

        viewModel.clearLastTrickWinner()

        assertNull(viewModel.gameResponse?.lastTrickWinnerId)
    }

    @Test
    fun `sendPrediction should set specific error when sum matches round`() = runTest {
        coEvery { GameStompClient.sendPrediction(any(), any(), any()) } throws Exception("exakt die Anzahl der Stiche")

        val result = viewModel.sendPrediction("game-id", "player-id", 2)

        assertFalse(result)
        assertEquals(
            "! Vorhersage nicht erlaubt – die Summe entspricht der Rundenzahl. Gib bitte einen anderen Wert ein.",
            viewModel.error
        )
    }

    @Test
    fun `connectAndJoin sets error if error is received from error topic`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        coEvery {
            GameStompClient.subscribeToGameUpdates(playerId = any(), onUpdate = any(), scope = any())
        } just Runs
        coEvery {
            GameStompClient.subscribeToErrors(playerId = any(), onError = captureLambda(), scope = any())
        } answers {
            lambda<(String) -> Unit>().invoke("Test error")
        }
        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } just Runs
        coEvery { GameStompClient.subscribeToScoreboard(any(), any(), any()) } just Runs

        viewModel.connectAndJoin("game1", "p1", "Alice")
        advanceUntilIdle()

        assertEquals("Test error", viewModel.error)
    }

    @Test
    fun `hasSubmittedPrediction should not reset if currentRound stays the same`() = runTest {
        coEvery { GameStompClient.connect() } returns true

        // Fake-Antworten mit identischem Round-Wert
        coEvery {
            GameStompClient.subscribeToGameUpdates(playerId = any(), onUpdate = any(), scope = any())
        } answers {
            val callback = args[1] as (GameResponse) -> Unit
            // Erstes Update → initialer Zustand
            callback(fakeResponse.copy(currentRound = 2))
            // Manuell: Benutzer hat eine Vorhersage abgegeben
            viewModel.hasSubmittedPrediction = true
            // Zweites Update → gleiche Runde => sollte hasSubmittedPrediction nicht zurücksetzen
            callback(fakeResponse.copy(currentRound = 2))
        }

        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } just Runs

        viewModel.connectAndJoin(TEST_GAME_ID, TEST_PLAYER_ID, TEST_PLAYER_NAME)
        advanceUntilIdle()

        assertTrue("Vorhersage sollte nicht zurückgesetzt werden, wenn sich die Runde nicht ändert", viewModel.hasSubmittedPrediction)
    }

    @Test
    fun `abortGameForAll should call GameStompClient with correct gameId`() = runTest {
        val testGameId = "game-to-abort"
        viewModel.gameId = testGameId
        coEvery { GameStompClient.sendForceEndGame(any()) } just Runs

        viewModel.abortGameForAll()
        advanceUntilIdle()

        coVerify { GameStompClient.sendForceEndGame(eq(testGameId)) }
    }

    @Test
    fun `abortGameForAll should do nothing if gameId is empty`() = runTest {
        viewModel.gameId = ""

       coEvery { GameStompClient.sendForceEndGame(any()) } just Runs

        viewModel.abortGameForAll()
        advanceUntilIdle()

        coVerify(exactly = 0) { GameStompClient.sendForceEndGame(any()) }
    }

    @Test
    fun `returnToLobbyForAll should call GameStompClient with correct gameId`() = runTest {
        val testGameId = "game-to-return"
        viewModel.gameId = testGameId
        coEvery { GameStompClient.sendReturnToLobbyRequest(any()) } just Runs

        viewModel.returnToLobbyForAll()
        advanceUntilIdle()

        coVerify { GameStompClient.sendReturnToLobbyRequest(eq(testGameId)) }
    }

    @Test
    fun `returnToLobbyForAll should do nothing if gameId is empty`() = runTest {
       viewModel.gameId = ""

        viewModel.returnToLobbyForAll()
        advanceUntilIdle()

        coVerify(exactly = 0) { GameStompClient.sendReturnToLobbyRequest(any()) }
    }

    @Test
    fun `toggleCheatState should enable cheating and trigger flashlight`() {
        val playerId = "test-player"
        val flashlightMock = mockk<FlashlightHelper>(relaxed = true)
        viewModel.flashlightHelper = flashlightMock

        viewModel.toggleCheatState(playerId)

        assertTrue(viewModel.cheatStates[playerId] == true)
        verify { flashlightMock.toggleFlashlight(true, 5000) }
    }

    @Test
    fun `toggleCheatState should disable cheating on second call`() {
        val playerId = "test-player"
        val flashlightMock = mockk<FlashlightHelper>(relaxed = true)
        viewModel.flashlightHelper = flashlightMock

        viewModel.toggleCheatState(playerId)
        viewModel.toggleCheatState(playerId)

        assertFalse(viewModel.cheatStates[playerId] ?: true)
        verify(exactly = 1) { flashlightMock.toggleFlashlight(true, 5000) }
    }

    @Test
    fun `onCleared should call cleanup if flashlightHelper is initialized`() {
        val flashlightMock = mockk<FlashlightHelper>(relaxed = true)
        viewModel.flashlightHelper = flashlightMock

        viewModel.performCleanupIfNeeded()

        verify { flashlightMock.cleanup() }
    }

    @Test
    fun `connectAndJoin sets showRoundSummaryScreen to true when status is ROUND_END_SUMMARY`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        coEvery { GameStompClient.subscribeToErrors(any(), any(), any()) } just Runs
        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } just Runs
        coEvery { GameStompClient.subscribeToScoreboard(any(), any(), any()) } just Runs

        coEvery {
            GameStompClient.subscribeToGameUpdates(
                playerId = any(),
                onUpdate = captureLambda(),
                scope = any()
            )
        } answers {
            lambda<(GameResponse) -> Unit>().invoke(
                GameResponse(
                    gameId = "game1",
                    status = GameStatus.ROUND_END_SUMMARY,
                    currentPlayerId = "p1",
                    players = emptyList(),
                    handCards = emptyList(),
                    lastPlayedCard = null,
                    lastTrickWinnerId = null,
                    currentRound = 1
                )
            )
        }

        viewModel.connectAndJoin("game1", "p1", "Alice")
        advanceUntilIdle()

        assertTrue(viewModel.showRoundSummaryScreen)
    }

    @Test
    fun `connectAndJoin sets showRoundSummaryScreen to false when status changes from ROUND_END_SUMMARY`() = runTest {
        coEvery { GameStompClient.connect() } returns true
        coEvery { GameStompClient.subscribeToErrors(any(), any(), any()) } just Runs
        coEvery { GameStompClient.sendJoinRequest(any(), any(), any()) } just Runs
        coEvery { GameStompClient.subscribeToScoreboard(any(), any(), any()) } just Runs

        val updateSlot = slot<(GameResponse) -> Unit>()

        coEvery {
            GameStompClient.subscribeToGameUpdates(
                playerId = any(),
                onUpdate = capture(updateSlot),
                scope = any()
            )
        } just Runs

        viewModel.connectAndJoin("game1", "p1", "Alice")
        advanceUntilIdle()

        updateSlot.captured.invoke(
            GameResponse(
                gameId = "game1",
                status = GameStatus.ROUND_END_SUMMARY,
                currentPlayerId = "p1",
                players = emptyList(),
                handCards = emptyList(),
                lastPlayedCard = null,
                lastTrickWinnerId = null,
                currentRound = 1
            )
        )
        assertTrue(viewModel.showRoundSummaryScreen)

        updateSlot.captured.invoke(
            GameResponse(
                gameId = "game1",
                status = GameStatus.PLAYING,
                currentPlayerId = "p1",
                players = emptyList(),
                handCards = emptyList(),
                lastPlayedCard = null,
                lastTrickWinnerId = null,
                currentRound = 1
            )
        )
        assertFalse(viewModel.showRoundSummaryScreen)
    }

    @Test
    fun `subscribeToScoreboard updates scoreboard correctly`() = runTest {
        // Arrange
        val scoreboardCallbackSlot = slot<(List<PlayerDto>) -> Unit>()
        val gameId = "test-game"

        val sampleBoard = listOf(
            PlayerDto("p1", "Alice", 10, true, 1, 1),
            PlayerDto("p2", "Bob", 15, true, 2, 2)
        )

        coEvery {
            GameStompClient.subscribeToScoreboard(
                gameId = gameId,
                onScoreboardReceived = capture(scoreboardCallbackSlot),
                scope = any()
            )
        } just Runs

        // Act
        viewModel.subscribeToScoreboard(gameId)
        advanceUntilIdle()

        scoreboardCallbackSlot.captured.invoke(sampleBoard)

        assertEquals(2, viewModel.scoreboard.size)
        assertEquals("Alice", viewModel.scoreboard[0].playerName)
        assertEquals("Bob", viewModel.scoreboard[1].playerName)
    }

    @Test
    fun `proceedToNextRound should call GameStompClient with correct gameId`() = runTest {
        val testGameId = "round-game-42"
        viewModel.gameId = testGameId

        coEvery { GameStompClient.sendProceedToNextRound(any()) } just Runs

        viewModel.proceedToNextRound()
        advanceUntilIdle()

        coVerify { GameStompClient.sendProceedToNextRound(eq(testGameId)) }
    }

    @Test
    fun `proceedToNextRound should not call GameStompClient when gameId is empty`() = runTest {
        viewModel.gameId = ""

        coEvery { GameStompClient.sendProceedToNextRound(any()) } just Runs

        viewModel.proceedToNextRound()
        advanceUntilIdle()

        coVerify(exactly = 0) { GameStompClient.sendProceedToNextRound(any()) }
    }

    @Test
    fun `resetGame should reset all game state`() {
        viewModel.gameResponse = fakeResponse
        viewModel.scoreboard = listOf(
            PlayerDto("p1", "Alice", 10, true, 1, 1)
        )
        viewModel.playerId = "player123"
        viewModel.playerName = "Bob"
        viewModel.showRoundSummaryScreen = true

        viewModel.resetGame()

        assertNull(viewModel.gameResponse)
        assertTrue(viewModel.scoreboard.isEmpty())
        assertEquals("", viewModel.playerId)
        assertEquals("", viewModel.playerName)
        assertFalse(viewModel.showRoundSummaryScreen)
    }

    @Test
    fun `endGameEarly should update game status to ENDED and hide summary`() {
        viewModel.gameResponse = fakeResponse.copy(status = GameStatus.PLAYING)
        viewModel.showRoundSummaryScreen = true

        viewModel.endGameEarly()

        assertEquals(GameStatus.ENDED, viewModel.gameResponse?.status)
        assertFalse(viewModel.showRoundSummaryScreen)
    }

    @Test
    fun `flashlightHelper should be initialized when toggleCheatState is called without prior init`() {
        // Neues ViewModel mit Kontext, aber ohne flashlightHelper-Zuweisung
        val freshViewModel = MainViewModel(context)

        // Stubbe AndroidTorchController intern, damit kein echter Zugriff erfolgt
        mockkConstructor(AndroidTorchController::class)
        every { anyConstructed<AndroidTorchController>().setTorchMode(any(), any()) } just Runs
        every { anyConstructed<AndroidTorchController>().getCameraIdList() } returns arrayOf("0")
        every { anyConstructed<AndroidTorchController>().isFlashAvailable("0") } returns true

        // Mocke Context → CameraManager
        val mockCameraManager = mockk<CameraManager>(relaxed = true)
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns mockCameraManager

        // Aufruf einer Methode, die flashlightHelper verwendet
        freshViewModel.toggleCheatState("p123")

        // Erwartung: flashlightHelper wurde initialisiert
        assertTrue(freshViewModel.isFlashlightHelperInitialized())
    }
}