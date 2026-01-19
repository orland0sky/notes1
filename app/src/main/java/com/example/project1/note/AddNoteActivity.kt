package com.example.project1.note

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.text.Editable
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project1.R
import com.example.project1.data.AppDatabase
import com.example.project1.model.ChecklistItem
import com.example.project1.model.Note
import kotlinx.coroutines.launch
import java.util.*

class AddNoteActivity : AppCompatActivity() {

    private var existingNote: Note? = null
    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var isUndoRedoAction = false
    private var selectedColor: Int = -1
    private var currentLabel: String? = null
    private var reminderTimestamp: Long? = null
    private var noteType: String = "text"

    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var noteParentLayout: RelativeLayout
    private lateinit var cardInputContainer: CardView
    private lateinit var checklistContainer: LinearLayout
    private lateinit var rvChecklist: RecyclerView
    private lateinit var checklistAdapter: ChecklistAdapter

    private lateinit var checkedItemsContainer: LinearLayout
    private lateinit var tvCheckedCount: TextView
    private lateinit var rvCheckedItems: RecyclerView
    private lateinit var checkedListAdapter: ChecklistAdapter
    private var isCheckedExpanded = true
    private var selectedImageUri: String? = null
    private lateinit var imgNotePhoto: ImageView
    private var noteId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        imgNotePhoto = findViewById(R.id.imgNotePhoto)

        val uriString = intent.getStringExtra("EXTRA_IMAGE_URI")
        if (!uriString.isNullOrEmpty()) {
            selectedImageUri = uriString
            imgNotePhoto.visibility = View.VISIBLE
            imgNotePhoto.setImageURI(Uri.parse(uriString))
        }


        noteParentLayout = findViewById<RelativeLayout>(R.id.noteParentLayout)
        cardInputContainer = findViewById<CardView>(R.id.cardInputContainer)
        val bottomToolbar = findViewById<LinearLayout>(R.id.bottomToolbar)
        val database = AppDatabase.getDatabase(this)

        etNoteTitle = findViewById<EditText>(R.id.etNoteTitle)
        etNoteContent = findViewById<EditText>(R.id.etNoteContent)
        checklistContainer = findViewById<LinearLayout>(R.id.checklistContainer)
        rvChecklist = findViewById<RecyclerView>(R.id.rvChecklist)
        val btnAddChecklistItem = findViewById<LinearLayout>(R.id.btnAddChecklistItem)

        checkedItemsContainer = findViewById<LinearLayout>(R.id.checkedItemsContainer)
        tvCheckedCount = findViewById<TextView>(R.id.tvCheckedCount)
        rvCheckedItems = findViewById<RecyclerView>(R.id.rvCheckedItems)
        val btnExpandChecked = findViewById<ImageView>(R.id.btnExpandChecked)

        val btnUndo = findViewById<ImageView>(R.id.btnUndo)
        val btnRedo = findViewById<ImageView>(R.id.btnRedo)
        val btnSetReminder = findViewById<ImageView>(R.id.btnSetReminder)

        noteType = intent.getStringExtra("NOTE_TYPE") ?: "text"
        currentLabel = intent.getStringExtra("LABEL_NAME")

        // Setup Checklists
        checklistAdapter = ChecklistAdapter(mutableListOf()) { updateCheckedSummary() }
        rvChecklist.layoutManager = LinearLayoutManager(this)
        rvChecklist.adapter = checklistAdapter

        checkedListAdapter = ChecklistAdapter(mutableListOf()) { updateCheckedSummary() }
        rvCheckedItems.layoutManager = LinearLayoutManager(this)
        rvCheckedItems.adapter = checkedListAdapter

        btnAddChecklistItem.setOnClickListener {
            checklistAdapter.getItems().add(ChecklistItem("", false))
            checklistAdapter.notifyItemInserted(checklistAdapter.itemCount - 1)
            updateCheckedSummary()
        }

        btnExpandChecked.setOnClickListener {
            isCheckedExpanded = !isCheckedExpanded
            rvCheckedItems.visibility = if (isCheckedExpanded) View.VISIBLE else View.GONE
            btnExpandChecked.rotation = if (isCheckedExpanded) 0f else -90f
        }

