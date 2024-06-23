package com.example.tubesrpll

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase


class AllNews : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter
    private val newsList = mutableListOf<NewsItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_news)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NewsAdapter(newsList)
        recyclerView.adapter = adapter

        fetchAllNews()
    }

    private fun fetchAllNews() {
        val db = Firebase.firestore
        db.collection("news")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val newsItem = document.toObject<NewsItem>()
                    newsList.add(newsItem)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w("AllNewsActivity", "Error getting documents.", exception)
            }
    }
}
