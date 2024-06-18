package com.example.tubesrpll

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Profile : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var button: FloatingActionButton
    private lateinit var nameProfile: TextView
    private lateinit var phoneProfile: TextView
    private lateinit var emailProfile: TextView

    companion object {
        const val IMAGE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        imageView = findViewById(R.id.imagePhoto)
        button = findViewById(R.id.cameraButton)
        nameProfile = findViewById(R.id.NameProfile)
        phoneProfile = findViewById(R.id.phoneProfile)
        emailProfile = findViewById(R.id.emailProfile)

        button.setOnClickListener {
            pickImageGallery()
        }

        // Fetch profile data from Firestore
        fetchProfileData()
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            imageView.setImageURI(data?.data)
        }
    }

    private fun fetchProfileData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            // Set email from authentication
            val email = currentUser.email
            emailProfile.text = email

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name")
                        val phone = document.getString("phone")

                        nameProfile.text = name
                        phoneProfile.text = phone
                    } else {
                        // Handle case where document does not exist
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle failure
                }
        } else {
            // Handle case where user is not logged in
        }
    }
}
