package com.example.tubesrpll.view

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tubesrpll.viewmodel.NewsAdapter
import com.example.tubesrpll.R
import com.example.tubesrpll.model.NewsItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class AllNews : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter
    private val newsList = mutableListOf<NewsItem>()
    private lateinit var profileImageView: ImageView
    private lateinit var textViewNews: TextView

    // Fungsi ini dipanggil saat activity dibuat
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_news)

        // Inisialisasi RecyclerView dan set LayoutManager
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inisialisasi adapter dan set ke RecyclerView
        adapter = NewsAdapter(newsList)
        recyclerView.adapter = adapter

        // Memanggil fungsi untuk mengambil semua berita
        fetchAllNews()

        // Inisialisasi TextView dan ImageView
        textViewNews = findViewById(R.id.textView)
        profileImageView = findViewById(R.id.imageProfileASL)

        // Memanggil fungsi untuk mengambil gambar profil
        fetchProfileImage()
    }

    // Fungsi untuk mengambil semua berita dari Firestore
    private fun fetchAllNews() {
        val db = Firebase.firestore
        db.collection("news")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val newsItem = document.toObject<NewsItem>().copy(id = document.id)
                    newsList.add(newsItem)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w("AllNewsActivity", "Error getting documents.", exception)
            }
    }

    // Fungsi ini dipanggil saat aktivitas dilanjutkan kembali
    override fun onResume() {
        super.onResume()
        fetchProfileImage()
    }

    // Fungsi untuk mengambil gambar profil dari Firestore
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
                            textViewNews.text = "Welcome $userName"
                        } else {
                            textViewNews.text = "Welcome Guest"
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to fetch profile image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error fetching profile image", exception)
                    textViewNews.text = "Welcome Guest"
                }
        } else {
            profileImageView.setImageResource(R.drawable.baseline_person_24)
            textViewNews.text = "Welcome Guest"
        }
    }
}
