package com.example.tubesrpll.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import com.example.tubesrpll.R

class Profile : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var button: FloatingActionButton
    private lateinit var nameProfile: TextView
    private lateinit var phoneProfile: TextView
    private lateinit var emailProfile: TextView
    private lateinit var buttonLogout: Button

    companion object {
        const val IMAGE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Inisialisasi komponen UI
        imageView = findViewById(R.id.imagePhoto)
        button = findViewById(R.id.cameraButton)
        nameProfile = findViewById(R.id.NameProfile)
        phoneProfile = findViewById(R.id.phoneProfile)
        emailProfile = findViewById(R.id.emailProfile)
        buttonLogout = findViewById(R.id.buttonLogout)

        // Listener untuk tombol ambil gambar
        button.setOnClickListener {
            pickImage()
        }

        // Listener untuk tombol logout
        buttonLogout.setOnClickListener {
            logout()
        }

        // Memuat data profil pengguna
        fetchProfileData()
    }

    // Fungsi untuk memilih gambar dari galeri menggunakan ImagePicker library
    private fun pickImage() {
        ImagePicker.with(this)
            .galleryOnly()
            .crop()  // Opsional, untuk memotong gambar
            .start(IMAGE_REQUEST_CODE)
    }

    // Handle hasil dari pemilihan gambar dari galeri
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                imageView.setImageURI(uri)
                uploadImageToFirebase(uri)
            }
        }
    }

    // Fungsi untuk mengunggah gambar ke Firebase Storage
    private fun uploadImageToFirebase(uri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val storageReference = FirebaseStorage.getInstance().reference.child("profile/$userId.jpg")

            storageReference.putFile(uri)
                .addOnSuccessListener {
                    storageReference.downloadUrl.addOnSuccessListener { downloadUri ->
                        updateProfileImage(downloadUri.toString())
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Gagal mengunggah gambar: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FirebaseStorage", "Error uploading image", exception)
                }
        }
    }

    // Fungsi untuk memperbarui URL gambar profil di Firestore
    private fun updateProfileImage(imageUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId)
                .update("profileImage", imageUrl)
                .addOnSuccessListener {
                    Toast.makeText(this, "Gambar profil diperbarui", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Gagal memperbarui gambar profil: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error updating profile image", exception)
                }
        }
    }

    // Fungsi untuk mengambil data profil pengguna dari Firestore
    private fun fetchProfileData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()
            val email = currentUser.email
            emailProfile.text = email

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name")
                        val phone = document.getString("phone")
                        val profileImage = document.getString("profileImage")

                        nameProfile.text = name
                        phoneProfile.text = phone
                        // Memuat gambar profil menggunakan Glide jika tersedia
                        if (profileImage != null && profileImage.isNotEmpty()) {
                            Glide.with(this)
                                .load(profileImage)
                                .into(imageView)
                        }
                    } else {
                        Toast.makeText(this, "Dokumen tidak ditemukan", Toast.LENGTH_SHORT).show()
                        Log.e("Firestore", "Dokumen tidak ditemukan")
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Gagal mengambil data profil: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error fetching profile data", exception)
                }
        } else {
            Toast.makeText(this, "Pengguna belum login", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi untuk pindah ke halaman EditProfile
    fun moveToEditProfile(view: View) {
        val intent = Intent(this, EditProfile::class.java)
        startActivity(intent)
    }

    // Fungsi untuk proses logout pengguna
    private fun logout() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
        builder.setTitle("Logout")
        builder.setMessage("Apakah Anda yakin ingin logout?")
        builder.setPositiveButton("Ya") { dialog, which ->
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, Home::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("Tidak") { dialog, which ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
}