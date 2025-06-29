package com.navigine.navigine.demo.ui.custom.decorators

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class SpaceItemDecorator(private val space: Int) : ItemDecoration() {
    private val spanCount = 5

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = space

        if (parent.getChildLayoutPosition(view) < spanCount) {
            outRect.top = space * 2
        } else {
            outRect.top = 0
        }
    }
}
