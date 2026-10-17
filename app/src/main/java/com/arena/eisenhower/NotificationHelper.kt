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
            val channel = NotificationChannel(CHANNEL_ID, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, soon: List<Task>, overdueCount: Int) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val contentPi = PendingIntent.getActivity(context, 500, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_QUICK_TASK).setLabel("Tarea rápida").build()
        
        // AQUI ESTABA EL CRASH: Flag mutable requerido para RemoteInput
        val addPi = PendingIntent.getBroadcast(context, 501, Intent(context, NotificationActionReceiver::class.java).setAction(NotificationActionReceiver.ACTION_NOTIFY_ADD), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val addAction = NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, "Añadir", addPi).addRemoteInput(remoteInput).build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget).setContentTitle("Eisenhower: acción pendiente")
            .setContentText("Tareas próximas").setContentIntent(contentPi).setAutoCancel(true).addAction(addAction).build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
