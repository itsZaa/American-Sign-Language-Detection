package com.example.tubesrpll.model

data class User (
    val id: String = "", //ID User
    val name: String = "", //Nama User
    val phone: String = "", //No telp User
    val role: String = "user", //Role User (users/admin)
)
