package com.example.who_are_you_today_template

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class MemeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_meme)
        val prefs = context.getSharedPreferences("MemePrefs", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastSelectedDate = prefs.getString("last_selected_date", "")

        if (lastSelectedDate == today) {
            val imageName = prefs.getString("selected_image", null)
            if (imageName != null) {
                val resourceId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                if (resourceId != 0) {
                    views.setImageViewResource(R.id.widget_image, resourceId)
                } else {
                    views.setImageViewResource(R.id.widget_image, android.R.color.transparent)
                }
            } else {
                views.setImageViewResource(R.id.widget_image, android.R.color.transparent)
            }

            val phrase = prefs.getString("selected_phrase", "")
            views.setTextViewText(R.id.widget_text, phrase)
        } else {
            views.setImageViewResource(R.id.widget_image, android.R.color.transparent)
            views.setTextViewText(R.id.widget_text, "Выбери картинку!")
        }

        val streak = prefs.getInt("streak", 0)
        views.setTextViewText(R.id.widget_streak, "🔥 $streak")

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MemeWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            val intent = Intent(context, MemeWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }
}