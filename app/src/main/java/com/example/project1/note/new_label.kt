package com.example.project1.note

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.project1.R
import com.example.project1.data.AppDatabase
import com.example.project1.model.Label
import kotlinx.coroutines.launch

class new_label : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_label)

        val database = AppDatabase.getDatabase(this)
        val etLabelName = findViewById<EditText>(R.id.etLabelName)
        val btnCancel = findViewById<AppCompatButton>(R.id.btnCancelLabel)
        val btnCreate = findViewById<AppCompatButton>(R.id.btnCreateLabel)

        btnCancel.setOnClickListener { finish() }

        btnCreate.setOnClickListener {
            val name = etLabelName.text.toString().trim()
            if (name.isNotEmpty()) {
                lifecycleScope.launch {
                    val newLabel = Label(name = name)
                    database.appDao().insertLabel(newLabel)
                    Toast.makeText(this@new_label, "Label '$name' dibuat", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Nama label tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        val mainView = findViewById<android.view.View>(R.id.main)
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }
}