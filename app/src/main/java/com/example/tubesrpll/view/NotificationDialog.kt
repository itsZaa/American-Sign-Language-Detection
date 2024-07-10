package com.example.tubesrpll.view

import android.content.Context
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tubesrpll.R

/**
 * Objek utilitas untuk menampilkan dialog notifikasi.
 */
object NotificationDialog {
    /**
     * Menampilkan dialog notifikasi dengan judul dan pesan yang diberikan.
     *
     * @param context Konteks dari aktivitas yang memanggil dialog.
     * @param title Judul dialog.
     * @param message Pesan yang ditampilkan dalam dialog.
     */
    fun showDialog(context: Context, title: String, message: String) {
        // Membuat builder untuk dialog dengan tema khusus
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
        // Mengambil inflater dari konteks yang diberikan
        val inflater = (context as AppCompatActivity).layoutInflater
        // Menggunakan inflater untuk meng-inflate layout dialog
        val view = inflater.inflate(R.layout.dialog_notification, null)

        // Mengatur judul dan pesan dalam dialog
        view.findViewById<TextView>(R.id.dialogTitle).text = title
        view.findViewById<TextView>(R.id.dialogMessage).text = message

        // Membuat dialog dengan view yang telah diatur
        val dialog = builder.setView(view).create()

        // Menambahkan aksi untuk tombol close pada dialog
        view.findViewById<ImageButton>(R.id.dialogCloseButton).setOnClickListener {
            dialog.dismiss() // Menutup dialog saat tombol ditekan
        }

        // Menampilkan dialog
        dialog.show()
    }
}
