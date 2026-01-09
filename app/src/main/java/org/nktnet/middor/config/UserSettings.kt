package org.nktnet.middor.config

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
private val START_DELAY_SECONDS_KEY = intPreferencesKey("user_settings.start_delay_seconds")

object UserSettings {
    private object Defaults {
        val THEME = ThemeOption.SYSTEM
        const val START_ON_LAUNCH = false
        const val FLIP_HORIZONTALLY = true
        const val ROTATE_180 = false
        const val START_DELAY_SECONDS = 0
    }

    val currentTheme: MutableState<ThemeOption> = mutableStateOf(Defaults.THEME)
    val startOnLaunch: MutableState<Boolean> = mutableStateOf(Defaults.START_ON_LAUNCH)
    val flipHorizontally: MutableState<Boolean> = mutableStateOf(Defaults.FLIP_HORIZONTALLY)
    val rotate180: MutableState<Boolean> = mutableStateOf(Defaults.ROTATE_180)
    val startDelaySeconds: MutableState<Int> = mutableIntStateOf(Defaults.START_DELAY_SECONDS)

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

    fun resetSettings(context: Context) {
        setTheme(context, Defaults.THEME)
        setStartOnLaunch(context, Defaults.START_ON_LAUNCH)
        setFlipHorizontally(context, Defaults.FLIP_HORIZONTALLY)
        setRotate180(context, Defaults.ROTATE_180)
        setStartDelay(context, Defaults.START_DELAY_SECONDS)
    }

    fun setStartDelay(context: Context, delayMs: Int) {
        startDelaySeconds.value = delayMs
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[START_DELAY_SECONDS_KEY] = delayMs
            }
        }
    }

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data.collectLatest { prefs ->
                currentTheme.value = ThemeOption.entries.find {
                    it.name == prefs[THEME_KEY]
                } ?: Defaults.THEME

                startOnLaunch.value = prefs[START_ON_LAUNCH_KEY] ?: Defaults.START_ON_LAUNCH
                flipHorizontally.value = prefs[FLIP_DISPLAY_KEY] ?: Defaults.FLIP_HORIZONTALLY
                rotate180.value = prefs[UPSIDE_DOWN_KEY] ?: Defaults.ROTATE_180
                startDelaySeconds.value = prefs[START_DELAY_SECONDS_KEY] ?: Defaults.START_DELAY_SECONDS
            }
        }
    }
}
