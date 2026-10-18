package com.arena.eisenhower

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("eisenhower_settings", Context.MODE_PRIVATE)
    private val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    var titleQ1: String
        get() = prefs.getString("title_q1", "Hazlo ya") ?: "Hazlo ya"
        set(value) { prefs.edit().putString("title_q1", value).apply() }
    var titleQ2: String
        get() = prefs.getString("title_q2", "Planifica") ?: "Planifica"
        set(value) { prefs.edit().putString("title_q2", value).apply() }
    var titleQ3: String
        get() = prefs.getString("title_q3", "Delega") ?: "Delega"
        set(value) { prefs.edit().putString("title_q3", value).apply() }
    var titleQ4: String
        get() = prefs.getString("title_q4", "Elimina") ?: "Elimina"
        set(value) { prefs.edit().putString("title_q4", value).apply() }

    // Colores oscuros (si el celular está en modo oscuro)
    private val defColorQ1 = if (isDarkMode) Color.parseColor("#7F1D1D") else Color.parseColor("#FECACA")
    private val defColorQ2 = if (isDarkMode) Color.parseColor("#78350F") else Color.parseColor("#FED7AA")
    private val defColorQ3 = if (isDarkMode) Color.parseColor("#1E3A8A") else Color.parseColor("#BFDBFE")
    private val defColorQ4 = if (isDarkMode) Color.parseColor("#14532D") else Color.parseColor("#BBF7D0")

    var colorQ1: Int
        get() = prefs.getInt("color_q1", defColorQ1)
        set(value) { prefs.edit().putInt("color_q1", value).apply() }
    var colorQ2: Int
        get() = prefs.getInt("color_q2", defColorQ2)
        set(value) { prefs.edit().putInt("color_q2", value).apply() }
    var colorQ3: Int
        get() = prefs.getInt("color_q3", defColorQ3)
        set(value) { prefs.edit().putInt("color_q3", value).apply() }
    var colorQ4: Int
        get() = prefs.getInt("color_q4", defColorQ4)
        set(value) { prefs.edit().putInt("color_q4", value).apply() }

    var quadrantOrder: String
        get() = prefs.getString("quadrant_order", "1,2,3,4") ?: "1,2,3,4"
        set(value) { prefs.edit().putString("quadrant_order", value).apply() }

    var showCompleted: Boolean
        get() = prefs.getBoolean("show_completed", false)
        set(value) { prefs.edit().putBoolean("show_completed", value).apply() }
}
