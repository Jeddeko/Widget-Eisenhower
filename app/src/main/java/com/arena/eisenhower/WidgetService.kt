package com.arena.eisenhower

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val quadrantKey = intent.getStringExtra("EXTRA_QUADRANT") ?: "do"
        return WidgetFactory(applicationContext, Quadrant.fromKey(quadrantKey))
    }
}

class WidgetFactory(private val context: Context, private val quadrant: Quadrant) : RemoteViewsService.RemoteViewsFactory {
    private var tasks: List<Task> = emptyList()
    private var quadrantColor: Int = Color.TRANSPARENT

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1.0 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness > 0.5
    }

    override fun onCreate() {}
    override fun onDataSetChanged() {
        val repo = TaskRepository(context)
        val settings = AppSettings(context)
        
        quadrantColor = when (quadrant) {
            Quadrant.DO_FIRST -> settings.colorQ1
            Quadrant.SCHEDULE -> settings.colorQ2
            Quadrant.DELEGATE -> settings.colorQ3
            Quadrant.ELIMINATE -> settings.colorQ4
        }

        tasks = repo.getAll().filter { it.quadrant == quadrant && !it.done }.sortedBy { it.order }
    }
    override fun onDestroy() {}
    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val task = tasks[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_task_row)
        
        val isBgDark = isColorDark(quadrantColor)
        val textColor = if (isBgDark) Color.WHITE else Color.BLACK

        val prefix = when {
            task.dueAt != null && task.dueAt < System.currentTimeMillis() -> "⚠ "
            else -> ""
        }
        rv.setTextViewText(R.id.row_title, prefix + task.title)
        rv.setTextViewText(R.id.row_check, "□")
        rv.setTextColor(R.id.row_check, textColor)
        rv.setTextColor(R.id.row_title, textColor)

        val toggleIntent = Intent().apply {
            putExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, task.id)
            putExtra(EisenhowerWidgetProvider.EXTRA_ACTION_TYPE, EisenhowerWidgetProvider.ACTION_TOGGLE)
        }
        rv.setOnClickFillInIntent(R.id.row_check, toggleIntent)

        val editIntent = Intent().apply {
            putExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, task.id)
            putExtra(EisenhowerWidgetProvider.EXTRA_ACTION_TYPE, EisenhowerWidgetProvider.ACTION_EDIT)
        }
        rv.setOnClickFillInIntent(R.id.row_title, editIntent)

        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = tasks[position].id
    override fun hasStableIds(): Boolean = true
}
