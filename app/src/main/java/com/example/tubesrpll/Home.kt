package com.example.tubesrpll

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Home : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        auth = FirebaseAuth.getInstance()

        val textView2 = findViewById<TextView>(R.id.textView)
        val userName = intent.getStringExtra("USER_NAME")
        if (userName != null){
            textView2.text = "Welcome $userName"
        } else {
            textView2.text = "Welcome Guest"
        }
    }

    fun moveToSignIn(view: View) {
        val userName = intent.getStringExtra("USER_NAME")
        if (userName == null) {
            val intent = Intent(this, SignIn::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }
    }

    fun moveToTextToVideo(view: View) {
        val userName = intent.getStringExtra("USER_NAME")
        if (userName == null) {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            builder.setMessage("Silahkan login terlebih dahulu")
                .setCancelable(false)
                .setPositiveButton("Login") { dialog, id ->
                    val loginIntent = Intent(this, SignIn::class.java)
                    startActivity(loginIntent)
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        } else {
            val intent = Intent(this, TranslateVideoToText::class.java)
            startActivity(intent)
        }
    }

}