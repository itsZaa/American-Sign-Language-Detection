package com.example.tubesrpll

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.storage.StorageReference

class ASLImageAdapter(private val context: Context) : RecyclerView.Adapter<ASLImageAdapter.ASLImageViewHolder>() {

    private var imageList: List<StorageReference> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ASLImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_asl_image, parent, false)
        return ASLImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ASLImageViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    fun updateImageList(newImageList: List<StorageReference>) {
        imageList = newImageList
        notifyDataSetChanged()
    }

    inner class ASLImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewASL)

        fun bind(imageRef: StorageReference) {

            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(context)
                    .load(uri)
                    .fitCenter()
                    .into(imageView)
            }.addOnFailureListener {
                // Handle any errors
            }
        }
    }
}
