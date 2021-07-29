package com.insspring.alterablebottomsheet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.item_spinner_text.view.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(root)

        root.vBtnMainShow.setOnClickListener {
            val intent = Intent(this, BottomSheetExampleActivity::class.java).apply {
                putExtra("backgroundRes", root.vSpBackground.selectedItem as String)
                putExtra("isDraggable", root.vSpIsDraggable.selectedItem as String)
                putExtra("type", root.vSpType.selectedItem as String)
                putExtra("intermediateHeight", root.vEtIntermediateHeight.text.toString().toInt())
                putExtra("height", root.vEtHeight.text.toString().toInt())
                putExtra("marginTop", root.vEtMarginTop.text.toString().toInt())
            }
            startActivity(intent)
        }

        root.vSpBackground.apply {
            val bgs_strs = ArrayList<String>().apply {
                add("bg_0")
                add("bg_1")
                add("bg_2")
            }
            adapter =
                CustomSpinnerAdapter(context, R.layout.item_spinner_text, bgs_strs as List<String>)
        }

        root.vSpIsDraggable.apply {
            val bools = ArrayList<String>().apply {
                add("true")
                add("false")
            }
            adapter =
                CustomSpinnerAdapter(context, R.layout.item_spinner_text, bools as List<String>)
        }

        root.vSpType.apply {
            val types = ArrayList<String>().apply {
                add("default_type")
                add("without_hide")
                add("mixed")
            }
            adapter =
                CustomSpinnerAdapter(context, R.layout.item_spinner_text, types as List<String>)
        }
    }
}


class CustomSpinnerAdapter(
    context: Context, textViewResourceId: Int,
    private val objects: List<String>,
) :
    ArrayAdapter<String?>(context, textViewResourceId, objects) {
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    override fun getCount(): Int {
        return objects.size
    }

    private fun getCustomView(
        position: Int, convertView: View?,
        parent: ViewGroup,
    ): View {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_spinner_text, parent, false)

        view.vTvString.text = objects[position]

        return view
    }
}
