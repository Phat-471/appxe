package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.model.VisualSpeedAlertState

class SpeedAlertWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    for (appWidgetId in appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId, null)
    }
  }

  companion object {
    fun updateAllWidgets(context: Context, state: VisualSpeedAlertState) {
      val appWidgetManager = AppWidgetManager.getInstance(context)
      val componentName = ComponentName(context, SpeedAlertWidgetProvider::class.java)
      val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

      for (appWidgetId in appWidgetIds) {
        updateAppWidget(context, appWidgetManager, appWidgetId, state)
      }
    }

    private fun updateAppWidget(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetId: Int,
      state: VisualSpeedAlertState?
    ) {
      val views = RemoteViews(context.packageName, R.layout.widget_speed_alert)

      val speed = state?.currentSpeedKmh ?: 0
      val limit = state?.speedLimitKmh ?: 50
      val road = state?.roadName ?: "Đang kết nối GPS..."
      val isOver = state?.isOverspeeding ?: false

      views.setTextViewText(R.id.widget_current_speed, "$speed")
      views.setTextViewText(R.id.widget_speed_limit, "$limit")
      views.setTextViewText(R.id.widget_road_name, road)

      if (isOver) {
        views.setTextColor(R.id.widget_current_speed, Color.parseColor("#EF4444")) // Red
        views.setTextViewText(R.id.widget_camera_warning, "🚨 VƯỢT QUÁ TỐC ĐỘ (+${state?.speedDeltaKmh} km/h)")
        views.setTextColor(R.id.widget_camera_warning, Color.parseColor("#EF4444"))
      } else {
        views.setTextColor(R.id.widget_current_speed, Color.parseColor("#38BDF8")) // Cyan Blue
        views.setTextViewText(R.id.widget_camera_warning, "🟢 Tốc độ an toàn")
        views.setTextColor(R.id.widget_camera_warning, Color.parseColor("#10B981"))
      }

      // Open app on click
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }
}
