package com.arena.eisenhower

import android.content.Context
import android.graphics.Color

class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("eisenhower_settings", Context.MODE_PRIVATE)

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

    var colorQ1: Int
        get() = prefs.getInt("color_q1", Color.parseColor("#DBEAFE"))
        set(value) { prefs.edit().putInt("color_q1", value).apply() }
        
    var colorQ2: Int
        get() = prefs.getInt("color_q2", Color.parseColor("#BFDBFE"))
        set(value) { prefs.edit().putInt("color_q2", value).apply() }
        
    var colorQ3: Int
        get() = prefs.getInt("color_q3", Color.parseColor("#93C5FD"))
        set(value) { prefs.edit().putInt("color_q3", value).apply() }
        
    var colorQ4: Int
        get() = prefs.getInt("color_q4", Color.parseColor("#60A5FA"))
        set(value) { prefs.edit().putInt("color_q4", value).apply() }

    var quadrantOrder: String
        get() = prefs.getString("quadrant_order", "1,2,3,4") ?: "1,2,3,4"
        set(value) { prefs.edit().putString("quadrant_order", value).apply() }

    var showCompleted: Boolean
        get() = prefs.getBoolean("show_completed", false)
        set(value) { prefs.edit().putBoolean("show_completed", value).apply() }
}
