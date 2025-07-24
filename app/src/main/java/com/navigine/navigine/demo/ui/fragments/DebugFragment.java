package com.navigine.navigine.demo.ui.fragments;

import static com.navigine.navigine.demo.utils.Constants.TAG;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

// Add these imports to your current code
import com.navigine.idl.java.MeasurementListener;
import com.navigine.idl.java.PositionListener;
import com.navigine.idl.java.SensorMeasurement;
import com.navigine.idl.java.SensorType;
import com.navigine.idl.java.Vector3d;
import com.navigine.navigine.demo.adapters.debug.DebugAdapterWifi;
import com.navigine.navigine.demo.adapters.debug.DebugAdapterSensors;
import com.navigine.navigine.demo.utils.NavigineSdkManager;
import java.util.HashMap;


import com.navigine.idl.java.Location;
import com.navigine.idl.java.LocationPoint;
import com.navigine.idl.java.Position;
import com.navigine.idl.java.SignalMeasurement;
import com.navigine.idl.java.SignalType;
import com.navigine.navigine.demo.BuildConfig;
import com.navigine.navigine.demo.R;
import com.navigine.navigine.demo.adapters.debug.DebugAdapterBase;
import com.navigine.navigine.demo.adapters.debug.DebugAdapterBeacons;
import com.navigine.navigine.demo.adapters.debug.DebugAdapterBle;
import com.navigine.navigine.demo.adapters.debug.DebugAdapterEddystone;
import com.navigine.navigine.demo.adapters.debug.DebugAdapterInfo;
import com.navigine.navigine.demo.utils.BeaconScannerManager;
import com.navigine.navigine.demo.viewmodel.SharedViewModel;

