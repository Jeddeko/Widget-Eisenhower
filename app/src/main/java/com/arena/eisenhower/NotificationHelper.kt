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
    const val CHANNEL_ID = "eisenhower_reminders"
    const val NOTIFICATION_ID = 4100

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios Eisenhower",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tareas próximas, vencidas y captura rápida"
                enableVibration(true)
            }

            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showReminder(
        context: Context,
        soon: List<Task>,
        overdueCount: Int
    ) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_IMMUTABLE

        val contentIntent = Intent(
            context,
            MainActivity::class.java
        ).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        )

        val contentPi = PendingIntent.getActivity(
            context,
            500,
            contentIntent,
            flags
        )

        val remoteInput = RemoteInput.Builder(
            NotificationActionReceiver.KEY_QUICK_TASK
        )
            .setLabel("Tarea para Hazlo ya")
            .build()

        val addIntent = Intent(
            context,
            NotificationActionReceiver::class.java
        ).setAction(NotificationActionReceiver.ACTION_NOTIFY_ADD)

        val addPi = PendingIntent.getBroadcast(
            context,
            501,
            addIntent,
            flags
        )

        val addAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_input_add,
            "Añadir tarea",
            addPi
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        val summary = if (overdueCount > 0) {
            "$overdueCount vencidas · ${soon.size} próximas"
        } else {
            "${soon.size} tareas próximas"
        }

        val expanded = soon
            .take(4)
            .joinToString("\n") { "• ${it.title}" }
            .ifBlank { summary }

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_widget)
            .setContentTitle("Eisenhower: acción pendiente")
            .setContentText(summary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expanded)
            )
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(addAction)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notification)
    }
}
