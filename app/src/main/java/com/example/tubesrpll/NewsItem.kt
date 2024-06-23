package com.example.tubesrpll

import com.google.firebase.Timestamp

data class NewsItem(
    val id: String = "",
    val content: String = "",
    val headline: String = "",
    val image: String = "",
    val timestamp: Timestamp? = null
)
