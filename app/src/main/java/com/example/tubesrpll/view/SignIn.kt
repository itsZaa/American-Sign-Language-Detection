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
import com.google.firebase.firestore.FirebaseFirestore

class SignIn : AppCompatActivity() {

    private lateinit var emailEt: EditText
    private lateinit var passEt: EditText
    private lateinit var signInButton: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        emailEt = findViewById(R.id.textInputEditText)
        passEt = findViewById(R.id.editTextPassword)
        signInButton = findViewById(R.id.button6)

        firebaseAuth = FirebaseAuth.getInstance()

        signInButton.setOnClickListener {
            val email = emailEt.text.toString()
            val pass = passEt.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        user?.let {
                            if (it.isEmailVerified) {
                                val userId = it.uid
                                firestore.collection("users").document(userId).get()
                                    .addOnSuccessListener { document ->
                                        if (document != null) {
                                            val userData = document.toObject(User::class.java)
                                            if (userData?.role == "user") {
                                                val name = userData.name
                                                Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this, Home::class.java)
                                                intent.putExtra("USER_NAME", name)
                                                startActivity(intent)
                                            } else {
                                                Toast.makeText(this, "Akses ditolak. Hanya pengguna dengan role 'user' yang bisa login.", Toast.LENGTH_LONG).show()
                                                firebaseAuth.signOut()
                                            }
                                        } else {
                                            Toast.makeText(this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FirestoreError", "Failed to get user data: ", e)
                                        Toast.makeText(this, "Gagal mendapatkan data user", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Email belum diverifikasi. Silakan cek email Anda untuk verifikasi.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Password/email salah", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            }
        }

        val forgotPasswordTextView = findViewById<TextView>(R.id.textViewForgot)
        val text = "Forgot Password?"
        val spannableString = SpannableString(text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@SignIn, ForgotPassword::class.java)
                startActivity(intent)
            }
        }
        spannableString.setSpan(clickableSpan, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        forgotPasswordTextView.text = spannableString
        forgotPasswordTextView.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        val textView = findViewById<TextView>(R.id.textView7)
        val text2 = "Don’t have accounts? Register now! "
        val spannableString2 = SpannableString(text2)
        val clickableSpan2 = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@SignIn, SignUp::class.java)
                startActivity(intent)
            }
        }
        spannableString2.setSpan(clickableSpan2, 21, 34, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannableString2
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }
}
