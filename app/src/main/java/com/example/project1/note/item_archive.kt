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

class item_archive : AppCompatActivity() {
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_archive)
        
        val database = AppDatabase.getDatabase(this)
        val rvArchive = findViewById<RecyclerView>(R.id.rvArchive)
        
        // Setup Adapter khusus untuk halaman Arsip
        rvArchive.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        noteAdapter = NoteAdapter(mutableListOf(), lifecycleScope, database) {
            loadArchivedNotes() // Refresh list jika ada aksi di dalam arsip
        }
        rvArchive.adapter = noteAdapter

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }

        loadArchivedNotes()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadArchivedNotes() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@item_archive)
            val archivedList = db.appDao().getArchivedNotes()
            noteAdapter.updateNotes(archivedList)
        }
    }
}