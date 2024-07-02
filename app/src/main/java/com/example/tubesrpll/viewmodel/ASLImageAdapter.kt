package com.example.tubesrpll.viewmodel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tubesrpll.R
import com.google.firebase.storage.StorageReference

class ASLImageAdapter(private val context: Context) : RecyclerView.Adapter<ASLImageAdapter.ASLImageViewHolder>() {

    private var imageList: List<List<StorageReference>> = emptyList()

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

    fun updateImageList(newImageList: List<List<StorageReference>>) {
        imageList = newImageList
        notifyDataSetChanged()
    }

    inner class ASLImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerViewASLLine)
        private val innerAdapter = InnerASLAdapter(context)

        init {
            recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.adapter = innerAdapter
        }

        fun bind(imageRefs: List<StorageReference>) {
            innerAdapter.updateImageList(imageRefs)
        }
    }
}

class InnerASLAdapter(private val context: Context) : RecyclerView.Adapter<InnerASLAdapter.InnerASLViewHolder>() {

    private var imageList: List<StorageReference> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerASLViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_asl_image_line, parent, false)
        return InnerASLViewHolder(view)
    }

    override fun onBindViewHolder(holder: InnerASLViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    fun updateImageList(newImageList: List<StorageReference>) {
        imageList = newImageList
        notifyDataSetChanged()
    }

    inner class InnerASLViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
