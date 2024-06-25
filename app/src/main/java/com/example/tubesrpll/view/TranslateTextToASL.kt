package com.example.tubesrpll.view

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tubesrpll.viewmodel.ASLImageAdapter
import com.example.tubesrpll.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class TranslateTextToASL : AppCompatActivity() {

    private lateinit var storageReference: StorageReference
    private lateinit var aslImageAdapter: ASLImageAdapter
    private lateinit var profileImageView: ImageView
    private lateinit var textViewASL: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translate_text_to_asl)

        storageReference = FirebaseStorage.getInstance().reference

        val textASL = findViewById<EditText>(R.id.textInputEditTextASL)
        val buttonASL = findViewById<Button>(R.id.buttonResultASL)
        val recyclerViewASL = findViewById<RecyclerView>(R.id.recyclerViewASL)

        aslImageAdapter = ASLImageAdapter(this)
        recyclerViewASL.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewASL.adapter = aslImageAdapter

        buttonASL.setOnClickListener {
            val inputText = textASL.text.toString()
            if (inputText.isNotEmpty()) {
                updateASLImages(inputText)
            }
        }

        textViewASL = findViewById(R.id.textView)
        profileImageView = findViewById(R.id.imageProfileASL)
        fetchProfileImage()
    }

    override fun onResume() {
        super.onResume()
        fetchProfileImage()
    }

    private fun fetchProfileImage() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val profileImage = document.getString("profileImage")
                        if (profileImage != null && profileImage.isNotEmpty()) {
                            Picasso.get().load(profileImage).into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.baseline_person_24)
                        }

                        val userName = document.getString("name")
                        if (userName != null && userName.isNotEmpty()) {
                            textViewASL.text = "Welcome $userName"
                        } else {
                            textViewASL.text = "Welcome Guest"
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to fetch profile image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error fetching profile image", exception)
                    textViewASL.text = "Welcome Guest"
                }
        } else {
            profileImageView.setImageResource(R.drawable.baseline_person_24)
            textViewASL.text = "Welcome Guest"
        }
    }

    private fun updateASLImages(text: String) {
        val imageList = mutableListOf<StorageReference>()
        val charArray = text.toCharArray()

        charArray.forEach { char ->
            // Cek jika karakter adalah spasi
            if (char == ' ') {
                val fileName = "ASL image/spasi.png"
                val imageRef = storageReference.child(fileName)
                imageList.add(imageRef)
            } else if (char.isLetter()) {
                val fileName = "ASL image/${char.lowercaseChar()}.png"
                val imageRef = storageReference.child(fileName)
                imageList.add(imageRef)
            }
        }

        aslImageAdapter.updateImageList(imageList)
    }
}
