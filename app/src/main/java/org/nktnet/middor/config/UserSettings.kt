package org.nktnet.middor.config

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
private val THEME_KEY = stringPreferencesKey("user_settings.theme_option")
private val START_ON_LAUNCH_KEY = booleanPreferencesKey("user_settings.start_on_launch")
private val FLIP_DISPLAY_KEY = booleanPreferencesKey("user_settings.flip_horizontally")
private val UPSIDE_DOWN_KEY = booleanPreferencesKey("user_settings.upside_down")

object UserSettings {
    val currentTheme: MutableState<ThemeOption> = mutableStateOf(ThemeOption.SYSTEM)
    val startOnLaunch: MutableState<Boolean> = mutableStateOf(false)
    val flipHorizontally: MutableState<Boolean> = mutableStateOf(true)
    val rotate180: MutableState<Boolean> = mutableStateOf(false)

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

    fun setFlipHorizontally(context: Context, value: Boolean) {
        flipHorizontally.value = value
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[FLIP_DISPLAY_KEY] = value
            }
        }
    }

    fun setRotate180(context: Context, value: Boolean) {
        rotate180.value = value
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[UPSIDE_DOWN_KEY] = value
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
                flipHorizontally.value = prefs[FLIP_DISPLAY_KEY] ?: true
                rotate180.value = prefs[UPSIDE_DOWN_KEY] ?: false
            }
        }
    }
}
