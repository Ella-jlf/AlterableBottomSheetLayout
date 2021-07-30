package com.insspring.alterablebottomsheet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.insspring.alterablebottomsheet.examplelist.QuickAdapter
import com.insspring.alterablebottomsheet.extensions.dpToPx
import com.insspring.alterablebottomsheet.view.ForegroundType
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.item_spinner_text.view.*


class MainActivity : AppCompatActivity() {
    val bgRes = HashMap<String, Int>().also {
        it["bg_0"] = R.drawable.bg_round_corners
        it["bg_1"] = R.drawable.bg_round_corners_1
        it["bg_2"] = R.drawable.bg_round_corners_2
    }
    val types = HashMap<String, ForegroundType>().also {
        it["default_type"] = ForegroundType.DefaultType
        it["without_hide"] = ForegroundType.WithoutHide
        it["mixed"] = ForegroundType.Mixed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(root)

        root.vBtnMainShow.setOnClickListener {
            root.vgBsMain.show()
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

        root.vRvMain.apply {
            adapter = QuickAdapter()
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

        root.vBtnMainShow.setOnClickListener {
            root.vgBsMain.show()
        }

        root.vBtnMainApply.setOnClickListener(applyProperties)
    }

    private val applyProperties = object : View.OnClickListener {
        override fun onClick(v: View?) {
            if (v == null)
                return

            val root = v.rootView

            val bg = root.vSpBackground.selectedItem
            root.vgBsMain.apply {
                bgRes[bg]?.let {
                    setBackground(it)
                }
            }

            val type = root.vSpType.selectedItem
            root.vgBsMain.apply {
                types[type]?.let {
                    setType(it)
                }
            }

            val intermediate = root.vEtIntermediateHeight.text.toString().toInt().dpToPx()
            root.vgBsMain.apply {
                setIntermediate(intermediate)
            }

            var height = root.vEtHeight.text.toString().toInt()
            if (height != -1 && height != -2)
                height = height.dpToPx()
            root.vgBsMain.apply {
                setHeight(height)
            }

            val marginTop = root.vEtMarginTop.text.toString().toInt().dpToPx()
            root.vgBsMain.apply {
                setMarginTop(marginTop)
            }

            val isDraggableStr = root.vSpIsDraggable.selectedItem
            var isDraggable = true
            if (isDraggableStr == "false")
                isDraggable = false
            root.vgBsMain.apply {
                setIsDrawable(isDraggable)
            }
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
