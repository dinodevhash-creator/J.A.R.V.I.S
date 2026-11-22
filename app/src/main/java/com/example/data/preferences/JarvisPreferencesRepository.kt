package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisDataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_prefs")

enum class JarvisPersona(val displayName: String, val systemTag: String) {
    TACTICAL_BUTLER("Pelayan Taktis (Klasik)", "Formal, cerdas, tenang, sedikit humor elegan ala J.A.R.V.I.S."),
    CONCISE_OPS("Operasional Ringkas", "Sangat singkat, padat, berorientasi aksi, tanpa basa-basi."),
    STRATEGIC_ANALYST("Analis Strategis", "Analitis, terstruktur dengan poin-poin teknis yang mendalam.")
}

data class JarvisSettings(
    val autoSpeakEnabled: Boolean = true,
    val speechRate: Float = 1.05f,
    val languageCode: String = "id-ID",
    val persona: JarvisPersona = JarvisPersona.TACTICAL_BUTLER,
    val dailyBriefingEnabled: Boolean = false,
    val briefingHour: Int = 7,
    val briefingMinute: Int = 0
)

class JarvisPreferencesRepository(private val context: Context) {
    private object Keys {
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak_enabled")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val PERSONA = stringPreferencesKey("persona")
        val DAILY_BRIEFING_ENABLED = booleanPreferencesKey("daily_briefing_enabled")
        val BRIEFING_HOUR = intPreferencesKey("briefing_hour")
        val BRIEFING_MINUTE = intPreferencesKey("briefing_minute")
    }

    val settings: Flow<JarvisSettings> = context.jarvisDataStore.data.map { prefs ->
        val personaName = prefs[Keys.PERSONA] ?: JarvisPersona.TACTICAL_BUTLER.name
        val persona = JarvisPersona.entries.firstOrNull { it.name == personaName }
            ?: JarvisPersona.TACTICAL_BUTLER
        JarvisSettings(
            autoSpeakEnabled = prefs[Keys.AUTO_SPEAK] ?: true,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.05f,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "id-ID",
            persona = persona,
            dailyBriefingEnabled = prefs[Keys.DAILY_BRIEFING_ENABLED] ?: false,
            briefingHour = prefs[Keys.BRIEFING_HOUR] ?: 7,
            briefingMinute = prefs[Keys.BRIEFING_MINUTE] ?: 0
        )
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.jarvisDataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setLanguageCode(code: String) {
        context.jarvisDataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun setPersona(persona: JarvisPersona) {
        context.jarvisDataStore.edit { it[Keys.PERSONA] = persona.name }
    }

    suspend fun setDailyBriefing(enabled: Boolean, hour: Int, minute: Int) {
        context.jarvisDataStore.edit {
            it[Keys.DAILY_BRIEFING_ENABLED] = enabled
            it[Keys.BRIEFING_HOUR] = hour.coerceIn(0, 23)
            it[Keys.BRIEFING_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
