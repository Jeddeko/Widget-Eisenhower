package com.arena.eisenhower

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_NOTIFY_ADD) {
            val results = RemoteInput.getResultsFromIntent(intent)
            val title = results
                ?.getCharSequence(KEY_QUICK_TASK)
                ?.toString()
                ?.trim()
                .orEmpty()

            if (title.isNotBlank()) {
                // Usamos la nueva firma addTask(Task)
                val newTask = repoAddTask(context, title)
                if (newTask != null) {
                    ReminderScheduler.schedule(context, newTask)
                }
                EisenhowerWidgetProvider.updateAllWidgets(context)
                CalendarWidgetProvider.updateAllWidgets(context)

                NotificationManagerCompat.from(context)
                    .cancel(NotificationHelper.NOTIFICATION_ID)
            }
        }
    }

    private fun repoAddTask(context: Context, title: String): Task? {
        val repo = TaskRepository(context)
        return repo.addTask(
            Task(
                id = 0, 
                title = title, 
                quadrant = Quadrant.DO_FIRST,
                isEvent = false
            )
        )
    }

    companion object {
        const val ACTION_NOTIFY_ADD = "com.arena.eisenhower.ACTION_NOTIFY_ADD"
        const val KEY_QUICK_TASK = "quick_task"
    }
}
