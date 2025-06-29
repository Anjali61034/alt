package com.navigine.navigine.demo.utils

import com.navigine.navigine.demo.R
import com.navigine.navigine.demo.application.NavigineApp

object ColorUtils {
    @JvmField
    var COLOR_PRIMARY: Int = NavigineApp.AppContext!!.getResources().getColor(R.color.colorPrimary)
    @JvmField
    var COLOR_SECONDARY: Int =
        NavigineApp.AppContext!!.getResources().getColor(R.color.colorSecondary)
}
