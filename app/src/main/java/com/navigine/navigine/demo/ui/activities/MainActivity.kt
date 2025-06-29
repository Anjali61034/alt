package com.navigine.navigine.demo.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.navigine.navigine.demo.R
import com.navigine.navigine.demo.models.BeaconMock
import com.navigine.navigine.demo.viewmodel.SharedViewModel
import com.navigine.navigine.demo.service.NavigationWorker
import com.navigine.navigine.demo.ui.custom.navigation.SavedBottomNavigationView
import com.navigine.navigine.demo.utils.NavigineSdkManager
import com.navigine.idl.java.NavigineSdk

class MainActivity : AppCompatActivity() {

    private var viewModel: SharedViewModel? = null
    private var mBottomNavigation: SavedBottomNavigationView? = null
    private var navGraphIds: MutableList<Int> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Navigine SDK
        // Set your user hash and server URL before getting the instance
        val sdk = NavigineSdk.getInstance()
        sdk.setUserHash("A9A7-BAAC-6B7F-313D")
        sdk.setServer("https://ips.navigine.com")

        initViewModel()
        initNavigationView()
        startNavigationService()

        // Example usage: check location after startup
        useLocation()
    }

    override fun onStart() {
        super.onStart()
        // You can also call useLocation() here if needed
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
    }

    private fun initNavigationView() {
        mBottomNavigation = findViewById(R.id.main__bottom_navigation)
        navGraphIds = mutableListOf(
            R.navigation.navigation_locations,
            R.navigation.navigation_navigation,
            R.navigation.navigation_debug,
            R.navigation.navigation_profile
        )

        mBottomNavigation?.setupWithNavController(
            navGraphIds,
            supportFragmentManager,
            R.id.nav_host_fragment_activity_main,
            intent
        )
    }

    private fun addBeaconGenerator() {
        NavigineSdkManager.MeasurementManager.addBeaconGenerator(
            BeaconMock.UUID,
            BeaconMock.MAJOR,
            BeaconMock.MINOR,
            BeaconMock.POWER,
            BeaconMock.TIMEOUT,
            BeaconMock.RSS_MIN,
            BeaconMock.RSS_MAX
        )
    }

    private fun startNavigationService() {
        val constraints = Constraints.Builder().build()
        val request = OneTimeWorkRequest.Builder(NavigationWorker::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(request)
    }

    // Method to safely access location data
    private fun useLocation() {
        val navigineInstance = NavigineSdk.getInstance()
        if (navigineInstance != null && NavigineSdkManager.LocationManager != null) {
            val locationId = NavigineSdkManager.LocationManager?.locationId
            if (locationId != null) {
                // Do something with the location
                Log.d("MainActivity", "Current location: $locationId")
            } else {
                Log.w("MainActivity", "Location ID is null")
            }
        } else {
            // SDK not initialized yet
            Log.w("MainActivity", "Navigine SDK or LocationManager is not initialized yet")
        }
    }
}