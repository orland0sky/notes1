package com.example.project1.note

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.project1.R
import androidx.core.content.FileProvider
import android.net.Uri
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import java.io.File

class image_picker : AppCompatActivity() {
    private var cameraUri: Uri? = null

    private lateinit var photoUri: Uri
    // Launcher kamera: simpan foto ke URI yang kita sediakan
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraUri != null) {

                // langsung buka catatan baru + kirim uri foto
                val intent = Intent(this, AddNoteActivity::class.java)
                intent.putExtra("EXTRA_IMAGE_URI", cameraUri.toString())

                finish()
            }
        }

    private fun openCamera() {
        val photoFile = File.createTempFile(
            "IMG_",
            ".jpg",
            cacheDir
        )

        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider", // ← KODE YANG KAMU TANYAKAN
            photoFile
        )

        takePictureLauncher.launch(photoUri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_picker)
        imgPreview = findViewById(R.id.imgPreview)

        val btnAlbum = findViewById<LinearLayout>(R.id.btnAlbum)
        findViewById<LinearLayout>(R.id.btnAlbum).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val btnCamera = findViewById<LinearLayout>(R.id.btnCamera)
        btnCamera.setOnClickListener {
            openCamera()
        }


    }
    private lateinit var imgPreview: ImageView

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                // 1) kirim uri ke Activity Catatan
                val intent = Intent(this, AddNoteActivity::class.java)
                intent.putExtra("EXTRA_IMAGE_URI", uri.toString())
                intent.putExtra("EXTRA_IS_NEW_NOTE", true) // optional tanda catatan baru
                startActivity(intent)

                // 2) tutup halaman pilih gambar biar langsung ke catatan
                finish()

            }
        }

}
