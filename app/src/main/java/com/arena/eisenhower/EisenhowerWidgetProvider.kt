package com.arena.eisenhower

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class EisenhowerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId -> 
            updateWidget(context, appWidgetManager, appWidgetId)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.q1_tasks)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.q2_tasks)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.q3_tasks)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.q4_tasks)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE)

        if (taskId != -1L) {
            if (actionType == ACTION_TOGGLE) {
                TaskRepository(context).toggleTask(taskId)
                updateAllWidgets(context)
            } else if (actionType == ACTION_EDIT) {
                val editIntent = Intent(context, QuickAddActivity::class.java).apply {
                    putExtra(EXTRA_TASK_ID, taskId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(editIntent)
            }
            return
        }

        if (intent.action == ACTION_REFRESH) {
            updateAllWidgets(context)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_TOGGLE = "com.arena.eisenhower.ACTION_TOGGLE"
        const val ACTION_EDIT = "com.arena.eisenhower.ACTION_EDIT"
        const val ACTION_REFRESH = "com.arena.eisenhower.ACTION_REFRESH"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_QUADRANT = "extra_quadrant"
        const val EXTRA_ACTION_TYPE = "extra_action_type"

        private data class SlotViews(val cardId: Int, val titleId: Int, val listId: Int, val addId: Int)

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, EisenhowerWidgetProvider::class.java))
            ids.forEach { id -> 
                updateWidget(context, manager, id)
                manager.notifyAppWidgetViewDataChanged(id, R.id.q1_tasks)
                manager.notifyAppWidgetViewDataChanged(id, R.id.q2_tasks)
                manager.notifyAppWidgetViewDataChanged(id, R.id.q3_tasks)
                manager.notifyAppWidgetViewDataChanged(id, R.id.q4_tasks)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val settings = AppSettings(context)
            val repo = TaskRepository(context)
            
            val views = RemoteViews(context.packageName, R.layout.eisenhower_widget)
            views.setTextColor(R.id.widget_header, android.graphics.Color.parseColor("#0F172A"))
            views.setTextColor(R.id.widget_sync, android.graphics.Color.parseColor("#2563EB"))

            val mainIntent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            views.setOnClickPendingIntent(R.id.widget_header, PendingIntent.getActivity(context, 800, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val refreshIntent = Intent(context, EisenhowerWidgetProvider::class.java).apply { action = ACTION_REFRESH }
            views.setOnClickPendingIntent(R.id.widget_sync, PendingIntent.getBroadcast(context, 900, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

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

            val clickIntent = Intent(context, EisenhowerWidgetProvider::class.java)
            val clickPi = PendingIntent.getBroadcast(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

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
                
                val activeCount = repo.getAll().count { it.quadrant == quadrant && !it.done }
                views.setTextViewText(slot.titleId, "$title · $activeCount")
                views.setTextColor(slot.titleId, android.graphics.Color.parseColor("#0F172A"))
                
                // El texto del boton dice "+ Añadir", pero si hay completadas, avisa
                val completedCount = repo.getAll().count { it.quadrant == quadrant && it.done }
                if (completedCount > 0) {
                    views.setTextViewText(slot.addId, "+ Añadir (Completadas: $completedCount)")
                } else {
                    views.setTextViewText(slot.addId, "+ Añadir")
                }
                views.setTextColor(slot.addId, android.graphics.Color.parseColor("#0F172A"))

                val serviceIntent = Intent(context, WidgetService::class.java).apply {
                    putExtra("EXTRA_QUADRANT", quadrant.key)
                    data = Uri.parse("eisenhower://widget/${quadrant.key}")
                }
                views.setRemoteAdapter(slot.listId, serviceIntent)
                views.setPendingIntentTemplate(slot.listId, clickPi)

                val addIntent = Intent(context, QuickAddActivity::class.java).apply {
                    putExtra(EXTRA_QUADRANT, quadrant.key)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                views.setOnClickPendingIntent(slot.addId, PendingIntent.getActivity(context, 1000 + quadrant.ordinal, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }

            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
