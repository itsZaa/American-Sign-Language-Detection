package com.example.tubesrpll

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

        emailEt = findViewById(R.id.textInputEditText)
        passEt = findViewById(R.id.editTextPassword)
        signInButton = findViewById(R.id.button6)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        signInButton.setOnClickListener {
            val email = emailEt.text.toString()
            val pass = passEt.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        user?.let {
                            val userId = it.uid
                            firestore.collection("users").document(userId).get()
                                .addOnSuccessListener { document ->
                                    if (document != null) {
                                        val name = document.getString("name")
                                        Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, MainActivity2::class.java)
                                        intent.putExtra("USER_NAME", name)
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show()
                                    }
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

        val textView = findViewById<TextView>(R.id.textView7)

        val text = "Don’t have accounts? Register Now!"
        val spannableString = SpannableString(text)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@SignIn, SignUp::class.java)
                startActivity(intent)
            }
        }

        spannableString.setSpan(clickableSpan, 21, 34, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannableString
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }
}
