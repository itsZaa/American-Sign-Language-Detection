package com.example.tubesrpll

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        val textView2 = findViewById<TextView>(R.id.textView)
        val userName = intent.getStringExtra("USER_NAME")
        textView2.text = "Welcome $userName"
    }

    fun pindah(view: View) {
        val intent = Intent(this, SignIn::class.java)
        startActivity(intent)
    }

    fun pindahKeTextToVideo(view: View) {
        val intent = Intent(this, TranslateVideoToText::class.java)
        startActivity(intent)
    }
}