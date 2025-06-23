package at.klu.client_wizardse2.helper

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.*

class FlashlightHelper(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var job: Job? = null


    fun toggleFlashlight(enable: Boolean, durationMs: Long = 2000) {
        job?.cancel() //alten job cancelen falls vorhanden

        if (enable) {
            job = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val cameraId = getCameraId()
                    cameraManager.setTorchMode(cameraId, true)
                    delay(durationMs)
                    cameraManager.setTorchMode(cameraId, false)
                } catch (e: Exception) {
                    Log.e("Flashlight", "Error controlling flashlight", e)
                }
            }
        }
        else {
            try {
                val cameraId = getCameraId()
                cameraManager.setTorchMode(cameraId, false)
            } catch (e: Exception) {
                Log.e("Flashlight", "Error turning off flashlight", e)
            }
        }
    }

    private fun getCameraId(): String {
        return cameraManager.cameraIdList.first { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }



    fun cleanup() {
        job?.cancel()
        toggleFlashlight(false)
    }
}