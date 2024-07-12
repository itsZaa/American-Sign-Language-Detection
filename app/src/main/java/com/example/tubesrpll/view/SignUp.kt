package com.example.tubesrpll.view

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tubesrpll.R
import com.example.tubesrpll.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class SignUp : AppCompatActivity() {

    // Inisialisasi variabel untuk elemen UI
    private lateinit var nameEt: EditText
    private lateinit var emailEt: EditText
    private lateinit var phoneEt: EditText
    private lateinit var passEt: EditText
    private lateinit var signUpButton: Button

    // Inisialisasi FirebaseAuth dan Firestore
    private lateinit var firebaseAuth: FirebaseAuth
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Menghubungkan variabel dengan elemen UI
        nameEt = findViewById(R.id.textInputEditNama)
        emailEt = findViewById(R.id.textInputEditEmail)
        phoneEt = findViewById(R.id.textInputEditPhone)
        passEt = findViewById(R.id.editTextPassword2)
        signUpButton = findViewById(R.id.buttonSignup)

        // Mendapatkan instance FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        // Set onClickListener untuk tombol daftar
        signUpButton.setOnClickListener {
            // Mendapatkan teks dari EditText
            val name = nameEt.text.toString()
            val email = emailEt.text.toString()
            val phone = phoneEt.text.toString()
            val pass = passEt.text.toString()

            // Validasi input
            if (name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() && pass.isNotEmpty()) {
                if (pass.length >= 8) {
                    // Membuat pengguna baru dengan email dan password
                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Mendapatkan pengguna yang baru terdaftar
                            val user = firebaseAuth.currentUser
                            user?.let {
                                val userId = it.uid
                                // Membuat objek pengguna baru
                                val newUser = User(id = userId, name = name, phone = phone, role = "user")
                                // Menyimpan data pengguna baru di Firestore
                                firestore.collection("users").document(userId).set(newUser)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Register Success, check email untuk LOGIN", Toast.LENGTH_SHORT).show()
                                        sendEmailVerification(user)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FirestoreError", "Failed to add user data: ", e)
                                        user.delete()
                                        NotificationDialog.showDialog(this, "Register Failed", "Please fill the requirement!")
                                    }
                            }
                        } else {
                            // Menangani kasus error saat pendaftaran
                            if (task.exception is FirebaseAuthUserCollisionException) {
                                NotificationDialog.showDialog(this, "Email sudah digunakan", "Silakan gunakan email lain")
                            } else {
                                NotificationDialog.showDialog(this, "Register Failed", "Coba lagi")
                            }
                        }
                    }
                } else {
                    NotificationDialog.showDialog(this, "Error", "Password harus minimal 8 karakter")
                }
            } else {
                NotificationDialog.showDialog(this, "Error", "Empty Fields Are not Allowed !!")
            }
        }

        // Inisialisasi TextView untuk teks "Already have accounts? Sign in"
        val textView = findViewById<TextView>(R.id.signin)
        val text = "Already have accounts? Sign in"
        val spannableString = SpannableString(text)

        // Membuat teks "Sign in" dapat diklik
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@SignUp, SignIn::class.java)
                startActivity(intent)
            }
        }

        spannableString.setSpan(clickableSpan, 23, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannableString
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    // Fungsi untuk mengirim email verifikasi
    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Email verifikasi telah dikirim. Silakan periksa email Anda untuk LOGIN.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, SignIn::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Gagal mengirim email verifikasi.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
