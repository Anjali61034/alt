package com.navigine.navigine.demo.viewmodel;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.navigine.idl.java.Location;
import com.navigine.idl.java.LocationListener;
import com.navigine.navigine.demo.utils.NavigineSdkManager;
import com.google.firebase.auth.FirebaseAuth;

public class SharedViewModel extends ViewModel {

    public MutableLiveData<Location> mLocation = new MutableLiveData<>(null);

    private LocationListener locationListener = null;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private FirebaseAuth firebaseAuth;

    public SharedViewModel() {
        firebaseAuth = FirebaseAuth.getInstance();

        // Only initialize location listener if user is authenticated
        if (isUserAuthenticated()) {
            initializeLocationListenerWithDelay();
        }
    }

    private boolean isUserAuthenticated() {
        return firebaseAuth.getCurrentUser() != null;
    }

    private void initializeLocationListenerWithDelay() {
        // Post a delayed task to ensure SDK initialization is complete
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Double-check authentication before initializing
                if (isUserAuthenticated()) {
                    initializeLocationListener();
                } else {
                    System.out.println("User not authenticated, skipping location listener initialization");
                }
            }
        }, 1000); // 1 second delay
    }

    private void initializeLocationListener() {
        // Check if LocationManager is initialized and user is still authenticated
        if (NavigineSdkManager.LocationManager != null && isUserAuthenticated()) {
            // Define the listener
            locationListener = new LocationListener() {
                @Override
                public void onLocationLoaded(Location location) {
                    // Only update location if user is still authenticated
                    if (isUserAuthenticated()) {
                        mLocation.postValue(location);
                    }
                }

                @Override
                public void onLocationFailed(int locationId, Error error) {
                    // Handle errors if needed
                    System.out.println("Location failed for ID: " + locationId + ", Error: " + error.getMessage());
                }

                @Override
                public void onLocationUploaded(int locationId) {
                    // Handle upload events if needed
                    System.out.println("Location uploaded for ID: " + locationId);
                }
            };

            // Register the listener
            NavigineSdkManager.LocationManager.addLocationListener(locationListener);
            System.out.println("LocationListener added successfully");
        } else if (!isUserAuthenticated()) {
            System.out.println("User not authenticated, skipping location listener initialization");
        } else {
            // LocationManager not initialized yet, retry after another delay
            System.out.println("LocationManager is null. Retrying in 2 seconds...");
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    initializeLocationListener();
                }
            }, 2000); // 2 second delay for retry
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove listener if it was registered
        if (NavigineSdkManager.LocationManager != null && locationListener != null) {
            NavigineSdkManager.LocationManager.removeLocationListener(locationListener);
            System.out.println("LocationListener removed");
        }

        // Remove any pending callbacks
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}