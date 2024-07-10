package com.example.tubesrpll.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tubesrpll.R

class LoadingPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_load_page)

        // Mengatur padding agar tidak bertabrakan dengan status bar dan navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handler untuk menunda transisi ke halaman Home selama 5 detik
        Handler().postDelayed({
            val intent = Intent(this@LoadingPage, Home::class.java)
            startActivity(intent)
            finish()
        }, 5000) // Waktu delay dalam milidetik
    }
}
