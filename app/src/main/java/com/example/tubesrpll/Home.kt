package com.example.tubesrpll

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        val textView2 = findViewById<TextView>(R.id.textView)
        val userName = intent.getStringExtra("USER_NAME")
        if (userName != null){
            textView2.text = "Welcome $userName"
        } else {
            textView2.text = "Welcome Guest"
        }
    }

    fun moveToSignIn(view: View) {
        val intent = Intent(this, SignIn::class.java)
        startActivity(intent)
    }

    fun moveToTextToVideo(view: View) {
        val userName = intent.getStringExtra("USER_NAME")
        if (userName == null) {
            Toast.makeText(this, "Silahkan login terlebih dahulu", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, TranslateVideoToText::class.java)
            startActivity(intent)
        }
    }
}