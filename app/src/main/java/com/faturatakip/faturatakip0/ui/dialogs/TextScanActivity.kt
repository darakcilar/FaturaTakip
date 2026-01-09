package com.faturatakip.faturatakip0.ui.dialogs

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
import com.faturatakip.faturatakip0.databinding.ActivityTextScanBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.faturatakip.faturatakip0.BuildConfig

class TextScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextScanBinding
    private var imageCapture: ImageCapture? = null
    private val apiKey = BuildConfig.GEMINI_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera()
        binding.captureButton.setOnClickListener { takePhoto() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) { Log.e("Scan", "Hata", e) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        binding.progressBar.visibility = View.VISIBLE
        binding.captureButton.isEnabled = false
        imageCapture?.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image ?: return
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                    .addOnSuccessListener { visionText ->
                        imageProxy.close()
                        analyzeText(visionText.text)
                    }
                    .addOnFailureListener { imageProxy.close(); showError() }
            }
            override fun onError(exc: ImageCaptureException) { showError() }
        })
    }

    private fun analyzeText(rawText: String) {
        val systemPrompt = "Fatura uzmanısın. Ham metinden kurum adı, tutar, tarih(GG/AA/YYYY) ve kategoriyi ayıkla. SADECE JSON formatında yanıt ver: { \"name\": \"\", \"amount\": 0.0, \"date\": \"\", \"category\": \"\" }"
        val jsonPayload = JSONObject().apply {
            put("contents", org.json.JSONArray().put(JSONObject().apply {
                put("parts", org.json.JSONArray().put(JSONObject().apply { put("text", "Metin: $rawText") }))
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
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { showError() } }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
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
                } else { runOnUiThread { showError() } }
            }
        })
    }

    private fun showError() {
        binding.progressBar.visibility = View.GONE
        binding.captureButton.isEnabled = true
        Toast.makeText(this, "Hata oluştu.", Toast.LENGTH_SHORT).show()
    }
}