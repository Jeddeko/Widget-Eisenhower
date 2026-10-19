package com.arena.eisenhower

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

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
    private val minCellHeight: Int
) : RemoteViewsService.RemoteViewsFactory {

    private data class CalendarEventItem(
        val id: Long,
        val title: String,
        val color: Int,
        val isReminder: Boolean,
        val timePrefix: String = ""
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
        val itemsByDay = mutableMapOf<LocalDate, MutableList<CalendarEventItem>>()
        val settings = AppSettings(context)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        TaskRepository(context).getAll().forEach { task ->
            if (task.done) return@forEach 
            
            val qColor = when (task.quadrant) {
                Quadrant.DO_FIRST -> settings.colorQ1
                Quadrant.SCHEDULE -> settings.colorQ2
                Quadrant.DELEGATE -> settings.colorQ3
                Quadrant.ELIMINATE -> settings.colorQ4
            }

            task.dueAt?.let { due ->
                val date = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
                itemsByDay.getOrPut(date) { mutableListOf() }.add(
                    CalendarEventItem(task.id, task.title, qColor, isReminder = false)
                )
            }

            task.reminders.forEach { reminderTime ->
                val remDate = Instant.ofEpochMilli(reminderTime).atZone(ZoneId.systemDefault()).toLocalDate()
                val timeStr = timeFormat.format(Date(reminderTime))
                itemsByDay.getOrPut(remDate) { mutableListOf() }.add(
                    CalendarEventItem(task.id, task.title, qColor, isReminder = true, timePrefix = "🔔 $timeStr - ")
                )
            }
        }

        val firstOfMonth = LocalDate.of(year, month + 1, 1)
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

        for (i in 0 until 7) {
            val cell = week[i]
            val cellId = context.resources.getIdentifier("cell_${i+1}", "id", context.packageName)
            val numId = context.resources.getIdentifier("day_${i+1}_number", "id", context.packageName)

            // FORZAMOS LA ALTURA CALCULADA (Si el widget es grande, llenará el espacio)
            rv.setInt(cellId, "setMinimumHeight", minCellHeight)

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

            for (t in 1..15) {
                val taskId = context.resources.getIdentifier("task_${i+1}_$t", "id", context.packageName)
                if (t <= cell.items.size) {
                    val item = cell.items[t - 1]
                    rv.setViewVisibility(taskId, View.VISIBLE)
                    
                    val displayText = if (item.isReminder) {
                        item.timePrefix + item.title
                    } else {
                        item.title
                    }
                    
                    rv.setTextViewText(taskId, displayText)
                    rv.setInt(taskId, "setBackgroundColor", item.color)
                    rv.setTextColor(taskId, if (isColorDark(item.color)) Color.WHITE else Color.BLACK)

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
