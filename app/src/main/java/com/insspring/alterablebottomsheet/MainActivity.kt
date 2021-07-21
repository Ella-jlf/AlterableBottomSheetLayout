package com.insspring.alterablebottomsheet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.item_filter.view.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(view)
        view.vBtnShow.setOnClickListener {
            view.vBS.show()
            Log.i("CLICK", "CLICKED")
        }
        view.vBtnFilterClearChosen.setOnClickListener {
            Log.i("CLICK", "CLEAR")
        }
        view.vRvBottomSheetFilter.adapter = QuickAdapter()
        (view.vRvBottomSheetFilter.layoutManager as LinearLayoutManager).orientation =
            LinearLayoutManager.VERTICAL
    }
}

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
                    R.layout.item_filter,
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