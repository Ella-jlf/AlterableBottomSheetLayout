package com.insspring.alterablebottomsheet.examplelist

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.insspring.alterablebottomsheet.R
import kotlinx.android.synthetic.main.item_rv_example.view.*

class QuickViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind() {
        itemView.vIvImage.setOnClickListener {
            Toast.makeText(itemView.context,
                "image $adapterPosition was click",
                Toast.LENGTH_LONG).show()
        }
        itemView.vTvText.setOnClickListener {
            Log.i("RV", "item $adapterPosition clicked")
        }
    }
}

class QuickAdapter : RecyclerView.Adapter<QuickViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickViewHolder {
        return QuickViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_rv_example,
                    parent,
                    false
                ))
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return 12
    }

}