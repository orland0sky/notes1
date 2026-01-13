package com.example.project1.note

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.project1.R
import com.example.project1.auth.LoginActivity
import com.example.project1.data.AppDatabase
import com.example.project1.model.Label
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class pengingat : AppCompatActivity() {

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var labelAdapter: LabelAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvPengingat: RecyclerView
    private lateinit var rvLabelsDrawer: RecyclerView
    private var isFabMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengingat)

        val database = AppDatabase.getDatabase(this)

        // 1. Inisialisasi View Utama
        drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnBack = findViewById<ImageView>(R.id.btnBackReminder)
        val btnSetting = findViewById<ImageView>(R.id.btnSetting)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        rvPengingat = findViewById<RecyclerView>(R.id.rvPengingat)
        val navView = findViewById<NavigationView>(R.id.navView)
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFabMenuOpen) {
                    val fabOverlay = findViewById<View>(R.id.fabOverlay)
                    val fabMenuContainer = findViewById<LinearLayout>(R.id.fabMenuContainer)
                    closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
                } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // 2. Setup RecyclerView Utama (Daftar Pengingat)
        rvPengingat.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        noteAdapter = NoteAdapter(mutableListOf(), lifecycleScope, database) {
            loadReminderNotes()
        }
        rvPengingat.adapter = noteAdapter

        // 3. Setup Drawer (Navigasi Kiri)
        rvLabelsDrawer = navView.findViewById<RecyclerView>(R.id.rvLabelsDrawer)
        rvLabelsDrawer.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        labelAdapter = LabelAdapter(mutableListOf(), { label ->
            val intent = Intent(this, com.example.project1.note.label::class.java)
            intent.putExtra("LABEL_NAME", label.name)
            startActivity(intent)
            finish()
        }, { label ->
            AlertDialog.Builder(this)
                .setTitle("Hapus Label")
                .setMessage("Hapus label '${label.name}'?")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        database.appDao().deleteLabel(label)
                        refreshDrawerData()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        })
        rvLabelsDrawer.adapter = labelAdapter

        // Drawer Navigasi
        val itemCatatan = navView.findViewById<LinearLayout>(R.id.itemCatatan)
        val itemArsip = navView.findViewById<LinearLayout>(R.id.itemArsip)
        val itemTrash = navView.findViewById<LinearLayout>(R.id.itemTrash)
        val btnAddLabel = navView.findViewById<ImageView>(R.id.btnAddLabel)

        btnBack.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        itemCatatan?.setOnClickListener { finish() } 
        itemArsip?.setOnClickListener {
            startActivity(Intent(this, item_archive::class.java))
            finish()
        }
        itemTrash?.setOnClickListener {
            startActivity(Intent(this, trash::class.java))
            finish()
        }
        btnAddLabel?.setOnClickListener {
            startActivity(Intent(this, new_label::class.java))
        }

        // 4. Logika FAB Menu (+)
        val fabOverlay = findViewById<View>(R.id.fabOverlay)
        val fabMenuContainer = findViewById<LinearLayout>(R.id.fabMenuContainer)
        val menuTeks = findViewById<LinearLayout>(R.id.menuTeks)
        val menuGambar = findViewById<LinearLayout>(R.id.menuGambar)
        val menuDaftar = findViewById<LinearLayout>(R.id.menuDaftar)

        fabAdd.setOnClickListener {
            if (!isFabMenuOpen) showFabMenu(fabAdd, fabOverlay, fabMenuContainer)
            else closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
        }

        fabOverlay.setOnClickListener { closeFabMenu(fabAdd, fabOverlay, fabMenuContainer) }

        menuTeks.setOnClickListener {
            closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

        menuGambar.setOnClickListener {
            closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
            startActivity(Intent(this, image_picker::class.java))
        }

        menuDaftar.setOnClickListener {
            closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
            val intent = Intent(this, AddNoteActivity::class.java)
            intent.putExtra("NOTE_TYPE", "checklist")
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        loadReminderNotes()
        refreshDrawerData()
    }

    private fun loadReminderNotes() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@pengingat)
            val listReminders = db.appDao().getReminderNotes()
            noteAdapter.updateNotes(listReminders)
        }
    }

    private fun refreshDrawerData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@pengingat)
            val listLabels = db.appDao().getAllLabels()
            labelAdapter.updateLabels(listLabels)
        }
    }

    private fun showFabMenu(fab: FloatingActionButton, overlay: View, container: LinearLayout) {
        isFabMenuOpen = true
        overlay.visibility = View.VISIBLE
        container.visibility = View.VISIBLE
        fab.setImageResource(R.drawable.ic_back_arrow)
        fab.rotation = 45f
    }

    private fun closeFabMenu(fab: FloatingActionButton, overlay: View, container: LinearLayout) {
        isFabMenuOpen = false
        overlay.visibility = View.GONE
        container.visibility = View.GONE
        fab.setImageResource(R.drawable.ic_add)
        fab.rotation = 0f
    }

    override fun onResume() {
        super.onResume()
        loadReminderNotes()
        refreshDrawerData()
    }
}