import org.altbeacon.beacon.BeaconData;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DebugFragment extends BaseFragment implements BeaconScannerManager.BeaconScanListener {

    private static String OS_VERSION = "UNKNOWN";
    public static final int DEBUG_TIMEOUT_NO_SIGNAL = 5000;

    private SharedViewModel viewModel = null;

    private Window mWindow = null;
    private NestedScrollView mRootView = null;
    private RecyclerView mListViewInfo = null;
    private RecyclerView mListViewBeacons = null;
    private RecyclerView mListViewEddystone = null;
    private RecyclerView mListViewBle = null;

    // Add these variables to your class
    private RecyclerView mListViewWifi = null;
    private RecyclerView mListViewSensors = null;
    private List<SignalMeasurement> wifiEntries = new ArrayList<>();
    private List<String[]> sensorEntries = new ArrayList<>();
    private DebugAdapterWifi debugWifiAdapter = null;
    private DebugAdapterSensors debugSensorsAdapter = null;
    private PositionListener mPositionListener = null;
    private MeasurementListener mMeasurementListener = null;


    private ArrayList<String[]> infoEntries = new ArrayList<>();
    private List<BeaconScannerManager.BeaconData> beaconEntries = new ArrayList<>();
    private List<BeaconScannerManager.BeaconData> eddyEntries = new ArrayList<>();
    private List<BeaconScannerManager.BeaconData> bleEntries = new ArrayList<>();

    private Location mLocation = null;

    private DebugAdapterInfo debugInfoAdapter = null;
    private DebugAdapterBeacons debugBeaconsAdapter = null;
    private DebugAdapterEddystone debugEddystoneAdapter = null;
    private DebugAdapterBle debugBleAdapter = null;

    private DividerItemDecoration mDivider = null;

    // BeaconScanner manager
    private BeaconScannerManager beaconScanner;

    private long timestampBeacons = 0L;
    private long timestampEddystones = 0L;
    private long timestampBle = 0L;

    private static final String TEST_DEVICE_ID = UUID.randomUUID().toString();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOsVersion();
        initViewModels();
        initAdapters();
        initListeners();  // Add this line

        // Get beacon scanner instance
        beaconScanner = BeaconScannerManager.getInstance(requireActivity().getApplication());
        timestampBeacons = timestampEddystones = timestampBle = System.currentTimeMillis();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug, container, false);
        initViews(view);
        setViewsParams();
        setAdapters();
        setAdaptersParams();
        setObservers();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        NavigineSdkManager.NavigationManager.addPositionListener(mPositionListener);
        NavigineSdkManager.MeasurementManager.addMeasurementListener(mMeasurementListener);
        // Register as listener to receive beacon updates
        beaconScanner.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        NavigineSdkManager.NavigationManager.removePositionListener(mPositionListener);
        NavigineSdkManager.MeasurementManager.removeMeasurementListener(mMeasurementListener);

        // Unregister listener but don't stop scanning
        beaconScanner.removeListener(this);
    }

    // BeaconScanListener interface method
    @Override
    public void onBeaconsDetected(List<BeaconScannerManager.BeaconData> beacons) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            beaconEntries.clear();
            eddyEntries.clear();
            bleEntries.clear();

            for (BeaconScannerManager.BeaconData beacon : beacons) {
                // Categorize beacons based on their type
                if (beacon.beaconTypeCode == 0x4c000215) { // iBeacon
                    beaconEntries.add(beacon);
                }else if (beacon.serviceUuid == 0xfeaa || beacon.beaconTypeCode == 0x20) {
                    Log.d(TAG, "→ Eddystone added: " + beacon.macAddress + ", namespace=" + beacon.namespace + ", instance=" + beacon.instance);

                    eddyEntries.add(beacon);

                }
                else { // Others (BLE, AltBeacon, etc.)
                    bleEntries.add(beacon);
                }
            }

            // Sort by RSSI (strongest first)
            Collections.sort(beaconEntries, (b1, b2) -> Float.compare(b2.rssi, b1.rssi));
            Collections.sort(eddyEntries, (b1, b2) -> Float.compare(b2.rssi, b1.rssi));
            Collections.sort(bleEntries, (b1, b2) -> Float.compare(b2.rssi, b1.rssi));

            updateBeaconAdapters();
        });
    }

    private void updateBeaconAdapters() {
        // Update iBeacons
        if (!beaconEntries.isEmpty()) {
            timestampBeacons = System.currentTimeMillis();
            debugBeaconsAdapter.submit(convertToSignalMeasurements(beaconEntries));
        } else if (System.currentTimeMillis() - timestampBeacons >= DEBUG_TIMEOUT_NO_SIGNAL) {
            debugBeaconsAdapter.submit(Collections.<SignalMeasurement>emptyList());
        }

        // Update Eddystone
        if (!eddyEntries.isEmpty()) {
            Log.d(TAG, "Eddystone entries:");
            for (BeaconScannerManager.BeaconData entry : eddyEntries) {
                Log.d(TAG, "-> Beacon typeCode: " + entry.beaconTypeCode
                        + ", serviceUuid: " + entry.serviceUuid);

            }

            timestampEddystones = System.currentTimeMillis();

            debugEddystoneAdapter.submit(convertToSignalMeasurements(eddyEntries));
        } else if (System.currentTimeMillis() - timestampEddystones >= DEBUG_TIMEOUT_NO_SIGNAL) {
            debugEddystoneAdapter.submit(Collections.<SignalMeasurement>emptyList());
        }

        // Update BLE
        if (!bleEntries.isEmpty()) {
            Log.d(TAG, "Updating Eddystone adapter with " + eddyEntries.size() + " items");
            timestampBle = System.currentTimeMillis();
            debugBleAdapter.submit(convertToSignalMeasurements(bleEntries));
        } else if (System.currentTimeMillis() - timestampBle >= DEBUG_TIMEOUT_NO_SIGNAL) {
            debugBleAdapter.submit(Collections.<SignalMeasurement>emptyList());
        }
    }

    // Convert List<BeaconData> to List<SignalMeasurement>
