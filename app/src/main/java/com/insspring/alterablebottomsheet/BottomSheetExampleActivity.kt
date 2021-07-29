package com.insspring.alterablebottomsheet

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.insspring.alterablebottomsheet.examplelist.QuickAdapter
import com.insspring.alterablebottomsheet.view.ForegroundType
import kotlinx.android.synthetic.main.activity_bottom_sheet_example.view.*

class BottomSheetExampleActivity : AppCompatActivity() {
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
        val root = layoutInflater.inflate(R.layout.activity_bottom_sheet_example, null)
        setContentView(root)

        val bg = intent.getStringExtra("backgroundRes")
        root.vBsExample.apply {
            bgRes[bg]?.let {
                setBackground(it)
            }
        }

        val type = intent.getStringExtra("type")
        root.vBsExample.apply {
            types[type]?.let {
                setType(it)
            }
        }

        val intermediate = intent.getIntExtra("intermediateHeight", 300)
        root.vBsExample.apply {
            setIntermediate(intermediate)
        }

        val height = intent.getIntExtra("height", ViewGroup.LayoutParams.MATCH_PARENT)
        root.vBsExample.apply {
            setHeight(height)
        }

        val marginTop = intent.getIntExtra("marginTop", 340)
        root.vBsExample.apply {
            setMarginTop(marginTop)
        }

        val isDraggableStr = intent.getStringExtra("isDraggable")
        var isDraggable = true
        if (isDraggableStr == "false")
            isDraggable = false
        root.vBsExample.apply {
            setIsDrawable(isDraggable)
        }



        root.vRvBottomSheetFilter.adapter = QuickAdapter()
        (root.vRvBottomSheetFilter.layoutManager as LinearLayoutManager).orientation =
            LinearLayoutManager.VERTICAL

        root.vBtnExampleShow.setOnClickListener {
            root.vBsExample.show()
        }
    }
}