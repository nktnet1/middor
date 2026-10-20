package org.nktnet.middor.config

import android.annotation.SuppressLint
import android.view.Gravity
import org.nktnet.middor.R

// These settings name physical screen corners, independent of the language's reading direction.
@SuppressLint("RtlHardcoded")
enum class CloseButtonPosition(val labelResId: Int, val gravity: Int) {
    TOP_RIGHT(R.string.settings_corner_top_right, Gravity.TOP or Gravity.RIGHT),
    TOP_LEFT(R.string.settings_corner_top_left, Gravity.TOP or Gravity.LEFT),
    BOTTOM_RIGHT(R.string.settings_corner_bottom_right, Gravity.BOTTOM or Gravity.RIGHT),
    BOTTOM_LEFT(R.string.settings_corner_bottom_left, Gravity.BOTTOM or Gravity.LEFT)
}
