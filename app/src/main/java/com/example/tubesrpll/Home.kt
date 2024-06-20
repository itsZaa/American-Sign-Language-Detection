package com.example.tubesrpll

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.squareup.picasso.Picasso
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class Home : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var mainTextHeadline: TextView
    private lateinit var mainTextContent: TextView
    private lateinit var mainTextTime: TextView
    private lateinit var mainNewsContainer: View
    private lateinit var profileImageView: ImageView
    private lateinit var textView2: TextView

    private var mainNewsId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        auth = FirebaseAuth.getInstance()

        textView2 = findViewById(R.id.textView)
        profileImageView = findViewById(R.id.imageProfile) // Ensure you have this ImageView in your layout

        val textViewAllNews = findViewById<TextView>(R.id.textViewAllNews)

        val text = "View All News"
        val spannableString = SpannableString(text)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@Home, SignUp::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
            }
        }

        spannableString.setSpan(
            clickableSpan,
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textViewAllNews.text = spannableString
        textViewAllNews.movementMethod = LinkMovementMethod.getInstance()

        mainTextHeadline = findViewById(R.id.textHeadline)
        mainTextContent = findViewById(R.id.textContent)
        mainTextTime = findViewById(R.id.textTime)
        mainNewsContainer = findViewById(R.id.newsContent)

        getMainNews()
        fetchProfileImage()

        textViewAllNews.setOnClickListener {
            val intent = Intent(this, AllNews::class.java)
            startActivity(intent)
        }

        profileImageView.setOnClickListener { moveToSignIn(it) }
    }

    override fun onResume() {
        super.onResume()
        fetchProfileImage()
    }

    private fun fetchProfileImage() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val profileImage = document.getString("profileImage")
                        if (profileImage != null && profileImage.isNotEmpty()) {
                            Picasso.get().load(profileImage).into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.baseline_person_24)
                        }

                        val userName = document.getString("name")
                        if (userName != null && userName.isNotEmpty()) {
                            textView2.text = "Welcome $userName"
                        } else {
                            textView2.text = "Welcome Guest"
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to fetch profile image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error fetching profile image", exception)
                    textView2.text = "Welcome Guest"
                }
        } else {
            profileImageView.setImageResource(R.drawable.baseline_person_24)
            textView2.text = "Welcome Guest"
        }
    }

    private fun getMainNews() {
        val db = FirebaseFirestore.getInstance()
        db.collection("news")
            .document("tMTHYlD0sUoV0LcMP3tr")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    mainNewsId = document.id
                    val headline = document.getString("Headline") ?: ""
                    val content = document.getString("Content") ?: ""
                    val timestamp = document.getTimestamp("Timestamp")?.toDate()

                    mainTextHeadline.text = headline

                    val sentences = content.split("(?<=\\.)\\s".toRegex())
                    val excerpt = sentences.take(1).joinToString(" ")

                    val spannableContent = SpannableString("$excerpt... selengkapnya")
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val intent = Intent(widget.context, NewsDetail::class.java)
                            intent.putExtra("documentId", mainNewsId)
                            widget.context.startActivity(intent)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = true
                        }
                    }
                    val startIndex = spannableContent.length - "selengkapnya".length
                    val endIndex = spannableContent.length
                    spannableContent.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                    mainTextContent.text = spannableContent
                    mainTextContent.movementMethod = LinkMovementMethod.getInstance()

                    val formattedDate = timestamp?.let {
                        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(it)
                    } ?: "Invalid timestamp"

                    mainTextTime.text = formattedDate
                } else {
                    mainTextHeadline.text = "No news available"
                    mainTextContent.text = "halo"
                    mainTextTime.text = "17 August 1945"
                }
            }
            .addOnFailureListener { e ->
                mainTextHeadline.text = "Error fetching news"
                mainTextContent.text = ""
                mainTextTime.text = ""
                Log.e("Firestore", "Error fetching news", e)
            }
            .addOnCanceledListener {
                mainTextHeadline.text = "Request canceled"
                mainTextContent.text = ""
                mainTextTime.text = ""
            }
            .addOnCompleteListener {
                Log.d("Firestore", "Request complete")
            }
    }

    fun moveToTextToVideo(view: View) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("name")
                        if (userName != null && userName.isNotEmpty()) {
                            textView2.text = "Welcome $userName"
                            val intent = Intent(this, TranslateVideoToText::class.java)
                            startActivity(intent)
                        }
                    }
                }

        } else {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            builder.setMessage("Silahkan login terlebih dahulu")
                .setCancelable(false)
                .setPositiveButton("Login") { dialog, id ->
                    val loginIntent = Intent(this, SignIn::class.java)
                    startActivity(loginIntent)
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }
    }
    fun moveToSignIn(view: View) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("name")
                        if (userName != null && userName.isNotEmpty()) {
                            textView2.text = "Welcome $userName"
                            val intent = Intent(this, Profile::class.java)
                            startActivity(intent)
                        } else {
                            val intent = Intent(this, SignIn::class.java)
                            startActivity(intent)
                        }
                    }
                }
        }
    }
}
