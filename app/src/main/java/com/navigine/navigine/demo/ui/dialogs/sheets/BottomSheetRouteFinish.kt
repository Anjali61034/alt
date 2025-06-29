package com.navigine.navigine.demo.ui.dialogs.sheets

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.navigine.navigine.demo.R

class BottomSheetRouteFinish : BottomSheetDialogFragment {
    private var mButton: MaterialButton? = null

    private var onFinishAction: Runnable? = null

    var isShown: Boolean = false
        private set

    private constructor()

    constructor(onFinishAction: Runnable?) {
        this.onFinishAction = onFinishAction
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sheet_bottom_route_finish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews(view)
        setViewsListeners()
    }

    private fun initViews(view: View) {
        mButton = view.findViewById<MaterialButton?>(R.id.sheet_bottom_route_finish_button)
    }

    private fun setViewsListeners() {
        if (mButton != null) mButton!!.setOnClickListener(View.OnClickListener { v: View? -> dismiss() })

        val dialog = getDialog()
        if (dialog != null) {
            dialog.setOnShowListener(DialogInterface.OnShowListener { dialog1: DialogInterface? ->
                this.isShown = true
            })
            dialog.setOnDismissListener(DialogInterface.OnDismissListener { dialog12: DialogInterface? ->
                if (onFinishAction != null) onFinishAction!!.run()
                this.isShown = false
            })
        }
    }
}
