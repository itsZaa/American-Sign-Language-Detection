package com.example.tubesrpll.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tubesrpll.R
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TranslateVideoToText : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var textViewResult: TextView

    private var myCameraCaptureSession: CameraCaptureSession? = null
    private lateinit var stringCameraID: String
    private lateinit var cameraManager: CameraManager
    private var myCameraDevice: CameraDevice? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private var isFrontCamera: Boolean = true

    private lateinit var tflite: Interpreter
    private lateinit var scheduler: ScheduledExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_translate_video_to_text)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PackageManager.PERMISSION_GRANTED
        )
        textureView = findViewById(R.id.textureView)
        textViewResult = findViewById(R.id.textViewResult)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        tflite = Interpreter(loadModelFile("assets/ASL_model.tflite"))

        startCamera()
    }

    private fun loadModelFile(modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onOpened(cameraDevice: CameraDevice) {
            myCameraDevice = cameraDevice
            startCameraSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            myCameraDevice?.close()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            myCameraDevice?.close()
            myCameraDevice = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startCameraSession() {
        val surfaceTexture: SurfaceTexture? = textureView.surfaceTexture
        val surface = Surface(surfaceTexture)
        try {
            captureRequestBuilder = myCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW) ?: return
            captureRequestBuilder.addTarget(surface)
            val outputConfiguration = OutputConfiguration(surface)
            val sessionConfiguration = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(outputConfiguration),
                mainExecutor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(@NonNull cameraCaptureSession: CameraCaptureSession) {
                        myCameraCaptureSession = cameraCaptureSession
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        try {
                            myCameraCaptureSession?.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                            startFrameProcessing() // Start processing frames
                        } catch (e: CameraAccessException) {
                            throw RuntimeException(e)
                        }
                    }

                    override fun onConfigureFailed(@NonNull cameraCaptureSession: CameraCaptureSession) {
                        myCameraCaptureSession = null
                    }
                }
            )
            myCameraDevice?.createCaptureSession(sessionConfiguration)
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }

    private fun startCamera() {
        try {
            stringCameraID = if (isFrontCamera) {
                cameraManager.cameraIdList.first { id ->
                    cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                }
            } else {
                cameraManager.cameraIdList.first { id ->
                    cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                }
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            cameraManager.openCamera(stringCameraID, stateCallback, null)
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }

    private fun startFrameProcessing() {
        scheduler = Executors.newScheduledThreadPool(1)
        scheduler.scheduleAtFixedRate({
            if (textureView.isAvailable) {
                processFrame()
            }
        }, 0, 100, TimeUnit.MILLISECONDS) // Adjust the interval as needed
    }

    private fun processFrame() {
        val bitmap = textureView.bitmap ?: return
        val inputBuffer = convertBitmapToByteBuffer(bitmap)

        val outputBuffer = ByteBuffer.allocateDirect(4 * 1) // Assuming output is a single float
        outputBuffer.order(ByteOrder.nativeOrder())

        // Run the model
        tflite.run(inputBuffer, outputBuffer)

        // Parse the output and update the TextView
        outputBuffer.rewind()
        val result = outputBuffer.float
        val resultText = "Recognized Sign: $result" // Modify this to fit your model's output
        runOnUiThread {
            textViewResult.text = resultText
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputImageWidth = 224 // Replace with your model's input width
        val inputImageHeight = 224 // Replace with your model's input height
        val inputBuffer = ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputImageWidth) {
            for (j in 0 until inputImageHeight) {
                val value = intValues[pixel++]

                inputBuffer.putFloat(((value shr 16 and 0xFF) - 128f) / 128f)
                inputBuffer.putFloat(((value shr 8 and 0xFF) - 128f) / 128f)
                inputBuffer.putFloat(((value and 0xFF) - 128f) / 128f)
            }
        }
        return inputBuffer
    }

    fun switchCamera(view: View) {
        isFrontCamera = !isFrontCamera
        myCameraCaptureSession?.close()
        myCameraDevice?.close()
        startCamera()
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
        scheduler.shutdown()
    }

    private fun stopCamera() {
        try {
            myCameraCaptureSession?.abortCaptures()
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)}
        }
}