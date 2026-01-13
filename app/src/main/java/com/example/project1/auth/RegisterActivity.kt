package com.example.project1.auth

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project1.R
import com.example.project1.data.AppDatabase
import com.example.project1.model.User
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val database = AppDatabase.getDatabase(this)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (fullName.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                lifecycleScope.launch {
                    val userExist = database.appDao().loginUser(username, password)

                    if (userExist == null) {
                        val newUser = User(fullName = fullName, username = username, password = password)
                        database.appDao().registerUser(newUser)

                        Toast.makeText(this@RegisterActivity, "Akun '$fullName' berhasil dibuat!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Username sudah terdaftar!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Tolong isi semua kolom!", Toast.LENGTH_SHORT).show()
            }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}