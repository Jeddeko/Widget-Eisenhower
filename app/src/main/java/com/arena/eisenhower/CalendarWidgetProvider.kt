package com.arena.eisenhower

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import java.util.Calendar

class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, manager, id) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateWidget(context, manager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("eisenhower_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        appWidgetIds.forEach { id ->
            editor.remove("cal_year_$id")
            editor.remove("cal_month_$id")
        }
        editor.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PREV_MONTH, ACTION_NEXT_MONTH -> {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (id == -1) return
                val (y, m) = getMonthState(context, id)
                val delta = if (intent.action == ACTION_NEXT_MONTH) 1 else -1
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y); set(Calendar.MONTH, m); set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, delta)
                }
                setMonthState(context, id, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                updateWidget(context, AppWidgetManager.getInstance(context), id)
            }
            Intent.ACTION_CONFIGURATION_CHANGED, ACTION_REFRESH -> updateAllWidgets(context)
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_PREV_MONTH = "com.arena.eisenhower.ACTION_CAL_PREV_MONTH"
        const val ACTION_NEXT_MONTH = "com.arena.eisenhower.ACTION_CAL_NEXT_MONTH"
        const val ACTION_REFRESH = "com.arena.eisenhower.ACTION_CAL_REFRESH"
        const val EXTRA_DUE_AT = "EXTRA_DUE_AT"
        const val EXTRA_MIN_HEIGHT = "EXTRA_MIN_HEIGHT"
        
        private val MONTHS = arrayOf("Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre")

        private fun getMonthState(context: Context, appWidgetId: Int): Pair<Int, Int> {
            val prefs = context.getSharedPreferences("eisenhower_settings", Context.MODE_PRIVATE)
            val y = prefs.getInt("cal_year_$appWidgetId", 0)
            val m = prefs.getInt("cal_month_$appWidgetId", -1)
            if (y == 0 || m == -1) {
                val c = Calendar.getInstance()
                return c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
            }
            return y to m
        }

        private fun setMonthState(context: Context, appWidgetId: Int, year: Int, month: Int) {
            context.getSharedPreferences("eisenhower_settings", Context.MODE_PRIVATE)
                .edit().putInt("cal_year_$appWidgetId", year).putInt("cal_month_$appWidgetId", month).apply()
        }

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CalendarWidgetProvider::class.java))
            ids.forEach { id -> updateWidget(context, manager, id) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val isDark = AppSettings(context).isDarkMode
            val (year, month) = getMonthState(context, appWidgetId)

            val bgColor = if (isDark) Color.parseColor("#121212") else Color.parseColor("#F8FAFC")
            val textColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")

            val views = RemoteViews(context.packageName, R.layout.calendar_widget)
            views.setInt(R.id.calendar_root, "setBackgroundColor", bgColor)
            views.setTextViewText(R.id.tv_month_title, "${MONTHS[month]} $year")
            views.setTextColor(R.id.tv_month_title, textColor)
            views.setTextColor(R.id.btn_prev_month, textColor)
            views.setTextColor(R.id.btn_next_month, textColor)

            val prevIntent = Intent(context, CalendarWidgetProvider::class.java).apply {
                action = ACTION_PREV_MONTH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(R.id.btn_prev_month, PendingIntent.getBroadcast(context, 100 + appWidgetId, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val nextIntent = Intent(context, CalendarWidgetProvider::class.java).apply {
                action = ACTION_NEXT_MONTH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(R.id.btn_next_month, PendingIntent.getBroadcast(context, 200 + appWidgetId, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            // --- CÁLCULO DE ALTURA REAL ---
            val options = manager.getAppWidgetOptions(appWidgetId)
            // MIN_HEIGHT nos da la altura ACTUAL en vertical
            val currentHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)
            val density = context.resources.displayMetrics.density
            
            // Restamos 80dp (cabecera, días de la semana y márgenes)
            val headerHeightPx = (80 * density).toInt()
            val totalAvailablePx = ((currentHeightDp * density).toInt() - headerHeightPx).coerceAtLeast(0)
            
            // Dividimos entre 6 semanas para obtener el alto mínimo de cada fila
            val minWeekHeightPx = totalAvailablePx / 6

            val serviceIntent = Intent(context, CalendarWidgetService::class.java).apply {
                putExtra("EXTRA_YEAR", year)
                putExtra("EXTRA_MONTH", month)
                putExtra(EXTRA_MIN_HEIGHT, minWeekHeightPx) 
                // IMPORTANTE: Agregamos minWeekHeightPx al final de la URL para que Android sepa que el tamaño cambió y redibuje las filas
                data = Uri.parse("eisenhower://calendar/$appWidgetId/$year/$month/$minWeekHeightPx")
            }
            views.setRemoteAdapter(R.id.calendar_week_list, serviceIntent)

            val templateIntent = Intent(context, QuickAddActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val templatePi = PendingIntent.getActivity(context, 400 + appWidgetId, templateIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            views.setPendingIntentTemplate(R.id.calendar_week_list, templatePi)

            manager.updateAppWidget(appWidgetId, views)
            manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.calendar_week_list)
        }
    }
}
