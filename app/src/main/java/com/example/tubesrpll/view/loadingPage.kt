package com.example.tubesrpll.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tubesrpll.R

/**
 * Aktivity untuk menampilkan halaman loading.
 */
class loadingPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_load_page)

        // Menambahkan padding ke view utama agar tidak tertutupi oleh sistem bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Menunda perpindahan ke aktivitas Home selama 5 detik
        Handler().postDelayed({
            val intent = Intent(this@loadingPage, Home::class.java)
            startActivity(intent)
            finish()
        }, 5000)
    }
}