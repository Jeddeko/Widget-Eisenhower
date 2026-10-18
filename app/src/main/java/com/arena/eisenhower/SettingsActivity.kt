package com.arena.eisenhower

import android.app.Activity
import android.app.AlertDialog
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
    private var currentIdxQ1 = 0
    private var currentIdxQ2 = 1
    private var currentIdxQ3 = 2
    private var currentIdxQ4 = 3
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = AppSettings(this)
        mAuth = FirebaseAuth.getInstance()

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

        val etTitleQ1 = findViewById<EditText>(R.id.etTitleQ1); val etTitleQ2 = findViewById<EditText>(R.id.etTitleQ2)
        val etTitleQ3 = findViewById<EditText>(R.id.etTitleQ3); val etTitleQ4 = findViewById<EditText>(R.id.etTitleQ4)
        val btnColorQ1 = findViewById<Button>(R.id.btnColorQ1); val btnColorQ2 = findViewById<Button>(R.id.btnColorQ2)
        val btnColorQ3 = findViewById<Button>(R.id.btnColorQ3); val btnColorQ4 = findViewById<Button>(R.id.btnColorQ4)
        val spinnerOrder = findViewById<Spinner>(R.id.spinnerOrder); val showCompleted = findViewById<CheckBox>(R.id.showCompleted)
        val btnTestNotify = findViewById<MaterialButton>(R.id.btnTestNotifySettings)

        etTitleQ1.setText(settings.titleQ1); etTitleQ2.setText(settings.titleQ2); etTitleQ3.setText(settings.titleQ3); etTitleQ4.setText(settings.titleQ4)
        
        // Cargar índices actuales
        currentIdxQ1 = settings.colorIndexQ1
        currentIdxQ2 = settings.colorIndexQ2
        currentIdxQ3 = settings.colorIndexQ3
        currentIdxQ4 = settings.colorIndexQ4

        fun actualizarColorBoton(button: Button, color: Int) {
            val drawable = GradientDrawable().apply { setColor(color); cornerRadius = 16f }
            button.background = drawable
            val darkness = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            button.setTextColor(if (darkness < 0.5) Color.WHITE else Color.BLACK)
        }

        actualizarColorBoton(btnColorQ1, settings.getColorForIndex(currentIdxQ1))
        actualizarColorBoton(btnColorQ2, settings.getColorForIndex(currentIdxQ2))
        actualizarColorBoton(btnColorQ3, settings.getColorForIndex(currentIdxQ3))
        actualizarColorBoton(btnColorQ4, settings.getColorForIndex(currentIdxQ4))

        val orderValues = listOf("1,2,3,4", "4,3,2,1", "2,1,4,3", "3,4,1,2")
        spinnerOrder.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Normal", "Invertido", "Cruzado", "Cruzado Inverso"))
        val currentOrderIndex = orderValues.indexOf(settings.quadrantOrder)
        if (currentOrderIndex >= 0) spinnerOrder.setSelection(currentOrderIndex)
        showCompleted.isChecked = settings.showCompleted

        // Abrir paleta con Índices
        btnColorQ1.setOnClickListener { showColorPicker(currentIdxQ1) { idx -> currentIdxQ1 = idx; actualizarColorBoton(btnColorQ1, settings.getColorForIndex(idx)) } }
        btnColorQ2.setOnClickListener { showColorPicker(currentIdxQ2) { idx -> currentIdxQ2 = idx; actualizarColorBoton(btnColorQ2, settings.getColorForIndex(idx)) } }
        btnColorQ3.setOnClickListener { showColorPicker(currentIdxQ3) { idx -> currentIdxQ3 = idx; actualizarColorBoton(btnColorQ3, settings.getColorForIndex(idx)) } }
        btnColorQ4.setOnClickListener { showColorPicker(currentIdxQ4) { idx -> currentIdxQ4 = idx; actualizarColorBoton(btnColorQ4, settings.getColorForIndex(idx)) } }

        btnTestNotify.setOnClickListener {
            val testTask = Task(id = -1L, title = "¡Prueba de notificación!", quadrant = Quadrant.DO_FIRST, dueAt = System.currentTimeMillis() + 3600000)
            NotificationHelper.showReminder(this, listOf(testTask), 0)
            Toast.makeText(this, "Notificación enviada", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            settings.titleQ1 = etTitleQ1.text.toString().ifBlank { "Hazlo ya" }; settings.titleQ2 = etTitleQ2.text.toString().ifBlank { "Planifica" }
            settings.titleQ3 = etTitleQ3.text.toString().ifBlank { "Delega" }; settings.titleQ4 = etTitleQ4.text.toString().ifBlank { "Elimina" }
            
            // Guardamos SOLO los índices, no los colores fijos
            settings.colorIndexQ1 = currentIdxQ1
            settings.colorIndexQ2 = currentIdxQ2
            settings.colorIndexQ3 = currentIdxQ3
            settings.colorIndexQ4 = currentIdxQ4
            
            settings.quadrantOrder = orderValues[spinnerOrder.selectedItemPosition]; settings.showCompleted = showCompleted.isChecked
            EisenhowerWidgetProvider.updateAllWidgets(this)
            Toast.makeText(this, "Ajustes guardados", Toast.LENGTH_SHORT).show(); finish()
        }
    }

    private fun showColorPicker(currentIdx: Int, onIndexSelected: (Int) -> Unit) {
        val colorNames = arrayOf("Rojo", "Naranja", "Azul", "Verde", "Morado", "Gris", "Amarillo", "Contraste")
        
        val settings = AppSettings(this)
        val items = colorNames.mapIndexed { index, name -> 
            SpannableString("   $name").apply { 
                setSpan(BackgroundColorSpan(settings.getColorForIndex(index)), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) 
            } 
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Elige un color")
            .setItems(items) { _, which -> onIndexSelected(which) }
            .show()
    }
}
