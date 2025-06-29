package com.navigine.navigine.demo.ui.dialogs.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.navigine.navigine.demo.R

class BottomSheetVenue : BottomSheetDialogFragment() {
    private var mSheetTitle: TextView? = null
    private var mVenueDescription: TextView? = null
    private var mVenueImage: ImageView? = null
    private var mCloseButton: MaterialButton? = null
    private var mRouteButton: MaterialButton? = null

    private var mTitle: String? = null
    private var mDescription: String? = null
    private var mImageRef: String? = null

    private var onClickListener: View.OnClickListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sheet_bottom_venue, container, false)
        initViews(view)
        setViewsParams()
        setViewsListeners()

        return view
    }

    private fun initViews(view: View) {
        mSheetTitle = view.findViewById<TextView?>(R.id.venue_dialog__title)
        mVenueDescription = view.findViewById<TextView?>(R.id.venue_dialog__description)
        mVenueImage = view.findViewById<ImageView?>(R.id.venue_dialog__image)
        mCloseButton = view.findViewById<MaterialButton?>(R.id.venue_dialog__search_btn_close)
        mRouteButton = view.findViewById<MaterialButton?>(R.id.venue_dialog__route_button)
    }

    private fun setViewsParams() {
        mSheetTitle!!.setText(mTitle)
        mVenueDescription!!.setText(mDescription)

        if (mImageRef != null && mImageRef != "") {
            Glide
                .with(requireActivity())
                .load(mImageRef)
                .apply(RequestOptions().fitCenter())
                .into(mVenueImage!!)
        } else {
            mVenueImage!!.setImageResource(R.drawable.elm_loading_venue_photo)
        }

        mRouteButton!!.setVisibility(VISIBILITY)
        mRouteButton!!.setOnClickListener(onClickListener)
    }

    private fun setViewsListeners() {
        mCloseButton!!.setOnClickListener(View.OnClickListener { v: View? -> dismiss() })
    }

    fun setSheetTitle(title: String?) {
        mTitle = title
    }

    fun setDescription(description: String?) {
        mDescription = description
    }

    fun setImageRef(reference: String?) {
        mImageRef = reference
    }

    fun setRouteButtonClick(listener: View.OnClickListener?) {
        onClickListener = listener
    }

    fun setRouteButtonVisibility(visibility: Int) {
        VISIBILITY = visibility
    }

    companion object {
        private var VISIBILITY = View.VISIBLE
    }
}