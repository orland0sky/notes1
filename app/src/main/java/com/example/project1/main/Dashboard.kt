package com.example.project1.main

import android.content.Context
import android.content.Intent
import android.os.Build
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
import com.example.project1.model.User
import com.example.project1.note.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class Dashboard : AppCompatActivity() {

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var labelAdapter: LabelAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvNotes: RecyclerView
    private lateinit var rvLabelsDrawer: RecyclerView
    private var isFabMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val database = AppDatabase.getDatabase(this)

        // 1. Inisialisasi View Utama
        drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val btnSetting = findViewById<ImageView>(R.id.btnSetting)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        rvNotes = findViewById(R.id.rvNotes)
        val navView = findViewById<NavigationView>(R.id.navView)
        
        // --- LOGIKA PROFIL USER (AMBIL DARI SESSION) ---
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val fullName = sharedPref.getString("FULL_NAME", "Pengguna")
        val email = sharedPref.getString("EMAIL", "email@example.com")

        val tvUserName = navView.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = navView.findViewById<TextView>(R.id.tvUserEmail)
        
        if (tvUserName != null && tvUserEmail != null) {
            tvUserName.text = "Hai, $fullName"
            tvUserEmail.text = email
        }

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

        rvLabelsDrawer = navView.findViewById<RecyclerView>(R.id.rvLabelsDrawer)
        rvLabelsDrawer.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        labelAdapter = LabelAdapter(mutableListOf(), { label ->
            val intent = Intent(this, com.example.project1.note.label::class.java)
            intent.putExtra("LABEL_NAME", label.name)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }, { label ->
            AlertDialog.Builder(this)
                .setTitle("Hapus Label")
                .setMessage("Hapus label '${label.name}'?")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        database.appDao().deleteLabel(label)
                        refreshNotes()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        })
        rvLabelsDrawer.adapter = labelAdapter

        val fabOverlay = findViewById<View>(R.id.fabOverlay)
        val fabMenuContainer = findViewById<LinearLayout>(R.id.fabMenuContainer)
        val menuTeks = findViewById<LinearLayout>(R.id.menuTeks)
        val menuGambar = findViewById<LinearLayout>(R.id.menuGambar)
        val menuDaftar = findViewById<LinearLayout>(R.id.menuDaftar)

        rvNotes.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        noteAdapter = NoteAdapter(mutableListOf(), lifecycleScope, database) {
            refreshNotes()
        }
        rvNotes.adapter = noteAdapter

        val itemCatatan = navView.findViewById<LinearLayout>(R.id.itemCatatan)
        val itemPengingat = navView.findViewById<LinearLayout>(R.id.itemPengingat)
        val itemArsip = navView.findViewById<LinearLayout>(R.id.itemArsip)
        val itemTrash = navView.findViewById<LinearLayout>(R.id.itemTrash)
        val btnAddLabel = navView.findViewById<ImageView>(R.id.btnAddLabel)
        val btnBackDrawer = navView.findViewById<ImageView>(R.id.btnBackDrawer)

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        btnBackDrawer?.setOnClickListener { drawerLayout.closeDrawer(GravityCompat.START) }
        itemCatatan?.setOnClickListener { drawerLayout.closeDrawer(GravityCompat.START) }
        
        itemPengingat?.setOnClickListener {
            startActivity(Intent(this, pengingat::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        itemArsip?.setOnClickListener {
            startActivity(Intent(this, item_archive::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        itemTrash?.setOnClickListener {
            startActivity(Intent(this, trash::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        btnAddLabel?.setOnClickListener {
            startActivity(Intent(this, new_label::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        btnSetting.setOnClickListener { showSettingPopup(it) }

        fabAdd.setOnClickListener {
            if (!isFabMenuOpen) showFabMenu(fabAdd, fabOverlay, fabMenuContainer)
            else closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
        }

        fabOverlay.setOnClickListener { closeFabMenu(fabAdd, fabOverlay, fabMenuContainer) }

        menuTeks.setOnClickListener {
            closeFabMenu(fabAdd, fabOverlay, fabMenuContainer)
            val intent = Intent(this, AddNoteActivity::class.java)
            intent.putExtra("NOTE_TYPE", "text")
            startActivity(intent)
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
    }

    private fun refreshNotes() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@Dashboard)
            val listNotes = db.appDao().getDashboardNotes()
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

    private fun showSettingPopup(view: View) {
        val popupView = layoutInflater.inflate(R.layout.activity_button_setting, null)
        val popupWindow = PopupWindow(
            popupView,
            resources.getDimensionPixelSize(R.dimen.popup_width),
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        popupView.findViewById<LinearLayout>(R.id.menuKeluar)?.setOnClickListener {
            popupWindow.dismiss()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(view, -300, 0)
    }

    override fun onResume() {
        super.onResume()
        refreshNotes()
    }
}