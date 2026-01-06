package uk.nktnet.middor.config

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("user_settings")
private val THEME_KEY = stringPreferencesKey("theme_option")

object UserSettings {
    val currentTheme: MutableState<ThemeOption> = mutableStateOf(ThemeOption.SYSTEM)

    fun setTheme(context: Context, theme: ThemeOption) {
        currentTheme.value = theme
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[THEME_KEY] = theme.name
            }
        }
    }

    fun loadTheme(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data
                .map { prefs ->
                    val value = prefs[THEME_KEY]
                    ThemeOption.entries.find { it.name == value } ?: ThemeOption.SYSTEM
                }
                .collectLatest { theme ->
                    currentTheme.value = theme
                }
        }
    }
}
