package at.klu.client_wizardse2.helper

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/**
 * A concrete implementation of [TorchController] that uses Android's [CameraManager]
 * to control the device's flashlight (torch).
 *
 * This class accesses camera characteristics and torch mode settings provided
 * by the Android Camera2 API.
 *
 * @property cameraManager The [CameraManager] used to interact with the device's cameras.
 */
class AndroidTorchController(private val cameraManager: CameraManager) : TorchController {

    override fun getCameraIdList(): Array<String> = cameraManager.cameraIdList

    override fun isFlashAvailable(cameraId: String): Boolean {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    override fun setTorchMode(cameraId: String, enabled: Boolean) {
        cameraManager.setTorchMode(cameraId, enabled)
    }
}