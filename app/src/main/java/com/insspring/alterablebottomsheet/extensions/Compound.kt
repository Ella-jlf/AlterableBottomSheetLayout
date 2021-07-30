package com.insspring.alterablebottomsheet.extensions

import android.content.res.Resources
import android.util.TypedValue

fun Number.dpToPx(): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()
}
