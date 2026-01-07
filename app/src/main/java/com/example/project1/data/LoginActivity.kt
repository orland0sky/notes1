package com.example.project1.data

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project1.MainActivity
import com.example.project1.R
import com.example.project1.model.User // Pastikan import ini benar
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Inisialisasi Database menggunakan Singleton
        val database = AppDatabase.getDatabase(this)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // 2. Gunakan lifecycleScope karena loginUser adalah fungsi 'suspend'
                lifecycleScope.launch {
                    val userExist = database.appDao().loginUser(username, password)

                    if (userExist != null) {
                        Toast.makeText(this@LoginActivity, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        // Jika user tidak ada, kita bantu daftarkan otomatis (untuk tes)
                        val newUser = User(username = username, password = password)
                        database.appDao().registerUser(newUser)

                        Toast.makeText(this@LoginActivity, "User '$username' terdaftar! Klik MASUK sekali lagi.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Tolong isi semua kolom", Toast.LENGTH_SHORT).show()
            }
        }
    }
}