package com.example.project1.note

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.project1.R
import com.example.project1.model.Note
import com.example.project1.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class NoteAdapter(
    private var notes: MutableList<Note>,
    private val scope: CoroutineScope,
    private val database: AppDatabase,
    private val onNoteAction: () -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvItemTitle)
        val content: TextView = view.findViewById(R.id.tvItemContent)
        val cardNote: androidx.cardview.widget.CardView = view.findViewById(R.id.cardNote)
        val checkIcon: ImageView = view.findViewById(R.id.btnToggleChecklist)
        val reminderContainer: View = view.findViewById(R.id.reminderInfoContainer)
        val tvReminderTime: TextView = view.findViewById(R.id.tvItemReminderTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.title.text = note.title
        
        val fullData = note.content
        if (fullData.contains("|||")) {
            val parts = fullData.split("|||")
            val plainText = parts[0]
            val metadata = if (parts.size > 1) parts[1] else ""
            val spannable = SpannableStringBuilder(plainText)
            applySerializedSpans(spannable, metadata)
            holder.content.text = spannable
        } else {
            holder.content.text = fullData
        }

        if (note.reminderTime != null) {
            holder.reminderContainer.visibility = View.VISIBLE
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = note.reminderTime
            val formattedDate = DateFormat.format("dd MMM, HH:mm", calendar).toString()
            holder.tvReminderTime.text = formattedDate
        } else {
            holder.reminderContainer.visibility = View.GONE
        }

        if (note.color != -1) {
            holder.cardNote.setCardBackgroundColor(note.color)
        } else {
            holder.cardNote.setCardBackgroundColor(Color.WHITE)
        }

        holder.checkIcon.alpha = if (note.isChecked) 1.0f else 0.3f
        
        holder.cardNote.setOnClickListener {
            if (!note.isDeleted) {
                val intent = Intent(holder.itemView.context, AddNoteActivity::class.java)
                intent.putExtra("EXTRA_NOTE", note)
                holder.itemView.context.startActivity(intent)
            }
        }

        holder.cardNote.setOnLongClickListener {
            showContextMenu(holder.itemView, note)
            true
        }
    }

    private fun applySerializedSpans(spannable: Spannable, serialized: String) {
        if (serialized.isEmpty()) return
        serialized.split(";").forEach { part ->
            if (part.isEmpty()) return@forEach
            try {
                val data = part.split(":")
                val type = data[0]
                val params = data[1].split(",")
                val start = params[0].toInt()
                val end = params[1].toInt()
                if (start >= spannable.length || end > spannable.length) return@forEach
                when (type) {
                    "ST" -> spannable.setSpan(StyleSpan(params[2].toInt()), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "UN" -> spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "SZ" -> spannable.setSpan(AbsoluteSizeSpan(params[2].toInt(), true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "AL" -> spannable.setSpan(AlignmentSpan.Standard(Layout.Alignment.valueOf(params[2])), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } catch (e: Exception) {}
        }
    }

    private fun showContextMenu(view: View, note: Note) {
        val options = mutableListOf<String>()
        if (note.isDeleted) {
            options.add("Pulihkan")
            options.add("Hapus Permanen")
        } else {
            options.add(if (note.reminderTime != null) "Ubah Pengingat" else "Tambah Pengingat")
            if (note.reminderTime != null) options.add("Hapus Pengingat")
            options.add(if (note.isArchived) "Keluarkan dari Arsip" else "Arsipkan")
            options.add("Hapus ke Sampah")
            options.add(if (note.isChecked) "Matikan Ceklis" else "Nyalakan Ceklis")
        }
        
        AlertDialog.Builder(view.context)
            .setTitle("Opsi Catatan")
            .setItems(options.toTypedArray()) { _, which ->
                val selected = options[which]
                when (selected) {
                    "Tambah Pengingat", "Ubah Pengingat" -> showDateTimePicker(view, note)
                    "Hapus Pengingat" -> moveNote(note.copy(reminderTime = null))
                    "Arsipkan" -> moveNote(note.copy(isArchived = true, isDeleted = false))
                    "Keluarkan dari Arsip", "Pulihkan" -> moveNote(note.copy(isArchived = false, isDeleted = false))
                    "Hapus ke Sampah" -> moveNote(note.copy(isDeleted = true, isArchived = false, reminderTime = null))
                    "Hapus Permanen" -> deletePermanent(note)
                    "Matikan Ceklis", "Nyalakan Ceklis" -> moveNote(note.copy(isChecked = !note.isChecked))
                }
            }
            .show()
    }

    private fun showDateTimePicker(view: View, note: Note) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(view.context, { _, year, month, day ->
            TimePickerDialog(view.context, { _, hour, minute ->
                val selectedTime = Calendar.getInstance()
                selectedTime.set(year, month, day, hour, minute)
                moveNote(note.copy(reminderTime = selectedTime.timeInMillis))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun moveNote(updatedNote: Note) {
        scope.launch(Dispatchers.IO) {
            database.appDao().updateNote(updatedNote)
            withContext(Dispatchers.Main) { onNoteAction() }
        }
    }

    private fun deletePermanent(note: Note) {
        scope.launch(Dispatchers.IO) {
            database.appDao().deleteNotePermanent(note)
            withContext(Dispatchers.Main) { onNoteAction() }
        }
    }

    override fun getItemCount() = notes.size

    fun updateNotes(newNotes: List<Note>) {
        this.notes.clear()
        this.notes.addAll(newNotes)
        notifyDataSetChanged()
    }
}