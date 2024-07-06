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
import android.util.Log
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

        Log.d("Lifecycle", "onCreate called")

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PackageManager.PERMISSION_GRANTED
            )
        } else {
            initializeComponents()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeComponents()
        } else {
            Log.e("Permissions", "Camera permission not granted")
        }
    }

    private fun initializeComponents() {
        Log.d("Lifecycle", "Initializing components")
        textureView = findViewById(R.id.textureView)
        textViewResult = findViewById(R.id.textViewResult)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Load your TFLite model
        tflite = Interpreter(loadModelFile("ASL_model.tflite"))
        Log.d("ModelLoading", "Model successfully loaded")

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
            Log.d("CameraState", "Camera opened")
            myCameraDevice = cameraDevice
            startCameraSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            Log.d("CameraState", "Camera disconnected")
            myCameraDevice?.close()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            Log.e("CameraState", "Error opening camera: $error")
            myCameraDevice?.close()
            myCameraDevice = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startCameraSession() {
        Log.d("CameraSession", "Starting camera session")
        val surfaceTexture: SurfaceTexture? = textureView.surfaceTexture
        if (surfaceTexture == null) {
            Log.e("CameraSession", "SurfaceTexture is null!")
            return
        }
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
                        Log.d("CameraSession", "Camera session configured")
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
                        Log.e("CameraSession", "Camera session configuration failed")
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
            Log.d("Camera", "Starting camera")
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
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PackageManager.PERMISSION_GRANTED
                )
                return
            }
            cameraManager.openCamera(stringCameraID, stateCallback, null)
        } catch (e: CameraAccessException) {
            Log.e("Camera", "Camera access exception", e)
            throw RuntimeException(e)
        }
    }

    private fun startFrameProcessing() {
        Log.d("FrameProcessing", "Starting frame processing")
        scheduler = Executors.newScheduledThreadPool(1)
        scheduler.scheduleAtFixedRate({
            if (textureView.isAvailable) {
                processFrame()
            }
        }, 0, 100, TimeUnit.MILLISECONDS) // Adjust the interval as needed
    }

    private fun processFrame() {
        Log.d("FrameProcessing", "Processing frame")
        val bitmap = textureView.bitmap ?: return
        Log.d("FrameProcessing", "Bitmap captured")
        val inputBuffer = convertBitmapToByteBuffer(bitmap)

        val outputBuffer = ByteBuffer.allocateDirect(4) // Assuming output is a single float
        outputBuffer.order(ByteOrder.nativeOrder())

        try {
            // Run the model
            tflite.run(inputBuffer, outputBuffer)
            Log.d("FrameProcessing", "Model run completed successfully")

            // Parse the output and update the TextView
            outputBuffer.rewind()
            val result = outputBuffer.float
            val resultText = "$result" // Modify this to fit your model's output format

            runOnUiThread {
                textViewResult.text = resultText
                Log.d("FrameProcessing", "Result updated: $resultText")
            }
        } catch (e: Exception) {
            Log.e("FrameProcessing", "Error running model", e)
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputImageWidth = 50 // Replace with your model's input width
        val inputImageHeight = 50 // Replace with your model's input height
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
        if (::scheduler.isInitialized) {
            scheduler.shutdown()
        }
    }

    private fun stopCamera() {
        try {
            myCameraCaptureSession?.abortCaptures()
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }
}
