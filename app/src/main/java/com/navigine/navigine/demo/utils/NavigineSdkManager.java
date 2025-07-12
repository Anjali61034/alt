package com.navigine.navigine.demo.utils;

import android.content.Context;
import android.util.Log;

import com.navigine.idl.java.LocationListManager;
import com.navigine.idl.java.LocationManager;
import com.navigine.idl.java.MeasurementManager;
import com.navigine.idl.java.NavigationManager;
import com.navigine.idl.java.NavigineSdk;
import com.navigine.idl.java.NotificationManager;
import com.navigine.idl.java.ResourceManager;
import com.navigine.idl.java.RouteManager;
import com.navigine.idl.java.ZoneManager;
import com.navigine.navigine.demo.utils.Constants;
import com.navigine.navigine.demo.models.UserSession;
import com.navigine.sdk.Navigine;

public class NavigineSdkManager {
    // managers
    public static LocationListManager LocationListManager = null;
    public static LocationManager LocationManager = null;
    public static ResourceManager ResourceManager = null;
    public static NavigationManager NavigationManager = null;
    public static NotificationManager NotificationManager = null;
    public static MeasurementManager MeasurementManager = null;
    public static RouteManager RouteManager = null;
    public static ZoneManager ZoneManager = null;

    // Initialization flag to prevent multiple initializations
    private static boolean isSDKInitialized = false;

    public static synchronized boolean initializeSdk() {
        if (isSDKInitialized) {
            Log.d(Constants.TAG, "SDK already initialized");
            return true;
        }

        if (UserSession.USER_HASH == null || UserSession.USER_HASH.isEmpty()) {
            return false;
        }

        try {
            NavigineSdk SDK = NavigineSdk.getInstance();
            SDK.setUserHash(UserSession.USER_HASH);
            SDK.setServer(UserSession.LOCATION_SERVER);

            LocationListManager = SDK.getLocationListManager();
            LocationManager = SDK.getLocationManager();
            ResourceManager = SDK.getResourceManager(LocationManager);
            NavigationManager = SDK.getNavigationManager(LocationManager);
            MeasurementManager = SDK.getMeasurementManager(LocationManager);
            RouteManager = SDK.getRouteManager(LocationManager, NavigationManager);
            NotificationManager = SDK.getNotificationManager(LocationManager);
            ZoneManager = SDK.getZoneManager(NavigationManager);

            isSDKInitialized = true;
            Log.d(Constants.TAG, "SDK initialized successfully");
        } catch (Exception e) {
            Log.e(Constants.TAG, "Failed initialize Navigine SDK " + e.getMessage());
            return false;
        }

        return true;
    }

    public static boolean isInitialized() {
        return isSDKInitialized;
    }

    // Method to reset if needed (for testing or restart scenarios)
    public static void resetInitializationFlag() {
        isSDKInitialized = false;
    }
}