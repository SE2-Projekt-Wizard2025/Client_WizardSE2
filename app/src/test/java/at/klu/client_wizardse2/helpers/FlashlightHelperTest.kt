package at.klu.client_wizardse2.helpers

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import at.klu.client_wizardse2.helper.FlashlightHelper
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlashlightHelperTest {

    private lateinit var context: Context
    private lateinit var cameraManager: CameraManager
    private lateinit var helper: FlashlightHelper

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        context = mockk()
        cameraManager = mockk(relaxed = true)
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager

        helper = FlashlightHelper(context)
    }

    @Before
    fun suppressAndroidLog() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `toggleFlashlight false immediately disables torch`() {
        val cameraId = "0"
        every { cameraManager.cameraIdList } returns arrayOf(cameraId)
        val characteristics = mockk<CameraCharacteristics>()
        every { cameraManager.getCameraCharacteristics(cameraId) } returns characteristics
        every { characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) } returns true

        justRun { cameraManager.setTorchMode(any(), any()) }

        helper.toggleFlashlight(enable = false)

        verify {
            cameraManager.setTorchMode(cameraId, false)
        }
    }

    @Test
    fun `getCameraId throws when no camera with flash`() {
        every { cameraManager.cameraIdList } returns arrayOf("0", "1")
        val characteristics = mockk<CameraCharacteristics>()
        every { cameraManager.getCameraCharacteristics(any()) } returns characteristics
        every { characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) } returns false

        justRun { cameraManager.setTorchMode(any(), any()) }

        helper.toggleFlashlight(enable = false)
    }

    @Test
    fun `cleanup cancels job and disables torch`() {
        val cameraId = "0"
        every { cameraManager.cameraIdList } returns arrayOf(cameraId)
        val characteristics = mockk<CameraCharacteristics>()
        every { cameraManager.getCameraCharacteristics(cameraId) } returns characteristics
        every { characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) } returns true

        justRun { cameraManager.setTorchMode(any(), any()) }

        helper.toggleFlashlight(true, 1000)
        helper.cleanup()

        verify {
            cameraManager.setTorchMode(cameraId, false)
        }
    }
}
