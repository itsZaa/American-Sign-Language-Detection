package com.example.tubesrpll.model

import com.google.firebase.Timestamp

//Data class untuk merepresentasikan item news.
data class NewsItem(
    val id: String = "", //ID unik untuk setiap berita.
    val Content: String = "", //Isi atau konten dari berita.
    val Headline: String = "", //Judul dari berita.
    val Image: String = "", //Image URL atau path gambar yang terkait dengan berita.
    val Timestamp: Timestamp? = null //Waktu kapan berita diunggah, menggunakan Timestamp dari Firebase.
)
