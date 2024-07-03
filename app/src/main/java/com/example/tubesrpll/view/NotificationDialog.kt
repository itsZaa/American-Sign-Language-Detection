package com.example.tubesrpll.view

import android.content.Context
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tubesrpll.R

object NotificationDialog {
    fun showDialog(context: Context, title: String, message: String) {
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
        val inflater = (context as AppCompatActivity).layoutInflater
        val view = inflater.inflate(R.layout.dialog_notification, null)

        view.findViewById<TextView>(R.id.dialogTitle).text = title
        view.findViewById<TextView>(R.id.dialogMessage).text = message

        val dialog = builder.setView(view).create()

        view.findViewById<ImageButton>(R.id.dialogCloseButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
