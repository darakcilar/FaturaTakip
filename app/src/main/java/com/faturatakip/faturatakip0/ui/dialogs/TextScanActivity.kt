package com.faturatakip.faturatakip0.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.faturatakip.faturatakip0.BuildConfig
import com.faturatakip.faturatakip0.databinding.ActivityTextScanBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.nio.ByteBuffer

/**
 * TextScanActivity: Rotasyon sorunu giderilmiş, dondurma özellikli
 * ve iki aşamalı (Çek -> Onayla -> Analiz) akıllı fatura tarayıcı.
 */
class TextScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextScanBinding
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private var capturedRawText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera()
        setupListeners()
    }

    private fun setupListeners() {
        // 1. AŞAMA: Fotoğraf Çek
        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        // 2. AŞAMA: Yapay Zeka Analizi
        binding.analyzeButton.setOnClickListener {
            if (capturedRawText.isNotEmpty()) {
                analyzeTextWithGemini(capturedRawText)
            } else {
                Toast.makeText(this, "Metin bulunamadı.", Toast.LENGTH_SHORT).show()
                resetToCaptureMode()
            }
        }

        // Yeniden Çekme
        binding.retakeButton.setOnClickListener {
            resetToCaptureMode()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("TextScan", "Kamera başlatılamadı", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        binding.progressBar.visibility = View.VISIBLE
        binding.captureButton.isEnabled = false

        imageCapture?.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                // Rotasyon bilgisini alıyoruz
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                // Rotasyon uygulanmış Bitmap'i oluşturuyoruz
                val bitmap = imageProxyToBitmap(imageProxy, rotationDegrees)

                val mediaImage = imageProxy.image ?: return
                val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                    .addOnSuccessListener { visionText ->
                        imageProxy.close()
                        capturedRawText = visionText.text

                        runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            if (capturedRawText.isBlank()) {
                                Toast.makeText(this@TextScanActivity, "Net metin algılanamadı.", Toast.LENGTH_SHORT).show()
                                resetToCaptureMode()
                            } else {
                                // Görüntüyü dondur (Doğru açıyla)
                                freezeScreen(bitmap)
                            }
                        }
                    }
                    .addOnFailureListener {
                        imageProxy.close()
                        runOnUiThread { showError("Okuma hatası.") }
                    }
            }
            override fun onError(exc: ImageCaptureException) {
                runOnUiThread { showError("Kamera hatası.") }
            }
        })
    }

    /**
     * Geliştirilmiş yardımcı fonksiyon: ImageProxy'yi alır, Matrix kullanarak
     * verilen açı kadar döndürür ve düzgün Bitmap döndürür.
     */
    private fun imageProxyToBitmap(image: ImageProxy, degrees: Int): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Matrix ile döndürme işlemi
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())

        return Bitmap.createBitmap(
            originalBitmap, 0, 0,
            originalBitmap.width, originalBitmap.height,
            matrix, true
        )
    }

    private fun freezeScreen(bitmap: Bitmap) {
        cameraProvider?.unbindAll()
        binding.capturedImageView.setImageBitmap(bitmap)
        binding.capturedImageView.visibility = View.VISIBLE
        binding.cameraPreview.visibility = View.INVISIBLE

        binding.captureButton.visibility = View.GONE
        binding.analysisControls.visibility = View.VISIBLE
        binding.analyzeButton.visibility = View.VISIBLE
        binding.retakeButton.visibility = View.VISIBLE
    }

    private fun resetToCaptureMode() {
        capturedRawText = ""
        binding.capturedImageView.visibility = View.GONE
        binding.cameraPreview.visibility = View.VISIBLE
        startCamera()
        binding.analysisControls.visibility = View.GONE
        binding.captureButton.visibility = View.VISIBLE
        binding.captureButton.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    private fun analyzeTextWithGemini(rawText: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.analyzeButton.isEnabled = false
        binding.retakeButton.isEnabled = false

        val systemPrompt = "Fatura uzmanısın. Metinden kurum adı, tutar, tarih(GG/AA/YYYY) ve kategoriyi ayıkla. SADECE JSON ver: { \"name\": \"\", \"amount\": 0.0, \"date\": \"\", \"category\": \"\" }"

        val jsonPayload = JSONObject().apply {
            put("contents", org.json.JSONArray().put(JSONObject().apply {
                put("parts", org.json.JSONArray().put(JSONObject().apply { put("text", "Metin:\n$rawText") }))
            }))
            put("systemInstruction", JSONObject().apply {
                put("parts", org.json.JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
            })
            put("generationConfig", JSONObject().apply { put("responseMimeType", "application/json") })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=$apiKey")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { showError("AI Bağlantı hatası.") } }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val text = JSONObject(body).getJSONArray("candidates").getJSONObject(0)
                            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                        val data = JSONObject(text)
                        runOnUiThread {
                            val result = Intent().apply {
                                putExtra("SCAN_NAME", data.optString("name"))
                                putExtra("SCAN_AMOUNT", data.optDouble("amount"))
                                putExtra("SCAN_DATE", data.optString("date"))
                                putExtra("SCAN_CATEGORY", data.optString("category"))
                            }
                            setResult(Activity.RESULT_OK, result)
                            finish()
                        }
                    } catch (e: Exception) { runOnUiThread { showError("Analiz hatası.") } }
                } else { runOnUiThread { showError("AI yanıt vermedi.") } }
            }
        })
    }

    private fun showError(msg: String) {
        binding.progressBar.visibility = View.GONE
        binding.captureButton.isEnabled = true
        binding.captureButton.visibility = View.VISIBLE
        binding.analysisControls.visibility = View.GONE
        if (binding.capturedImageView.visibility == View.VISIBLE) resetToCaptureMode()
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}