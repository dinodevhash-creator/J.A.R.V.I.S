package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "JarvisImageRepo"

enum class StudioAspectRatio(val label: String) {
    SQUARE("1:1"),
    LANDSCAPE("16:9"),
    PORTRAIT("9:16")
}

class JarvisImageRepository {

    private fun buildModel(aspectRatio: StudioAspectRatio) = Firebase.ai.generativeModel(
        modelName = "gemini-nano-banana-2.1",
        generationConfig = when (aspectRatio) {
            StudioAspectRatio.SQUARE -> generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
            StudioAspectRatio.LANDSCAPE -> generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
            StudioAspectRatio.PORTRAIT -> generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.PORTRAIT_9x16)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        }
    )

    suspend fun generateImage(
        prompt: String,
        aspectRatio: StudioAspectRatio = StudioAspectRatio.SQUARE
    ): Bitmap {
        try {
            val model = buildModel(aspectRatio)
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            return imagePart?.image
                ?: throw IllegalStateException("Model tidak mengembalikan gambar.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("Pembuatan gambar melebihi batas waktu 60 detik.")
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            throw IllegalStateException("Kuota API gambar terlampaui. Pastikan proyek Firebase menggunakan paket Blaze atau coba lagi nanti.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw e
        }
    }

    suspend fun editImage(
        inputBitmap: Bitmap,
        editInstruction: String,
        aspectRatio: StudioAspectRatio = StudioAspectRatio.SQUARE
    ): Bitmap {
        try {
            val model = buildModel(aspectRatio)
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(inputBitmap)
                        text(editInstruction)
                    }
                )
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            return imagePart?.image
                ?: throw IllegalStateException("Model tidak mengembalikan gambar hasil suntingan.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image edit timed out", e)
            throw IllegalStateException("Penyuntingan gambar melebihi batas waktu 60 detik.")
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image edit quota exceeded", e)
            throw IllegalStateException("Kuota API gambar terlampaui. Pastikan proyek Firebase menggunakan paket Blaze atau coba lagi nanti.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image edit failed", e)
            throw e
        }
    }
}
