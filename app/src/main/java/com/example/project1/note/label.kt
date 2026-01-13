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

class label : AppCompatActivity() {

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var labelAdapter: LabelAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvLabelNotes: RecyclerView
    private lateinit var rvLabelsDrawer: RecyclerView
    private var isFabMenuOpen = false
    private lateinit var tvLabelTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label)

        val database = AppDatabase.getDatabase(this)

        // 1. Inisialisasi View Utama
        drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnMenu = findViewById<ImageView>(R.id.btnMenuLabel)
        val btnSetting = findViewById<ImageView>(R.id.btnSetting)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        rvLabelNotes = findViewById<RecyclerView>(R.id.rvLabelNotes)
        val navView = findViewById<NavigationView>(R.id.navView)
        tvLabelTitle = findViewById<TextView>(R.id.tvLabelTitle)

        // Setup OnBackPressedDispatcher (Fix Deprecated onBackPressed)
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

        // Ambil nama label
        val labelName = intent.getStringExtra("LABEL_NAME") ?: "Tugas"
        tvLabelTitle.text = labelName

        // 2. Setup Dashboard Style Menu
        val fabOverlay = findViewById<View>(R.id.fabOverlay)
        val fabMenuContainer = findViewById<LinearLayout>(R.id.fabMenuContainer)
        val menuTeks = findViewById<LinearLayout>(R.id.menuTeks)
        val menuGambar = findViewById<LinearLayout>(R.id.menuGambar)

        // 3. Setup RecyclerView Utama (Daftar Catatan)
        rvLabelNotes.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        noteAdapter = NoteAdapter(mutableListOf(), lifecycleScope, database) {
            refreshData()
        }
        rvLabelNotes.adapter = noteAdapter

        // 4. Setup Drawer (Navigasi Kiri)
        rvLabelsDrawer = navView.findViewById<RecyclerView>(R.id.rvLabelsDrawer)
        rvLabelsDrawer.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        labelAdapter = LabelAdapter(mutableListOf(), { selectedLabel ->
            tvLabelTitle.text = selectedLabel.name
            refreshData()
            drawerLayout.closeDrawer(GravityCompat.START)
        }, { labelToDelete ->
            AlertDialog.Builder(this)
                .setTitle("Hapus Label")
                .setMessage("Hapus label '${labelToDelete.name}'?")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        database.appDao().deleteLabel(labelToDelete)
                        refreshData()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        })
        rvLabelsDrawer.adapter = labelAdapter

        // 5. Drawer Navigasi Links
        val itemCatatan = navView.findViewById<LinearLayout>(R.id.itemCatatan)
        val itemArsip = navView.findViewById<LinearLayout>(R.id.itemArsip)
        val itemTrash = navView.findViewById<LinearLayout>(R.id.itemTrash)
        val btnAddLabel = navView.findViewById<ImageView>(R.id.btnAddLabel)

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
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

        // 6. Logika FAB Menu (+)
        fabAdd.setOnClickListener {
            if (!isFabMenuOpen) showFabMenu(fabAdd, fabOverlay, fabMenuContainer)
            else closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
        }

        fabOverlay.setOnClickListener { closeFabMenu(fabAdd, fabOverlay, fabMenuContainer) }

        menuTeks.setOnClickListener {
            closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
            val intent = Intent(this, AddNoteActivity::class.java)
            intent.putExtra("LABEL_NAME", tvLabelTitle.text.toString())
            startActivity(intent)
        }

        menuGambar.setOnClickListener {
            closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
            startActivity(Intent(this, image_picker::class.java))
        }

        // 7. Fix Nabrak (Window Insets)
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@label)
            val listNotes = db.appDao().getNotesByLabel(tvLabelTitle.text.toString())
            noteAdapter.updateNotes(listNotes)
            
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
        refreshData()
    }
}