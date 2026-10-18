package com.arena.eisenhower

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("eisenhower_settings", Context.MODE_PRIVATE)
    val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    var titleQ1: String get() = prefs.getString("title_q1", "Hazlo ya") ?: "Hazlo ya"; set(value) { prefs.edit().putString("title_q1", value).apply() }
    var titleQ2: String get() = prefs.getString("title_q2", "Planifica") ?: "Planifica"; set(value) { prefs.edit().putString("title_q2", value).apply() }
    var titleQ3: String get() = prefs.getString("title_q3", "Delega") ?: "Delega"; set(value) { prefs.edit().putString("title_q3", value).apply() }
    var titleQ4: String get() = prefs.getString("title_q4", "Elimina") ?: "Elimina"; set(value) { prefs.edit().putString("title_q4", value).apply() }

    // AHORA GUARDAMOS EL ÍNDICE DE LA PALETA (0 al 7)
    var colorIndexQ1: Int get() = prefs.getInt("color_idx_q1", 0); set(value) { prefs.edit().putInt("color_idx_q1", value).apply() }
    var colorIndexQ2: Int get() = prefs.getInt("color_idx_q2", 1); set(value) { prefs.edit().putInt("color_idx_q2", value).apply() }
    var colorIndexQ3: Int get() = prefs.getInt("color_idx_q3", 2); set(value) { prefs.edit().putInt("color_idx_q3", value).apply() }
    var colorIndexQ4: Int get() = prefs.getInt("color_idx_q4", 3); set(value) { prefs.edit().putInt("color_idx_q4", value).apply() }

    // Esta función decide el color real según el modo del celular
    fun getColorForIndex(index: Int): Int {
        val lightColors = arrayOf(
            "#FECACA", "#FED7AA", "#BFDBFE", "#BBF7D0", // Rojo, Naranja, Azul, Verde (Pastel)
            "#E9D5FF", "#E5E7EB", "#FDE68A", "#FFFFFF"  // Morado, Gris, Amarillo, Blanco
        )
        val darkColors = arrayOf(
            "#7F1D1D", "#78350F", "#1E3A8A", "#14532D", // Rojo, Naranja, Azul, Verde (Profundos)
            "#4C1D95", "#374151", "#78350F", "#1F2937"  // Morado, Gris, Naranja oscuro, Pizarra
        )
        return Color.parseColor(if (isDarkMode) darkColors[index] else lightColors[index])
    }

    // Propiedades de solo lectura para obtener el color actual
    val colorQ1: Int get() = getColorForIndex(colorIndexQ1)
    val colorQ2: Int get() = getColorForIndex(colorIndexQ2)
    val colorQ3: Int get() = getColorForIndex(colorIndexQ3)
    val colorQ4: Int get() = getColorForIndex(colorIndexQ4)

    var quadrantOrder: String get() = prefs.getString("quadrant_order", "1,2,3,4") ?: "1,2,3,4"; set(value) { prefs.edit().putString("quadrant_order", value).apply() }
    var showCompleted: Boolean get() = prefs.getBoolean("show_completed", false); set(value) { prefs.edit().putBoolean("show_completed", value).apply() }
}
