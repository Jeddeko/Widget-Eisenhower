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
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

        private fun isColorDark(color: Int): Boolean {
            val darkness = 1.0 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            return darkness > 0.5
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val settings = AppSettings(context)
            val isDark = settings.isDarkMode
            val (year, month) = getMonthState(context, appWidgetId)

            val bgColor = if (isDark) Color.parseColor("#121212") else Color.parseColor("#F8FAFC")
            val textColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
            val mutedColor = if (isDark) Color.parseColor("#64748B") else Color.parseColor("#94A3B8")
            val todayBgColor = if (isDark) Color.parseColor("#1E3A8A") else Color.parseColor("#DBEAFE")
            val todayTextColor = if (isDark) Color.parseColor("#60A5FA") else Color.parseColor("#2563EB")

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

            val today = LocalDate.now()
            val itemsByDay = mutableMapOf<LocalDate, MutableList<Task>>()
            TaskRepository(context).getAll().forEach { task ->
                if (task.done) return@forEach
                task.dueAt?.let { due ->
                    val date = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
                    itemsByDay.getOrPut(date) { mutableListOf() }.add(task)
                }
            }

            val firstOfMonth = LocalDate.of(year, month + 1, 1)
            val cell0Date = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())
            
            views.removeAllViews(R.id.week_container)

            for (w in 0 until 6) {
                val weekView = RemoteViews(context.packageName, R.layout.calendar_week_row)
                
                for (d in 0 until 7) {
                    val cellIndex = (w * 7) + d
                    val date = cell0Date.plusDays(cellIndex.toLong())
                    val isCurrentMonth = date.year == year && date.monthValue == (month + 1)
                    val isToday = date == today
                    
                    val cellId = context.resources.getIdentifier("cell_${d+1}", "id", context.packageName)
                    val numId = context.resources.getIdentifier("day_${d+1}_number", "id", context.packageName)
                    
                    weekView.setTextViewText(numId, date.dayOfMonth.toString())
                    val dayNumColor = when {
                        isToday -> todayTextColor
                        !isCurrentMonth -> mutedColor
                        else -> textColor
                    }
                    weekView.setTextColor(numId, dayNumColor)
                    
                    if (isToday) {
                        weekView.setInt(cellId, "setBackgroundColor", todayBgColor)
                    } else {
                        val bgRes = if (!isCurrentMonth) R.drawable.bg_calendar_cell_outside else R.drawable.bg_calendar_cell
                        weekView.setInt(cellId, "setBackgroundResource", bgRes)
                    }
                    
                    // CLICK EN EL DÍA: Abrir QuickAdd con la fecha preseleccionada
                    val addIntent = Intent(context, QuickAddActivity::class.java).apply {
                        data = Uri.parse("eisenhower://add/${date.toString()}")
                        putExtra(EXTRA_DUE_AT, date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    val addPi = PendingIntent.getActivity(context, date.hashCode(), addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    weekView.setOnClickPendingIntent(cellId, addPi)

                    val dayTasks = itemsByDay[date] ?: emptyList()
                    // Límite de 5 tareas por día para no reventar la memoria del widget
                    for (t in 1..5) {
                        val taskIdView = context.resources.getIdentifier("task_${d+1}_$t", "id", context.packageName)
                        if (t <= dayTasks.size) {
                            val task = dayTasks[t - 1]
                            weekView.setViewVisibility(taskIdView, View.VISIBLE)
                            weekView.setTextViewText(taskIdView, task.title)
                            
                            val qColor = when (task.quadrant) {
                                Quadrant.DO_FIRST -> settings.colorQ1
                                Quadrant.SCHEDULE -> settings.colorQ2
                                Quadrant.DELEGATE -> settings.colorQ3
                                Quadrant.ELIMINATE -> settings.colorQ4
                            }
                            weekView.setInt(taskIdView, "setBackgroundColor", qColor)
                            weekView.setTextColor(taskIdView, if (isColorDark(qColor)) Color.WHITE else Color.BLACK)
                            
                            // CLICK EN LA TAREA: Abrir QuickAdd para editarla
                            val editIntent = Intent(context, QuickAddActivity::class.java).apply {
                                data = Uri.parse("eisenhower://edit/${task.id}")
                                putExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, task.id)
                                putExtra(EisenhowerWidgetProvider.EXTRA_ACTION_TYPE, EisenhowerWidgetProvider.ACTION_EDIT)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            val editPi = PendingIntent.getActivity(context, task.id.toInt(), editIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            weekView.setOnClickPendingIntent(taskIdView, editPi)
                            
                        } else {
                            weekView.setViewVisibility(taskIdView, View.GONE)
                        }
                    }
                }
                views.addView(R.id.week_container, weekView)
            }

            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
