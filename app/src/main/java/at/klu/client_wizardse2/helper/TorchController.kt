package at.klu.client_wizardse2.helper

/**
 * An abstraction for controlling a device's flashlight (torch).
 *
 * This interface allows querying available camera IDs, checking for flashlight support,
 * and enabling or disabling the torch mode. It is designed to decouple flashlight logic
 * from Android-specific implementations to improve testability and modularity.
 */
interface TorchController {

    /**
     * Returns a list of available camera IDs on the device.
     *
     * @return An array of camera ID strings.
     */
    fun getCameraIdList(): Array<String>

    /**
     * Checks whether the specified camera supports flashlight functionality.
     *
     * @param cameraId The ID of the camera to check.
     * @return `true` if the camera has a flashlight, `false` otherwise.
     */
    fun isFlashAvailable(cameraId: String): Boolean

    /**
     * Enables or disables the torch (flashlight) mode for the specified camera.
     *
     * @param cameraId The ID of the camera.
     * @param enabled `true` to turn the flashlight on, `false` to turn it off.
     */
    fun setTorchMode(cameraId: String, enabled: Boolean)
}