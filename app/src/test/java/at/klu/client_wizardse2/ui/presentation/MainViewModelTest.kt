package at.klu.client_wizardse2.ui.presentation

import at.klu.client_wizardse2.data.websocket.GStompClient
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals


class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var stompClient: GStompClient

    @Before
    fun setUp() {
        mockkConstructor(GStompClient::class)
        stompClient = spyk(GStompClient(mockk()))
        every { anyConstructed<GStompClient>().connect() } just Runs
        every { anyConstructed<GStompClient>().sendHello() } just Runs
        every { anyConstructed<GStompClient>().sendJson() } just Runs

        viewModel = MainViewModel()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onResponse adds message to list`() {
        val message = "Hello from server"
        viewModel.onResponse(message)

        assertEquals(1, viewModel.messages.size)
        assertEquals(message, viewModel.messages[0])
    }

    @Test
    fun `sendHello calls GameStompClient sendHello`() {
        viewModel.sendHello()

        verify { anyConstructed<GStompClient>().sendHello() }
    }

    @Test
    fun `sendJson calls GameStompClient sendJson`() {
        viewModel.sendJson()

        verify { anyConstructed<GStompClient>().sendJson() }
    }
}
