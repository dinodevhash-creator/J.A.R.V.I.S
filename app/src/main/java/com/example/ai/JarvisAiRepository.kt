package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.cloud.CloudDirective
import com.example.data.cloud.CloudMessage
import com.example.data.preferences.JarvisPersona
import com.example.system.DeviceTelemetry
import com.example.system.LocationInfo
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "JarvisAiRepo"

enum class ActionType {
    NONE,
    SET_ALARM,
    SET_TIMER,
    ADD_DIRECTIVE,
    WEB_SEARCH,
    OPEN_CALENDAR,
    OPEN_MAPS
}

data class JarvisActionCommand(
    val type: ActionType,
    val label: String,
    val payloadJson: String
)

interface JarvisAiRepository {
    fun streamResponse(
        prompt: String,
        history: List<CloudMessage>,
        attachedImage: Bitmap?,
        persona: JarvisPersona,
        languageCode: String,
        locationInfo: LocationInfo?
    ): Flow<String>

    suspend fun detectActionCommand(prompt: String): JarvisActionCommand?

    suspend fun generateDailyBriefing(
        directives: List<CloudDirective>,
        locationInfo: LocationInfo?,
        telemetry: DeviceTelemetry,
        persona: JarvisPersona,
        languageCode: String
    ): String
}

class FirebaseJarvisAiRepository : JarvisAiRepository {

    private val actionSchema = Schema.obj(
        properties = mapOf(
            "actionType" to Schema.enumeration(
                values = listOf(
                    "NONE",
                    "SET_ALARM",
                    "SET_TIMER",
                    "ADD_DIRECTIVE",
                    "WEB_SEARCH",
                    "OPEN_CALENDAR",
                    "OPEN_MAPS"
                ),
                description = "Detected device, map, or protocol action requested by the user"
            ),
            "label" to Schema.string(description = "Short button label in user language, e.g. Buka di Peta or Setel Alarm 06:00"),
            "query" to Schema.string(description = "Search query, place query, directive title, or calendar event title"),
            "hour" to Schema.integer(description = "Alarm hour 0-23 if SET_ALARM"),
            "minute" to Schema.integer(description = "Alarm minute 0-59 if SET_ALARM"),
            "durationSeconds" to Schema.integer(description = "Timer duration in seconds if SET_TIMER")
        ),
        optionalProperties = listOf("label", "query", "hour", "minute", "durationSeconds")
    )

