package com.navigine.navigine.demo.ui.custom.lists

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.AbsListView

class BottomSheetListView(context: Context?, p_attrs: AttributeSet?) : ListView(context, p_attrs) {
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (canScrollVertically(this)) getParent().requestDisallowInterceptTouchEvent(true)
        return super.onTouchEvent(ev)
    }

    fun canScrollVertically(view: AbsListView?): Boolean {
        var canScroll = false

        if (view != null && view.getChildCount() > 0) {
            val isOnTop = view.getFirstVisiblePosition() != 0 || view.getChildAt(0).getTop() != 0
            val isAllItemsVisible = isOnTop && view.getLastVisiblePosition() == view.getChildCount()

            if (isOnTop || isAllItemsVisible) canScroll = true
        }

        return canScroll
    }
}