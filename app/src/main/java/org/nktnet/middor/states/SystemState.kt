package org.nktnet.middor.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SystemState {
    var hasStartedOnLaunch by mutableStateOf(false)
}