        ViewCompat.setOnApplyWindowInsetsListener(noteParentLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (noteType == "checklist") {
            etNoteContent.visibility = View.GONE
            checklistContainer.visibility = View.VISIBLE
        }

        // Fix Deprecated getSerializableExtra
        existingNote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("EXTRA_NOTE", Note::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("EXTRA_NOTE") as? Note
        }

        if (existingNote != null) {
            noteType = existingNote?.type ?: "text"
            etNoteTitle.setText(existingNote?.title)

            if (noteType == "checklist") {
                etNoteContent.visibility = View.GONE
                checklistContainer.visibility = View.VISIBLE
                val rawContent = existingNote?.content ?: ""
                val unChecked = mutableListOf<ChecklistItem>()
                val checked = mutableListOf<ChecklistItem>()
                rawContent.split("\n").forEach { line ->
                    if (line.startsWith("[x] ")) checked.add(ChecklistItem(line.substring(4), true))
                    else if (line.startsWith("[ ] ")) unChecked.add(ChecklistItem(line.substring(4), false))
                }
                checklistAdapter.updateItems(unChecked)
                checkedListAdapter.updateItems(checked)
                updateCheckedSummary()
            } else {
                val fullData = existingNote?.content ?: ""
                if (fullData.contains("|||")) {
                    val parts = fullData.split("|||")
                    val text = parts[0]
                    val metadata = if (parts.size > 1) parts[1] else ""
                    val spannable = SpannableStringBuilder(text)
                    applySerializedSpans(spannable, metadata)
                    etNoteContent.setText(spannable)
                } else {
                    etNoteContent.setText(fullData)
                }
            }

            selectedColor = existingNote?.color ?: -1
            if (selectedColor != -1) {
                noteParentLayout.setBackgroundColor(selectedColor)
                cardInputContainer.setCardBackgroundColor(selectedColor)
            }
            currentLabel = existingNote?.label
            reminderTimestamp = existingNote?.reminderTime
        } else {
            // Push initial state for new note
            undoStack.push("|||")
        }

        // History Tracker
        etNoteContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUndoRedoAction && s != null) {
                    val state = s.toString() + "|||" + serializeSpans(s)
                    if (undoStack.isEmpty() || undoStack.peek() != state) {
                        undoStack.push(state)
                        redoStack.clear()
                    }
                }
            }
        })

        // --- FORMATTING TOOLS ---
        fun applySpan(span: Any) {
            val start = etNoteContent.selectionStart
            val end = etNoteContent.selectionEnd
            if (start != end) {
                etNoteContent.text.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                // Trigger history update manually for span changes
                val s = etNoteContent.text
                val state = s.toString() + "|||" + serializeSpans(s)
                undoStack.push(state)
                redoStack.clear()
            }
        }

        findViewById<View>(R.id.btnBold).setOnClickListener { applySpan(StyleSpan(Typeface.BOLD)) }
        findViewById<View>(R.id.btnItalic).setOnClickListener { applySpan(StyleSpan(Typeface.ITALIC)) }
        findViewById<View>(R.id.btnUnderline).setOnClickListener {
            val start = etNoteContent.selectionStart
            val end = etNoteContent.selectionEnd
            if (start != end) {
                val spannable = etNoteContent.text
                val spans = spannable.getSpans(start, end, UnderlineSpan::class.java)
                if (spans.isNotEmpty()) for (s in spans) spannable.removeSpan(s)
                else spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                val state = spannable.toString() + "|||" + serializeSpans(spannable)
                undoStack.push(state)
                redoStack.clear()
            }
        }

        findViewById<View>(R.id.btnH1).setOnClickListener {
            val start = etNoteContent.selectionStart
            val end = etNoteContent.selectionEnd
            if (start != end) {
                val spannable = etNoteContent.text
                spannable.getSpans(start, end, AbsoluteSizeSpan::class.java).forEach { spannable.removeSpan(it) }
                spannable.setSpan(AbsoluteSizeSpan(24, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                val state = spannable.toString() + "|||" + serializeSpans(spannable)
                undoStack.push(state)
                redoStack.clear()
            }
        }

        findViewById<View>(R.id.btnH2).setOnClickListener {
            val start = etNoteContent.selectionStart
            val end = etNoteContent.selectionEnd
            if (start != end) {
                val spannable = etNoteContent.text
                spannable.getSpans(start, end, AbsoluteSizeSpan::class.java).forEach { spannable.removeSpan(it) }
                spannable.setSpan(AbsoluteSizeSpan(18, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                val state = spannable.toString() + "|||" + serializeSpans(spannable)
                undoStack.push(state)
                redoStack.clear()
            }
        }

        findViewById<View>(R.id.btnAlignLeft).setOnClickListener { applySpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL)) }
        findViewById<View>(R.id.btnAlignCenter).setOnClickListener { applySpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER)) }
        findViewById<View>(R.id.btnAlignRight).setOnClickListener { applySpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE)) }

        // Color Logic
        fun updateBg(colorCode: String) {
            selectedColor = Color.parseColor(colorCode)
            noteParentLayout.setBackgroundColor(selectedColor)
            cardInputContainer.setCardBackgroundColor(selectedColor)
        }
        findViewById<View>(R.id.colorRed).setOnClickListener { updateBg("#FFCDD2") }
        findViewById<View>(R.id.colorBlue).setOnClickListener { updateBg("#BBDEFB") }
        findViewById<View>(R.id.colorGreen).setOnClickListener { updateBg("#C8E6C9") }
        findViewById<View>(R.id.colorPich).setOnClickListener { updateBg("#FBCCCC") }
        findViewById<ImageView>(R.id.btnColorWheel).setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.menu_color, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_pink -> updateBg("#F8BBD0")
                    R.id.menu_blue -> updateBg("#BBDEFB")
                    R.id.menu_green -> updateBg("#C8E6C9")
                    R.id.menu_purple -> updateBg("#E1BEE7")
                    R.id.menu_peach -> updateBg("#FFE0B2")
                    R.id.menu_mint -> updateBg("#B2DFDB")
                    R.id.menu_lavender -> updateBg("#D1C4E9")
                    R.id.menu_sky -> updateBg("#E3F2FD")
                    R.id.menu_cream -> updateBg("#FFF9C4")
                    R.id.menu_grey -> updateBg("#ECEFF1")
                }
                true
            }

            popupMenu.show()
        }

        // Reminder Logic
        btnSetReminder.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                TimePickerDialog(this, { _, hour, minute ->
                    val selectedTime = Calendar.getInstance()
                    selectedTime.set(year, month, day, hour, minute)
                    reminderTimestamp = selectedTime.timeInMillis
                    Toast.makeText(this, "Pengingat dipasang!", Toast.LENGTH_SHORT).show()
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // --- UNDO / REDO ---
        btnUndo.setOnClickListener {
            if (undoStack.size > 1) {
                isUndoRedoAction = true
                redoStack.push(undoStack.pop())
                val state = undoStack.peek()
                val parts = state.split("|||")
                val textPart = parts[0]
                val metadataPart = if (parts.size > 1) parts[1] else ""
                val spannable = SpannableStringBuilder(textPart)
                applySerializedSpans(spannable, metadataPart)
                etNoteContent.setText(spannable)
                etNoteContent.setSelection(spannable.length)
                isUndoRedoAction = false
            }
        }

        btnRedo.setOnClickListener {
            if (redoStack.isNotEmpty()) {
                isUndoRedoAction = true
                val nextState = redoStack.pop()
                undoStack.push(nextState)
                val parts = nextState.split("|||")
                val textPart = parts[0]
                val metadataPart = if (parts.size > 1) parts[1] else ""
                val spannable = SpannableStringBuilder(textPart)
                applySerializedSpans(spannable, metadataPart)
                etNoteContent.setText(spannable)
                etNoteContent.setSelection(spannable.length)
                isUndoRedoAction = false
            } else {
                Toast.makeText(this, "Tidak ada yang bisa di-redo", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnSave).setOnClickListener {
            val title = etNoteTitle.text.toString().trim()
            val contentFinal = if (noteType == "checklist") {
                val allItems = checklistAdapter.getItems() + checkedListAdapter.getItems()
                allItems.joinToString("\n") { (if (it.isChecked) "[x] " else "[ ] ") + it.text }
            } else {
                etNoteContent.text.toString() + "|||" + serializeSpans(etNoteContent.text)
            }

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@AddNoteActivity)
                val note = if (existingNote != null) {
                    existingNote!!.copy(title = title, content = contentFinal, color = selectedColor, label = currentLabel, reminderTime = reminderTimestamp, type = noteType)
                } else {
                    Note(title = title, content = contentFinal, color = selectedColor, label = currentLabel, reminderTime = reminderTimestamp, type = noteType)
                }
                if (existingNote != null) db.appDao().updateNote(note) else db.appDao().insertNote(note)
                finish()
            }
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun updateCheckedSummary() {
        val unchecked = checklistAdapter.getItems().filter { !it.isChecked }.toMutableList()
        val checkedFromUnchecked = checklistAdapter.getItems().filter { it.isChecked }
        val checked = (checkedListAdapter.getItems() + checkedFromUnchecked).filter { it.isChecked }.toMutableList()
        val uncheckedFromChecked = checkedListAdapter.getItems().filter { !it.isChecked }
        unchecked.addAll(uncheckedFromChecked)

        if (checkedFromUnchecked.isNotEmpty() || uncheckedFromChecked.isNotEmpty()) {
            checklistAdapter.updateItems(unchecked)
            checkedListAdapter.updateItems(checked)
        }

        val checkedCount = checkedListAdapter.itemCount
        if (checkedCount > 0) {
            checkedItemsContainer.visibility = View.VISIBLE
            tvCheckedCount.text = "$checkedCount item dicentang"
        } else {
            checkedItemsContainer.visibility = View.GONE
        }
    }

    private fun serializeSpans(spannable: Spannable): String {
        val sb = StringBuilder()
        spannable.getSpans(0, spannable.length, StyleSpan::class.java).forEach { sb.append("ST:${spannable.getSpanStart(it)},${spannable.getSpanEnd(it)},${it.style};") }
        spannable.getSpans(0, spannable.length, UnderlineSpan::class.java).forEach { sb.append("UN:${spannable.getSpanStart(it)},${spannable.getSpanEnd(it)};") }
        spannable.getSpans(0, spannable.length, AbsoluteSizeSpan::class.java).forEach { sb.append("SZ:${spannable.getSpanStart(it)},${spannable.getSpanEnd(it)},${it.size};") }
        spannable.getSpans(0, spannable.length, AlignmentSpan.Standard::class.java).forEach { sb.append("AL:${spannable.getSpanStart(it)},${spannable.getSpanEnd(it)},${it.alignment.name};") }
        return sb.toString()
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
}
