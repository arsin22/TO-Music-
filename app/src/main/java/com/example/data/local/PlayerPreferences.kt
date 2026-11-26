package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.EqPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "avaplayer_prefs")

data class EqSettingsState(
    val preset: EqPreset = EqPreset.FLAT,
    val gainsDb: List<Int> = listOf(0, 0, 0, 0, 0),
    val playbackRate: Float = 1f,
    val volume: Float = 0.85f
)

data class AppearanceSettingsState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val language: AppLanguage = AppLanguage.SYSTEM
)

class PlayerPreferences(private val context: Context) {
    private object Keys {
        val SEEDED = booleanPreferencesKey("avaplayer_seeded_v1")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_GAINS = stringPreferencesKey("eq_gains")
        val PLAYBACK_RATE = floatPreferencesKey("playback_rate")
        val VOLUME = floatPreferencesKey("volume")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val IGNORED_MEDIASTORE_IDS = stringSetPreferencesKey("ignored_mediastore_ids")
    }

    val eqSettingsFlow: Flow<EqSettingsState> = context.dataStore.data.map { prefs ->
        val presetName = prefs[Keys.EQ_PRESET]
        val preset = EqPreset.entries.firstOrNull { it.name == presetName } ?: EqPreset.FLAT
        val gainsRaw = prefs[Keys.EQ_GAINS]
        val gains = gainsRaw
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 5 }
            ?: preset.gainsDb
        val rate = prefs[Keys.PLAYBACK_RATE] ?: 1f
        val vol = prefs[Keys.VOLUME] ?: 0.85f
        EqSettingsState(
            preset = preset,
            gainsDb = gains,
            playbackRate = rate.coerceIn(0.75f, 1.5f),
            volume = vol.coerceIn(0f, 1f)
        )
    }

    val appearanceSettingsFlow: Flow<AppearanceSettingsState> = context.dataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_MODE]
        val theme = AppThemeMode.entries.firstOrNull { it.name == themeName } ?: AppThemeMode.DARK
        val langName = prefs[Keys.APP_LANGUAGE]
        val lang = AppLanguage.entries.firstOrNull { it.name == langName } ?: AppLanguage.SYSTEM
        AppearanceSettingsState(themeMode = theme, language = lang)
    }

    suspend fun isSeeded(): Boolean {
        return context.dataStore.data.map { it[Keys.SEEDED] ?: false }.first()
    }

    suspend fun setSeeded(seeded: Boolean) {
        context.dataStore.edit { it[Keys.SEEDED] = seeded }
    }

    suspend fun saveThemeMode(themeMode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun saveLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun getIgnoredMediaStoreIds(): Set<String> {
        return context.dataStore.data.map { it[Keys.IGNORED_MEDIASTORE_IDS] ?: emptySet() }.first()
    }

    suspend fun addIgnoredMediaStoreIds(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.IGNORED_MEDIASTORE_IDS] ?: emptySet()
            prefs[Keys.IGNORED_MEDIASTORE_IDS] = current + ids
        }
    }

    suspend fun clearIgnoredMediaStoreIds() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.IGNORED_MEDIASTORE_IDS)
        }
    }

    suspend fun saveEqSettings(preset: EqPreset, gainsDb: List<Int>, playbackRate: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EQ_PRESET] = preset.name
            prefs[Keys.EQ_GAINS] = gainsDb.joinToString(",")
            prefs[Keys.PLAYBACK_RATE] = playbackRate
        }
    }

    suspend fun saveVolume(volume: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VOLUME] = volume.coerceIn(0f, 1f)
        }
    }
}
