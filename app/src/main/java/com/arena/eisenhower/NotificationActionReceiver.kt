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
                TaskRepository(context).addTask(title, Quadrant.DO_FIRST)
                EisenhowerWidgetProvider.updateAllWidgets(context)

                NotificationManagerCompat.from(context)
                    .cancel(NotificationHelper.NOTIFICATION_ID)
            }
        }
    }

    companion object {
        const val ACTION_NOTIFY_ADD = "com.arena.eisenhower.ACTION_NOTIFY_ADD"
        const val KEY_QUICK_TASK = "quick_task"
    }
}
