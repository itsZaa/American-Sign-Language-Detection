package com.example.tubesrpll

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.view.View
import android.view.TextureView
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import androidx.annotation.RequiresApi
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.view.Surface
import androidx.annotation.NonNull

class TranslateVideoToText : AppCompatActivity() {

    private lateinit var textureView: TextureView

    private var myCameraCaptureSession: CameraCaptureSession? = null
    private lateinit var stringCameraID: String
    private lateinit var cameraManager: CameraManager
    private var myCameraDevice: CameraDevice? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

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
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        startCamera()
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            myCameraDevice = cameraDevice
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            myCameraDevice?.close()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            myCameraDevice?.close()
            myCameraDevice = null
        }
    }

    private fun startCamera() {
        try {
            stringCameraID = cameraManager.cameraIdList[1]
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            cameraManager.openCamera(stringCameraID, stateCallback, null)
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun buttonStartCamera(view: View) {
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

    fun buttonStopCamera(view: View) {
        try {
            myCameraCaptureSession?.abortCaptures()
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }
}