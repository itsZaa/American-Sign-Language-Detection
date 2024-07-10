package com.example.tubesrpll.model

import com.google.firebase.Timestamp

data class NewsItem(
    val id: String = "", //ID berita
    val Content: String = "", //Konten berita
    val Headline: String = "", //Judul Berita
    val Image: String = "", //Path Gambar Berita
    val Timestamp: Timestamp? = null //Timestamp berita ditambahkan
)