// Convert List<BeaconData> to List<SignalMeasurement>
    private List<SignalMeasurement> convertToSignalMeasurements(List<BeaconScannerManager.BeaconData> beaconDataList) {
        List<SignalMeasurement> measurements = new ArrayList<>();
        for (BeaconScannerManager.BeaconData beacon : beaconDataList) {
            try {
                // Create SignalMeasurement with required parameters
                SignalType signalType = determineSignalType(beacon);

                // Create identifier based on beacon type
                String identifier;
                if (beacon.serviceUuid == 0xfeaa && beacon.isEddystoneUID) {
                    // For Eddystone UID, show last 3-4 digits of namespace and complete instance
                    String namespace = beacon.namespace != null ? beacon.namespace : "N/A";
                    String instance = beacon.instance != null ? beacon.instance : "N/A";

                    // Get last 4 digits of namespace
                    String shortNamespace = namespace.length() > 4 ?
                            namespace.substring(namespace.length() - 4) : namespace;

                    identifier = "NS:" + shortNamespace + " | ID:" + instance;
                }
                else if (beacon.serviceUuid == 0xfeaa) {
                    // For other Eddystone types (TLM, URL), show MAC as fallback
                    identifier = beacon.macAddress != null ? beacon.macAddress : "unknown";
                } else {
                    // For iBeacon and others, show MAC address
                    identifier = beacon.macAddress != null ? beacon.macAddress : "unknown";
                }

                float rssi = beacon.rssi;
                float distance = calculateDistance(beacon.rssi);
                long timestamp = System.currentTimeMillis();

                SignalMeasurement measurement = new SignalMeasurement(signalType, identifier, rssi, distance, timestamp);
                measurements.add(measurement);
            } catch (Exception e) {
                Log.e(TAG, "Error creating SignalMeasurement: " + e.getMessage());
            }
        }
        return measurements;
    }


    // Helper method to determine signal type based on beacon data
    private SignalType determineSignalType(BeaconScannerManager.BeaconData beacon) {
        // Check what SignalType enum values are actually available in your project
        // Common Navigine SignalType values based on documentation:

        if (beacon.beaconTypeCode == 0x4c000215) {
            return SignalType.BEACON; // iBeacon
        } else if (beacon.serviceUuid == 0xfeaa) {
            return SignalType.EDDYSTONE; // Eddystone
        } else {
            // If BLE doesn't exist, try these alternatives:
            // return SignalType.WIFI;
            // return SignalType.BLUETOOTH;
            // return SignalType.OTHER;
            // return SignalType.UNKNOWN;

            // For now, let's use WIFI as fallback (change this based on your actual enum values)
            try {
                return SignalType.WIFI;
            } catch (Exception e) {
                // If WIFI doesn't exist either, you'll need to check your SignalType enum
                // and replace with an appropriate value
                Log.e(TAG, "SignalType enum value not found, check your SignalType class");
                return SignalType.BEACON; // Fallback to a known working value
            }
        }
    }

    // Helper method to estimate distance from RSSI
    private float calculateDistance(float rssi) {
        if (rssi == 0) {
            return -1.0f; // Cannot determine distance
        }

        double ratio = (double) rssi / -59.0; // Assume -59 dBm at 1 meter
        if (ratio < 1.0) {
            return (float) Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return (float) accuracy;
        }
    }

    // Rest of your existing methods remain the same...
    private void getOsVersion() {
        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            try {
                if (field.getInt(Build.VERSION_CODES.class) == Build.VERSION.SDK_INT) {
                    OS_VERSION = field.getName();
                }
            } catch (IllegalAccessException e) {
                Log.e(TAG, getString(R.string.err_debug_os_version));
            }
        }
    }

    private void initViewModels() {
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    private void initAdapters() {
        debugInfoAdapter = new DebugAdapterInfo();
        debugBeaconsAdapter = new DebugAdapterBeacons();
        debugEddystoneAdapter = new DebugAdapterEddystone();
        debugBleAdapter = new DebugAdapterBle();
        debugWifiAdapter = new DebugAdapterWifi();        // Add this
        debugSensorsAdapter = new DebugAdapterSensors();  // Add this
    }

    private void initViews(View view) {
        mWindow = requireActivity().getWindow();
        mRootView = view.findViewById(R.id.debug__root);
        mListViewInfo = view.findViewById(R.id.debug__info);
        mListViewWifi = view.findViewById(R.id.debug__wifi);      // Add this
        mListViewSensors = view.findViewById(R.id.debug__sensors);
        mListViewBeacons = view.findViewById(R.id.debug__beacons);
        mListViewEddystone = view.findViewById(R.id.debug__eddystone);
        mListViewBle = view.findViewById(R.id.debug__ble);
        mDivider = new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL);
    }

    private void setViewsParams() {
        mDivider.setDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.divider_transparent_list_item));
        mListViewInfo.addItemDecoration(mDivider);
        mListViewBeacons.addItemDecoration(mDivider);
        mListViewWifi.addItemDecoration(mDivider);     // Add this
        mListViewSensors.addItemDecoration(mDivider);
        mListViewEddystone.addItemDecoration(mDivider);
        mListViewBle.addItemDecoration(mDivider);

        LayoutAnimationController animationController = AnimationUtils.loadLayoutAnimation(requireActivity(), R.anim.layout_animation_fall_down);
        mListViewBeacons.setLayoutAnimation(animationController);
        mListViewEddystone.setLayoutAnimation(animationController);
        mListViewWifi.setLayoutAnimation(animationController);
        mListViewBle.setLayoutAnimation(animationController);
    }

    private void setAdapters() {
        mListViewInfo.setAdapter(debugInfoAdapter);
        mListViewBeacons.setAdapter(debugBeaconsAdapter);
        mListViewEddystone.setAdapter(debugEddystoneAdapter);
        mListViewBle.setAdapter(debugBleAdapter);
        mListViewWifi.setAdapter(debugWifiAdapter);        // Add this
        mListViewSensors.setAdapter(debugSensorsAdapter);  // Add this
    }

    private void initListeners() {
        mPositionListener = new PositionListener() {
            @Override
            public void onPositionUpdated(Position position) {
                updateInfoGeneral(position);
            }

            @Override
            public void onPositionError(Error error) {
                updateInfoGeneral(null);
            }
        };

        mMeasurementListener = new MeasurementListener() {
            @Override
            public void onSensorMeasurementDetected(HashMap<SensorType, SensorMeasurement> hashMap) {
                sensorEntries.clear();

                // Accelerometer
                if (hashMap.containsKey(SensorType.ACCELEROMETER)) {
                    SensorMeasurement measurement = hashMap.get(SensorType.ACCELEROMETER);
                    if (measurement != null) {
                        Vector3d values = measurement.getValues();
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_1), String.format(Locale.ENGLISH, "%.4f, %.4f, %.4f", values.getX(), values.getY(), values.getZ())});
                    } else {
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_1), "---"});
                    }
                } else {
                    sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_1), "---"});
                }

                // Magnetometer
                if (hashMap.containsKey(SensorType.MAGNETOMETER)) {
                    SensorMeasurement measurement = hashMap.get(SensorType.MAGNETOMETER);
                    if (measurement != null) {
                        Vector3d values = measurement.getValues();
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_2), String.format(Locale.ENGLISH, "%.4f, %.4f, %.4f", values.getX(), values.getY(), values.getZ())});
                    } else {
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_2), "---"});
                    }
                } else {
                    sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_2), "---"});
                }

                // Gyroscope
                if (hashMap.containsKey(SensorType.GYROSCOPE)) {
                    SensorMeasurement measurement = hashMap.get(SensorType.GYROSCOPE);
                    if (measurement != null) {
                        Vector3d values = measurement.getValues();
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_3), String.format(Locale.ENGLISH, "%.4f, %.4f, %.4f", values.getX(), values.getY(), values.getZ())});
                    } else {
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_3), "---"});
                    }
                } else {
                    sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_3), "---"});
                }

                // Barometer
                if (hashMap.containsKey(SensorType.BAROMETER)) {
                    SensorMeasurement measurement = hashMap.get(SensorType.BAROMETER);
                    if (measurement != null) {
                        Vector3d values = measurement.getValues();
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_4), String.format(Locale.ENGLISH, "%.2f", values.getX())});
                    } else {
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_4), "---"});
                    }
                } else {
                    sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_4), "---"});
                }

                // Orientation
                if (hashMap.containsKey(SensorType.ORIENTATION)) {
                    SensorMeasurement measurement = hashMap.get(SensorType.ORIENTATION);
                    if (measurement != null) {
                        Vector3d values = measurement.getValues();
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_5), String.format(Locale.ENGLISH, "%.2f", values.getX() * 180 / 3.14159f)});
                    } else {
                        sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_5), "---"});
                    }
                } else {
                    sensorEntries.add(new String[]{getString(R.string.debug_sensor_field_5), "---"});
                }

                if (!sensorEntries.isEmpty()) {
                    debugSensorsAdapter.submit(sensorEntries);
                }
            }

            @Override
            public void onSignalMeasurementDetected(HashMap<String, SignalMeasurement> hashMap) {
                wifiEntries.clear();

                for (SignalMeasurement signal : hashMap.values()) {
                    if (signal.getType() == SignalType.WIFI) {
                        wifiEntries.add(signal);
                    }
                }

                Collections.sort(wifiEntries, (result1, result2) -> Float.compare(result2.getRssi(), result1.getRssi()));

                if (!wifiEntries.isEmpty()) {
                    debugWifiAdapter.submit(wifiEntries);
                }
            }
        };
    }


    private void setAdaptersParams() {
        DebugAdapterBase.setRootView(mRootView);
    }

    private void setObservers() {
        viewModel.mLocation.observe(getViewLifecycleOwner(), location -> mLocation = location);
    }

    private void updateInfoGeneral(@Nullable Position position) {
        infoEntries.clear();

        infoEntries.add(new String[]{getString(R.string.debug_info_field_1), String.format(Locale.ENGLISH, "%s", BuildConfig.VERSION_NAME)});
        infoEntries.add(new String[]{getString(R.string.debug_info_field_2), String.format("%s", TEST_DEVICE_ID)});

        if (mLocation != null) {
            infoEntries.add(new String[]{getString(R.string.debug_info_field_3), String.format(Locale.ENGLISH, "%s v. %s", mLocation.getName(), mLocation.getVersion())});
        } else {
            infoEntries.add(new String[]{getString(R.string.debug_info_field_3), "---"});
        }

        if (position != null) {
            LocationPoint lp = position.getLocationPoint();
            if (lp != null) {
                infoEntries.add(new String[]{getString(R.string.debug_info_field_4), String.format(Locale.ENGLISH, "%d/%d, x=%.1f, y=%.1f", lp.getLocationId(), lp.getSublocationId(),
                        lp.getPoint().getX(), lp.getPoint().getY())});
            } else {
                infoEntries.add(new String[]{getString(R.string.debug_info_field_4), String.format(Locale.ENGLISH, "-/-, lat=%.1f, lon=%.1f",
                        position.getPoint().getLatitude(), position.getPoint().getLongitude())});
            }
        } else {
            infoEntries.add(new String[]{getString(R.string.debug_info_field_4), "---"});
        }

        infoEntries.add(new String[]{getString(R.string.debug_info_field_6), String.format(Locale.ENGLISH, "%s [ %s (%s) ] ", Build.MODEL, Build.VERSION.RELEASE, OS_VERSION)});

        debugInfoAdapter.submit(infoEntries);
    }


    @Override
    protected void updateStatusBar() {

    }
}