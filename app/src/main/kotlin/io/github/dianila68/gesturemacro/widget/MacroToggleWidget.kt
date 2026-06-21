package io.github.dianila68.gesturemacro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.dianila68.gesturemacro.R
import io.github.dianila68.gesturemacro.service.GestureCaptureService

class MacroToggleWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val armed = isArmed(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, armed))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_ARM) {
            val newArmed = !isArmed(context)
            setArmed(context, newArmed)
            if (newArmed) GestureCaptureService.start(context)
            else GestureCaptureService.stop(context)
            refreshAll(context, newArmed)
        }
    }

    private fun refreshAll(context: Context, armed: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, MacroToggleWidget::class.java))
        ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, armed)) }
    }

    private fun buildViews(context: Context, armed: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_macro_toggle)
        views.setTextViewText(R.id.widget_status_text, if (armed) "Armed" else "Disarmed")
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TOGGLE,
            Intent(context, MacroToggleWidget::class.java).setAction(ACTION_TOGGLE_ARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pi)
        return views
    }

    companion object {
        const val ACTION_TOGGLE_ARM = "io.github.dianila68.gesturemacro.ACTION_TOGGLE_ARM"
        private const val PREFS_NAME = "macro_widget_prefs"
        private const val KEY_ARMED = "armed"
        private const val REQUEST_CODE_TOGGLE = 100

        fun isArmed(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ARMED, false)

        fun setArmed(context: Context, armed: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ARMED, armed).apply()
        }
    }
}
