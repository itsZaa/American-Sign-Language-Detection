package com.example.tubesrpll

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class NewsDetail : AppCompatActivity() {

    private lateinit var textDetailHeadline: TextView
    private lateinit var textDetailTime: TextView
    private lateinit var textDetailContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news_detail)

        textDetailHeadline = findViewById(R.id.textDetailHeadline)
        textDetailTime = findViewById(R.id.textDetailTime)
        textDetailContent = findViewById(R.id.textDetailContent)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val documentId = intent.getStringExtra("documentId")
        if (documentId != null) {
            fetchNewsDetail(documentId)
        }
    }

    private fun fetchNewsDetail(documentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("news")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val headline = document.getString("Headline") ?: ""
                    val content = document.getString("Content") ?: ""
                    val timestamp = document.getTimestamp("Timestamp")?.toDate()

                    textDetailHeadline.text = headline
                    textDetailContent.text = formatContent(content)

                    val formattedDate = timestamp?.let {
                        SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(it)
                    } ?: "Invalid timestamp"

                    textDetailTime.text = formattedDate
                } else {
                    textDetailHeadline.text = "No news available"
                    textDetailContent.text = ""
                    textDetailTime.text = "Unknown date"
                }
            }
            .addOnFailureListener { e ->
                textDetailHeadline.text = "Error fetching news"
                textDetailContent.text = ""
                textDetailTime.text = ""
                Log.e("Firestore", "Error fetching news", e)
            }
    }

    private fun formatContent(content: String): String {
        val sentences = content.split(". ")
        val stringBuilder = StringBuilder()
        var sentenceCount = 0

        for (sentence in sentences) {
            if (sentenceCount % 5 == 0) {
                stringBuilder.append(".\n\n")
            } else {
                stringBuilder.append(". ")
            }
            stringBuilder.append(sentence.trim())
            sentenceCount++
        }
        return stringBuilder.toString().trim()
    }
}