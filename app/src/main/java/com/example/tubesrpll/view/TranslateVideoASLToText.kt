package com.example.tubesrpll.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
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
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TranslateVideoASLToText : AppCompatActivity() {

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
    private lateinit var imageReader: ImageReader

    private lateinit var labels: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_translate_video_asl_to_text)
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

        // Load labels
        labels = loadLabels()
        Log.d("ModelLoading", "Labels successfully loaded")

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

    private fun loadLabels(): List<String> {
        val labels = mutableListOf<String>()
        try {
            assets.open("labels.txt").bufferedReader().useLines { lines ->
                lines.forEach { labels.add(it) }
            }
        } catch (e: IOException) {
            Log.e("LabelLoading", "Error loading label file", e)
        }
        return labels
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

        // Initialize ImageReader
        imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener(onImageAvailableListener, null)
        val imageReaderSurface = imageReader.surface

        try {
            captureRequestBuilder = myCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW) ?: return
            captureRequestBuilder.addTarget(surface)
            captureRequestBuilder.addTarget(imageReaderSurface)

            val outputConfigurations = listOf(
                OutputConfiguration(surface),
                OutputConfiguration(imageReaderSurface)
            )

            val sessionConfiguration = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigurations,
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
            // Frame processing is handled by ImageReader.OnImageAvailableListener
        }, 0, 100, TimeUnit.MILLISECONDS) // Adjust the interval as needed
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        Log.d("FrameProcessing", "Image captured")
        val inputBuffer = convertImageToByteBuffer(image)
        image.close()

        // Allocate output buffer according to the model output size
        val outputBuffer = ByteBuffer.allocateDirect(4 * 80 * 80) // Ukuran output sesuai dengan model
        outputBuffer.order(ByteOrder.nativeOrder())

        try {
            // Run the model
            tflite.run(inputBuffer, outputBuffer)
            Log.d("FrameProcessing", "Model run completed successfully")

            // Parse the output and update the TextView
            outputBuffer.rewind()
            val resultIndex = outputBuffer.asFloatBuffer().let { buffer ->
                var maxProb = -Float.MAX_VALUE
                var maxIndex = -1
                for (i in 0 until 80 * 80) { // Periksa 80x80 output
                    val prob = buffer.get(i)
                    if (prob > maxProb) {
                        maxProb = prob
                        maxIndex = i
                    }
                }
                maxIndex
            }
            val resultText = labels.getOrNull(resultIndex) ?: "Unknown"

            runOnUiThread {
                textViewResult.text = resultText
                Log.d("FrameProcessing", "Result updated: $resultText")
            }
        } catch (e: Exception) {
            Log.e("FrameProcessing", "Error running model", e)
        }
    }


    private fun convertImageToByteBuffer(image: Image): ByteBuffer {
        val inputImageWidth = 50 // Lebar input model yang benar
        val inputImageHeight = 50 // Tinggi input model yang benar
        val inputBuffer = ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val yuvBytes = ByteArray(ySize + uSize + vSize)
        yBuffer.get(yuvBytes, 0, ySize)
        uBuffer.get(yuvBytes, ySize, uSize)
        vBuffer.get(yuvBytes, ySize + uSize, vSize)

        var pixel = 0
        for (i in 0 until inputImageWidth) {
            for (j in 0 until inputImageHeight) {
                val y = yuvBytes[pixel].toInt() and 0xFF
                val u = yuvBytes[ySize + (pixel / 2)].toInt() and 0xFF
                val v = yuvBytes[ySize + uSize + (pixel / 2)].toInt() and 0xFF

                // Convert YUV to RGB
                val r = y + (1.370705 * (v - 128)).toInt()
                val g = y - (0.337633 * (u - 128)).toInt() - (0.698001 * (v - 128)).toInt()
                val b = y + (1.732446 * (u - 128)).toInt()

                inputBuffer.putFloat((r / 255f))
                inputBuffer.putFloat((g / 255f))
                inputBuffer.putFloat((b / 255f))

                pixel++
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