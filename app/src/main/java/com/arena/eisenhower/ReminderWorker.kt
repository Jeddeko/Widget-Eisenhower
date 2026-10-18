package com.arena.eisenhower

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val context = applicationContext
        val repo = TaskRepository(context)
        
        // Si viene un TASK_ID, es una alarma exacta
        val specificTaskId = inputData.getLong("TASK_ID", -1L)
        
        if (specificTaskId != -1L) {
            val task = repo.getAll().firstOrNull { it.id == specificTaskId }
            if (task != null && !task.done && task.dueAt != null && task.dueAt <= System.currentTimeMillis() + 60000) {
                NotificationHelper.showReminder(context, listOf(task), 0)
            }
        } else {
            // Lógica de respaldo (revisión periódica)
            val now = System.currentTimeMillis()
            val windowEnd = now + 12 * 60 * 60 * 1000L
            val active = repo.getAll().filterNot { it.done }

            val overdue = active.filter { it.dueAt?.let { due -> due < now } == true }
            val soon = active.filter { it.dueAt?.let { due -> due in now..windowEnd } == true }

            if (overdue.isNotEmpty() || soon.isNotEmpty()) {
                NotificationHelper.showReminder(context, soon, overdue.size)
            }
        }

        return Result.success()
    }
}
