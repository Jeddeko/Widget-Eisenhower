package com.arena.eisenhower

import android.content.Context
import android.graphics.Color

class AppSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("eisenhower_settings", Context.MODE_PRIVATE)

    // --- Nombres de los cuadrantes ---
    var titleQ1: String
        get() = prefs.getString("title_q1", "Hazlo ya") ?: "Hazlo ya"
        set(value) = prefs.edit().putString("title_q1", value).apply()

    var titleQ2: String
        get() = prefs.getString("title_q2", "Planifica") ?: "Planifica"
        set(value) = prefs.edit().putString("title_q2", value).apply()

    var titleQ3: String
        get() = prefs.getString("title_q3", "Delega") ?: "Delega"
        set(value) = prefs.edit().putString("title_q3", value).apply()

    var titleQ4: String
        get() = prefs.getString("title_q4", "Elimina") ?: "Elimina"
        set(value) = prefs.edit().putString("title_q4", value).apply()

    // --- Colores de fondo de los cuadrantes ---
    // Por defecto: Rojo, Naranja, Azul, Verde (pastel para legibilidad)
    var colorQ1: Int
        get() = prefs.getInt("color_q1", Color.parseColor("#FECACA"))
        set(value) = prefs.edit().putInt("color_q1", value).apply()

    var colorQ2: Int
        get() = prefs.getInt("color_q2", Color.parseColor("#FED7AA"))
        set(value) = prefs.edit().putInt("color_q2", value).apply()

    var colorQ3: Int
        get() = prefs.getInt("color_q3", Color.parseColor("#BFDBFE"))
        set(value) = prefs.edit().putInt("color_q3", value).apply()

    var colorQ4: Int
        get() = prefs.getInt("color_q4", Color.parseColor("#BBF7D0"))
        set(value) = prefs.edit().putInt("color_q4", value).apply()

    // --- Orden de los cuadrantes (por defecto 1234) ---
    // Se guardará como un string con los números del 1 al 4 en el orden elegido.
    var quadrantOrder: String
        get() = prefs.getString("quadrant_order", "1,2,3,4") ?: "1,2,3,4"
        set(value) = prefs.edit().putString("quadrant_order", value).apply()

    // --- Opciones generales ---
    var showCompleted: Boolean
        get() = prefs.getBoolean("show_completed", false)
        set(value) = prefs.edit().putBoolean("show_completed", value).apply()

    var maxPerQuadrant: Int
        get() = prefs.getInt("max_per_quadrant", 3).coerceIn(1, 5)
        set(value) = prefs.edit().putInt("max_per_quadrant", value.coerceIn(1, 5)).apply()
}
