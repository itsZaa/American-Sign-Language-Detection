package com.example.tubesrpll.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tubesrpll.R
import com.example.tubesrpll.viewmodel.ASLImageAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class TranslateTextToBISINDO : AppCompatActivity() {
    private lateinit var storageReference: StorageReference
    private lateinit var bisindoImageAdapter: ASLImageAdapter
    private lateinit var profileImageView: ImageView
    private lateinit var textViewASL: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translate_text_to_bisindo)

        storageReference = FirebaseStorage.getInstance().reference

        val textASL = findViewById<EditText>(R.id.textInputEditTextBISINDO)
        val buttonASL = findViewById<Button>(R.id.buttonResultBISINDO)
        val recyclerViewASL = findViewById<RecyclerView>(R.id.recyclerViewBISINDO)

        bisindoImageAdapter = ASLImageAdapter(this)
        recyclerViewASL.layoutManager = LinearLayoutManager(this)
        recyclerViewASL.adapter = bisindoImageAdapter

        textASL.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length > 50) {
                    textASL.setText(s.subSequence(0, 50))
                    textASL.setSelection(50)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        buttonASL.setOnClickListener {
            val inputText = textASL.text.toString()
            if (inputText.isNotEmpty()) {
                updateBisindoImages(inputText)
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

    private fun updateBisindoImages(text: String) {
        val imageList = mutableListOf<List<StorageReference>>()
        val lines = text.split(" ")

        lines.forEach { line ->
            val charList = mutableListOf<StorageReference>()
            line.forEach { char ->
                val fileName = "BISINDO image/${char.lowercaseChar()}.png"
                val imageRef = storageReference.child(fileName)
                charList.add(imageRef)
            }
            imageList.add(charList)
        }

        bisindoImageAdapter.updateImageList(imageList)
    }
}