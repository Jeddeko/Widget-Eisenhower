package com.arena.eisenhower

import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val backgroundColor: Int,
    private val onCheckClick: (Task) -> Unit,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val check: CheckBox = view.findViewById(R.id.rowCheck)
        val title: TextView = view.findViewById(R.id.rowTitle)
        val meta: TextView = view.findViewById(R.id.rowMeta)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_task_main, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        
        // Aplicar color de fondo con bordes redondeados
        val drawable = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 12f
        }
        holder.itemView.background = drawable
        
        // Margen entre tarjetas
        val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
        params.setMargins(0, 0, 0, 8)
        holder.itemView.layoutParams = params

        holder.check.isChecked = task.done
        val titleText = SpannableString(task.title)
        if (task.done) titleText.setSpan(StrikethroughSpan(), 0, titleText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        holder.title.text = titleText
        holder.meta.text = "Orden: ${task.order} · ${DueDateUtils.format(task.dueAt)}"
        holder.btnDelete.visibility = View.GONE // Ocultamos el botón de borrar para limpiar la vista

        holder.check.setOnClickListener { onCheckClick(task) }
        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount(): Int = tasks.size
}
