package com.example.tubesrpll.viewmodel

import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tubesrpll.R
import com.example.tubesrpll.model.NewsItem
import com.example.tubesrpll.view.NewsDetail
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class NewsAdapter(private val newsList: List<NewsItem>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val newsImage: ImageView = itemView.findViewById(R.id.newsImage)
        val newsTitle: TextView = itemView.findViewById(R.id.newsTitle)
        val newsDescription: TextView = itemView.findViewById(R.id.newsDescription)
        val newsDate: TextView = itemView.findViewById(R.id.newsDate)
    }

    // Membuat ViewHolder untuk setiap item dalam RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    // Mengikat data ke ViewHolder pada posisi tertentu
    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = newsList[position]

        // Memuat gambar dari Firebase Storage menggunakan Picasso
        if (newsItem.Image.isNotEmpty()) {
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(newsItem.Image)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Picasso.get().load(uri).into(holder.newsImage)
            }.addOnFailureListener { exception ->
                Log.w("NewsAdapter", "Error getting image URL.", exception)
            }
        } else {
            holder.newsImage.setImageResource(R.drawable.baseline_person_24)
        }

        holder.newsTitle.text = newsItem.Headline

        val firstTwoSentences = getFirstTwoSentences(newsItem.Content)
        val spannableContent = SpannableString("$firstTwoSentences... selengkapnya")

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val context = widget.context
                val intent = Intent(context, NewsDetail::class.java)
                intent.putExtra("documentId", newsItem.id)
                context.startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
            }
        }

        val startIndex = spannableContent.length - "selengkapnya".length
        val endIndex = spannableContent.length
        spannableContent.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        holder.newsDescription.text = spannableContent
        holder.newsDescription.movementMethod = LinkMovementMethod.getInstance()

        // Format timestamp untuk ditampilkan dalam format yang mudah dibaca
        val formattedDate = newsItem.Timestamp?.let {
            SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(it.toDate())
        } ?: "Waktu tidak valid"
        holder.newsDate.text = formattedDate
    }

    // Mengembalikan jumlah total item dalam RecyclerView
    override fun getItemCount(): Int {
        return newsList.size
    }

    // Fungsi bantuan untuk mengekstrak dua kalimat pertama dari konten berita
    private fun getFirstTwoSentences(content: String): String {
        val sentences = content.split(". ")
        return if (sentences.size >= 1) {
            "${sentences[0]}}."
        } else {
            content
        }
    }
}
