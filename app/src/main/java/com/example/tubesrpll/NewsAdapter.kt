package com.example.tubesrpll

import android.content.ContentValues.TAG
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.TextPaint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = newsList[position]

        // Load image from Firebase Storage using Picasso
        Picasso.get().load(newsItem.image).into(holder.newsImage)

        // Bind other data to TextViews
        holder.newsTitle.text = newsItem.headline
        holder.newsDescription.text = newsItem.content

        // Format timestamp to display in a readable format
        val formattedDate = newsItem.timestamp?.let {
            SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(it)
        } ?: "Invalid timestamp"
        holder.newsDate.text = formattedDate
    }

    override fun getItemCount(): Int {
        return newsList.size
    }
}
