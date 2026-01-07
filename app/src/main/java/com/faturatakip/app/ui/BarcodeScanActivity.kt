package com.faturatakip.app.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.faturatakip.app.databinding.ActivityBarcodeScanBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScanActivity : AppCompatActivity() {

    // DÜZELTME: Doğru binding sınıfı adı kullanıldı
    private lateinit var binding: ActivityBarcodeScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // DÜZELTME: Doğru binding sınıfı adı kullanıldı
        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                        if (!isProcessing) {
                            isProcessing = true
                            parseBarcodeAndFinish(barcode)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("BarcodeScanActivity", "Kamera başlatılamadı.", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun parseBarcodeAndFinish(barcode: Barcode) {
        val rawValue = barcode.rawValue ?: ""
        Log.d("BarcodeScanActivity", "Barkod okundu: $rawValue")

        var amount: Double? = null
        var date: String? = null

        try {
            rawValue.split('&').forEach { pair ->
                val parts = pair.split('=')
                if (parts.size == 2) {
                    when (parts[0].uppercase(java.util.Locale.ROOT)) {
                        "TUTAR" -> amount = parts[1].replace(',', '.').toDoubleOrNull()
                        "TARİH", "SKT" -> date = parts[1]
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BarcodeScanActivity", "Barkod verisi ayrıştırılamadı.", e)
        }

        if (amount == null && date == null) {
            runOnUiThread {
                Toast.makeText(this, "Desteklenmeyen Fatura Barkodu", Toast.LENGTH_SHORT).show()
                isProcessing = false
            }
            return
        }

        val resultIntent = Intent().apply {
            putExtra("SCAN_AMOUNT", amount)
            putExtra("SCAN_DATE", date)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private class BarcodeAnalyzer(private val onBarcodeFound: (Barcode) -> Unit) : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC, Barcode.FORMAT_CODE_128)
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.let {
                            onBarcodeFound(it)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("BarcodeAnalyzer", "Barkod okuma başarısız oldu.", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }
    }
}

