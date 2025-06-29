package com.navigine.navigine.demo.utils

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager

object KeyboardController {
    @JvmStatic
    fun hideSoftKeyboard(activity: Activity) {
        if (activity.getCurrentFocus() != null) {
            val inputMethodManager =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus()!!.getWindowToken(), 0
            )
        }
    }
}
