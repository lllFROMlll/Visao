package com.robotarm.visiontracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Surface
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var statusText: TextView
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        statusText = findViewById(R.id.statusText)

        val lastCrash = CrashHandler.readLastCrash(applicationContext)
        if (lastCrash != null) {
            statusText.text = "ERRO DA ÚLTIMA VEZ:\n${lastCrash.take(500)}"
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            previewView.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            previewView.post { startCamera() }
        } else {
            statusText.text = "Permissão de câmera negada."
        }
    }

    private fun startCamera() {
        val safeRotation = previewView.display?.rotation ?: Surface.ROTATION_0

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setTargetRotation(safeRotation)
                    .build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val analysis = ImageAnalysis.Builder()
                    .setTargetRotation(safeRotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var frameCount = 0
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    frameCount++
                    runOnUiThread {
                        statusText.text = "Câmera ativa - frame #$frameCount " +
                            "(${imageProxy.width}x${imageProxy.height})"
                    }
                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
            } catch (exc: Exception) {
                statusText.text = "Erro ao iniciar câmera: ${exc.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }
}
