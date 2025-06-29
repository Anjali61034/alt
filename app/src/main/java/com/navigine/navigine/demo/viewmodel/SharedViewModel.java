package com.navigine.navigine.demo.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.navigine.idl.java.Location;
import com.navigine.idl.java.LocationListener;
import com.navigine.navigine.demo.utils.NavigineSdkManager;

public class SharedViewModel extends ViewModel {

    public MutableLiveData<Location> mLocation = new MutableLiveData<>(null);

    private LocationListener locationListener = null;

    public SharedViewModel() {
        // Lazy initialization of LocationListener
        initializeLocationListener();
    }

    private void initializeLocationListener() {
        // Check if LocationManager is initialized
        if (NavigineSdkManager.LocationManager != null) {
            // Define the listener
            locationListener = new LocationListener() {
                @Override
                public void onLocationLoaded(Location location) {
                    mLocation.postValue(location);
                }

                @Override
                public void onLocationFailed(int i, Error error) {
                    // Handle errors if needed
                }

                @Override
                public void onLocationUploaded(int i) {
                    // Handle upload events if needed
                }
            };
            // Register the listener
            NavigineSdkManager.LocationManager.addLocationListener(locationListener);
        } else {
            // LocationManager not initialized yet
            // You can log or handle this situation here
            // For example:
            System.out.println("LocationManager is null. Cannot add LocationListener now.");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove listener if it was registered
        if (NavigineSdkManager.LocationManager != null && locationListener != null) {
            NavigineSdkManager.LocationManager.removeLocationListener(locationListener);
        }
    }
}