package com.weyya.app.ui.common

import androidx.annotation.StringRes
import com.weyya.app.R
import com.weyya.app.domain.model.BlockingMode

/**
 * Single source of truth mapping a [BlockingMode] to its visible label resource.
 * Returns @StringRes (not a resolved String) so domain stays free of Context.
 * The exhaustive `when` makes adding a new mode a compile error here, in one place.
 */
@StringRes
fun BlockingMode.labelRes(): Int = when (this) {
    BlockingMode.UNKNOWN_CALLERS -> R.string.mode_unknown
    BlockingMode.ALL_CALLERS -> R.string.mode_all
}
