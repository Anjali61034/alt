package com.navigine.navigine.demo.ui.custom.lists

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView
import com.navigine.navigine.demo.R

class ListViewLimit @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ListView(context, attrs, defStyleAttr) {
    private var HEIGHT_MAX = 0

    init {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ListViewLimit)
            HEIGHT_MAX = typedArray.getDimensionPixelSize(R.styleable.ListViewLimit_maxHeight, 0)
            typedArray.recycle()
        } else {
            HEIGHT_MAX = 0
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpec = heightMeasureSpec
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (HEIGHT_MAX > 0 && HEIGHT_MAX < measuredHeight) {
            val measureMode = MeasureSpec.getMode(heightMeasureSpec)
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(HEIGHT_MAX, measureMode)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
