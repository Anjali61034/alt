package com.navigine.navigine.demo.adapters.debug

import android.content.ClipboardManager
import android.content.Context
import android.os.Vibrator
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

abstract class DebugAdapterBase<T : RecyclerView.ViewHolder?, V> : RecyclerView.Adapter<T?>() {
    @JvmField
    protected var mContext: Context? = null
    @JvmField
    protected var mRecyclerView: RecyclerView? = null
    @JvmField
    protected var mClipboardManager: ClipboardManager? = null
    @JvmField
    protected var mGestureDetector: GestureDetector? = null
    private var mVibrator: Vibrator? = null

    @JvmField
    protected var mCurrentList: MutableList<V?> = ArrayList<V?>()

    @JvmField
    protected var isPressed: Boolean = false
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
        mContext = mRecyclerView!!.getContext()
        mClipboardManager =
            mContext!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        mVibrator = mContext!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?

        mGestureDetector = GestureDetector(mContext, object : GestureDetector.OnGestureListener {

            override fun onDown(e: MotionEvent): Boolean {
                return false
            }

            override fun onShowPress(e: MotionEvent) {
                TODO("Not yet implemented")
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return false
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                onCopyContent()
                onVibrate()
                isPressed = false
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                TODO("Not yet implemented")
            }
        })
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return TYPE_ROUNDED_TOP
        if (position == mCurrentList.size - 1) return TYPE_ROUNDED_BOTTOM
        return TYPE_RECT
    }

    override fun getItemCount(): Int {
        return mCurrentList.size
    }

    open fun submit(list: MutableList<V?>) {
        mCurrentList.clear()
        mCurrentList.addAll(list)
        notifyDataSetChanged()
    }

    private fun onVibrate() {
        mVibrator!!.vibrate(VIBRATION_DELAY.toLong())
    }

    abstract fun onCopyContent()

    companion object {
        const val VIBRATION_DELAY: Int = 75

        private const val TYPE_ROUNDED_TOP = 0
        private const val TYPE_ROUNDED_BOTTOM = 1
        private const val TYPE_RECT = 2

        @JvmField
        protected var isRootScrolling: Boolean = false

        @JvmStatic
        fun setRootView(view: View?) {
            if (view is NestedScrollView) {
                view.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
                    isRootScrolling = abs((scrollY - oldScrollY).toDouble()) > 2
                })
            }
        }
    }
}
