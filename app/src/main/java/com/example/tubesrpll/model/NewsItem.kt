package com.example.tubesrpll.model

import com.google.firebase.Timestamp

data class NewsItem(
    val id: String = "",
    val Content: String = "",
    val Headline: String = "",
    val Image: String = "",
    val Timestamp: Timestamp? = null
)
