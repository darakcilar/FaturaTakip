package com.faturatakip.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.faturatakip.app.databinding.ActivityTextScanBinding

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TextScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()

        binding.captureButton.setOnClickListener { takePhoto() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("TextScanActivity", "Kamera başlatılamadı.", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        binding.progressBar.visibility = View.VISIBLE
        binding.captureButton.isEnabled = false

        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                imageProxy.close()
                                parseTextAndFinish(visionText.text)
                            }
                            .addOnFailureListener { e ->
                                imageProxy.close()
                                Log.e("TextScanActivity", "Metin tanıma başarısız oldu", e)
                                Toast.makeText(baseContext, "Metin okunamadı.", Toast.LENGTH_SHORT).show()
                                binding.progressBar.visibility = View.GONE
                                binding.captureButton.isEnabled = true
                            }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("TextScanActivity", "Fotoğraf çekilemedi: ${exception.message}", exception)
                    binding.progressBar.visibility = View.GONE
                    binding.captureButton.isEnabled = true
                }
            }
        )
    }

    private fun parseTextAndFinish(text: String) {
        var amount: Double? = null
        var date: String? = null

        val amountRegex = """(TOPLAM|TUTAR|Total|Amount)\s*[:\s]*(\d+[,.]\d{2})""".toRegex(RegexOption.IGNORE_CASE)
        amountRegex.findAll(text).lastOrNull()?.let {
            val amountString = it.groupValues[2].replace(',', '.')
            amount = amountString.toDoubleOrNull()
        }

        val dateRegex = """(\d{2}[./-]\d{2}[./-]\d{4})""".toRegex()
        dateRegex.find(text)?.let {
            date = it.value.replace('.', '/').replace('-', '/')
        }

        val resultIntent = Intent()
        resultIntent.putExtra("SCAN_AMOUNT", amount)
        resultIntent.putExtra("SCAN_DATE", date)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
