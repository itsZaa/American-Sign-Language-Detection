package com.example.tubesrpll.view

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tubesrpll.viewmodel.NewsAdapter
import com.example.tubesrpll.R
import com.example.tubesrpll.model.NewsItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

/**
 * Aktivity untuk menampilkan home page.
 */
class Home : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var profileImageView: ImageView
    private lateinit var textView2: TextView
    private lateinit var recyclerView: RecyclerView
    private val newsList = mutableListOf<NewsItem>()
    private lateinit var adapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Inisialisasi FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Inisialisasi RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NewsAdapter(newsList)
        recyclerView.adapter = adapter

        textView2 = findViewById(R.id.textView)
        profileImageView = findViewById(R.id.imageProfile) // Ensure you have this ImageView in your layout

        // Menyiapkan teks klik untuk "View All News"
        val textViewAllNews = findViewById<TextView>(R.id.textViewAllNews)
        val text = "View All News"
        val spannableString = SpannableString(text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@Home, AllNews::class.java)
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

        // Mengambil berita utama dan gambar profil
        getMainNews()
        fetchProfileImage()
    }

    override fun onResume() {
        super.onResume()
        fetchProfileImage()
    }

    /**
     * Mengambil gambar profil pengguna dari Firestore.
     */
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

    /**
     * Mengambil berita utama dari Firestore.
     */
    private fun getMainNews() {
        val db = FirebaseFirestore.getInstance()
        db.collection("news")
            .orderBy("Timestamp", Query.Direction.DESCENDING) // Order by timestamp descending
            .limit(1) // Limit to the latest document
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val newsItem = document.toObject<NewsItem>()!!.copy(id = document.id)
                    newsList.add(newsItem)
                    adapter.notifyDataSetChanged()
                } else {
                    Log.d("Home", "No news found")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Home", "Error getting documents.", exception)
            }
    }

    /**
     * Pindah ke aktivitas TranslateVideoASLToText jika pengguna sudah login.
     */
    fun moveToVideoASLToText(view: View) {
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
                            val intent = Intent(this, TranslateVideoASLToText::class.java)
                            startActivity(intent)
                        }
                    }
                }

        } else {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            builder.setMessage("You need to login to \n" +
                    "use the features")
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

    /**
     * Pindah ke aktivitas TranslateVideoBISINDOToText jika pengguna sudah login.
     */
    fun moveToVideoBISINDOToText(view: View) {
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
                            val intent = Intent(this, TranslateVideoBISINDOToText::class.java)
                            startActivity(intent)
                        }
                    }
                }

        } else {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            builder.setMessage("You need to login to \n" +
                    "use the features")
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

    /**
     * Pindah ke aktivitas TranslateTextToASL jika pengguna sudah login.
     */
    fun moveToTextToASL(view: View) {
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
                            val intent = Intent(this, TranslateTextToASL::class.java)
                            startActivity(intent)
                        }
                    }
                }
        } else {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            builder.setMessage(
                "You need to login to \n" +
                        "use the features"
            )
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

    /**
     * Pindah ke aktivitas TranslateTextToBISINDO jika pengguna sudah login.
     */
    fun moveToTextToBISINDO(view: View) {
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
                            val intent = Intent(this, TranslateTextToBISINDO::class.java)
                            startActivity(intent)
                        }
                    }
                }
        } else {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            builder.setMessage(
                "You need to login to \n" +
                        "use the features"
            )
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

    /**
     * Pindah ke halaman profil jika pengguna sudah login, atau ke halaman SignIn jika belum login.
     */
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
                        }
                    }
                }
        } else {
            val intent = Intent(this, SignIn::class.java)
            startActivity(intent)
        }
    }
}
