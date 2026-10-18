package com.arena.eisenhower

import android.app.Activity
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : Activity() {
    private lateinit var settings: AppSettings
    private var currentColorQ1 = 0; private var currentColorQ2 = 0; private var currentColorQ3 = 0; private var currentColorQ4 = 0
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = AppSettings(this)
        mAuth = FirebaseAuth.getInstance()

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")

        // Referencias de Login
        val layoutLogin = findViewById<LinearLayout>(R.id.layoutLogin)
        val layoutSync = findViewById<LinearLayout>(R.id.layoutSync)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnSyncNow = findViewById<MaterialButton>(R.id.btnSyncNow)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)

        fun updateAuthUI() {
            if (mAuth.currentUser != null) {
                layoutLogin.visibility = View.GONE
                layoutSync.visibility = View.VISIBLE
                tvUserEmail.text = "Conectado como: ${mAuth.currentUser?.email}"
            } else {
                layoutLogin.visibility = View.VISIBLE
                layoutSync.visibility = View.GONE
            }
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            if (email.isBlank() || pass.length < 6) {
                Toast.makeText(this, "Correo válido y contraseña de mín. 6 caracteres", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Usuario registrado", Toast.LENGTH_SHORT).show()
                    updateAuthUI()
                    CloudSync.uploadTasks(this)
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            if (email.isBlank() || pass.isBlank()) {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Sesión iniciada", Toast.LENGTH_SHORT).show()
                    updateAuthUI()
                    CloudSync.downloadTasks(this) {
                        runOnUiThread { Toast.makeText(this, "Tareas sincronizadas", Toast.LENGTH_SHORT).show() }
                    }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnSyncNow.setOnClickListener {
            CloudSync.uploadTasks(this)
            CloudSync.downloadTasks(this) {
                runOnUiThread { Toast.makeText(this, "Sincronizado con la nube", Toast.LENGTH_SHORT).show() }
            }
        }

        btnLogout.setOnClickListener {
            mAuth.signOut()
            updateAuthUI()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        }

        updateAuthUI()

        // --- Resto de la lógica de Ajustes ---
        val etTitleQ1 = findViewById<EditText>(R.id.etTitleQ1); val etTitleQ2 = findViewById<EditText>(R.id.etTitleQ2)
        val etTitleQ3 = findViewById<EditText>(R.id.etTitleQ3); val etTitleQ4 = findViewById<EditText>(R.id.etTitleQ4)
        val btnColorQ1 = findViewById<Button>(R.id.btnColorQ1); val btnColorQ2 = findViewById<Button>(R.id.btnColorQ2)
        val btnColorQ3 = findViewById<Button>(R.id.btnColorQ3); val btnColorQ4 = findViewById<Button>(R.id.btnColorQ4)
        val spinnerOrder = findViewById<Spinner>(R.id.spinnerOrder); val showCompleted = findViewById<CheckBox>(R.id.showCompleted)
        val btnTestNotify = findViewById<MaterialButton>(R.id.btnTestNotifySettings)

        etTitleQ1.setText(settings.titleQ1); etTitleQ2.setText(settings.titleQ2); etTitleQ3.setText(settings.titleQ3); etTitleQ4.setText(settings.titleQ4)
        currentColorQ1 = settings.colorQ1; currentColorQ2 = settings.colorQ2; currentColorQ3 = settings.colorQ3; currentColorQ4 = settings.colorQ4

        // Función para actualizar el botón y el color de su texto
        fun actualizarColorBoton(button: Button, color: Int) {
            val drawable = GradientDrawable().apply { setColor(color); cornerRadius = 16f }
            button.background = drawable
            
            // Calcular si el color es oscuro para poner el texto blanco o negro
            val darkness = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            button.setTextColor(if (darkness < 0.5) Color.WHITE else Color.BLACK)
        }

        actualizarColorBoton(btnColorQ1, currentColorQ1); actualizarColorBoton(btnColorQ2, currentColorQ2)
        actualizarColorBoton(btnColorQ3, currentColorQ3); actualizarColorBoton(btnColorQ4, currentColorQ4)

        val orderValues = listOf("1,2,3,4", "4,3,2,1", "2,1,4,3", "3,4,1,2")
        spinnerOrder.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Normal", "Invertido", "Cruzado", "Cruzado Inverso"))
        val currentOrderIndex = orderValues.indexOf(settings.quadrantOrder)
        if (currentOrderIndex >= 0) spinnerOrder.setSelection(currentOrderIndex)
        showCompleted.isChecked = settings.showCompleted

        // Paleta de colores dinámica
        btnColorQ1.setOnClickListener { showColorPicker(currentColorQ1, isDark) { c -> currentColorQ1 = c; actualizarColorBoton(btnColorQ1, c) } }
        btnColorQ2.setOnClickListener { showColorPicker(currentColorQ2, isDark) { c -> currentColorQ2 = c; actualizarColorBoton(btnColorQ2, c) } }
        btnColorQ3.setOnClickListener { showColorPicker(currentColorQ3, isDark) { c -> currentColorQ3 = c; actualizarColorBoton(btnColorQ3, c) } }
        btnColorQ4.setOnClickListener { showColorPicker(currentColorQ4, isDark) { c -> currentColorQ4 = c; actualizarColorBoton(btnColorQ4, c) } }

        btnTestNotify.setOnClickListener {
            val testTask = Task(id = -1L, title = "¡Prueba de notificación!", quadrant = Quadrant.DO_FIRST, dueAt = System.currentTimeMillis() + 3600000)
            NotificationHelper.showReminder(this, listOf(testTask), 0)
            Toast.makeText(this, "Notificación enviada", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            settings.titleQ1 = etTitleQ1.text.toString().ifBlank { "Hazlo ya" }; settings.titleQ2 = etTitleQ2.text.toString().ifBlank { "Planifica" }
            settings.titleQ3 = etTitleQ3.text.toString().ifBlank { "Delega" }; settings.titleQ4 = etTitleQ4.text.toString().ifBlank { "Elimina" }
            settings.colorQ1 = currentColorQ1; settings.colorQ2 = currentColorQ2; settings.colorQ3 = currentColorQ3; settings.colorQ4 = currentColorQ4
            settings.quadrantOrder = orderValues[spinnerOrder.selectedItemPosition]; settings.showCompleted = showCompleted.isChecked
            EisenhowerWidgetProvider.updateAllWidgets(this)
            Toast.makeText(this, "Ajustes guardados", Toast.LENGTH_SHORT).show(); finish()
        }
    }

    private fun showColorPicker(initialColor: Int, isDark: Boolean, onColorSelected: (Int) -> Unit) {
        // La paleta cambia según el modo del sistema
        val colors = if (isDark) {
            arrayOf(
                Color.parseColor("#7F1D1D"), // Rojo oscuro
                Color.parseColor("#78350F"), // Naranja oscuro
                Color.parseColor("#1E3A8A"), // Azul oscuro
                Color.parseColor("#14532D"), // Verde oscuro
                Color.parseColor("#312E81"), // Morado oscuro
                Color.parseColor("#000000"), // Negro
                Color.parseColor("#374151"), // Gris oscuro
                Color.parseColor("#1F2937")  // Pizarra
            )
        } else {
            arrayOf(
                Color.parseColor("#DBEAFE"), // Azul claro
                Color.parseColor("#BFDBFE"), // Azul medio
                Color.parseColor("#93C5FD"), // Azul
                Color.parseColor("#60A5FA"), // Azul fuerte
                Color.parseColor("#FECACA"), // Rojo
                Color.parseColor("#FED7AA"), // Naranja
                Color.parseColor("#BBF7D0"), // Verde
                Color.parseColor("#E5E7EB")  // Gris
            )
        }
        
        val colorNames = if (isDark) {
            arrayOf("Rojo", "Naranja", "Azul", "Verde", "Morado", "Negro", "Gris", "Pizarra")
        } else {
            arrayOf("Azul Claro", "Azul Medio", "Azul", "Azul Fuerte", "Rojo", "Naranja", "Verde", "Gris")
        }

        val items = colorNames.mapIndexed { index, name -> 
            SpannableString("   $name").apply { 
                setSpan(BackgroundColorSpan(colors[index]), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) 
            } 
        }.toTypedArray()
        
        AlertDialog.Builder(this).setTitle("Elige un color").setItems(items) { _, which -> onColorSelected(colors[which]) }.show()
    }
}
