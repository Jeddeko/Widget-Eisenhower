package com.arena.eisenhower

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast

class SettingsActivity : Activity() {
    private lateinit var settings: AppSettings
    private var currentColorQ1 = 0; private var currentColorQ2 = 0; private var currentColorQ3 = 0; private var currentColorQ4 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = AppSettings(this)

        val etTitleQ1 = findViewById<EditText>(R.id.etTitleQ1); val etTitleQ2 = findViewById<EditText>(R.id.etTitleQ2)
        val etTitleQ3 = findViewById<EditText>(R.id.etTitleQ3); val etTitleQ4 = findViewById<EditText>(R.id.etTitleQ4)
        val btnColorQ1 = findViewById<Button>(R.id.btnColorQ1); val btnColorQ2 = findViewById<Button>(R.id.btnColorQ2)
        val btnColorQ3 = findViewById<Button>(R.id.btnColorQ3); val btnColorQ4 = findViewById<Button>(R.id.btnColorQ4)
        val spinnerOrder = findViewById<Spinner>(R.id.spinnerOrder); val showCompleted = findViewById<CheckBox>(R.id.showCompleted)

        etTitleQ1.setText(settings.titleQ1); etTitleQ2.setText(settings.titleQ2); etTitleQ3.setText(settings.titleQ3); etTitleQ4.setText(settings.titleQ4)
        currentColorQ1 = settings.colorQ1; currentColorQ2 = settings.colorQ2; currentColorQ3 = settings.colorQ3; currentColorQ4 = settings.colorQ4

        actualizarColorBoton(btnColorQ1, currentColorQ1); actualizarColorBoton(btnColorQ2, currentColorQ2)
        actualizarColorBoton(btnColorQ3, currentColorQ3); actualizarColorBoton(btnColorQ4, currentColorQ4)

        val orderValues = listOf("1,2,3,4", "4,3,2,1", "2,1,4,3", "3,4,1,2")
        spinnerOrder.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Normal", "Invertido", "Cruzado", "Cruzado Inverso"))
        val currentOrderIndex = orderValues.indexOf(settings.quadrantOrder)
        if (currentOrderIndex >= 0) spinnerOrder.setSelection(currentOrderIndex)
        showCompleted.isChecked = settings.showCompleted

        btnColorQ1.setOnClickListener { showColorPicker(currentColorQ1) { c -> currentColorQ1 = c; actualizarColorBoton(btnColorQ1, c) } }
        btnColorQ2.setOnClickListener { showColorPicker(currentColorQ2) { c -> currentColorQ2 = c; actualizarColorBoton(btnColorQ2, c) } }
        btnColorQ3.setOnClickListener { showColorPicker(currentColorQ3) { c -> currentColorQ3 = c; actualizarColorBoton(btnColorQ3, c) } }
        btnColorQ4.setOnClickListener { showColorPicker(currentColorQ4) { c -> currentColorQ4 = c; actualizarColorBoton(btnColorQ4, c) } }

        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            settings.titleQ1 = etTitleQ1.text.toString().ifBlank { "Hazlo ya" }; settings.titleQ2 = etTitleQ2.text.toString().ifBlank { "Planifica" }
            settings.titleQ3 = etTitleQ3.text.toString().ifBlank { "Delega" }; settings.titleQ4 = etTitleQ4.text.toString().ifBlank { "Elimina" }
            settings.colorQ1 = currentColorQ1; settings.colorQ2 = currentColorQ2; settings.colorQ3 = currentColorQ3; settings.colorQ4 = currentColorQ4
            settings.quadrantOrder = orderValues[spinnerOrder.selectedItemPosition]; settings.showCompleted = showCompleted.isChecked
            EisenhowerWidgetProvider.updateAllWidgets(this)
            Toast.makeText(this, "Ajustes guardados", Toast.LENGTH_SHORT).show(); finish()
        }
    }

    private fun actualizarColorBoton(button: Button, color: Int) {
        val drawable = GradientDrawable().apply { setColor(color); cornerRadius = 16f }
        button.background = drawable
    }

    private fun showColorPicker(initialColor: Int, onColorSelected: (Int) -> Unit) {
        val colors = arrayOf(Color.parseColor("#DBEAFE"), Color.parseColor("#BFDBFE"), Color.parseColor("#93C5FD"), Color.parseColor("#60A5FA"), Color.parseColor("#FECACA"), Color.parseColor("#FED7AA"), Color.parseColor("#BBF7D0"), Color.parseColor("#E5E7EB"))
        val colorNames = arrayOf("Azul Claro", "Azul Medio", "Azul", "Azul Fuerte", "Rojo", "Naranja", "Verde", "Gris")
        val items = colorNames.mapIndexed { index, name -> SpannableString("   $name").apply { setSpan(BackgroundColorSpan(colors[index]), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) } }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Elige un color").setItems(items) { _, which -> onColorSelected(colors[which]) }.show()
    }
}
