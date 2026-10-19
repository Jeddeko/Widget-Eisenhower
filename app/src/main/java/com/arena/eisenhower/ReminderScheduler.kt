package com.arena.eisenhower

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun schedule(context: Context, task: Task) {
        if (task.done) return
        val now = System.currentTimeMillis()
        val wm = WorkManager.getInstance(context)
        
        // Cancelar alarmas anteriores de esta tarea
        wm.cancelAllWorkByTag("task_${task.id}")

        // Programar cada recordatorio de la lista
        task.reminders.forEach { time ->
            if (time > now) {
                val data = Data.Builder().putLong("TASK_ID", task.id).build()
                val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(time - now, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("task_${task.id}")
                    .build()
                wm.enqueue(request)
            }
        }
    }

    fun cancel(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("task_$taskId")
    }
}
