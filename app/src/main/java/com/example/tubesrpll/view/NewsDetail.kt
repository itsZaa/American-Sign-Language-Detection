package com.example.tubesrpll.view

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.tubesrpll.R
import com.example.tubesrpll.model.NewsItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class NewsDetail : AppCompatActivity() {

    // Deklarasi variabel untuk komponen UI
    private lateinit var profileImageView: ImageView
    private lateinit var textView2: TextView
    private lateinit var textDetailHeadline: TextView
    private lateinit var textDetailTime: TextView
    private lateinit var textDetailContent: TextView
    private lateinit var imageDetail: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news_detail)

        // Inisialisasi komponen UI
        textDetailHeadline = findViewById(R.id.textDetailHeadline)
        textDetailTime = findViewById(R.id.textDetailTime)
        textDetailContent = findViewById(R.id.textDetailContent)
        imageDetail = findViewById(R.id.imageView2)

        // Set listener untuk window insets agar tampilan full-screen
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Mendapatkan ID dokumen dari intent dan mengambil detail berita
        val documentId = intent.getStringExtra("documentId")
        documentId?.let {
            fetchNewsDetail(it)
        }

        // Inisialisasi gambar profil dan teks sambutan
        textView2 = findViewById(R.id.textView)
        profileImageView = findViewById(R.id.imageProfile)
        fetchProfileImage()
    }

    // Fungsi untuk mengambil detail berita dari Firestore
    private fun fetchNewsDetail(documentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("news")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val newsItem = document.toObject(NewsItem::class.java)
                    newsItem?.let {
                        displayNewsDetails(it)
                    } ?: run {
                        displayNoNewsAvailable()
                    }
                } else {
                    displayNoNewsAvailable()
                }
            }
            .addOnFailureListener { e ->
                displayErrorFetchingNews(e)
            }
    }

    // Fungsi untuk menampilkan detail berita pada UI
    private fun displayNewsDetails(newsItem: NewsItem) {
        textDetailHeadline.text = newsItem.Headline
        textDetailContent.text = formatContent(newsItem.Content)

        // Format timestamp ke tanggal yang dapat dibaca
        val formattedDate = newsItem.Timestamp?.let {
            SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(it.toDate())
        } ?: "Invalid timestamp"
        textDetailTime.text = formattedDate

        // Memuat gambar berita menggunakan Glide
        if (newsItem.Image.isNotEmpty()) {
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(newsItem.Image)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .override(850, 750)
                    .into(imageDetail)
            }.addOnFailureListener { e ->
                Log.e("FirebaseStorage", "Error fetching image", e)
            }
        }
    }

    // Fungsi untuk menampilkan pesan ketika berita tidak tersedia
    private fun displayNoNewsAvailable() {
        textDetailHeadline.text = "Berita tidak tersedia"
        textDetailContent.text = ""
        textDetailTime.text = "Tanggal tidak diketahui"
    }

    // Fungsi untuk menangani kesalahan saat mengambil berita
    private fun displayErrorFetchingNews(e: Exception) {
        textDetailHeadline.text = "Gagal mengambil berita"
        textDetailContent.text = ""
        textDetailTime.text = ""
        Log.e("Firestore", "Error fetching news", e)
    }

    // Fungsi untuk mengambil gambar profil dari Firestore
    private fun fetchProfileImage() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            val userId = it.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val profileImage = document.getString("profileImage")
                        // Memuat gambar profil menggunakan Picasso
                        if (!profileImage.isNullOrEmpty()) {
                            Picasso.get().load(profileImage).into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.baseline_person_24)
                        }

                        // Mengatur teks sambutan dengan nama pengguna
                        val userName = document.getString("name")
                        textView2.text = if (!userName.isNullOrEmpty()) {
                            "Selamat datang $userName"
                        } else {
                            "Selamat datang guest"
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Gagal mengambil gambar profil: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error fetching profile image", exception)
                    textView2.text = "Selamat datang guest"
                }
        } ?: run {
            profileImageView.setImageResource(R.drawable.baseline_person_24)
            textView2.text = "Selamat datang guest"
        }
    }

    // Fungsi untuk memformat konten berita agar lebih mudah dibaca
    private fun formatContent(content: String): String {
        val sentences = content.split(". ")
        val stringBuilder = StringBuilder()
        var sentenceCount = 0

        for (sentence in sentences) {
            // Menambahkan baris baru setiap 5 kalimat untuk keterbacaan
            if (sentenceCount % 5 == 0 && sentenceCount != 0) {
                stringBuilder.append(".\n\n")
            } else if (sentenceCount != 0) {
                stringBuilder.append(". ")
            }
            stringBuilder.append(sentence.trim())
            sentenceCount++
        }
        return stringBuilder.toString().trim()
    }
}
