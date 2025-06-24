package at.klu.client_wizardse2.helpers

import android.util.Log
import at.klu.client_wizardse2.helper.FlashlightHelper
import at.klu.client_wizardse2.helper.TorchController
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class FlashlightHelperTest {

    private val controller = mockk<TorchController>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()
    private lateinit var helper: FlashlightHelper

    @Before
    fun setup() {
        helper = FlashlightHelper(controller, dispatcher)
        every { controller.getCameraIdList() } returns arrayOf("0")
        every { controller.isFlashAvailable("0") } returns true

        // suppress Android Log
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `startFlashlightJob logs error if exception thrown`() = runTest(dispatcher) {
        every { controller.getCameraIdList() } throws RuntimeException("Camera failure")

        helper.toggleFlashlight(true, durationMs = 500)
        advanceUntilIdle()
    }

    @Test
    fun `toggleFlashlight false disables flashlight immediately`() = runTest(dispatcher) {
        helper.toggleFlashlight(false)
        runCurrent()
        verify { controller.setTorchMode("0", false) }
    }

    @Test
    fun `cleanup cancels job and disables flashlight`() = runTest(dispatcher) {
        helper.toggleFlashlight(true, durationMs = 100)
        helper.cleanup()
        runCurrent()
        verify { controller.setTorchMode("0", false) }
    }

    @Test
    fun `turnOffFlashlightSafely logs error when exception occurs`() = runTest(dispatcher) {
        every { controller.getCameraIdList() } returns arrayOf("0")
        every { controller.isFlashAvailable("0") } returns true
        every { controller.setTorchMode("0", false) } throws RuntimeException("Failed to turn off")

        helper.toggleFlashlight(false)
        runCurrent() // ensure coroutine runs

        verify { controller.setTorchMode("0", false) }
    }
}

