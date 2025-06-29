package com.navigine.navigine.demo.adapters.debug

import android.content.ClipData
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.navigine.idl.java.SignalMeasurement
import com.navigine.navigine.demo.R
import java.util.Locale

abstract class DebugAdapterBaseExpanded<T : DebugViewHolderBase?, V : SignalMeasurement?> :
    DebugAdapterBase<T?, V?>() {
    protected var expand: Boolean = false

    private val copyTextBuilder = StringBuilder()

    open fun onBindViewHolder(holder: T, position: Int) {
        if (mCurrentList.size <= LIST_SIZE_DEFAULT) holder!!.title.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            0,
            0
        )
        else {
            if (expand) holder!!.title.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_arrow_circle_up,
                0
            )
            else holder!!.title.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_arrow_circle_down,
                0
            )
        }
        holder.title.setOnTouchListener(View.OnTouchListener { v: View?, event: MotionEvent? ->
            if (event!!.getX() >= holder.title.getWidth() - holder.title.getTotalPaddingEnd()) {
                if (holder.title.getCompoundDrawables()[2] != null) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        isPressed = false
                        expand = !expand
                        mRecyclerView!!.scheduleLayoutAnimation()
                        v!!.performClick()
                        notifyDataSetChanged()
                    }
                }
            } else {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v!!.postDelayed(Runnable {
                        isPressed = !isRootScrolling
                        if (isPressed) mGestureDetector!!.onTouchEvent(event)
                    }, 300)
                }
            }
            true
        })
    }

    public override fun getItemCount(): Int {
        if (!expand) return LIST_SIZE_DEFAULT
        else return mCurrentList.size + 1
    }

    public override fun submit(list: MutableList<V?>) {
        if (!isPressed) {
            mCurrentList.clear()
            mCurrentList.addAll(list)
            if (mCurrentList.size <= LIST_SIZE_DEFAULT) expand = false
            notifyDataSetChanged()
        }
    }

    override fun onCopyContent() {
        copyTextBuilder.setLength(0)
        for (signalMeasurement in mCurrentList) {
            copyTextBuilder.append(signalMeasurement!!.getId())
            copyTextBuilder.append(" ")
            copyTextBuilder.append(
                String.format(
                    Locale.ENGLISH,
                    "%.1f",
                    signalMeasurement.getRssi()
                )
            )
            copyTextBuilder.append("  ")
            copyTextBuilder.append(
                String.format(
                    Locale.ENGLISH,
                    "%.1fm",
                    signalMeasurement.getDistance()
                )
            )
            copyTextBuilder.append('\n')
        }
        val clip = ClipData.newPlainText("list content", copyTextBuilder.toString())
        mClipboardManager!!.setPrimaryClip(clip)
        Toast.makeText(mContext, R.string.debug_copy_list_content, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val LIST_SIZE_DEFAULT: Int = 6
    }
}
