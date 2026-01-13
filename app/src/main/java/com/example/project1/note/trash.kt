package com.example.project1.note

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.project1.R
import com.example.project1.data.AppDatabase
import kotlinx.coroutines.launch

class trash : AppCompatActivity() {
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)
        
        val database = AppDatabase.getDatabase(this)
        val rvTrash = findViewById<RecyclerView>(R.id.rvTrash)
        
        rvTrash.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        noteAdapter = NoteAdapter(mutableListOf(), lifecycleScope, database) {
            loadDeletedNotes() // Refresh list jika ada aksi (pulihkan/hapus permanen)
        }
        rvTrash.adapter = noteAdapter

        findViewById<ImageView>(R.id.btnBackTrash)?.setOnClickListener { finish() }

        loadDeletedNotes()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadDeletedNotes() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@trash)
            val deletedList = db.appDao().getDeletedNotes()
            noteAdapter.updateNotes(deletedList)
        }
    }
}