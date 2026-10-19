package com.arena.eisenhower

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val context = applicationContext
        val repo = TaskRepository(context)
        val specificTaskId = inputData.getLong("TASK_ID", -1L)

        if (specificTaskId != -1L) {
            val task = repo.getAll().firstOrNull { it.id == specificTaskId }
            if (task != null && !task.done) {
                NotificationHelper.showReminder(context, listOf(task), 0)
            }
        }
        return Result.success()
    }
}
