package com.example.project1.note

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.project1.R
import com.example.project1.model.ChecklistItem

class ChecklistAdapter(
    private var items: MutableList<ChecklistItem>,
    private val onItemsChanged: () -> Unit // Callback untuk update UI induk
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    class ChecklistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbItem: CheckBox = view.findViewById(R.id.cbItem)
        val etText: EditText = view.findViewById(R.id.etChecklistText)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemoveItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val item = items[position]
        
        holder.cbItem.setOnCheckedChangeListener(null) // Reset listener agar tidak terpicu saat binding
        holder.cbItem.isChecked = item.isChecked
        holder.etText.setText(item.text)

        // Gaya teks coret jika sudah dicheck (mirip Keep)
        updateTextStyle(holder.etText, item.isChecked)

        holder.cbItem.setOnCheckedChangeListener { _, isChecked ->
            item.isChecked = isChecked
            updateTextStyle(holder.etText, isChecked)
            onItemsChanged()
        }

        holder.etText.addTextChangedListener {
            item.text = it.toString()
        }

        holder.btnRemove.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                items.removeAt(currentPos)
                notifyItemRemoved(currentPos)
                onItemsChanged()
            }
        }
    }

    private fun updateTextStyle(editText: EditText, isChecked: Boolean) {
        if (isChecked) {
            editText.paintFlags = editText.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            editText.alpha = 0.5f
        } else {
            editText.paintFlags = editText.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            editText.alpha = 1.0f
        }
    }

    override fun getItemCount() = items.size

    fun getItems() = items

    fun updateItems(newItems: List<ChecklistItem>) {
        this.items.clear()
        this.items.addAll(newItems)
        notifyDataSetChanged()
    }
}