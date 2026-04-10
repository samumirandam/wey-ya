package com.weyya.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weyya.app.R
import com.weyya.app.domain.model.BlockingMode
import com.weyya.app.ui.theme.WeyYaGreen
import com.weyya.app.ui.theme.WeyYaOrange
import com.weyya.app.ui.theme.WeyYaRed

@Composable
fun BigToggle(
    isActive: Boolean,
    mode: BlockingMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "toggle_scale",
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isActive -> MaterialTheme.colorScheme.surfaceVariant
            mode == BlockingMode.ALL_CALLERS -> WeyYaRed
            else -> WeyYaGreen
        },
        label = "toggle_color",
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            !isActive -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> Color.White
        },
        label = "icon_color",
    )

    Box(
        modifier = modifier
            .size(200.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = stringResource(if (isActive) R.string.toggle_deactivate else R.string.toggle_activate),
            modifier = Modifier.size(100.dp),
            tint = iconColor,
        )
    }
}
