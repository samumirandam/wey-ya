package com.weyya.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.weyya.app.R

class WeyYaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetDataHelper.readState(context)
        val statusLabel = if (state.isActive) context.getString(R.string.widget_on)
            else context.getString(R.string.widget_off)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    isActive = state.isActive,
                    modeName = state.modeName,
                    blockedToday = state.blockedToday,
                    statusLabel = statusLabel,
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(isActive: Boolean, modeName: String, blockedToday: Int, statusLabel: String) {
        val bgColor = if (isActive) Color(0xFFFF6B35) else Color(0xFF9E9E9E)
        val textColor = ColorProvider(Color.White)

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(16.dp)
                .clickable(actionRunCallback<ToggleAction>())
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_tile),
                contentDescription = null,
                modifier = GlanceModifier.size(28.dp),
                colorFilter = ColorFilter.tint(textColor),
            )
            Spacer(GlanceModifier.width(10.dp))
            Column {
                Text(
                    text = statusLabel,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                if (isActive) {
                    Text(
                        text = modeName,
                        style = TextStyle(color = textColor, fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "$blockedToday",
                style = TextStyle(
                    color = textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

class ToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetDataHelper.toggle(context)
        WeyYaWidget().update(context, glanceId)
    }
}
