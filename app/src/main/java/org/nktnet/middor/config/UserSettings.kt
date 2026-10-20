package org.nktnet.middor.config

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.Preferences
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

object UserSettings {
    private object Keys {
        val THEME_KEY = stringPreferencesKey("user_settings.theme_option")
        val START_ON_LAUNCH_KEY = booleanPreferencesKey("user_settings.start_on_launch")
        val FLIP_DISPLAY_KEY = booleanPreferencesKey("user_settings.flip_horizontally")
        val UPSIDE_DOWN_KEY = booleanPreferencesKey("user_settings.upside_down")
        val REMOVE_SYSTEM_BARS_KEY = booleanPreferencesKey("user_settings.remove_system_bars")
        val START_DELAY_SECONDS_KEY = intPreferencesKey("user_settings.start_delay_seconds")
        val TOUCH_CONTROLS_KEY = booleanPreferencesKey("user_settings.touch_controls")
        val TOUCH_FEEDBACK_KEY = booleanPreferencesKey("user_settings.touch_feedback")
        val CLOSE_BUTTON_POSITION_KEY = stringPreferencesKey("user_settings.close_button_position")
        val TAP_DURATION_KEY = intPreferencesKey("user_settings.tap_duration")
    }

    private object Defaults {
        val THEME = ThemeOption.SYSTEM
        const val START_ON_LAUNCH = false
        const val FLIP_HORIZONTALLY = true
        const val ROTATE_180 = false
        const val REMOVE_SYSTEM_BARS = false
        const val START_DELAY_SECONDS = 0
        const val TOUCH_CONTROLS = true
        const val TOUCH_FEEDBACK = false
        val CLOSE_BUTTON_POSITION = CloseButtonPosition.TOP_RIGHT
        const val TAP_DURATION = 40
    }

    val currentTheme: MutableState<ThemeOption> = mutableStateOf(Defaults.THEME)
    val startOnLaunch: MutableState<Boolean> = mutableStateOf(Defaults.START_ON_LAUNCH)
    val flipHorizontally: MutableState<Boolean> = mutableStateOf(Defaults.FLIP_HORIZONTALLY)
    val rotate180: MutableState<Boolean> = mutableStateOf(Defaults.ROTATE_180)
    val removeSystemBars: MutableState<Boolean> = mutableStateOf(Defaults.REMOVE_SYSTEM_BARS)
    val startDelaySeconds: MutableState<Int> = mutableIntStateOf(Defaults.START_DELAY_SECONDS)
    val touchControls: MutableState<Boolean> = mutableStateOf(Defaults.TOUCH_CONTROLS)
    val touchFeedback: MutableState<Boolean> = mutableStateOf(Defaults.TOUCH_FEEDBACK)
    val closeButtonPosition: MutableState<CloseButtonPosition> =
        mutableStateOf(Defaults.CLOSE_BUTTON_POSITION)
    val tapDuration: MutableState<Int> = mutableIntStateOf(Defaults.TAP_DURATION)

    fun setTheme(context: Context, theme: ThemeOption) {
        currentTheme.value = theme
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[Keys.THEME_KEY] = theme.name
            }
        }
    }

    private fun <T> setPreference(
        context: Context,
        key: Preferences.Key<T>,
        state: MutableState<T>,
        value: T
    ) {
        state.value = value
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[key] = value
            }
        }
    }

    fun setStartOnLaunch(context: Context, value: Boolean) =
        setPreference(context, Keys.START_ON_LAUNCH_KEY, startOnLaunch, value)

    fun setFlipHorizontally(context: Context, value: Boolean) =
        setPreference(context, Keys.FLIP_DISPLAY_KEY, flipHorizontally, value)

    fun setRotate180(context: Context, value: Boolean) =
        setPreference(context, Keys.UPSIDE_DOWN_KEY, rotate180, value)

    fun setRemoveSystemBars(context: Context, value: Boolean) =
        setPreference(context, Keys.REMOVE_SYSTEM_BARS_KEY, removeSystemBars, value)

    fun setStartDelay(context: Context, delayMs: Int) =
        setPreference(
            context,
            Keys.START_DELAY_SECONDS_KEY,
            startDelaySeconds,
            delayMs
        )

    fun setTouchControls(context: Context, value: Boolean) =
        setPreference(context, Keys.TOUCH_CONTROLS_KEY, touchControls, value)

    fun setTouchFeedback(context: Context, value: Boolean) =
        setPreference(context, Keys.TOUCH_FEEDBACK_KEY, touchFeedback, value)

    fun setCloseButtonPosition(context: Context, value: CloseButtonPosition) {
        closeButtonPosition.value = value
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[Keys.CLOSE_BUTTON_POSITION_KEY] = value.name
            }
        }
    }

    fun setTapDuration(context: Context, value: Int) =
        setPreference(context, Keys.TAP_DURATION_KEY, tapDuration, value.coerceIn(16, 200))

    fun resetSettings(context: Context) {
        setTheme(context, Defaults.THEME)
        setStartOnLaunch(context, Defaults.START_ON_LAUNCH)
        setFlipHorizontally(context, Defaults.FLIP_HORIZONTALLY)
        setRotate180(context, Defaults.ROTATE_180)
        setRemoveSystemBars(context, Defaults.REMOVE_SYSTEM_BARS)
        setStartDelay(context, Defaults.START_DELAY_SECONDS)
        setTouchControls(context, Defaults.TOUCH_CONTROLS)
        setTouchFeedback(context, Defaults.TOUCH_FEEDBACK)
        setCloseButtonPosition(context, Defaults.CLOSE_BUTTON_POSITION)
        setTapDuration(context, Defaults.TAP_DURATION)
    }

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data.collectLatest { prefs ->
                currentTheme.value = ThemeOption.entries.find {
                    it.name == prefs[Keys.THEME_KEY]
                } ?: Defaults.THEME

                startOnLaunch.value = prefs[Keys.START_ON_LAUNCH_KEY] ?: Defaults.START_ON_LAUNCH
                flipHorizontally.value = prefs[Keys.FLIP_DISPLAY_KEY] ?: Defaults.FLIP_HORIZONTALLY
                rotate180.value = prefs[Keys.UPSIDE_DOWN_KEY] ?: Defaults.ROTATE_180
                removeSystemBars.value = prefs[Keys.REMOVE_SYSTEM_BARS_KEY]
                    ?: Defaults.REMOVE_SYSTEM_BARS
                startDelaySeconds.value = prefs[Keys.START_DELAY_SECONDS_KEY]
                    ?: Defaults.START_DELAY_SECONDS
                touchControls.value = prefs[Keys.TOUCH_CONTROLS_KEY] ?: Defaults.TOUCH_CONTROLS
                touchFeedback.value = prefs[Keys.TOUCH_FEEDBACK_KEY] ?: Defaults.TOUCH_FEEDBACK
                closeButtonPosition.value = CloseButtonPosition.entries.find {
                    it.name == prefs[Keys.CLOSE_BUTTON_POSITION_KEY]
                } ?: Defaults.CLOSE_BUTTON_POSITION
                tapDuration.value = (prefs[Keys.TAP_DURATION_KEY] ?: Defaults.TAP_DURATION)
                    .coerceIn(16, 200)
            }
        }
    }
}
