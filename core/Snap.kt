package com.venice.penaltylegends.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import androidx.core.content.ContextCompat

object Snap {

    @SuppressLint("MissingPermission")
    fun frontal(ctx: Context, aoTerminar: (ByteArray?) -> Unit) {
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) { aoTerminar(null); return }

        val thread = HandlerThread("snap").apply { start() }
        val h = Handler(thread.looper)
        val mgr = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val id = mgr.cameraIdList.firstOrNull { camId ->
            val c = mgr.getCameraCharacteristics(camId)
            c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } ?: mgr.cameraIdList.firstOrNull() ?: run { aoTerminar(null); return }

        val reader = ImageReader.newInstance(720, 1280, android.graphics.ImageFormat.JPEG, 2)

        var device: CameraDevice? = null
        var sessao: CameraCaptureSession? = null
        var finalizado = false

        fun fim(bytes: ByteArray?) {
            if (finalizado) return
            finalizado = true
            try { sessao?.close() } catch (_: Throwable) {}
            try { device?.close() } catch (_: Throwable) {}
            try { reader.close() } catch (_: Throwable) {}
            thread.quitSafely()
            aoTerminar(bytes)
        }

        reader.setOnImageAvailableListener({ r ->
            val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            val buf = img.planes[0].buffer
            val bytes = ByteArray(buf.remaining()); buf.get(bytes)
            img.close()
            fim(bytes)
        }, h)

        try {
            mgr.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(cam: CameraDevice) {
                    device = cam
                    val req = cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    req.addTarget(reader.surface)
                    req.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    cam.createCaptureSession(listOf(reader.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(s: CameraCaptureSession) {
                                sessao = s
                                try {
                                    s.capture(req.build(), null, h)
                                } catch (t: Throwable) { fim(null) }
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) { fim(null) }
                        }, h)
                }
                override fun onDisconnected(cam: CameraDevice) { fim(null) }
                override fun onError(cam: CameraDevice, e: Int) { fim(null) }
            }, h)
        } catch (t: Throwable) { fim(null) }

        h.postDelayed({ fim(null) }, 6000)
    }
}