package com.example.tubesrpll.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tubesrpll.R

class TranslateVideoBISINDOToText : AppCompatActivity() {
    private lateinit var textureView: TextureView

    private var myCameraCaptureSession: CameraCaptureSession? = null
    private lateinit var stringCameraID: String
    private lateinit var cameraManager: CameraManager
    private var myCameraDevice: CameraDevice? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private var isFrontCamera: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_translate_video_bisindo_to_text)
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
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        startCamera()
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

    fun switchCamera(view: View) {
        isFrontCamera = !isFrontCamera
        myCameraCaptureSession?.close()
        myCameraDevice?.close()
        startCamera()
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    private fun stopCamera() {
        try {
            myCameraCaptureSession?.abortCaptures()
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }
}