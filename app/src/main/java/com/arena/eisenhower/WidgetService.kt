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

    override fun onCreate() {}
    override fun onDataSetChanged() {
        val repo = TaskRepository(context)
        val settings = AppSettings(context)
        tasks = repo.getAll()
            .filter { it.quadrant == quadrant && (settings.showCompleted || !it.done) }
            .sortedBy { it.order }
    }
    override fun onDestroy() {}
    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val task = tasks[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_task_row)
        
        val prefix = when {
            task.done -> "✓ "
            task.dueAt != null && task.dueAt < System.currentTimeMillis() -> "⚠ "
            else -> ""
        }
        rv.setTextViewText(R.id.row_title, prefix + task.title)
        rv.setTextViewText(R.id.row_check, if (task.done) "☑" else "□")
        rv.setTextColor(R.id.row_check, if (task.done) Color.parseColor("#2563EB") else Color.parseColor("#475569"))
        rv.setTextColor(R.id.row_title, if (task.done) Color.parseColor("#94A3B8") else Color.parseColor("#0F172A"))

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
