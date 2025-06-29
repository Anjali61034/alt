package com.navigine.navigine.demo.ui.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.navigine.navigine.demo.R
import com.navigine.navigine.demo.utils.Constants
import com.navigine.navigine.demo.utils.PermissionUtils

abstract class BaseFragment : Fragment() {
    private var locationManager: LocationManager? = null
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    private var receiver: StateReceiver? = null
    private var filter: IntentFilter? = null

    @JvmField
    protected var bluetoothState: String? = null
    @JvmField
    protected var geoLocationState: String? = null

    private var mNavigationView: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSystemServices()
        initBroadcastReceiver()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mNavigationView =
            requireActivity().findViewById<BottomNavigationView?>(R.id.main__bottom_navigation)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            updateStatusBar()
            updateUiState()
            updateWarningMessageState()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
        updateGeolocationState()
        updateBluetoothState()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver()
    }

    protected abstract fun updateStatusBar()

    protected open fun updateUiState() {}

    protected open fun updateWarningMessageState() {}

    protected fun onGpsStateChanged() {
        updateGeolocationState()
        updateWarningMessageState()
    }

    protected fun onBluetoothStateChanged() {
        updateBluetoothState()
        updateWarningMessageState()
    }

    private fun updateGeolocationState() {
        geoLocationState =
            if (this.isGpsEnabled) getString(R.string.state_on) else getString(R.string.state_off)
    }

    private fun updateBluetoothState() {
        bluetoothState =
            if (this.isBluetoothEnabled) getString(R.string.state_on) else getString(R.string.state_off)
    }

    private fun initSystemServices() {
        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        bluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager!!.getAdapter()
    }

    private fun initBroadcastReceiver() {
        receiver = StateReceiver() // Correct: inner class instance
        filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        filter!!.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter!!.addAction(Constants.LOCATION_CHANGED)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver() {
        requireActivity().registerReceiver(receiver, filter)
    }

    private fun unregisterReceiver() {
        requireActivity().unregisterReceiver(receiver)
    }

    protected val isGpsEnabled: Boolean
        get() {
            if (locationManager != null) {
                val isGpsEnabled =
                    locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled =
                    locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                return isGpsEnabled || isNetworkEnabled
            } else return false
        }

    protected val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter != null && bluetoothAdapter!!.isEnabled()

    protected fun hasLocationPermission(): Boolean {
        return PermissionUtils.hasLocationPermission(requireActivity())
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    protected fun hasBluetoothPermission(): Boolean {
        return PermissionUtils.hasBluetoothPermission(requireActivity())
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    protected fun hasBackgroundLocationPermission(): Boolean {
        return PermissionUtils.hasLocationBackgroundPermission(requireActivity())
    }

    protected fun openLocationsScreen() {
        mNavigationView!!.setSelectedItemId(R.id.navigation_locations)
    }

    // Declare inner class as 'inner'
    private inner class StateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                LocationManager.PROVIDERS_CHANGED_ACTION -> onGpsStateChanged()
                BluetoothAdapter.ACTION_STATE_CHANGED -> onBluetoothStateChanged()
            }
        }
    }
}