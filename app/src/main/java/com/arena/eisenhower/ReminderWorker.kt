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
        val now = System.currentTimeMillis()
        val windowEnd = now + 12 * 60 * 60 * 1000L // Ventana de 12 horas

        val active = repo.getAll().filterNot { it.done }

        // Buscar tareas vencidas
        val overdue = active.filter {
            it.dueAt?.let { due -> due < now } == true
        }

        // Buscar tareas próximas a vencer
        val soon = active
            .filter {
                it.dueAt?.let { due -> due in now..windowEnd } == true
            }
            .sortedWith(compareBy { it.dueAt ?: Long.MAX_VALUE })

        // Si hay tareas pendientes, lanzar la notificación
        if (overdue.isNotEmpty() || soon.isNotEmpty()) {
            NotificationHelper.showReminder(
                context,
                soon,
                overdue.size
            )
        }

        return Result.success()
    }
}
