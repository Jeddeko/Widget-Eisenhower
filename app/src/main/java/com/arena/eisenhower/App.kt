package com.arena.eisenhower

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        // Se eliminó el Worker periódico inútil. ReminderScheduler ya maneja las alarmas exactas.
    }
}
