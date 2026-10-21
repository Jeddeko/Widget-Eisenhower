package com.arena.eisenhower

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CalendarWidgetFactory(
            applicationContext,
            intent.getIntExtra("EXTRA_YEAR", 0),
            intent.getIntExtra("EXTRA_MONTH", 0),
            intent.getIntExtra(CalendarWidgetProvider.EXTRA_MIN_HEIGHT, 0)
        )
    }
}

class CalendarWidgetFactory(
    private val context: Context,
    private val year: Int,
    private val month: Int,
    private val minRowHeight: Int
) : RemoteViewsService.RemoteViewsFactory {

    private data class CalendarEventItem(
        val id: Long,
        val title: String,
        val color: Int,
        val isReminder: Boolean,
        val timePrefix: String = "",
        val isDone: Boolean = false,
        val isGhost: Boolean = false // Si es un registro de tarea atrasada
    )

    private data class DayCell(
        val date: LocalDate,
        val dayNumber: Int,
        val isToday: Boolean,
        val isCurrentMonth: Boolean,
        val items: List<CalendarEventItem>
    )

    private var weeks: List<List<DayCell>> = emptyList()
    private var textColor: Int = Color.BLACK
    private var mutedColor: Int = Color.GRAY
    private var todayBgColor: Int = Color.LTGRAY
    private var todayTextColor: Int = Color.BLUE

    private val cellIds = intArrayOf(R.id.cell_1, R.id.cell_2, R.id.cell_3, R.id.cell_4, R.id.cell_5, R.id.cell_6, R.id.cell_7)
    private val numIds = intArrayOf(R.id.day_1_number, R.id.day_2_number, R.id.day_3_number, R.id.day_4_number, R.id.day_5_number, R.id.day_6_number, R.id.day_7_number)
    private val taskIds = arrayOf(
        intArrayOf(R.id.task_1_1, R.id.task_1_2, R.id.task_1_3, R.id.task_1_4, R.id.task_1_5, R.id.task_1_6, R.id.task_1_7, R.id.task_1_8, R.id.task_1_9, R.id.task_1_10, R.id.task_1_11, R.id.task_1_12, R.id.task_1_13, R.id.task_1_14, R.id.task_1_15),
        intArrayOf(R.id.task_2_1, R.id.task_2_2, R.id.task_2_3, R.id.task_2_4, R.id.task_2_5, R.id.task_2_6, R.id.task_2_7, R.id.task_2_8, R.id.task_2_9, R.id.task_2_10, R.id.task_2_11, R.id.task_2_12, R.id.task_2_13, R.id.task_2_14, R.id.task_2_15),
        intArrayOf(R.id.task_3_1, R.id.task_3_2, R.id.task_3_3, R.id.task_3_4, R.id.task_3_5, R.id.task_3_6, R.id.task_3_7, R.id.task_3_8, R.id.task_3_9, R.id.task_3_10, R.id.task_3_11, R.id.task_3_12, R.id.task_3_13, R.id.task_3_14, R.id.task_3_15),
        intArrayOf(R.id.task_4_1, R.id.task_4_2, R.id.task_4_3, R.id.task_4_4, R.id.task_4_5, R.id.task_4_6, R.id.task_4_7, R.id.task_4_8, R.id.task_4_9, R.id.task_4_10, R.id.task_4_11, R.id.task_4_12, R.id.task_4_13, R.id.task_4_14, R.id.task_4_15),
        intArrayOf(R.id.task_5_1, R.id.task_5_2, R.id.task_5_3, R.id.task_5_4, R.id.task_5_5, R.id.task_5_6, R.id.task_5_7, R.id.task_5_8, R.id.task_5_9, R.id.task_5_10, R.id.task_5_11, R.id.task_5_12, R.id.task_5_13, R.id.task_5_14, R.id.task_5_15),
        intArrayOf(R.id.task_6_1, R.id.task_6_2, R.id.task_6_3, R.id.task_6_4, R.id.task_6_5, R.id.task_6_6, R.id.task_6_7, R.id.task_6_8, R.id.task_6_9, R.id.task_6_10, R.id.task_6_11, R.id.task_6_12, R.id.task_6_13, R.id.task_6_14, R.id.task_6_15),
        intArrayOf(R.id.task_7_1, R.id.task_7_2, R.id.task_7_3, R.id.task_7_4, R.id.task_7_5, R.id.task_7_6, R.id.task_7_7, R.id.task_7_8, R.id.task_7_9, R.id.task_7_10, R.id.task_7_11, R.id.task_7_12, R.id.task_7_13, R.id.task_7_14, R.id.task_7_15)
    )

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1.0 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness > 0.5
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val isDark = AppSettings(context).isDarkMode
        textColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        mutedColor = if (isDark) Color.parseColor("#64748B") else Color.parseColor("#94A3B8")
        todayBgColor = if (isDark) Color.parseColor("#1E3A8A") else Color.parseColor("#DBEAFE")
        todayTextColor = if (isDark) Color.parseColor("#60A5FA") else Color.parseColor("#2563EB")

        val today = LocalDate.now()
        val todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val itemsByDay = mutableMapOf<LocalDate, MutableList<CalendarEventItem>>()
        val settings = AppSettings(context)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val firstOfMonth = LocalDate.of(year, month + 1, 1)
        val monthStartMillis = firstOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEndMillis = firstOfMonth.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        TaskRepository(context).getAll().forEach { task ->
            val qColor = when (task.quadrant) {
                Quadrant.DO_FIRST -> settings.colorQ1
                Quadrant.SCHEDULE -> settings.colorQ2
                Quadrant.DELEGATE -> settings.colorQ3
                Quadrant.ELIMINATE -> settings.colorQ4
            }

            val occurrences = task.getOccurrences(monthStartMillis, monthEndMillis)
            occurrences.forEach { time ->
                val date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
                
                if (task.done) {
                    // Lógica de tarea completada
                    val completionDay = Instant.ofEpochMilli(task.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                    val dueDay = Instant.ofEpochMilli(task.dueAt ?: task.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()

                    if (date == dueDay && date == completionDay) {
                        // Completada a tiempo
                        itemsByDay.getOrPut(date) { mutableListOf() }.add(
                            CalendarEventItem(task.id, task.title, qColor, isReminder = false, isDone = true)
                        )
                    } else if (date == dueDay) {
                        // Fecha original de tarea atrasada (Ghost en gris)
                        itemsByDay.getOrPut(date) { mutableListOf() }.add(
                            CalendarEventItem(task.id, task.title, Color.parseColor("#94A3B8"), isReminder = false, isDone = true, isGhost = true)
                        )
                    } else if (date == completionDay) {
                        // Día en que se completó tarde
                        val daysLate = TimeUnit.MILLISECONDS.toDays(task.updatedAt - (task.dueAt ?: task.updatedAt)).toInt()
                        val prefix = if (daysLate == 1) "COMPLETADO 1 DÍA DESPUÉS: " else "COMPLETADO $daysLate DÍAS DESPUÉS: "
                        itemsByDay.getOrPut(date) { mutableListOf() }.add(
                            CalendarEventItem(task.id, prefix + task.title, qColor, isReminder = false, isDone = true)
                        )
                    }
                } else {
                    // Tarea normal no completada
                    itemsByDay.getOrPut(date) { mutableListOf() }.add(
                        CalendarEventItem(task.id, task.title, qColor, isReminder = false, isDone = false)
                    )
                }
            }

            task.reminders.forEach { reminderTime ->
                if (reminderTime in monthStartMillis..monthEndMillis && reminderTime >= todayStart) {
                    val remDate = Instant.ofEpochMilli(reminderTime).atZone(ZoneId.systemDefault()).toLocalDate()
                    val timeStr = timeFormat.format(Date(reminderTime))
                    itemsByDay.getOrPut(remDate) { mutableListOf() }.add(
                        CalendarEventItem(task.id, task.title, qColor, isReminder = true, timePrefix = "🔔 $timeStr - ")
                    )
                }
            }
        }

        val cell0Date = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())

        val allCells = (0 until 42).map { i ->
            val date = cell0Date.plusDays(i.toLong())
            DayCell(
                date = date,
                dayNumber = date.dayOfMonth,
                isToday = date == today,
                isCurrentMonth = date.year == year && date.monthValue == (month + 1),
                items = itemsByDay[date]?.sortedBy { it.timePrefix } ?: emptyList()
            )
        }
        weeks = allCells.chunked(7)
    }

    override fun onDestroy() {}
    override fun getCount(): Int = weeks.size

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.calendar_week_row)
        val week = weeks[position]

        rv.setInt(R.id.week_row, "setMinimumHeight", minRowHeight)

        for (i in 0 until 7) {
            val cell = week[i]
            val cellId = cellIds[i]
            val numId = numIds[i]

            rv.setInt(cellId, "setMinimumHeight", minRowHeight)

            rv.setTextViewText(numId, cell.dayNumber.toString())
            val dayNumColor = when {
                cell.isToday -> todayTextColor
                !cell.isCurrentMonth -> mutedColor
                else -> textColor
            }
            rv.setTextColor(numId, dayNumColor)

            if (cell.isToday) {
                rv.setInt(cellId, "setBackgroundColor", todayBgColor)
            } else {
                val bgRes = if (!cell.isCurrentMonth) R.drawable.bg_calendar_cell_outside else R.drawable.bg_calendar_cell
                rv.setInt(cellId, "setBackgroundResource", bgRes)
            }

            val addIntent = Intent().apply {
                putExtra(CalendarWidgetProvider.EXTRA_DUE_AT, cell.date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            }
            rv.setOnClickFillInIntent(cellId, addIntent)

            for (t in 0 until 15) {
                val taskId = taskIds[i][t]
                if (t < cell.items.size) {
                    val item = cell.items[t]
                    rv.setViewVisibility(taskId, View.VISIBLE)
                    
                    val displayText = if (item.isReminder) item.timePrefix + item.title else item.title
                    
                    rv.setTextViewText(taskId, displayText)
                    rv.setInt(taskId, "setBackgroundColor", item.color)
                    rv.setTextColor(taskId, if (isColorDark(item.color)) Color.WHITE else Color.BLACK)

                    // TACHADO SI ESTÁ COMPLETADA
                    if (item.isDone) {
                        rv.setInt(taskId, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
                    } else {
                        rv.setInt(taskId, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
                    }

                    val editIntent = Intent().apply {
                        putExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, item.id)
                        putExtra(EisenhowerWidgetProvider.EXTRA_ACTION_TYPE, EisenhowerWidgetProvider.ACTION_EDIT)
                    }
                    rv.setOnClickFillInIntent(taskId, editIntent)
                } else {
                    rv.setViewVisibility(taskId, View.GONE)
                }
            }
        }
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
