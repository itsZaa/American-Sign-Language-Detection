package com.example.tubesrpll.view

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tubesrpll.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfile : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val nameInput = findViewById<EditText>(R.id.textInputEditNama)
        val radioGroupGender = findViewById<RadioGroup>(R.id.radioGroupGender)
        val phoneInput = findViewById<EditText>(R.id.textInputEditPhone)
        val addressInput = findViewById<EditText>(R.id.textInputEditAddress)
        val saveButton = findViewById<Button>(R.id.buttonSave)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name")
                        nameInput.setText(name)

                        val gender = document.getString("gender")
                        if (gender == "Male") {
                            radioGroupGender.check(R.id.radioMale)
                        } else if (gender == "Female") {
                            radioGroupGender.check(R.id.radioFemale)
                        }

                        phoneInput.setText(document.getString("phone"))
                        addressInput.setText(document.getString("address"))
                    } else {
                        Toast.makeText(this, "No such document", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error getting documents: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString()
            val selectedGenderId = radioGroupGender.checkedRadioButtonId
            val gender = if (selectedGenderId == R.id.radioMale) "Male" else "Female"
            val phone = phoneInput.text.toString()
            val address = addressInput.text.toString()

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || gender.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateProfile(currentUser?.uid, name, gender, phone, address)
        }
    }

    private fun updateProfile(userId: String?, name: String, gender: String, phone: String, address: String) {
        userId?.let {
            db.collection("users").document(it).update(
                mapOf(
                    "name" to name,
                    "gender" to gender,
                    "phone" to phone,
                    "address" to address
                )
            ).addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