    private val actionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = actionSchema
                temperature = 0.1f
            },
            systemInstruction = content {
                text(
                    "Classify if the user prompt explicitly asks to set an alarm (SET_ALARM), " +
                        "set a countdown timer (SET_TIMER), save a task/protocol/reminder/directive (ADD_DIRECTIVE), " +
                        "search the web (WEB_SEARCH), add a calendar event (OPEN_CALENDAR), " +
                        "or find places/directions/restaurants/navigation nearby on maps (OPEN_MAPS). " +
                        "For general questions, conversation, analysis, or coding help, return actionType=NONE."
                )
            }
        )
    }

    private fun buildSystemPrompt(
        persona: JarvisPersona,
        languageCode: String,
        locationInfo: LocationInfo?
    ): String {
        val langInstruction = if (languageCode.startsWith("id")) {
            "Utamakan Bahasa Indonesia yang elegan, natural, dan presisi (kecuali pengguna berbicara dalam bahasa lain)."
        } else {
            "Prioritize articulate, refined English (unless the user writes in another language)."
        }
        val locationContext = if (locationInfo != null) {
            "Current user GPS location: ${locationInfo.addressLabel} (City: ${locationInfo.city}, Lat: ${locationInfo.latitude}, Lng: ${locationInfo.longitude}). Use this context when the user asks about nearby places, routes, or local info."
        } else {
            "User location is not currently locked; if they ask for nearby places, provide general guidance and mention the map search button."
        }
        return """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), an advanced tactical AI assistant running on the user's Android command HUD.
            Persona style: ${persona.systemTag}
            Language preference: $langInstruction
            $locationContext
            Guidelines:
            - Be sharp, helpful, proactive, and structured. Address the user politely ("Sir" or "Komandan" occasionally when fitting).
            - Keep answers clean and easy to read on mobile screens; use short paragraphs or concise bullet points when helpful.
            - If the user attaches an image, perform a thorough visual tactical scan and answer their question directly.
            - If the user asks to set an alarm, timer, calendar event, web search, map navigation, or create a directive/task, acknowledge the command briskly and confirm the action card is ready.
        """.trimIndent()
    }

    override fun streamResponse(
        prompt: String,
        history: List<CloudMessage>,
        attachedImage: Bitmap?,
        persona: JarvisPersona,
        languageCode: String,
        locationInfo: LocationInfo?
    ): Flow<String> = flow {
        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(buildSystemPrompt(persona, languageCode, locationInfo))
            }
        )

        val recentHistory = history
            .filter { !it.errorFlag && it.text.isNotBlank() }
            .takeLast(12)
            .map { msg ->
                content(if (msg.role == "user") "user" else "model") {
                    text(msg.text)
                }
            }

        val chat = model.startChat(history = recentHistory)
        val userContent = content("user") {
            if (attachedImage != null) {
                image(attachedImage)
            }
            text(prompt.ifBlank { "Analisis gambar ini secara taktis dan jelaskan detail pentingnya." })
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(userContent).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("J.A.R.V.I.S. menerima respons kosong dari model AI.")
        }
    }

    override suspend fun detectActionCommand(prompt: String): JarvisActionCommand? {
        if (prompt.isBlank()) return null
        return try {
            val response = withTimeout(15_000L) {
                actionModel.generateContent(prompt)
            }
            val rawJson = response.text ?: return null
            val json = JSONObject(rawJson)
            val actionName = json.optString("actionType", "NONE")
            val type = runCatching { ActionType.valueOf(actionName) }.getOrDefault(ActionType.NONE)
            if (type == ActionType.NONE) return null

            val label = json.optString("label").ifBlank {
                when (type) {
                    ActionType.SET_ALARM -> "Pasang Alarm"
                    ActionType.SET_TIMER -> "Mulai Timer"
                    ActionType.ADD_DIRECTIVE -> "Simpan Protokol"
                    ActionType.WEB_SEARCH -> "Cari di Web"
                    ActionType.OPEN_CALENDAR -> "Buka Kalender"
                    ActionType.OPEN_MAPS -> "Buka di Google Maps"
                    ActionType.NONE -> ""
                }
            }
            JarvisActionCommand(
                type = type,
                label = label,
                payloadJson = json.toString()
            )
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Action classification timed out", e)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Action classification failed", e)
            null
        }
    }

    override suspend fun generateDailyBriefing(
        directives: List<CloudDirective>,
        locationInfo: LocationInfo?,
        telemetry: DeviceTelemetry,
        persona: JarvisPersona,
        languageCode: String
    ): String {
        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(buildSystemPrompt(persona, languageCode, locationInfo))
            }
        )
        val nowFormatted = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
        val activeDirectives = directives.filter { !it.completed }
        val directiveList = if (activeDirectives.isEmpty()) {
            "Tidak ada direktif aktif."
        } else {
            activeDirectives.joinToString("\n") { "- [${it.priority}] ${it.title}" }
        }

        val briefingPrompt = """
            Buat ringkasan harian (Executive Morning/Tactical Briefing) singkat dan berenergi untuk Komandan.
            Waktu saat ini: $nowFormatted
            Lokasi: ${locationInfo?.addressLabel ?: "Belum terdeteksi"}
            Status perangkat: Baterai ${telemetry.batteryPercent}% (${if (telemetry.isCharging) "mengisi" else "baterai"}), Jaringan ${telemetry.networkStatus}.
            Direktif aktif (${activeDirectives.size}):
            $directiveList

            Berikan:
            1. Sapaan singkat sesuai waktu.
            2. Prioritas fokus utama dari daftar direktif (atau saran produktivitas jika kosong).
            3. Satu tips taktis singkat untuk hari ini.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            model.generateContent(briefingPrompt)
        }
        return response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Ringkasan harian kosong dari model.")
    }
}
