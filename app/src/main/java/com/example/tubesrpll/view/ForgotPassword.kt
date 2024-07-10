package com.example.tubesrpll.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tubesrpll.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Aktivity untuk mengelola fitur lupa kata sandi.
 */
class ForgotPassword : AppCompatActivity() {

    private lateinit var emailEt: EditText // EditText untuk input email
    private lateinit var resetPassword: Button // Button untuk reset password
    private lateinit var firebaseAuth: FirebaseAuth // Instance FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Inisialisasi FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        // Mendapatkan referensi ke komponen UI
        emailEt = findViewById(R.id.textInputEditText)
        resetPassword = findViewById(R.id.button6)

        // Menangani klik tombol reset password
        resetPassword.setOnClickListener {
            val email = emailEt.text.toString()
            if (email.isNotEmpty()) {
                // Mengirim email reset password
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Email reset password telah dikirim. Silakan cek email Anda.",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this, SignIn::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Gagal mengirim email reset password. Periksa email Anda dan coba lagi.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(
                    this,
                    "Masukkan email Anda untuk reset password",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}