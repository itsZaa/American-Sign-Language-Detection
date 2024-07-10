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
import com.example.tubesrpll.viewmodel.BISINDOImageAdapter
import com.example.tubesrpll.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class TranslateTextToBISINDO : AppCompatActivity() {
    private lateinit var storageReference: StorageReference
    private lateinit var bisindoImageAdapter: BISINDOImageAdapter
    private lateinit var profileImageView: ImageView
    private lateinit var textViewBISINDO: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translate_text_to_bisindo)

        // Inisialisasi Firebase Storage Reference
        storageReference = FirebaseStorage.getInstance().reference

        // Inisialisasi elemen UI dan adapter RecyclerView
        val textASL = findViewById<EditText>(R.id.textInputEditTextBISINDO)
        val buttonASL = findViewById<Button>(R.id.buttonResultBISINDO)
        val recyclerViewASL = findViewById<RecyclerView>(R.id.recyclerViewBISINDO)

        bisindoImageAdapter = BISINDOImageAdapter(this)
        recyclerViewASL.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewASL.adapter = bisindoImageAdapter

        // Set onClickListener untuk tombol hasil BISINDO
        buttonASL.setOnClickListener {
            val inputText = textASL.text.toString()
            if (inputText.isNotEmpty()) {
                updateASLImages(inputText)
            }
        }

        // Inisialisasi elemen UI untuk profil pengguna
        textViewBISINDO = findViewById(R.id.textView)
        profileImageView = findViewById(R.id.imageProfileASL)
        fetchProfileImage()
    }

    override fun onResume() {
        super.onResume()
        // Memanggil kembali fungsi untuk mengambil gambar profil saat activity di-resume
        fetchProfileImage()
    }

    // Method untuk mengambil gambar profil pengguna dari Firestore
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
                            // Load gambar profil menggunakan Picasso jika tersedia
                            Picasso.get().load(profileImage).into(profileImageView)
                        } else {
                            // Set default gambar profil jika tidak ada
                            profileImageView.setImageResource(R.drawable.baseline_person_24)
                        }

                        val userName = document.getString("name")
                        if (userName != null && userName.isNotEmpty()) {
                            // Tampilkan nama pengguna jika tersedia
                            textViewBISINDO.text = "Welcome, $userName"
                        } else {
                            // Tampilkan sebagai tamu jika nama tidak tersedia
                            textViewBISINDO.text = "Welcome, Guest"
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Tangani jika terjadi kesalahan dalam mengambil gambar profil
                    Toast.makeText(this, "Gagal mengambil gambar profil: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error fetching profile image", exception)
                    textViewBISINDO.text = "Welcome, Guest"
                }
        } else {
            // Set default gambar profil dan teks selamat datang untuk tamu
            profileImageView.setImageResource(R.drawable.baseline_person_24)
            textViewBISINDO.text = "Welcome, Guest"
        }
    }

    // Method untuk memperbarui daftar gambar BISINDO berdasarkan teks input
    private fun updateASLImages(text: String) {
        val imageList = mutableListOf<StorageReference>()
        val charArray = text.toCharArray()

        charArray.forEach { char ->
            if (char == ' ') {
                // Jika karakter adalah spasi, ambil gambar spasi
                val fileName = "BISINDO image/spasi.png"
                val imageRef = storageReference.child(fileName)
                imageList.add(imageRef)
            } else {
                // Jika karakter adalah huruf, ambil gambar BISINDO sesuai dengan karakter
                val fileName = "BISINDO image/${char.lowercaseChar()}.png"
                val imageRef = storageReference.child(fileName)
                imageList.add(imageRef)
            }
        }

        // Update adapter RecyclerView dengan daftar gambar BISINDO baru
        bisindoImageAdapter.updateImageList(imageList)
    }
}
