package com.navigine.navigine.demo.ui.activities;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import com.navigine.navigine.demo.R;
import com.navigine.navigine.demo.models.BeaconMock;
import com.navigine.navigine.demo.models.UserSession;
import com.navigine.navigine.demo.viewmodel.SharedViewModel;
import com.navigine.navigine.demo.service.NavigationWorker;
import com.navigine.navigine.demo.ui.custom.navigation.SavedBottomNavigationView;
import com.navigine.navigine.demo.utils.NavigineSdkManager;
import com.navigine.idl.java.NavigineSdk;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SharedViewModel viewModel;
    private SavedBottomNavigationView mBottomNavigation;
    private List<Integer> navGraphIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hardcode user credentials (replace login functionality)
        initializeUserSession();

        // Initialize Navigine SDK
        if (initializeSdk()) {
            initViewModel();
            initNavigationView();
            startNavigationService();
            useLocation();
        } else {
            Log.e("MainActivity", "Failed to initialize Navigine SDK");
            // Handle initialization failure - you might want to show an error or retry
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // You can also call useLocation() here if needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }

    private void initializeUserSession() {
        // Hardcode the user hash and location server (previously entered in login page)
        UserSession.USER_HASH = "A9A7-BAAC-6B7F-313D";  // Replace with your actual user hash
        UserSession.LOCATION_SERVER = "https://ips.navigine.com";  // Replace with your actual server

        // Optional: Set other user session data if needed
        UserSession.USER_NAME = "user";
        UserSession.USER_COMPANY = "company";
        UserSession.USER_EMAIL = "default@mail.com";
        UserSession.USER_AVATAR_URL = "";
    }

    private boolean initializeSdk() {
        try {
            // Initialize Navigine SDK with hardcoded credentials
            NavigineSdk sdk = NavigineSdk.getInstance();
            sdk.setUserHash(UserSession.USER_HASH);
            sdk.setServer(UserSession.LOCATION_SERVER);

            // Initialize SDK managers
            return NavigineSdkManager.initializeSdk();
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to initialize SDK: " + e.getMessage());
            return false;
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);
    }

    private void initNavigationView() {
        mBottomNavigation = findViewById(R.id.main__bottom_navigation);
        navGraphIds = new ArrayList<>();
        navGraphIds.add(R.navigation.navigation_locations);  // Start with locations fragment
        navGraphIds.add(R.navigation.navigation_navigation);
        navGraphIds.add(R.navigation.navigation_debug);
        navGraphIds.add(R.navigation.navigation_profile);

        mBottomNavigation.setupWithNavController(
                navGraphIds,
                getSupportFragmentManager(),
                R.id.nav_host_fragment_activity_main,
                getIntent()
        );
    }

    private void addBeaconGenerator() {
        if (NavigineSdkManager.MeasurementManager != null) {
            NavigineSdkManager.MeasurementManager.addBeaconGenerator(
                    BeaconMock.UUID,
                    BeaconMock.MAJOR,
                    BeaconMock.MINOR,
                    BeaconMock.POWER,
                    BeaconMock.TIMEOUT,
                    BeaconMock.RSS_MIN,
                    BeaconMock.RSS_MAX
            );
        }
    }

    private void startNavigationService() {
        Constraints constraints = new Constraints.Builder().build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(NavigationWorker.class)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(request);
    }

    // Method to safely access location data
    private void useLocation() {
        NavigineSdk navigineInstance = NavigineSdk.getInstance();
        if (navigineInstance != null && NavigineSdkManager.LocationManager != null) {
            Integer locationId = NavigineSdkManager.LocationManager.getLocationId();
            if (locationId != null) {
                // Do something with the location
                Log.d("MainActivity", "Current location: " + locationId);
            } else {
                Log.w("MainActivity", "Location ID is null");
            }
        } else {
            // SDK not initialized yet
            Log.w("MainActivity", "Navigine SDK or LocationManager is not initialized yet");
        }
    }
}