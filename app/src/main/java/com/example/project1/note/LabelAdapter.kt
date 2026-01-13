package com.example.project1.note

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project1.R
import com.example.project1.model.Label

class LabelAdapter(
    private var labels: MutableList<Label>,
    private val onLabelClick: (Label) -> Unit,
    private val onLabelLongClick: (Label) -> Unit // Tambahkan callback untuk klik lama
) : RecyclerView.Adapter<LabelAdapter.LabelViewHolder>() {

    class LabelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvLabelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_label_drawer, parent, false)
        return LabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val label = labels[position]
        holder.name.text = label.name
        holder.itemView.setOnClickListener { onLabelClick(label) }
        
        // Setup Klik Lama
        holder.itemView.setOnLongClickListener {
            onLabelLongClick(label)
            true
        }
    }

    override fun getItemCount() = labels.size

    fun updateLabels(newLabels: List<Label>) {
        this.labels.clear()
        this.labels.addAll(newLabels)
        notifyDataSetChanged()
    }
}