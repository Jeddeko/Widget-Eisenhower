package com.arena.eisenhower

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private lateinit var settings: AppSettings

    private var currentColorQ1 = 0
    private var currentColorQ2 = 0
    private var currentColorQ3 = 0
    private var currentColorQ4 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings = AppSettings(this)

        // Referencias a los campos de texto
        val etTitleQ1 = findViewById<EditText>(R.id.etTitleQ1)
        val etTitleQ2 = findViewById<EditText>(R.id.etTitleQ2)
        val etTitleQ3 = findViewById<EditText>(R.id.etTitleQ3)
        val etTitleQ4 = findViewById<EditText>(R.id.etTitleQ4)
        
        // Referencias a los botones de colores
        val btnColorQ1 = findViewById<Button>(R.id.btnColorQ1)
        val btnColorQ2 = findViewById<Button>(R.id.btnColorQ2)
        val btnColorQ3 = findViewById<Button>(R.id.btnColorQ3)
        val btnColorQ4 = findViewById<Button>(R.id.btnColorQ4)

        val spinnerOrder = findViewById<Spinner>(R.id.spinnerOrder)
        val showCompleted = findViewById<CheckBox>(R.id.showCompleted)
        val seekMax = findViewById<SeekBar>(R.id.seekMax)
        val seekLabel = findViewById<TextView>(R.id.seekLabel)
        val save = findViewById<Button>(R.id.btnSaveSettings)

        // --- Cargar valores actuales ---
        etTitleQ1.setText(settings.titleQ1)
        etTitleQ2.setText(settings.titleQ2)
        etTitleQ3.setText(settings.titleQ3)
        etTitleQ4.setText(settings.titleQ4)

        currentColorQ1 = settings.colorQ1
        currentColorQ2 = settings.colorQ2
        currentColorQ3 = settings.colorQ3
        currentColorQ4 = settings.colorQ4

        actualizarColorBoton(btnColorQ1, currentColorQ1)
        actualizarColorBoton(btnColorQ2, currentColorQ2)
        actualizarColorBoton(btnColorQ3, currentColorQ3)
        actualizarColorBoton(btnColorQ4, currentColorQ4)

        // Configurar Spinner de orden
        val orderOptions = listOf("Normal (1-2-3-4)", "Invertido (4-3-2-1)", "Cruzado (2-1-4-3)", "Cruzado Inverso (3-4-1-2)")
        val orderValues = listOf("1,2,3,4", "4,3,2,1", "2,1,4,3", "3,4,1,2")
        spinnerOrder.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, orderOptions)
        
        val currentOrderIndex = orderValues.indexOf(settings.quadrantOrder)
        if (currentOrderIndex >= 0) {
            spinnerOrder.setSelection(currentOrderIndex)
        }

        showCompleted.isChecked = settings.showCompleted
        seekMax.progress = (settings.maxPerQuadrant - 1).coerceIn(0, 4)

        fun updateLabel() {
            seekLabel.text = "Máximo de tareas visibles por cuadrante: ${seekMax.progress + 1}"
        }
        updateLabel()

        seekMax.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { updateLabel() }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        // --- Acciones de los botones de color ---
        btnColorQ1.setOnClickListener { showColorPicker(currentColorQ1) { color -> currentColorQ1 = color; actualizarColorBoton(btnColorQ1, color) } }
        btnColorQ2.setOnClickListener { showColorPicker(currentColorQ2) { color -> currentColorQ2 = color; actualizarColorBoton(btnColorQ2, color) } }
        btnColorQ3.setOnClickListener { showColorPicker(currentColorQ3) { color -> currentColorQ3 = color; actualizarColorBoton(btnColorQ3, color) } }
        btnColorQ4.setOnClickListener { showColorPicker(currentColorQ4) { color -> currentColorQ4 = color; actualizarColorBoton(btnColorQ4, color) } }

        // --- Guardar ---
        save.setOnClickListener {
            settings.titleQ1 = etTitleQ1.text.toString().ifBlank { "Hazlo ya" }
            settings.titleQ2 = etTitleQ2.text.toString().ifBlank { "Planifica" }
            settings.titleQ3 = etTitleQ3.text.toString().ifBlank { "Delega" }
            settings.titleQ4 = etTitleQ4.text.toString().ifBlank { "Elimina" }

            settings.colorQ1 = currentColorQ1
            settings.colorQ2 = currentColorQ2
            settings.colorQ3 = currentColorQ3
            settings.colorQ4 = currentColorQ4

            settings.quadrantOrder = orderValues[spinnerOrder.selectedItemPosition]
            settings.showCompleted = showCompleted.isChecked
            settings.maxPerQuadrant = seekMax.progress + 1

            EisenhowerWidgetProvider.updateAllWidgets(this)

            Toast.makeText(this, "Ajustes guardados y widget actualizado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun actualizarColorBoton(button: Button, color: Int) {
        // Pintamos un cuadradito del color elegido al lado del texto del botón
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.setColor(color)
        drawable.setCornerRadius(8f)
        button.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        button.compoundDrawablePadding = 20
    }

    private fun showColorPicker(initialColor: Int, onColorSelected: (Int) -> Unit) {
        val colors = arrayOf(
            Color.parseColor("#FECACA"), // Rojo pastel
            Color.parseColor("#FED7AA"), // Naranja pastel
            Color.parseColor("#BFDBFE"), // Azul pastel
            Color.parseColor("#BBF7D0"), // Verde pastel
            Color.parseColor("#E9D5FF"), // Morado pastel
            Color.parseColor("#FDE68A"), // Amarillo pastel
            Color.parseColor("#FFFFFF"), // Blanco
            Color.parseColor("#E5E7EB")  // Gris
        )
        val colorNames = arrayOf("Rojo", "Naranja", "Azul", "Verde", "Morado", "Amarillo", "Blanco", "Gris")

        AlertDialog.Builder(this)
            .setTitle("Elige un color de fondo")
            .setItems(colorNames) { _, which ->
                onColorSelected(colors[which])
            }
            .show()
    }
}
