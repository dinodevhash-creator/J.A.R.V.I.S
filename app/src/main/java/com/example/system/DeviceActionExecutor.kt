package com.example.system

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.example.ai.ActionType
import org.json.JSONObject

data class DeviceTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val networkStatus: String,
    val deviceLabel: String
)

object DeviceActionExecutor {

    fun readTelemetry(context: Context): DeviceTelemetry {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = batteryManager?.isCharging ?: false

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)
        val networkStatus = when {
            capabilities == null -> "OFFLINE"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "SELULER"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "ONLINE"
        }

        val deviceLabel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
        return DeviceTelemetry(
            batteryPercent = batteryLevel.coerceIn(0, 100),
            isCharging = isCharging,
            networkStatus = networkStatus,
            deviceLabel = deviceLabel
        )
    }

    fun executeAction(
        context: Context,
        actionTypeName: String,
        payloadJson: String,
        locationInfo: LocationInfo? = null
    ): Result<String> {
        val type = runCatching { ActionType.valueOf(actionTypeName) }.getOrDefault(ActionType.NONE)
        val json = runCatching { JSONObject(payloadJson) }.getOrElse { JSONObject() }

        return when (type) {
            ActionType.SET_ALARM -> {
                val hour = json.optInt("hour", 7).coerceIn(0, 23)
                val minute = json.optInt("minute", 0).coerceIn(0, 59)
                val label = json.optString("query").ifBlank { "J.A.R.V.I.S. Alarm" }
                launchAlarm(context, hour, minute, label)
            }
            ActionType.SET_TIMER -> {
                val seconds = json.optInt("durationSeconds", 300).coerceAtLeast(5)
                val label = json.optString("query").ifBlank { "J.A.R.V.I.S. Timer" }
                launchTimer(context, seconds, label)
            }
            ActionType.WEB_SEARCH -> {
                val query = json.optString("query").ifBlank { "J.A.R.V.I.S." }
                launchWebSearch(context, query)
            }
            ActionType.OPEN_CALENDAR -> {
                val title = json.optString("query").ifBlank { "Agenda J.A.R.V.I.S." }
                launchCalendarInsert(context, title)
            }
            ActionType.OPEN_MAPS -> {
                val query = json.optString("query").ifBlank { "Tempat terdekat" }
                launchMapsSearch(context, query, locationInfo)
            }
            ActionType.ADD_DIRECTIVE,
            ActionType.NONE -> Result.success("Protokol dicatat.")
        }
    }

    fun launchMapsSearch(
        context: Context,
        query: String,
        locationInfo: LocationInfo? = null
    ): Result<String> {
        return try {
            val encodedQuery = Uri.encode(query)
            val geoUri = if (locationInfo != null) {
                Uri.parse("geo:${locationInfo.latitude},${locationInfo.longitude}?q=$encodedQuery")
            } else {
                Uri.parse("geo:0,0?q=$encodedQuery")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = if (locationInfo != null) {
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery&center=${locationInfo.latitude},${locationInfo.longitude}")
                } else {
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
            Result.success("Membuka peta untuk \"$query\".")
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Tidak ada aplikasi peta atau peramban yang tersedia."))
        }
    }

    fun launchAlarm(context: Context, hour: Int, minute: Int, message: String): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Membuka aplikasi Jam untuk alarm %02d:%02d.".format(hour, minute))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Tidak ada aplikasi Jam yang mendukung penyetelan alarm."))
        }
    }

    fun launchTimer(context: Context, seconds: Int, message: String): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Membuka timer selama $seconds detik.")
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Tidak ada aplikasi Jam yang mendukung timer."))
        }
    }

    fun launchWebSearch(context: Context, query: String): Result<String> {
        return try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (searchIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(searchIntent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
            Result.success("Membuka pencarian web untuk \"$query\".")
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Tidak dapat membuka peramban web."))
        }
    }

    fun launchCalendarInsert(context: Context, title: String): Result<String> {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Membuka Kalender untuk menjadwalkan \"$title\".")
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Aplikasi Kalender tidak ditemukan."))
        }
    }
}
