package com.arena.eisenhower

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat

object NotificationHelper {
    const val CHANNEL_ID = "eisenhower_reminders_v2"
    const val NOTIFICATION_ID = 4100

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, 
                "Recordatorios Eisenhower", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tareas próximas y captura rápida"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    // Función auxiliar para saber qué nombre tiene el cuadrante
    private fun getQuadrantName(settings: AppSettings, quadrant: Quadrant): String {
        return when (quadrant) {
            Quadrant.DO_FIRST -> settings.titleQ1
            Quadrant.SCHEDULE -> settings.titleQ2
            Quadrant.DELEGATE -> settings.titleQ3
            Quadrant.ELIMINATE -> settings.titleQ4
        }
    }

    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, soon: List<Task>, overdueCount: Int) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        
        val settings = AppSettings(context)
        val contentIntent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentPi = PendingIntent.getActivity(context, 500, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_QUICK_TASK).setLabel("Tarea rápida").build()
        val addIntent = Intent(context, NotificationActionReceiver::class.java).setAction(NotificationActionReceiver.ACTION_NOTIFY_ADD)
        
        val addPi = PendingIntent.getBroadcast(context, 501, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val addAction = NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, "Añadir", addPi).addRemoteInput(remoteInput).build()

        val title: String
        val text: String
        val expanded: String

        // Si la alarma es de UNA sola tarea (como cuando suena a su hora)
        if (soon.size == 1) {
            val task = soon[0]
            val qName = getQuadrantName(settings, task.quadrant)
            title = task.title
            text = "Cuadrante: $qName"
            expanded = "Tarea pendiente en el cuadrante $qName:\n• ${task.title}"
        } else {
            // Si es un resumen de varias tareas (el vigilante de respaldo)
            title = "${soon.size} tareas próximas"
            text = if (overdueCount > 0) "$overdueCount vencidas · Toca para ver" else "Toca para ver los detalles"
            expanded = soon.take(4).joinToString("\n") { 
                "• ${it.title} (${getQuadrantName(settings, it.quadrant)})" 
            }.ifBlank { title }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget)
            .setContentTitle(title) // El título de la notificación ahora será el nombre de la tarea
            .setContentText(text)   // El subtítulo será el cuadrante (Hazlo ya, Planifica, etc.)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(addAction)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
