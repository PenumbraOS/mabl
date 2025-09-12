package com.penumbraos.mabl.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.penumbraos.mabl.MainActivity
import com.penumbraos.mabl.R
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "CameraService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "camera_service_channel"

class CameraService : Service() {

    private val binder = CameraBinder()

    private val backgroundThread = HandlerThread("CameraServiceBackground").also { it.start() }
    private val backgroundHandler = Handler(backgroundThread.looper)

    private val wakeLock: PowerManager.WakeLock by lazy {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MABL::CameraServiceWakeLock"
        )
    }

    private val exposureMeteringKey = CaptureRequest.Key<Int>(
        "org.codeaurora.qcamera3.exposure_metering.exposure_metering_mode",
        Int::class.java
    )

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var primaryImageReader: ImageReader? = null
    private var previewReader: ImageReader? = null

    inner class CameraBinder : Binder() {
        fun getService(): CameraService = this@CameraService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()

        closeCamera()
        backgroundThread.quitSafely()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Camera Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Camera service for MABL vision interactions"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MABL Camera Service")
            .setContentText("Camera ready for vision interactions")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    suspend fun takePicture(): ByteArray? {
        wakeLock.acquire(10 * 1000L)
        try {
            openCamera()
            val imageData = captureImage()
            closeCamera()
            return imageData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take picture", e)
            closeCamera()
            return null
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager

        suspendCancellableCoroutine { continuation ->
            try {
                val cameraId = manager.cameraIdList[0]

                primaryImageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
                previewReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 1)

                manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        Log.d(TAG, "Camera opened successfully")
                        cameraDevice = camera
                        continuation.resume(Unit)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        Log.d(TAG, "Camera disconnected")
                        camera.close()
                        cameraDevice = null
                        continuation.resumeWithException(Exception("Camera disconnected"))
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e(TAG, "Camera error: $error")
                        camera.close()
                        cameraDevice = null
                        continuation.resumeWithException(Exception("Camera error: $error"))
                    }
                }, backgroundHandler)
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Camera access exception", e)
                continuation.resumeWithException(e)
            }

            continuation.invokeOnCancellation {
                closeCamera()
            }
        }

        // Create capture session
        createCaptureSession()
    }

    private suspend fun createCaptureSession() {
        suspendCancellableCoroutine { continuation ->
            try {
                cameraDevice?.createCaptureSession(
                    listOf(primaryImageReader!!.surface, previewReader!!.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            Log.d(TAG, "Capture session configured")
                            captureSession = session
                            continuation.resume(Unit)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            continuation.resumeWithException(Exception("Failed to configure capture session"))
                        }
                    },
                    backgroundHandler
                )
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to create capture session", e)
                continuation.resumeWithException(e)
            }
        }
    }

    private fun createCaptureBuilder(
        imageReader: ImageReader,
        requestTemplate: Int
    ): CaptureRequest.Builder {
        val captureBuilder =
            cameraDevice!!.createCaptureRequest(requestTemplate)
        captureBuilder.addTarget(imageReader.surface)

        captureBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_OFF
        )
        captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f)
        captureBuilder.set(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON
        )
        captureBuilder.set(
            CaptureRequest.CONTROL_AWB_MODE,
            CaptureRequest.CONTROL_AWB_MODE_AUTO
        )
        captureBuilder.set(
            CaptureRequest.NOISE_REDUCTION_MODE,
            CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
        )
        captureBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
        captureBuilder.set(exposureMeteringKey, 1)

        return captureBuilder
    }

    private suspend fun captureImage(): ByteArray {
        if (cameraDevice == null || captureSession == null) {
            throw Exception("Camera not ready")
        }

        triggerAEPrecapture()

        return suspendCancellableCoroutine { continuation ->
            primaryImageReader?.setOnImageAvailableListener(
                { reader ->
                    val image = reader.acquireLatestImage()
                    try {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        Log.d(TAG, "Image captured, size: ${bytes.size}")
                        continuation.resume(bytes)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    } finally {
                        image.close()
                    }
                },
                backgroundHandler
            )

            try {
                val captureBuilder =
                    createCaptureBuilder(primaryImageReader!!, CameraDevice.TEMPLATE_STILL_CAPTURE)

                val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        val aeMode = result.get(CaptureResult.CONTROL_AE_MODE)
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        val aeCompensation =
                            result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)
                        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
                        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)

                        Log.d(TAG, "Photo capture completed - AE mode: $aeMode, AE state: $aeState")
                        Log.d(
                            TAG,
                            "AE compensation: $aeCompensation, ISO: $iso, exposure time: $exposureTime ns"
                        )
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        continuation.resumeWithException(Exception("Photo capture failed: ${failure.reason}"))
                    }
                }

                captureSession!!.capture(captureBuilder.build(), captureCallback, backgroundHandler)
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to take picture", e)
                continuation.resumeWithException(e)
            }
        }
    }

    private suspend fun triggerAEPrecapture() {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                val precaptureBuilder =
                    createCaptureBuilder(previewReader!!, CameraDevice.TEMPLATE_RECORD)

                var hasResumed = false
                val precaptureCallback = object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)

                        when (aeState) {
                            CaptureResult.CONTROL_AE_STATE_CONVERGED -> {
                                if (!hasResumed) {
                                    hasResumed = true
                                    Log.d(TAG, "AE converged")
                                    captureSession?.stopRepeating()
                                    continuation.resume(Unit)
                                }
                            }
                        }
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        if (!hasResumed) {
                            hasResumed = true
                            Log.w(TAG, "AE precapture failed, proceeding anyway")
                            captureSession?.stopRepeating()
                            continuation.resume(Unit)
                        }
                    }
                }

                captureSession!!.setRepeatingRequest(
                    precaptureBuilder.build(),
                    precaptureCallback,
                    backgroundHandler
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to trigger AE precapture", e)
                continuation.resumeWithException(e)
            }
        }
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null

        cameraDevice?.close()
        cameraDevice = null

        primaryImageReader?.close()
        primaryImageReader = null

        previewReader?.close()
        previewReader = null
    }
}