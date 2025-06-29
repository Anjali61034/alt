package com.navigine.navigine.demo.ui.activities;

import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private SharedViewModel viewModel;
    private SavedBottomNavigationView mBottomNavigation;
    private List<Integer> navGraphIds = new ArrayList<>();
    private FirebaseAuth firebaseAuth;
    private boolean isAppInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");

        // Don't set content view immediately - wait for auth check

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        Log.d(TAG, "FirebaseAuth instance created");

        // Use AuthStateListener to properly check authentication state
        firebaseAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                Log.d(TAG, "AuthStateChanged - User: " + (user != null ? user.getEmail() : "null"));

                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "User is authenticated: " + user.getEmail());
                    if (!isAppInitialized) {
                        initializeAppAfterAuth();
                    }
                } else {
                    // User is signed out
                    Log.d(TAG, "User is not authenticated - redirecting to login");
                    redirectToLogin();
                }
            }
        });
    }

    private void initializeAppAfterAuth() {
        Log.d(TAG, "initializeAppAfterAuth() called");

        if (isAppInitialized) {
            Log.d(TAG, "App already initialized, skipping");
            return;
        }

        // Set content view only after authentication
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Content view set");

        // Get user credentials from intent or Firebase
        getUserCredentialsFromLogin();

        // Initialize Navigine SDK only after successful authentication
        if (initializeSdk()) {
            Log.d(TAG, "SDK initialized successfully");
            initViewModel();
            initNavigationView();
            startNavigationService();
            useLocation();
            isAppInitialized = true;
        } else {
            Log.e(TAG, "Failed to initialize Navigine SDK");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed() called");
        finishAffinity();
    }

    private boolean isUserAuthenticated() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        boolean isAuth = currentUser != null;
        Log.d(TAG, "isUserAuthenticated: " + isAuth + " (User: " +
                (currentUser != null ? currentUser.getEmail() : "null") + ")");
        return isAuth;
    }

    private void redirectToLogin() {
        Log.d(TAG, "redirectToLogin() called");
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void getUserCredentialsFromLogin() {
        Log.d(TAG, "getUserCredentialsFromLogin() called");

        // Get data from intent (passed from LoginActivity)
        Intent intent = getIntent();
        String email = intent.getStringExtra("email");
        String name = intent.getStringExtra("name");

        Log.d(TAG, "Intent extras - Email: " + email + ", Name: " + name);

        // Get Firebase user info
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Firebase user - Email: " + currentUser.getEmail() + ", Name: " + currentUser.getDisplayName());
            if (email == null) email = currentUser.getEmail();
            if (name == null) name = currentUser.getDisplayName();
        }

        // Set user session data
        UserSession.USER_HASH = "A9A7-BAAC-6B7F-313D";
        UserSession.LOCATION_SERVER = "https://ips.navigine.com";
        UserSession.USER_NAME = name != null ? name : "user";
        UserSession.USER_EMAIL = email != null ? email : "default@mail.com";
        UserSession.USER_COMPANY = "company";
        UserSession.USER_AVATAR_URL = currentUser != null && currentUser.getPhotoUrl() != null ?
                currentUser.getPhotoUrl().toString() : "";

        Log.d(TAG, "UserSession set - Hash: " + UserSession.USER_HASH + ", Email: " + UserSession.USER_EMAIL);
    }

    private boolean initializeSdk() {
        Log.d(TAG, "initializeSdk() called");

        try {
            if (UserSession.USER_HASH == null || UserSession.USER_HASH.isEmpty()) {
                Log.e(TAG, "User hash not available");
                return false;
            }

            NavigineSdk sdk = NavigineSdk.getInstance();
            sdk.setUserHash(UserSession.USER_HASH);
            sdk.setServer(UserSession.LOCATION_SERVER);

            Log.d(TAG, "SDK configured with hash: " + UserSession.USER_HASH);

            boolean result = NavigineSdkManager.initializeSdk();
            Log.d(TAG, "NavigineSdkManager.initializeSdk() result: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SDK: " + e.getMessage(), e);
            return false;
        }
    }

    private void initViewModel() {
        Log.d(TAG, "initViewModel() called");
        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);
    }

    private void initNavigationView() {
        Log.d(TAG, "initNavigationView() called");

        mBottomNavigation = findViewById(R.id.main__bottom_navigation);
        navGraphIds = new ArrayList<>();
        navGraphIds.add(R.navigation.navigation_locations);
        navGraphIds.add(R.navigation.navigation_navigation);
        navGraphIds.add(R.navigation.navigation_debug);
        navGraphIds.add(R.navigation.navigation_profile);

        Log.d(TAG, "Setting up navigation with " + navGraphIds.size() + " graphs");

        mBottomNavigation.setupWithNavController(
                navGraphIds,
                getSupportFragmentManager(),
                R.id.nav_host_fragment_activity_main,
                getIntent()
        );

        Log.d(TAG, "Navigation setup complete");
    }

    private void addBeaconGenerator() {
        Log.d(TAG, "addBeaconGenerator() called");
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
            Log.d(TAG, "Beacon generator added");
        } else {
            Log.w(TAG, "MeasurementManager is null, cannot add beacon generator");
        }
    }

    private void startNavigationService() {
        Log.d(TAG, "startNavigationService() called");

        Constraints constraints = new Constraints.Builder().build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(NavigationWorker.class)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(request);

        Log.d(TAG, "Navigation service started");
    }

    private void useLocation() {
        Log.d(TAG, "useLocation() called");

        NavigineSdk navigineInstance = NavigineSdk.getInstance();
        if (navigineInstance != null && NavigineSdkManager.LocationManager != null) {
            Integer locationId = NavigineSdkManager.LocationManager.getLocationId();
            if (locationId != null) {
                Log.d(TAG, "Current location: " + locationId);
            } else {
                Log.w(TAG, "Location ID is null");
            }
        } else {
            Log.w(TAG, "Navigine SDK or LocationManager is not initialized yet");
        }
    }

    public void logout() {
        Log.d(TAG, "logout() called");
        firebaseAuth.signOut();
        redirectToLogin();
    }
}