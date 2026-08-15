package com.currencyconverter.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "converter_settings")

data class PersistedSettings(
    val fromCode: String = "USD",
    val toCode: String = "UZS",
    val darkTheme: Boolean = false,
    /** Ordered by when they were starred. Empty by default — the user picks their own favourites. */
    val favorites: List<String> = emptyList(),
)

/** Persists from/to/theme/favourites across launches (spec section: "Persist from, to, favourites, theme"). */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val FROM = stringPreferencesKey("from_code")
        val TO = stringPreferencesKey("to_code")
        val DARK = booleanPreferencesKey("dark_theme")
        val FAVS = stringPreferencesKey("favorites_csv")
    }

    suspend fun load(): PersistedSettings {
        val prefs = context.dataStore.data.first()
        val defaults = PersistedSettings()
        val favsRaw = prefs[Keys.FAVS]
        val favs = favsRaw?.split(",")?.filter { it.isNotBlank() } ?: defaults.favorites
        return PersistedSettings(
            fromCode = prefs[Keys.FROM] ?: defaults.fromCode,
            toCode = prefs[Keys.TO] ?: defaults.toCode,
            darkTheme = prefs[Keys.DARK] ?: defaults.darkTheme,
            favorites = favs,
        )
    }

    suspend fun save(settings: PersistedSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FROM] = settings.fromCode
            prefs[Keys.TO] = settings.toCode
            prefs[Keys.DARK] = settings.darkTheme
            prefs[Keys.FAVS] = settings.favorites.joinToString(",")
        }
    }
}
