package com.insspring.alterablebottomsheet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.view.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(view)
        view.vBtnShow.setOnClickListener {
            view.vBS.show()
            Log.i("CLICK", "CLICKED")
        }
    }
}