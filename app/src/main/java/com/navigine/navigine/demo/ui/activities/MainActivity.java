package com.navigine.navigine.demo.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

// Navigation Helper Class
class NavigationHelper {
    private SavedBottomNavigationView bottomNav;

    public NavigationHelper(SavedBottomNavigationView bottomNav) {
        this.bottomNav = bottomNav;
    }

    public void setup(List<Integer> navGraphIds,
                      androidx.fragment.app.FragmentManager fragmentManager,
                      int containerId,
                      Intent intent) {
        try {
            bottomNav.setupWithNavController(navGraphIds, fragmentManager, containerId, intent);
        } catch (Exception e) {
            Log.e("NavigationHelper", "Error setting up navigation: " + e.getMessage(), e);
        }
    }
}

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

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

        // Initialize SDK with delay to prevent stack overflow
        initializeSdk();
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
        UserSession.USER_HASH = "8007-5FE9-B121-5B6C";
        UserSession.LOCATION_SERVER = "https://ips.navigine.com";
        UserSession.USER_NAME = name != null ? name : "user";
        UserSession.USER_EMAIL = email != null ? email : "default@mail.com";
        UserSession.USER_COMPANY = "company";
        UserSession.USER_AVATAR_URL = currentUser != null && currentUser.getPhotoUrl() != null ?
                currentUser.getPhotoUrl().toString() : "";

        Log.d(TAG, "UserSession set - Hash: " + UserSession.USER_HASH + ", Email: " + UserSession.USER_EMAIL);
    }

    private void initializeSdk() {
        Log.d(TAG, "initializeSdk() called");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (UserSession.USER_HASH == null || UserSession.USER_HASH.isEmpty()) {
                    Log.e(TAG, "User hash not available");
                    return;
                }

                Log.d(TAG, "SDK configured with hash: " + UserSession.USER_HASH);

                // Just initialize the SDK managers
                boolean result = NavigineSdkManager.initializeSdk();
                Log.d(TAG, "NavigineSdkManager.initializeSdk() result: " + result);

                if (result) {
                    Log.d(TAG, "SDK initialized successfully");
                    initViewModel();
                    initNavigationView();

                    // Request permissions before starting service
                    requestLocationPermissions();

                    useLocation();
                    isAppInitialized = true;
                } else {
                    Log.e(TAG, "Failed to initialize Navigine SDK");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize SDK: " + e.getMessage(), e);
            }
        }, 2000); // Increased delay to 2 seconds
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

        // Using Navigation Helper for compatibility
        NavigationHelper helper = new NavigationHelper(mBottomNavigation);
        helper.setup(navGraphIds, getSupportFragmentManager(), R.id.nav_host_fragment_activity_main, getIntent());

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

    private void requestLocationPermissions() {
        Log.d(TAG, "requestLocationPermissions() called");

        List<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // For Android 14+ (API 34+), also request FOREGROUND_SERVICE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // All permissions granted, start the service
            startNavigationService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Log.d(TAG, "All location permissions granted");
                startNavigationService();
            } else {
                Log.e(TAG, "Location permissions denied - cannot start navigation service");
                // Show dialog explaining why permissions are needed
            }
        }
    }

    public void logout() {
        Log.d(TAG, "logout() called");
        firebaseAuth.signOut();
        redirectToLogin();
    }
}