package com.navigine.navigine.demo.utils;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.navigine.navigine.demo.utils.Constants.TAG;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

public class BeaconScannerManager implements BeaconConsumer, RangeNotifier {

    private static BeaconScannerManager instance;
    private BeaconManager beaconManager;
    private Application application;
    private boolean isScanning = false;

    // Bluetooth LE scanner
    private BluetoothLeScanner bleScanner;
    private boolean isBleScanning = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<BeaconData> rawBleDevices = new ArrayList<>();

    private boolean isBeaconServiceConnected = false;

    // Thread-safe list for listeners
    private final List<BeaconScanListener> listeners = new CopyOnWriteArrayList<>();

    // Region for scanning all beacons
    private static final Region ALL_BEACONS_REGION = new Region("all-beacons", null, null, null);

    // Beacon data class
    public static class BeaconData {
        public String uuid;
        public int major;
        public int minor;
        public float rssi;
        public float distance;
        public boolean isEddystoneUID;
        public boolean isEddystoneTLM;
        public String namespace;      // For UID
        public String instance;
        public String macAddress;
        public int serviceUuid;
        public int beaconTypeCode;
        public long timestamp;

        public BeaconData() {
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "BeaconData{" +
                    "uuid='" + uuid + '\'' +
                    ", major=" + major +
                    ", minor=" + minor +
                    ", rssi=" + rssi +
                    ", distance=" + distance +
                    ", macAddress='" + macAddress + '\'' +
                    ", serviceUuid=" + serviceUuid +
                    ", beaconTypeCode=" + beaconTypeCode +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    // Listener interface
    public interface BeaconScanListener {
        void onBeaconsDetected(List<BeaconData> beacons);
    }

    // Singleton accessor
    public static synchronized BeaconScannerManager getInstance(Application application) {
        if (instance == null) {
            instance = new BeaconScannerManager(application);
        }
        return instance;
    }

    private BeaconScannerManager(Application application) {
        this.application = application;
        initializeBeaconManager();
    }

    private void initializeBeaconManager() {
        beaconManager = BeaconManager.getInstanceForApplication(application);

        // Add support for multiple beacon formats
        beaconManager.getBeaconParsers().add(
                new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        beaconManager.getBeaconParsers().add(
                new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.getBeaconParsers().add(
                new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.getBeaconParsers().add(
                new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.getBeaconParsers().add(
                new BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-21v"));

        // Set scan periods
        beaconManager.setForegroundScanPeriod(1100L);
        beaconManager.setForegroundBetweenScanPeriod(0L);
        beaconManager.setBackgroundScanPeriod(10000L);
        beaconManager.setBackgroundBetweenScanPeriod(60000L);

        // Bind to beacon service
        beaconManager.bind(this);

        // Initialize BLE scanner
        BluetoothManager bluetoothManager = (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }
    }

    // BLE scan callback
    private final ScanCallback bleScanCallback = new ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                String macAddress = result.getDevice().getAddress();
                int rssi = result.getRssi();

                BeaconData bleDevice = new BeaconData();
                bleDevice.macAddress = macAddress;
                bleDevice.rssi = rssi;
                bleDevice.distance = calculateDistance(rssi);
                bleDevice.beaconTypeCode = 0; // Mark as raw BLE
                bleDevice.serviceUuid = 0;
                bleDevice.uuid = (result.getDevice().getName() != null) ? result.getDevice().getName() : macAddress;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    updateBleDevicesList(bleDevice);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing BLE scan result: " + e.getMessage());
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateBleDevicesList(BeaconData newDevice) {
        rawBleDevices.removeIf(device -> device.macAddress.equals(newDevice.macAddress));
        rawBleDevices.add(newDevice);
    }

    // Start scanning
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void startScanning() {
        if (!isScanning && isBeaconServiceConnected) {
            try {
                beaconManager.startRangingBeaconsInRegion(ALL_BEACONS_REGION);
                isScanning = true;
                Log.d(TAG, "Started beacon scanning");
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to start beacon scanning", e);
            }
        } else {
            Log.w(TAG, "Cannot start beacon scanning. isScanning: " + isScanning + ", isServiceConnected: " + isBeaconServiceConnected);
        }

        if (bleScanner != null && !isBleScanning) {
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            bleScanner.startScan(null, settings, bleScanCallback);
            isBleScanning = true;
            Log.d(TAG, "Started BLE scanning");
        }
    }

    // Stop scanning
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void stopScanning() {
        if (isScanning && isBeaconServiceConnected) {
            try {
                beaconManager.stopRangingBeaconsInRegion(ALL_BEACONS_REGION);
                isScanning = false;
                Log.d(TAG, "Stopped beacon scanning");
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to stop beacon scanning", e);
            }
        }
        if (bleScanner != null && isBleScanning) {
            bleScanner.stopScan(bleScanCallback);
            isBleScanning = false;
            Log.d(TAG, "Stopped BLE scanning");
        }
    }

    // Check if Bluetooth is enabled
    public boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return (bluetoothAdapter != null) && bluetoothAdapter.isEnabled();
    }

    // Safe conversion of identifier to int
    private int safeIdentifierToInt(Identifier identifier) {
        if (identifier == null) return 0;
        try {
            return identifier.toInt();
        } catch (UnsupportedOperationException e) {
            String idString = identifier.toString();
            if (idString.length() >= 36) {
                String lastFourChars = idString.replaceAll("-", "");
                if (lastFourChars.length() >= 4) {
                    lastFourChars = lastFourChars.substring(lastFourChars.length() - 4);
                    try {
                        return Integer.parseInt(lastFourChars, 16);
                    } catch (NumberFormatException nfe) {
                        return Math.abs(idString.hashCode()) % 65536;
                    }
                }
            }
            return Math.abs(idString.hashCode()) % 65536;
        } catch (Exception e) {
            Log.e(TAG, "Error converting identifier to int: " + e.getMessage());
            return 0;
        }
    }

    // Get service UUID from identifier
    private int getServiceUuid(Identifier identifier) {
        if (identifier == null) return 0;
        try {
            String idString = identifier.toString().toLowerCase();
            if (idString.contains("feaa") || idString.contains("0xfeaa")) {
                return 0xFEAA;
            }
            if (idString.length() >= 36) {
                return safeIdentifierToInt(identifier);
            } else {
                return safeIdentifierToInt(identifier);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting service UUID: " + e.getMessage());
            return 0;
        }
    }

    // BeaconConsumer methods
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public void onBeaconServiceConnect() {
        isBeaconServiceConnected = true;
        beaconManager.addRangeNotifier(this);
        Log.d(TAG, "Beacon service connected");
        if (!listeners.isEmpty()) {
            startScanning();
        }
    }

    @Override
    public Context getApplicationContext() {
        return application.getApplicationContext();
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        application.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return application.bindService(intent, serviceConnection, i);
    }

    // RangeNotifier method
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        List<BeaconData> detectedBeacons = new ArrayList<>();
        for (Beacon beacon : beacons) {
            Log.d(TAG, "Beacon detected → typeCode: " + beacon.getBeaconTypeCode()
                    + ", serviceUuid: " + beacon.getServiceUuid()
                    + ", mac: " + beacon.getBluetoothAddress()
                    + ", rssi: " + beacon.getRssi());
            try {
                BeaconData beaconData = new BeaconData();
                beaconData.rssi = (float) beacon.getRssi();
                beaconData.distance = (float) beacon.getDistance();
                beaconData.macAddress = beacon.getBluetoothAddress();

                List<Identifier> identifiers = beacon.getIdentifiers();

                boolean isEddystone = false;
                if (beacon.getServiceUuid() == 0xfeaa) {
                    isEddystone = true;
                    int typeCode = beacon.getBeaconTypeCode();
                    beaconData.serviceUuid = 0xfeaa;
                    beaconData.beaconTypeCode = typeCode;

                    if (typeCode == 0x00) { // Eddystone-UID
                        beaconData.isEddystoneUID = true;
                        if (identifiers.size() >= 2) {
                            beaconData.namespace = identifiers.get(0).toString();
                            beaconData.instance = identifiers.get(1).toString();
                        }
                    } else if (typeCode == 0x20) { // Eddystone-TLM
                        beaconData.isEddystoneTLM = true;
                    }
                }

                if (identifiers.size() >= 1) {
                    Identifier id1 = identifiers.get(0);
                    if (id1 != null) {
                        String idString = id1.toString();
                        beaconData.uuid = idString;
                        if (!isEddystone) {
                            if (idString.length() >= 36) {
                                beaconData.serviceUuid = getServiceUuid(id1);
                            } else {
                                beaconData.serviceUuid = safeIdentifierToInt(id1);
                            }
                        }
                    }
                }

                if (identifiers.size() >= 2) {
                    beaconData.major = safeIdentifierToInt(identifiers.get(1));
                }

                if (identifiers.size() >= 3) {
                    beaconData.minor = safeIdentifierToInt(identifiers.get(2));
                }

                if (beaconData.beaconTypeCode == 0) {
                    int beaconTypeCode = beacon.getBeaconTypeCode();
                    if (beaconTypeCode != 0) {
                        beaconData.beaconTypeCode = beaconTypeCode;
                    } else {
                        if (identifiers.size() == 3) {
                            String firstId = identifiers.get(0).toString();
                            if (firstId.length() >= 36) {
                                beaconData.beaconTypeCode = 0x4c000215; // iBeacon
                            }
                        }
                    }
                }

                detectedBeacons.add(beaconData);
            } catch (Exception e) {
                Log.e(TAG, "Error processing beacon: " + e.getMessage(), e);
            }
        }

        // Combine detected beacons and BLE devices
        List<BeaconData> allDevices = new ArrayList<>();
        allDevices.addAll(detectedBeacons);
        allDevices.addAll(rawBleDevices);

        // Notify all listeners
        for (BeaconScanListener listener : listeners) {
            try {
                listener.onBeaconsDetected(allDevices);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying beacon listener: " + e.getMessage(), e);
            }
        }

        if (!detectedBeacons.isEmpty()) {
            Log.d(TAG, "Detected " + detectedBeacons.size() + " beacons");
        }
    }

    // Add listener
    public void addListener(BeaconScanListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "BeaconScanListener added. Total listeners: " + listeners.size());
        }
    }

    // Remove listener
    public void removeListener(BeaconScanListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            Log.d(TAG, "BeaconScanListener removed. Total listeners: " + listeners.size());
        }
    }

    // Cleanup method
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void destroy() {
        stopScanning();
        listeners.clear();

        if (beaconManager != null && isBeaconServiceConnected) {
            beaconManager.removeAllRangeNotifiers();
            beaconManager.unbind(this);
            isBeaconServiceConnected = false;
        }

        instance = null;
        Log.d(TAG, "BeaconScannerManager destroyed");
    }

    // Helper methods
    public boolean isScanning() {
        return isScanning;
    }

    public boolean isServiceConnected() {
        return isBeaconServiceConnected;
    }

    public int getListenerCount() {
        return listeners.size();
    }

    private float calculateDistance(int rssi) {
        // Your distance calculation logic here
        return 0f;
    }
}