package com.yourapp.timemanagement.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourapp.timemanagement.domain.CardStyle
import com.yourapp.timemanagement.domain.DashboardModule
import com.yourapp.timemanagement.domain.FocusPreset
import com.yourapp.timemanagement.domain.LayoutDensity
import com.yourapp.timemanagement.domain.ScoringStyle
import com.yourapp.timemanagement.domain.ThemeMode
import com.yourapp.timemanagement.domain.UserSettings
import com.yourapp.timemanagement.domain.defaultDashboardModules
import com.yourapp.timemanagement.domain.defaultFocusPresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "time_management_settings")

@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
            themeMode = enumValue(prefs[Keys.themeMode], ThemeMode.System),
            accentColor = prefs[Keys.accentColor] ?: 0xFF1F8A70,
            cardStyle = enumValue(prefs[Keys.cardStyle], CardStyle.Rounded),
            density = enumValue(prefs[Keys.density], LayoutDensity.Spacious),
            dashboardModules = decodeModules(prefs[Keys.dashboardModules]),
            scoringStyle = enumValue(prefs[Keys.scoringStyle], ScoringStyle.Balanced),
            focusPresets = decodePresets(prefs[Keys.focusPresets]),
            notificationTone = prefs[Keys.notificationTone] ?: "Gentle chime",
            seededSampleData = prefs[Keys.seededSampleData] ?: false,
            xp = prefs[Keys.xp] ?: 0,
            level = prefs[Keys.level] ?: 1,
            streakDays = prefs[Keys.streakDays] ?: 0,
            unlockedAchievementIds = decodeStringSet(prefs[Keys.unlockedAchievements]),
        )
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { it[Keys.onboardingComplete] = value }
    }

    suspend fun setSeededSampleData(value: Boolean) {
        dataStore.edit { it[Keys.seededSampleData] = value }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        dataStore.edit { it[Keys.themeMode] = value.name }
    }

    suspend fun setAccentColor(value: Long) {
        dataStore.edit { it[Keys.accentColor] = value }
    }

    suspend fun setCardStyle(value: CardStyle) {
        dataStore.edit { it[Keys.cardStyle] = value.name }
    }

    suspend fun setDensity(value: LayoutDensity) {
        dataStore.edit { it[Keys.density] = value.name }
    }

    suspend fun setScoringStyle(value: ScoringStyle) {
        dataStore.edit { it[Keys.scoringStyle] = value.name }
    }

    suspend fun setNotificationTone(value: String) {
        dataStore.edit { it[Keys.notificationTone] = value }
    }

    suspend fun setFocusPresets(values: List<FocusPreset>) {
        dataStore.edit { prefs ->
            prefs[Keys.focusPresets] = values.joinToString("|") { preset ->
                "${preset.id},${preset.name},${preset.focusMinutes},${preset.breakMinutes}"
            }
        }
    }

    suspend fun setGamification(xp: Int, level: Int, streakDays: Int, unlockedAchievementIds: Set<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.xp] = xp.coerceAtLeast(0)
            prefs[Keys.level] = level.coerceAtLeast(1)
            prefs[Keys.streakDays] = streakDays.coerceAtLeast(0)
            prefs[Keys.unlockedAchievements] = unlockedAchievementIds.sorted().joinToString(",")
        }
    }

    suspend fun setDashboardModules(values: List<DashboardModule>) {
        dataStore.edit { prefs ->
            prefs[Keys.dashboardModules] = values.joinToString("|") { module ->
                "${module.key},${module.label},${module.visible}"
            }
        }
    }

    private fun decodeModules(raw: String?): List<DashboardModule> {
        if (raw.isNullOrBlank()) return defaultDashboardModules
        return raw.split("|").mapNotNull { item ->
            val parts = item.split(",")
            if (parts.size < 3) null else DashboardModule(
                key = parts[0],
                label = parts[1],
                visible = parts[2].toBooleanStrictOrNull() ?: true,
            )
        }.ifEmpty { defaultDashboardModules }
    }

    private fun decodePresets(raw: String?): List<FocusPreset> {
        if (raw.isNullOrBlank()) return defaultFocusPresets
        if (!raw.contains("|") && !raw.contains(",")) return defaultFocusPresets
        val legacy = raw.split(",").mapNotNull { it.toIntOrNull() }.filter { it in 5..180 }
        if (legacy.isNotEmpty()) {
            return legacy.mapIndexed { index, minutes -> FocusPreset(index + 1L, "${minutes}m Focus", minutes, 5) }
        }
        return raw.split("|").mapNotNull { item ->
            val parts = item.split(",")
            val id = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val name = parts.getOrNull(1)?.ifBlank { null } ?: return@mapNotNull null
            val focus = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(5, 180) ?: return@mapNotNull null
            val breakMinutes = parts.getOrNull(3)?.toIntOrNull()?.coerceIn(1, 60) ?: 5
            FocusPreset(id, name, focus, breakMinutes)
        }.ifEmpty { defaultFocusPresets }
    }

    private fun decodeStringSet(raw: String?): Set<String> =
        raw?.split(",")?.map(String::trim)?.filter(String::isNotBlank)?.toSet().orEmpty()

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val seededSampleData = booleanPreferencesKey("seeded_sample_data")
        val themeMode = stringPreferencesKey("theme_mode")
        val accentColor = longPreferencesKey("accent_color")
        val cardStyle = stringPreferencesKey("card_style")
        val density = stringPreferencesKey("density")
        val dashboardModules = stringPreferencesKey("dashboard_modules")
        val scoringStyle = stringPreferencesKey("scoring_style")
        val focusPresets = stringPreferencesKey("focus_presets")
        val notificationTone = stringPreferencesKey("notification_tone")
        val xp = intPreferencesKey("xp")
        val level = intPreferencesKey("level")
        val streakDays = intPreferencesKey("streak_days")
        val unlockedAchievements = stringPreferencesKey("unlocked_achievements")
    }
}
