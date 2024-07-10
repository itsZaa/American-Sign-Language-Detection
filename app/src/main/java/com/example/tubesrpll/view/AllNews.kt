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


/**
 * Aktivity untuk menampilkan semua berita.
 */
class AllNews : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView // RecyclerView untuk menampilkan daftar berita
    private lateinit var adapter: NewsAdapter // Adapter untuk RecyclerView
    private val newsList = mutableListOf<NewsItem>() //Daftar Berita
    private lateinit var profileImageView: ImageView //ImageView untuk Gambar Profile Pengguna
    private lateinit var textViewNews: TextView //TextView untuk menampilkan teks


    /**
     * Metode yang dipanggil saat aktivitas dibuat.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_news)

        // Inisialisasi RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NewsAdapter(newsList)
        recyclerView.adapter = adapter

        // Memuat berita dari Firebase
        fetchAllNews()

        // Inisialisasi TextView dan ImageView
        textViewNews = findViewById(R.id.textView)
        profileImageView = findViewById(R.id.imageProfileASL)
        fetchProfileImage()
    }

    /**
     * Metode untuk memuat semua berita dari Firestore.
     */
    private fun fetchAllNews() {
        val db = Firebase.firestore
        db.collection("news")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    // Mengonversi dokumen Firestore menjadi objek NewsItem
                    val newsItem = document.toObject<NewsItem>().copy(id = document.id)
                    newsList.add(newsItem)
                }
                adapter.notifyDataSetChanged() // Memberitahu adapter bahwa data telah berubah
            }
            .addOnFailureListener { exception ->
                Log.w("AllNewsActivity", "Error getting documents.", exception)
            }
    }

    /**
     * Metode yang dipanggil saat aktivitas dilanjutkan.
     */
    override fun onResume() {
        super.onResume()
        fetchProfileImage() // Memuat ulang gambar profil saat aktivitas dilanjutkan
    }

    /**
     * Metode untuk memuat gambar profil pengguna dari Firestore.
     */
    private fun fetchProfileImage() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Memuat gambar profil dari URL
                        val profileImage = document.getString("profileImage")
                        if (profileImage != null && profileImage.isNotEmpty()) {
                            Picasso.get().load(profileImage).into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.baseline_person_24)
                        }

                        // Menampilkan nama pengguna atau sambutan default
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
