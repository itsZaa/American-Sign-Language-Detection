package com.example.tubesrpll

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class NewsDetail : AppCompatActivity() {

    private lateinit var textDetailHeadline: TextView
    private lateinit var textDetailTime: TextView
    private lateinit var textDetailContent: TextView
    private lateinit var imageDetail: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news_detail)

        textDetailHeadline = findViewById(R.id.textDetailHeadline)
        textDetailTime = findViewById(R.id.textDetailTime)
        textDetailContent = findViewById(R.id.textDetailContent)
        imageDetail = findViewById(R.id.imageView2)

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
                    val imagePath = document.getString("Image") ?: ""

                    textDetailHeadline.text = headline
                    textDetailContent.text = formatContent(content)

                    val formattedDate = timestamp?.let {
                        SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID")).format(it)
                    } ?: "Tanggal tidak valid"


                    textDetailTime.text = formattedDate

                    // Load the image using Glide
                    if (imagePath.isNotEmpty()) {
                        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imagePath)
                        storageReference.downloadUrl.addOnSuccessListener { uri ->
                            Glide.with(this)
                                .load(uri)
                                .override(850, 750)
                                .into(imageDetail)
                        }.addOnFailureListener { e ->
                            Log.e("FirebaseStorage", "Error fetching image", e)
                        }
                    }
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
            if (sentenceCount % 5 == 0 && sentenceCount != 0) {
                stringBuilder.append(".\n\n")
            } else {
                if (sentenceCount != 0) {
                    stringBuilder.append(". ")
                }
            }
            stringBuilder.append(sentence.trim())
            sentenceCount++
        }
        return stringBuilder.toString().trim()
    }
}
