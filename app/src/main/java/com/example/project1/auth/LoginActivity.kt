package com.example.project1.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project1.R
import com.example.project1.data.AppDatabase
import com.example.project1.model.User
import com.example.project1.main.Dashboard
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val database = AppDatabase.getDatabase(this)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            lifecycleScope.launch {
                val allUsers = database.appDao().getAllUsers()

                if (allUsers.isEmpty()) {
                    Toast.makeText(this@LoginActivity, "DB Kosong! Daftar dulu.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                } else {
                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        val userExist = database.appDao().loginUser(username, password)

                        if (userExist != null) {
                            // --- SIMPAN SESSION USER ---
                            val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("FULL_NAME", userExist.fullName)
                                putString("EMAIL", userExist.username)
                                apply()
                            }

                            Toast.makeText(this@LoginActivity, "Login Berhasil!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@LoginActivity, Dashboard::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Username/Sandi Salah!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Tolong isi semua kolom!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}