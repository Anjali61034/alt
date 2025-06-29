package com.navigine.navigine.demo.ui.custom.lists

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView

open class ListView : ListView {
    private var mListener: OnOverScrollListener? = null

    interface OnOverScrollListener {
        fun onOverScroll(scrollY: Int)
    }

    constructor(context: Context?) : super(context) {
        setOverScrollMode(OVER_SCROLL_ALWAYS)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setOverScrollMode(OVER_SCROLL_ALWAYS)
    }

    fun setOnOverScrollListener(listener: OnOverScrollListener?) {
        mListener = listener
    }

    override fun overScrollBy(
        deltaX: Int, deltaY: Int, scrollX: Int,
        scrollY: Int, scrollRangeX: Int, scrollRangeY: Int,
        maxOverScrollX: Int, maxOverScrollY: Int,
        isTouchEvent: Boolean
    ): Boolean {
        if (mListener != null) mListener!!.onOverScroll(deltaY)
        return super.overScrollBy(0, deltaY, 0, scrollY, 0, scrollRangeY, 0, 0, isTouchEvent)
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }
}