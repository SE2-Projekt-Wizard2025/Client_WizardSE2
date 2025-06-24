package at.klu.client_wizardse2.helper

import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A helper class that manages the device's flashlight (torch) using a provided [TorchController].
 * It allows turning the flashlight on for a given duration, and safely turning it off.
 *
 * This class uses coroutines to handle asynchronous flashlight control, and allows injection
 * of a custom [CoroutineDispatcher] for testability.
 *
 * @property torchController The abstraction that provides access to torch control functions.
 * @property ioDispatcher Dispatcher used for executing flashlight operations. Defaults to [Dispatchers.IO].
 */
class FlashlightHelper(
    private val torchController: TorchController,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var job: Job? = null

    /**
     * Turns the flashlight on or off.
     *
     * If [enable] is true, the flashlight will be turned on for [durationMs] milliseconds,
     * then automatically turned off. If [enable] is false, the flashlight will be turned off immediately.
     *
     * Any currently running flashlight operation will be canceled before a new one begins.
     *
     * @param enable Whether to enable or disable the flashlight.
     * @param durationMs Duration in milliseconds to keep the flashlight on if [enable] is true. Defaults to 2000ms.
     */
    fun toggleFlashlight(enable: Boolean, durationMs: Long = 2000) {
        val previousJob = job
        job = CoroutineScope(Dispatchers.IO).launch {
            previousJob?.cancelAndJoin()

            if (enable) {
                startFlashlightJob(durationMs)
            } else {
                turnOffFlashlightSafely()
            }
        }
    }

    /**
     * Starts a coroutine that turns on the flashlight for [durationMs] milliseconds,
     * then turns it off. Exceptions are caught and logged.
     *
     * @param durationMs The duration in milliseconds for which the flashlight should stay on.
     */
    private suspend fun startFlashlightJob(durationMs: Long) {
        try {
            val cameraId = findFlashCameraId()
            torchController.setTorchMode(cameraId, true)
            delay(durationMs)
            torchController.setTorchMode(cameraId, false)
        } catch (e: Exception) {
            Log.e("Flashlight", "Error controlling flashlight", e)
        }
    }

    /**
     * Turns off the flashlight safely by identifying the correct camera ID
     * and disabling the torch mode. Exceptions are caught and logged.
     */
    private fun turnOffFlashlightSafely() {
        try {
            val cameraId = findFlashCameraId()
            torchController.setTorchMode(cameraId, false)
        } catch (e: Exception) {
            Log.e("Flashlight", "Error turning off flashlight", e)
        }
    }

    /**
     * Finds the first available camera ID that supports flashlight functionality.
     *
     * @return The camera ID as a [String].
     * @throws NoSuchElementException if no flashlight-capable camera is found.
     */
    @VisibleForTesting
    internal fun findFlashCameraId(): String {
        return torchController.getCameraIdList().first { id ->
            torchController.isFlashAvailable(id)
        }
    }

    /**
     * Cancels any running flashlight job and ensures the flashlight is turned off.
     * Should be called during cleanup (e.g., in `onStop` or `onDestroy`).
     */
    fun cleanup() {
        job?.cancel()
        toggleFlashlight(false)
    }
}