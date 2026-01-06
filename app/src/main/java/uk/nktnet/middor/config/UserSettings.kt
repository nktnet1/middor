package uk.nktnet.middor.config

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("user_settings")
private val THEME_KEY = stringPreferencesKey("theme_option")
private val START_ON_LAUNCH_KEY = booleanPreferencesKey("start_on_launch")

object UserSettings {
    val currentTheme: MutableState<ThemeOption> = mutableStateOf(ThemeOption.SYSTEM)
    val startOnLaunch: MutableState<Boolean> = mutableStateOf(false)

    fun setTheme(context: Context, theme: ThemeOption) {
        currentTheme.value = theme
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[THEME_KEY] = theme.name
            }
        }
    }

    fun setStartOnLaunch(context: Context, value: Boolean) {
        startOnLaunch.value = value
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[START_ON_LAUNCH_KEY] = value
            }
        }
    }

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data.collectLatest { prefs ->
                currentTheme.value = ThemeOption.entries.find {
                    it.name == prefs[THEME_KEY]
                } ?: ThemeOption.SYSTEM
                startOnLaunch.value = prefs[START_ON_LAUNCH_KEY] ?: false
            }
        }
    }
}
