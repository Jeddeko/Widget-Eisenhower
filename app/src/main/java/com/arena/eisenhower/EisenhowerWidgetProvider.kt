package com.arena.eisenhower

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class EisenhowerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE -> {
                val id = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (id > 0L) {
                    TaskRepository(context).toggleTask(id)
                    updateAllWidgets(context)
                }
                return
            }
            ACTION_REFRESH -> {
                updateAllWidgets(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_TOGGLE = "com.arena.eisenhower.ACTION_TOGGLE"
        const val ACTION_REFRESH = "com.arena.eisenhower.ACTION_REFRESH"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_QUADRANT = "extra_quadrant"

        private data class SlotViews(
            val cardId: Int,
            val titleId: Int,
            val tasksId: Int,
            val addId: Int
        )

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, EisenhowerWidgetProvider::class.java)
            )
            ids.forEach { id ->
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val repo = TaskRepository(context)
            val settings = AppSettings(context)
            val now = System.currentTimeMillis()

            val allTasks = repo.getAll()
            val visible = allTasks.filter { settings.showCompleted || !it.done }

            val options = manager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 220)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val perQuadrantHeight = ((minHeight - 56).coerceAtLeast(80) / 2)
            val rowsByHeight = ((perQuadrantHeight - 40) / 28).coerceAtLeast(1)
            val rowsByWidth = if (minWidth < 160) 1 else 5
            val maxPer = minOf(settings.maxPerQuadrant, rowsByHeight, rowsByWidth).coerceAtLeast(1)

            val views = RemoteViews(context.packageName, R.layout.eisenhower_widget)
            views.setTextColor(R.id.widget_header, android.graphics.Color.parseColor("#0F172A"))
            views.setTextColor(R.id.widget_sync, android.graphics.Color.parseColor("#2563EB"))

            val refreshIntent = Intent(context, EisenhowerWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            views.setOnClickPendingIntent(
                R.id.widget_sync,
                PendingIntent.getBroadcast(
                    context, 900, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val slots = listOf(
                SlotViews(R.id.q1_card, R.id.q1_title, R.id.q1_tasks, R.id.q1_add),
                SlotViews(R.id.q2_card, R.id.q2_title, R.id.q2_tasks, R.id.q2_add),
                SlotViews(R.id.q3_card, R.id.q3_title, R.id.q3_tasks, R.id.q3_add),
                SlotViews(R.id.q4_card, R.id.q4_title, R.id.q4_tasks, R.id.q4_add)
            )

            val orderKeys = settings.quadrantOrder.split(",")
            val quadrantsToRender = orderKeys.map { key ->
                when (key.trim()) {
                    "1" -> Quadrant.DO_FIRST
                    "2" -> Quadrant.SCHEDULE
                    "3" -> Quadrant.DELEGATE
                    "4" -> Quadrant.ELIMINATE
                    else -> Quadrant.DO_FIRST
                }
            }

            slots.forEachIndexed { index, slot ->
                val quadrant = quadrantsToRender.getOrNull(index) ?: return@forEachIndexed
                
                val title = when (quadrant) {
                    Quadrant.DO_FIRST -> settings.titleQ1
                    Quadrant.SCHEDULE -> settings.titleQ2
                    Quadrant.DELEGATE -> settings.titleQ3
                    Quadrant.ELIMINATE -> settings.titleQ4
                }
                val color = when (quadrant) {
                    Quadrant.DO_FIRST -> settings.colorQ1
                    Quadrant.SCHEDULE -> settings.colorQ2
                    Quadrant.DELEGATE -> settings.colorQ3
                    Quadrant.ELIMINATE -> settings.colorQ4
                }

                views.setInt(slot.cardId, "setBackgroundColor", color)

                val tasks = visible
                    .filter { it.quadrant == quadrant }
                    .sortedWith(
                        compareBy<Task> { it.done }
                            .thenBy { it.dueAt ?: Long.MAX_VALUE }
                            .thenByDescending { it.createdAt }
                    )

                val pendingCount = tasks.count { !it.done }
                views.setTextViewText(slot.titleId, "$title · $pendingCount")
                views.setTextColor(slot.titleId, android.graphics.Color.parseColor("#0F172A"))
                views.setTextColor(slot.addId, android.graphics.Color.parseColor("#0F172A"))

                views.removeAllViews(slot.tasksId)

                if (tasks.isEmpty()) {
                    val empty = RemoteViews(context.packageName, R.layout.widget_empty_row)
                    empty.setTextViewText(R.id.empty_text, "—")
                    empty.setTextColor(R.id.empty_text, android.graphics.Color.parseColor("#64748B"))
                    views.addView(slot.tasksId, empty)
                } else {
                    tasks.take(maxPer).forEach { task ->
                        val row = RemoteViews(context.packageName, R.layout.widget_task_row)
                        val prefix = when {
                            task.done -> "✓ "
                            task.dueAt != null && task.dueAt < now -> "⚠ "
                            else -> ""
                        }
                        row.setTextViewText(R.id.row_title, prefix + task.title)
                        
                        // Usamos texto en lugar de CheckBox
                        row.setTextViewText(R.id.row_check, if (task.done) "☑" else "□")
                        row.setTextColor(R.id.row_check, if (task.done) android.graphics.Color.parseColor("#2563EB") else android.graphics.Color.parseColor("#475569"))
                        row.setTextColor(R.id.row_title, if (task.done) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#0F172A"))

                        val togglePi = togglePendingIntent(context, task.id)
                        row.setOnClickPendingIntent(R.id.row_check, togglePi)
                        row.setOnClickPendingIntent(R.id.row_root, togglePi)

                        views.addView(slot.tasksId, row)
                    }

                    val hidden = tasks.size - maxPer
                    if (hidden > 0) {
                        val more = RemoteViews(context.packageName, R.layout.widget_empty_row)
                        more.setTextViewText(R.id.empty_text, "+$hidden más")
                        more.setTextColor(R.id.empty_text, android.graphics.Color.parseColor("#2563EB"))
                        val mainIntent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        more.setOnClickPendingIntent(R.id.empty_text, PendingIntent.getActivity(context, 2000 + quadrant.ordinal, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                        views.addView(slot.tasksId, more)
                    }
                }

                val addIntent = Intent(context, QuickAddActivity::class.java).apply {
                    putExtra(EXTRA_QUADRANT, quadrant.key)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                views.setOnClickPendingIntent(slot.addId, PendingIntent.getActivity(context, 1000 + quadrant.ordinal, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }

            manager.updateAppWidget(appWidgetId, views)
        }

        private fun togglePendingIntent(context: Context, taskId: Long): PendingIntent {
            val intent = Intent(context, EisenhowerWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
                putExtra(EXTRA_TASK_ID, taskId)
            }
            return PendingIntent.getBroadcast(
                context, taskId.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
