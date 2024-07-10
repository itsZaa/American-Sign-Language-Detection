package com.example.tubesrpll.view

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tubesrpll.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignIn : AppCompatActivity() {

    private lateinit var emailEt: EditText
    private lateinit var passEt: EditText
    private lateinit var signInButton: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Inisialisasi UI dan instance Firebase
        emailEt = findViewById(R.id.textInputEditText)
        passEt = findViewById(R.id.editTextPassword)
        signInButton = findViewById(R.id.button6)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set onClickListener untuk tombol sign in
        signInButton.setOnClickListener {
            val email = emailEt.text.toString()
            val pass = passEt.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        user?.let {
                            if (it.isEmailVerified) {
                                val userId = it.uid
                                // Ambil data pengguna dari Firestore setelah berhasil login
                                firestore.collection("users").document(userId).get()
                                    .addOnSuccessListener { document ->
                                        if (document != null) {
                                            val name = document.getString("name")
                                            Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this, Home::class.java)
                                            intent.putExtra("USER_NAME", name)
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(this, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                // Tampilkan pesan jika email belum diverifikasi
                                Toast.makeText(this, "Email belum diverifikasi. Silakan cek email Anda untuk verifikasi.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        // Tampilkan pesan jika login gagal
                        Toast.makeText(this, "Password/email salah", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Tampilkan pesan jika ada field yang kosong
                Toast.makeText(this, "Field kosong tidak diizinkan!", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup teks yang bisa diklik untuk navigasi ke halaman SignUp
        val textView = findViewById<TextView>(R.id.textView7)
        val text = "Belum punya akun? Daftar sekarang!"
        val spannableString = SpannableString(text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@SignIn, SignUp::class.java)
                startActivity(intent)
            }
        }
        // Atur bagian teks yang bisa diklik
        spannableString.setSpan(clickableSpan, 21, 34, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannableString
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }
}
