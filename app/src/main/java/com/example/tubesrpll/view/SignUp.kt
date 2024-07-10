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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.firestore

class SignUp : AppCompatActivity() {

    private lateinit var nameEt: EditText
    private lateinit var emailEt: EditText
    private lateinit var phoneEt: EditText
    private lateinit var passEt: EditText
    private lateinit var signUpButton: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Inisialisasi elemen UI dan instance Firebase
        nameEt = findViewById(R.id.textInputEditNama)
        emailEt = findViewById(R.id.textInputEditEmail)
        phoneEt = findViewById(R.id.textInputEditPhone)
        passEt = findViewById(R.id.editTextPassword2)
        signUpButton = findViewById(R.id.buttonSignup)

        firebaseAuth = FirebaseAuth.getInstance()

        // Set onClickListener untuk tombol sign up
        signUpButton.setOnClickListener {
            val name = nameEt.text.toString()
            val email = emailEt.text.toString()
            val phone = phoneEt.text.toString()
            val pass = passEt.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        user?.let {
                            val userId = it.uid
                            val userMap = hashMapOf(
                                "name" to name,
                                "phone" to phone
                            )
                            // Simpan data pengguna ke Firestore setelah berhasil registrasi
                            db.collection("users").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registrasi berhasil, check email untuk LOGIN", Toast.LENGTH_SHORT).show()
                                    sendEmailVerification(user)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FirestoreError", "Gagal menambahkan data pengguna: ", e)
                                    user.delete()
                                    Toast.makeText(this, "Registrasi gagal, coba lagi", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // Tangani jika registrasi gagal
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Email sudah digunakan. Silakan gunakan email lain.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Registrasi gagal, coba lagi", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // Tampilkan pesan jika ada field yang kosong
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup teks yang bisa diklik untuk navigasi ke halaman SignIn
        val textView = findViewById<TextView>(R.id.signin)
        val text = "Sudah punya akun? Masuk"
        val spannableString = SpannableString(text)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@SignUp, SignIn::class.java)
                startActivity(intent)
            }
        }

        spannableString.setSpan(clickableSpan, 20, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannableString
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    // Method untuk mengirim email verifikasi
    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Email verifikasi telah terkirim. Periksa email Anda untuk lanjut LOGIN.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, SignIn::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Gagal mengirim email verifikasi.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
