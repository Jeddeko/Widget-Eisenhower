package com.arena.eisenhower

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun schedule(context: Context, task: Task) {
        if (task.dueAt == null || task.done) return
        
        val delay = task.dueAt - System.currentTimeMillis()
        if (delay > 0) {
            val data = Data.Builder().putLong("TASK_ID", task.id).build()
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            // Reemplaza cualquier alarma anterior para esta misma tarea
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_${task.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
