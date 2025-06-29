package com.navigine.navigine.demo.ui.dialogs.sheets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.airbnb.lottie.LottieAnimationView
import com.android.volley.AuthFailureError
import com.android.volley.NetworkError
import com.android.volley.NoConnectionError
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.navigine.navigine.demo.R
import com.navigine.navigine.demo.models.UserSession
import com.navigine.navigine.demo.utils.Constants
import com.navigine.navigine.demo.utils.DimensionUtils
import com.navigine.navigine.demo.utils.NetworkUtils

class BottomSheetHost : BottomSheetDialogFragment() {
    private var mHostEdit: EditText? = null
    private var mCloseButton: MaterialButton? = null
    private var mSubtitle: TextView? = null
    private var mChangeButton: MaterialButton? = null
    private var animContainer: FrameLayout? = null
    private var progressIndicator: CircularProgressIndicator? = null
    private var statusAnim: LottieAnimationView? = null
    private var mHostEditStroke: GradientDrawable? = null

    private var colorSuccess = 0
    private var colorFailed = 0

    private var requestQueue: RequestQueue? = null

    private var textWatcherHost: TextWatcher? = null

    private val ANIM_DURATION = 500

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sheet_bottom_host, container, false)
        initViews(view)
        initViewsListeners()
        setViewsListeners()
        return view
    }

    override fun onStart() {
        super.onStart()
        setViewsParams()
    }

    private fun initViews(view: View) {
        mSubtitle = view.findViewById<TextView?>(R.id.host__sheet_subtitle)
        mCloseButton = view.findViewById<MaterialButton?>(R.id.host__sheet_close_button)
        mHostEdit = view.findViewById<EditText?>(R.id.host__sheet_edit)
        mChangeButton = view.findViewById<MaterialButton?>(R.id.host__sheet_change_button)
        animContainer = view.findViewById<FrameLayout?>(R.id.host__anim_container)
        progressIndicator =
            view.findViewById<CircularProgressIndicator?>(R.id.host__progress_circular_indicator)
        statusAnim = view.findViewById<LottieAnimationView?>(R.id.host__anim_status)
        mHostEditStroke = mHostEdit!!.getBackground() as GradientDrawable?
    }

    private fun initViewsListeners() {
        textWatcherHost = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                mChangeButton!!.setEnabled(charSequence.length >= 1)
            }

            override fun afterTextChanged(editable: Editable?) {
            }
        }
    }

    private fun setViewsListeners() {
        statusAnim!!.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                hideStatusAnimation()
                dismiss()
            }
        })
        mCloseButton!!.setOnClickListener(View.OnClickListener { view: View? ->
            if (requestQueue != null) requestQueue!!.cancelAll(Constants.HOST_VERIFY_TAG)
            dismiss()
        })

        mHostEdit!!.addTextChangedListener(textWatcherHost)

        mChangeButton!!.setOnClickListener(View.OnClickListener { view: View? ->
            cancelHealthCheckRequest()
            showLoadingAnimation()
            disableChangeHostButton()
            if (NetworkUtils.isNetworkActive(view!!.getContext())) {
                sendHealthCheckRequest(createHealthCheckRequest())
            } else {
                hideLoadingAnimation()
                updateHostField(false, getString(R.string.err_network_no_connection))
            }
        })
    }

    private fun setViewsParams() {
        colorSuccess = ContextCompat.getColor(requireActivity(), R.color.colorSuccess)
        colorFailed = ContextCompat.getColor(requireActivity(), R.color.colorError)
        mHostEdit!!.setText(UserSession.LOCATION_SERVER)
    }

    private fun hideStatusAnimation() {
        TransitionManager.beginDelayedTransition(
            (getView() as android.view.ViewGroup?)!!,
            ChangeBounds()
        )
        statusAnim!!.clearAnimation()
        statusAnim!!.setVisibility(View.GONE)
        animContainer!!.setVisibility(View.GONE)
    }

    private fun showLoadingAnimation() {
        TransitionManager.beginDelayedTransition(
            (getView() as android.view.ViewGroup?)!!,
            ChangeBounds()
        )
        animContainer!!.setVisibility(View.VISIBLE)
        progressIndicator!!.show()
    }

    private fun hideLoadingAnimation() {
        progressIndicator!!.hide()
    }

    private fun cancelHealthCheckRequest() {
        if (requestQueue != null) requestQueue!!.cancelAll(Constants.HOST_VERIFY_TAG)
    }

    private fun createHealthCheckRequest(): Request<String?> {
        if (requestQueue == null) requestQueue = Volley.newRequestQueue(requireActivity())

        val stringRequest = StringRequest(
            Request.Method.HEAD,
            this.healthCheckUrl,
            Response.Listener { response: String? -> onHealthCheckSuccess() },
            Response.ErrorListener { error: VolleyError? -> onHealthCheckFail(error!!) })

        stringRequest.setTag(Constants.HOST_VERIFY_TAG)

        return stringRequest
    }

    private fun sendHealthCheckRequest(request: Request<String?>?) {
        if (requestQueue != null) requestQueue!!.add<String?>(request)
    }

    private fun onHealthCheckFail(error: VolleyError) {
        hideLoadingAnimation()
        updateHostField(false, getErrorMessage(error))
        Log.e(Constants.TAG, error.toString())
    }

    private fun onHealthCheckSuccess() {
        updateLocationServer()
        hideLoadingAnimation()
        updateHostField(true, getString(R.string.server_correct))
    }

    private fun updateLocationServer() {
        UserSession.LOCATION_SERVER = mHostEdit!!.getText().toString()
    }

    private fun disableChangeHostButton() {
        mChangeButton!!.setEnabled(false)
    }

    private val healthCheckUrl: String
        get() = mHostEdit!!.getText()
            .toString() + Constants.ENDPOINT_HEALTH_CHECK

    private fun updateHostField(isOperationSuccess: Boolean, infoMessage: String?) {
        val animRes: Int
        val animSize: Int
        val animColor: Int
        val animStartProgress: Float

        if (isOperationSuccess) {
            animRes = R.raw.verify
            animSize = DimensionUtils.pxFromDp(Constants.SIZE_SUCCESS).toInt()
            animColor = colorSuccess
            animStartProgress = 0f
        } else {
            animRes = R.raw.failed
            animSize = DimensionUtils.pxFromDp(Constants.SIZE_FAILED).toInt()
            animColor = colorFailed
            animStartProgress = .2f
        }

        animContainer!!.postDelayed(Runnable {
            statusAnim!!.setAnimation(animRes)
            statusAnim!!.getLayoutParams().height = animSize
            statusAnim!!.setMinProgress(animStartProgress)
            statusAnim!!.setVisibility(View.VISIBLE)
            statusAnim!!.playAnimation()
            mSubtitle!!.setText(infoMessage)
            mSubtitle!!.setTextColor(animColor)
            mHostEditStroke!!.setStroke(4, animColor)
        }, ANIM_DURATION.toLong())
    }

    private fun getErrorMessage(error: VolleyError?): String {
        val message: String?

        if (error is NetworkError) {
            message = getString(R.string.err_network_server_incorrect)
        } else if (error is ServerError) {
            message = getString(R.string.err_network_server)
        } else if (error is AuthFailureError) {
            message = getString(R.string.err_network_auth)
        } else if (error is ParseError) {
            message = getString(R.string.err_network_parse)
        } else if (error is NoConnectionError) {
            message = getString(R.string.err_network_no_connection)
        } else if (error is TimeoutError) {
            message = getString(R.string.err_network_timeout)
        } else message = getString(R.string.err_network_server_incorrect)

        return message
    }
}
