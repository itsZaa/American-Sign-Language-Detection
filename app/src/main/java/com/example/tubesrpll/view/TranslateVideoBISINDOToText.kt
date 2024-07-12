package com.example.tubesrpll.view

import android.Manifest
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
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
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.tubesrpll.R
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TranslateVideoBISINDOToText : AppCompatActivity() {
    // Deklarasi variabel
    private lateinit var textureView: TextureView
    private lateinit var imageViewResult: ImageView

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

    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    // Metode onCreate dijalankan saat aktivitas dibuat
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_translate_video_bisindo_to_text)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Memeriksa izin kamera
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

    // Memproses hasil permintaan izin
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeComponents()
        } else {
            Log.e("Permissions", "Camera permission not granted")
        }
    }

    // Inisialisasi komponen
    private fun initializeComponents() {
        textureView = findViewById(R.id.textureView)
        imageViewResult = findViewById(R.id.imageViewResult)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Memuat model TFLite
        tflite = Interpreter(loadModelFile("ASL_model.tflite"))
        Log.d("ModelLoading", "Model successfully loaded")

        // Memuat label
        labels = loadLabels()
        Log.d("ModelLoading", "Labels successfully loaded")

        // Inisialisasi Firebase Storage
        firebaseStorage = FirebaseStorage.getInstance()

        startCamera()
    }

    // Memuat file model
    private fun loadModelFile(modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Memuat label dari file
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

    // Callback untuk status kamera
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
        if (surfaceTexture == null) {
            return
        }
        val surface = Surface(surfaceTexture)

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
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        myCameraCaptureSession = cameraCaptureSession
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        try {
                            myCameraCaptureSession?.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                            startFrameProcessing()
                        } catch (e: CameraAccessException) {
                            throw RuntimeException(e)
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        myCameraCaptureSession = null
                    }
                }
            )
            myCameraDevice?.createCaptureSession(sessionConfiguration)
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }

    // Memulai kamera
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
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PackageManager.PERMISSION_GRANTED
                )
                return
            }
            cameraManager.openCamera(stringCameraID, stateCallback, null)
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }

    // Memulai pemrosesan frame
    private fun startFrameProcessing() {
        scheduler = Executors.newScheduledThreadPool(1)
        scheduler.scheduleAtFixedRate({
            // Pemrosesan frame ditangani oleh ImageReader.OnImageAvailableListener
        }, 0, 100, TimeUnit.MILLISECONDS)
    }

    // Listener untuk gambar yang tersedia
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        val inputBuffer = convertImageToByteBuffer(image)
        image.close()

        val outputBuffer = ByteBuffer.allocateDirect(4 * 80 * 80)
        outputBuffer.order(ByteOrder.nativeOrder())

        try {
            tflite.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val resultIndex = outputBuffer.asFloatBuffer().let { buffer ->
                var maxProb = -Float.MAX_VALUE
                var maxIndex = -1
                for (i in 0 until 80 * 80) {
                    val prob = buffer.get(i)
                    if (prob > maxProb) {
                        maxProb = prob
                        maxIndex = i
                    }
                }
                maxIndex
            }
            val resultText = labels.getOrNull(resultIndex) ?: "Unknown"
            loadImageFromFirebase(resultText)
        } catch (e: Exception) {
            Log.e("FrameProcessing", "Error running model", e)
        }
    }

    // Konversi gambar menjadi ByteBuffer
    private fun convertImageToByteBuffer(image: Image): ByteBuffer {
        val inputImageWidth = 50
        val inputImageHeight = 50
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

    // Memuat gambar dari Firebase berdasarkan label
    private fun loadImageFromFirebase(label: String) {
        val imageRef = firebaseStorage.getReference("BISINDO image/${label.lowercase()}.png")

        imageRef.downloadUrl.addOnSuccessListener { uri ->
            if (!isDestroyed && !isFinishing) {
                Glide.with(this)
                    .load(uri)
                    .override(400, 400)
                    .into(imageViewResult)
                imageViewResult.visibility = View.VISIBLE
            }
        }.addOnFailureListener { e ->
            Log.e("FirebaseStorage", "Error downloading image: $e")
            e.printStackTrace()
        }
    }

    // Fungsi untuk mengganti kamera
    fun switchCamera(view: View) {
        isFrontCamera = !isFrontCamera
        myCameraCaptureSession?.close()
        myCameraDevice?.close()
        startCamera()
    }

    // Metode onPause untuk menghentikan kamera saat aktivitas dijeda
    override fun onPause() {
        super.onPause()
        stopCamera() // Hentikan pratinjau kamera dan lepaskan sumber daya
        // Hentikan operasi Glide jika imageViewResult diinisialisasi
        if (::imageViewResult.isInitialized) {
            Glide.with(this).clear(imageViewResult)
        }
    }

    // Metode onDestroy untuk melepaskan sumber daya saat aktivitas dihancurkan
    override fun onDestroy() {
        super.onDestroy()
        // Lepaskan sumber daya yang tersisa, seperti perangkat kamera
        closeCamera()
    }

    // Menghentikan kamera
    private fun stopCamera() {
        try {
            myCameraCaptureSession?.abortCaptures()
            myCameraCaptureSession?.close()
            myCameraCaptureSession = null
            myCameraDevice?.close()
            myCameraDevice = null
        } catch (e: CameraAccessException) {
            Log.e(ContentValues.TAG, "Error stopping camera", e)
        }
    }

    // Menutup kamera
    private fun closeCamera() {
        stopCamera()
        imageReader.close()
        // Tugas pembersihan lainnya terkait kamera
    }
}
