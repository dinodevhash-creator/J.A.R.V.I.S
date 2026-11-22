package com.example.system

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val addressLabel: String,
    val city: String
)

object LocationManager {

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Result<LocationInfo> =
        withContext(Dispatchers.IO) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val cancellationSource = CancellationTokenSource()
                val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
                    cont.invokeOnCancellation { cancellationSource.cancel() }
                    client.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationSource.token
                    ).addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                } ?: return@withContext Result.failure(
                    IllegalStateException("Lokasi tidak dapat diperoleh saat ini. Pastikan GPS perangkat aktif.")
                )

                val (addressLabel, city) = reverseGeocode(context, location.latitude, location.longitude)
                Result.success(
                    LocationInfo(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        addressLabel = addressLabel,
                        city = city
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(context: Context, lat: Double, lng: Double): Pair<String, String> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val first = addresses?.firstOrNull()
            if (first != null) {
                val line = first.getAddressLine(0) ?: "${first.locality ?: ""}, ${first.countryName ?: ""}"
                val city = first.locality ?: first.subAdminArea ?: first.adminArea ?: "Lokasi Saat Ini"
                line.trim() to city.trim()
            } else {
                "%.4f, %.4f".format(lat, lng) to "Koordinat GPS"
            }
        } catch (_: Exception) {
            "%.4f, %.4f".format(lat, lng) to "Koordinat GPS"
        }
    }

    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String = "JARVIS_${System.currentTimeMillis()}.jpg"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/JarvisAI")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Gagal membuat berkas MediaStore.")

            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IllegalStateException("Gagal mengompres gambar.")
                }
            } ?: throw IllegalStateException("Gagal membuka stream keluaran galeri.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
