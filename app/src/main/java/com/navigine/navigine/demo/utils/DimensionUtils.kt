package com.navigine.navigine.demo.utils

import android.util.DisplayMetrics
import android.util.TypedValue
import com.navigine.navigine.demo.R
import com.navigine.navigine.demo.application.NavigineApp

object DimensionUtils {
    @JvmField
    var STROKE_WIDTH: Int =
        NavigineApp.AppContext!!.getResources().getDimension(R.dimen.search_stroke_width).toInt()

    // Display settings
    private var DisplayMetrics: DisplayMetrics? = null

    var DisplayWidthPx: Float = 0.0f
    var DisplayHeightPx: Float = 0.0f
    var DisplayWidthDp: Float = 0.0f
    var DisplayHeightDp: Float = 0.0f
    @JvmField
    var DisplayDensity: Float = 0.0f

    fun setDisplayMetrics(displayMetrics: DisplayMetrics) {
        DisplayWidthPx = displayMetrics.widthPixels.toFloat()
        DisplayHeightPx = displayMetrics.heightPixels.toFloat()
        DisplayDensity = displayMetrics.density
        DisplayWidthDp = DisplayWidthPx / DisplayDensity
        DisplayHeightDp = DisplayHeightPx / DisplayDensity
        DisplayMetrics = displayMetrics
    }

    @JvmStatic
    fun pxFromDp(dp: Int): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), DisplayMetrics)
    }
}
