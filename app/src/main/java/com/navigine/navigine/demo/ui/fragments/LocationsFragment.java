package com.navigine.navigine.demo.ui.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.navigine.idl.java.LocationInfo; // SDK's LocationInfo
import com.navigine.idl.java.LocationListListener;
import com.navigine.idl.java.LocationListener;
import com.navigine.idl.java.Location;
import com.navigine.idl.java.Sublocation;
import com.navigine.idl.java.Venue;
import com.navigine.navigine.demo.R;
import com.navigine.navigine.demo.adapters.locations.LocationManager;

import com.navigine.navigine.demo.databinding.FragmentLocationsBinding;
import com.navigine.navigine.demo.utils.NavigineSdkManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "LocationsFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int recordAudioPermissionRequestCode = 1002;

    private FragmentLocationsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap googleMap;

    private LocationManager locationManager; // your custom LocationManager

    private ActivityResultLauncher<Intent> speechRecognitionLauncher;

    private double userLatitude = 0.0;
    private double userLongitude = 0.0;

    private boolean isLocationCanaryConnected = false;
    private LocationInfo matchedLocationInfo = null;
    private String currentGeocoderAddress = "Unknown Area";

    private boolean isCanaryConnected = false;

    private List<Venue> currentVenues = new ArrayList<>();

    // Store loaded locations from SDK
    private HashMap<Integer, LocationInfo> loadedLocations = new HashMap<>();
    private HashMap<Integer, Location> loadedLocationObjects = new HashMap<>();

    // Keep track of which locations we're currently loading
    private HashMap<Integer, Boolean> loadingLocations = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = new LocationManager(requireContext());

        initializeNavigineAndLoadLocations();

        speechRecognitionLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Speech recognition result code: " + result.getResultCode());
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> spokenText = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (spokenText != null && !spokenText.isEmpty()) {
                            String text = spokenText.get(0);
                            Log.d(TAG, "Speech result: " + text);
                            requireActivity().runOnUiThread(() -> {
                                if (binding != null && binding.searchView != null) {
                                    binding.searchView.setQuery(text, false);
                                    binding.searchView.setIconified(false);
                                    binding.searchView.requestFocus();
                                }
                            });
                        }
                    }
                }
        );

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_locations, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setDefaultSuggestions();
        requestLocationPermissions();
        setupMicButton();
        setupSuggestionCards();

        // Setup search view click to navigate to SearchFragment
        binding.searchView.setOnClickListener(v -> {
            navigateToSearchFragment();
        });

        // Also handle when search view gets focus
        binding.searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                navigateToSearchFragment();
            }
        });
    }

    private void navigateToSearchFragment() {
        try {
            // Try navigation component first
            try {
                NavController navController = Navigation.findNavController(requireView());

                // Create bundle with arguments
                Bundle args = new Bundle();
                args.putBoolean(SearchFragment.ARG_IS_CANARY_CONNECTED, isCanaryConnected);

                if (isCanaryConnected && !currentVenues.isEmpty()) {
                    // Pass venue names as strings
                    ArrayList<String> venueNames = new ArrayList<>();
                    for (Venue venue : currentVenues) {
                        if (venue != null && venue.getName() != null) {
                            venueNames.add(venue.getName());
                        }
                    }
                    args.putStringArrayList(SearchFragment.ARG_VENUE_NAMES, venueNames);

                    // Also pass location info if needed
                    if (matchedLocationInfo != null) {
                        args.putString("location_name", matchedLocationInfo.getName());
                        args.putInt("location_id", matchedLocationInfo.getId());
                    }
                }

                // Navigate to SearchFragment
                navController.navigate(R.id.fr_search, args);

            } catch (Exception navException) {
                Log.w(TAG, "Navigation component failed, trying fragment transaction: " + navException.getMessage());
                launchSearchActivityFallback();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to search fragment: " + e.getMessage());
            // Fallback to the old method if navigation fails
            launchSearchActivityFallback();
        }
    }

    private void launchSearchActivityFallback() {
        try {
            SearchFragment searchFragment;

            if (isCanaryConnected && !currentVenues.isEmpty()) {
                ArrayList<String> venueNames = getVenueNamesArray();
                searchFragment = SearchFragment.newInstance(isCanaryConnected, venueNames);
            } else {
                searchFragment = SearchFragment.newInstance(false, null);
            }

            // Use activity fragment manager and correct container
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, searchFragment)  // Try system container first
                    .hide(this)
                    .addToBackStack("search_fragment")  // Give it a name for debugging
                    .commit();

        } catch (Exception e) {
            Log.e(TAG, "Error launching search fragment fallback: " + e.getMessage());
        }
    }


    private void setupSuggestionCards() {
        try {
            binding.suggestionCard1.setOnClickListener(v -> onSuggestionCardClicked(binding.suggestionText1.getText().toString()));
            binding.suggestionCard2.setOnClickListener(v -> onSuggestionCardClicked(binding.suggestionText2.getText().toString()));
            binding.suggestionCard3.setOnClickListener(v -> onSuggestionCardClicked(binding.suggestionText3.getText().toString()));
            binding.suggestionCard4.setOnClickListener(v -> onSuggestionCardClicked(binding.suggestionText4.getText().toString()));
        } catch (Exception e) {
            Log.d(TAG, "Suggestion cards setup error: " + e.getMessage());
        }
    }

    private void onSuggestionCardClicked(String venueName) {
        // Navigate to search fragment with the selected venue name
        try {
            // Try navigation component first
            try {
                NavController navController = Navigation.findNavController(requireView());

                Bundle args = new Bundle();
                args.putBoolean(SearchFragment.ARG_IS_CANARY_CONNECTED, isCanaryConnected);
                args.putString(SearchFragment.ARG_INITIAL_QUERY, venueName); // Pass the clicked venue name

                if (isCanaryConnected && !currentVenues.isEmpty()) {
                    ArrayList<String> venueNames = new ArrayList<>();
                    for (Venue venue : currentVenues) {
                        if (venue != null && venue.getName() != null) {
                            venueNames.add(venue.getName());
                        }
                    }
                    args.putStringArrayList(SearchFragment.ARG_VENUE_NAMES, venueNames);
                }

                navController.navigate(R.id.fr_search, args);

            } catch (Exception navException) {
                Log.w(TAG, "Navigation component failed for suggestion, trying fragment transaction: " + navException.getMessage());
                // Fallback to manual fragment transaction
                SearchFragment searchFragment = SearchFragment.newInstance(isCanaryConnected,
                        isCanaryConnected && !currentVenues.isEmpty() ? getVenueNamesArray() : null,
                        venueName);

                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, searchFragment)
                        .addToBackStack(null)
                        .commit();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to search fragment from suggestion: " + e.getMessage());
            // Final fallback
            if (binding.searchView != null) {
                binding.searchView.setQuery(venueName, false);
                binding.searchView.setIconified(false);
                navigateToSearchFragment();
            }
        }
    }

    private ArrayList<String> getVenueNamesArray() {
        ArrayList<String> venueNames = new ArrayList<>();
        for (Venue venue : currentVenues) {
            if (venue != null && venue.getName() != null) {
                venueNames.add(venue.getName());
            }
        }
        return venueNames;
    }

    private void setupMicButton() {
        binding.micButton.setClickable(true);
        binding.micButton.setFocusable(true);
        binding.micButton.setOnClickListener(v -> {
            Log.d(TAG, "Mic button clicked");
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, recordAudioPermissionRequestCode);
            } else {
                startSpeechRecognition();
            }
        });
    }

    private void setDefaultSuggestions() {
        String[] defaults = {"Reception", "Office", "Hall", "Doctor"};
        binding.suggestionText1.setText(defaults[0]);
        binding.suggestionText2.setText(defaults[1]);
        binding.suggestionText3.setText(defaults[2]);
        binding.suggestionText4.setText(defaults[3]);
    }

    private void updateSuggestionsWithVenues(List<Venue> venues) {
        requireActivity().runOnUiThread(() -> {
            if (venues != null && !venues.isEmpty()) {
                Log.d(TAG, "Updating suggestions with " + venues.size() + " venues");

                if (venues.size() > 0) {
                    binding.suggestionText1.setText(venues.get(0).getName());
                    Log.d(TAG, "Set suggestion 1: " + venues.get(0).getName());
                }
                if (venues.size() > 1) {
                    binding.suggestionText2.setText(venues.get(1).getName());
                    Log.d(TAG, "Set suggestion 2: " + venues.get(1).getName());
                } else {
                    binding.suggestionText2.setText("Reception");
                }
                if (venues.size() > 2) {
                    binding.suggestionText3.setText(venues.get(2).getName());
                    Log.d(TAG, "Set suggestion 3: " + venues.get(2).getName());
                } else {
                    binding.suggestionText3.setText("Office");
                }
                if (venues.size() > 3) {
                    binding.suggestionText4.setText(venues.get(3).getName());
                    Log.d(TAG, "Set suggestion 4: " + venues.get(3).getName());
                } else {
                    binding.suggestionText4.setText("Hall");
                }
            } else {
                Log.d(TAG, "No venues found, setting default suggestions");
                setDefaultSuggestions();
            }
        });
    }

    private void loadVenuesForLocation(int locationId) {
        Log.d(TAG, "Loading venues for location ID: " + locationId);

        // Check if we already have the location object loaded
        Location location = getLocationById(locationId);
        if (location != null) {
            Log.d(TAG, "Found cached location object for ID: " + locationId);
            processLocationForVenues(location);
        } else {
            // Load the location from SDK
            Log.d(TAG, "Location object not cached, loading from SDK for ID: " + locationId);
            loadLocationFromSDK(locationId);
        }
    }

    private void loadLocationFromSDK(int locationId) {
        // Check if we're already loading this location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (loadingLocations.getOrDefault(locationId, false)) {
                Log.d(TAG, "Location " + locationId + " is already being loaded");
                return;
            }
        }

        loadingLocations.put(locationId, true);

        try {
            LocationListener locationListener = new LocationListener() {
                public void onLocationFailed(int i, Error error) {
                    Log.e(TAG, "Failed to load location " + locationId + ": " + error.getMessage());
                    loadingLocations.put(locationId, false);

                    // Only fallback if we haven't already loaded venues successfully
                    if (currentVenues.isEmpty()) {
                        requireActivity().runOnUiThread(() -> {
                            setDefaultSuggestions();
                        });
                    }
                }

                @Override
                public void onLocationLoaded(@NonNull Location location) {
                    Log.d(TAG, "Location loaded successfully: " + location.getId() + " - " + location.getName());

                    // Cache the loaded location
                    loadedLocationObjects.put(location.getId(), location);
                    loadingLocations.put(locationId, false);

                    // Process venues for this location
                    if (location.getId() == locationId) {
                        processLocationForVenues(location);
                    }
                }

                @Override
                public void onLocationUploaded(int i) {
                    // No implementation needed here
                }

                // Add other overrides if necessary
            };

            // Add listener and load the specific location
            NavigineSdkManager.LocationManager.addLocationListener(locationListener);
            NavigineSdkManager.LocationManager.setLocationId(locationId);

        } catch (Exception e) {
            Log.e(TAG, "Error loading location from SDK: " + e.getMessage());
            loadingLocations.put(locationId, false);
            setDefaultSuggestions();
        }
    }

    private void processLocationForVenues(Location location) {
        Log.d(TAG, "Processing location for venues: " + location.getName());

        try {
            List<Sublocation> sublocations = location.getSublocations();
            if (sublocations != null && !sublocations.isEmpty()) {
                Log.d(TAG, "Found " + sublocations.size() + " sublocations in location");

                // Collect venues from all sublocations
                List<Venue> allVenues = new ArrayList<>();
                for (Sublocation sublocation : sublocations) {
                    if (sublocation == null) {
                        Log.w(TAG, "Null sublocation encountered");
                        continue;
                    }

                    Log.d(TAG, "Processing sublocation: " + sublocation.getName());

                    try {
                        ArrayList<Venue> venues = sublocation.getVenues();
                        if (venues != null && !venues.isEmpty()) {
                            Log.d(TAG, "Found " + venues.size() + " venues in sublocation: " + sublocation.getName());

                            // Safely process venues to avoid pointer tagging issues
                            for (Venue venue : venues) {
                                if (venue != null) {
                                    try {
                                        String venueName = venue.getName();
                                        if (venueName != null && !venueName.trim().isEmpty()) {
                                            allVenues.add(venue);
                                            Log.d(TAG, "Venue: " + venueName);
                                        }
                                    } catch (Exception venueException) {
                                        Log.w(TAG, "Error processing venue: " + venueException.getMessage());
                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, "No venues in sublocation: " + sublocation.getName());
                        }
                    } catch (Exception sublocationException) {
                        Log.e(TAG, "Error processing sublocation venues: " + sublocationException.getMessage());
                    }
                }

                if (!allVenues.isEmpty()) {
                    Log.d(TAG, "Total venues found: " + allVenues.size());
                    currentVenues.clear();
                    currentVenues.addAll(allVenues);
                    updateSuggestionsWithVenues(allVenues);
                } else {
                    Log.d(TAG, "No venues found in any sublocation");
                    setDefaultSuggestions();
                }
            } else {
                Log.d(TAG, "No sublocations found for location: " + location.getName());
                setDefaultSuggestions();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing location for venues: " + e.getMessage());
            // Fallback to default suggestions in case of any error
            setDefaultSuggestions();
        }
    }

    private Location getLocationById(int id) {
        // Return from loaded location objects if available
        if (loadedLocationObjects.containsKey(id)) {
            Log.d(TAG, "Returning cached location object for ID: " + id);
            return loadedLocationObjects.get(id);
        }
        Log.d(TAG, "Location object not found in cache for ID: " + id);
        return null;
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else if (requestCode == recordAudioPermissionRequestCode && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition();
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLatitude = location.getLatitude();
                userLongitude = location.getLongitude();
                updateMapWithUserLocation();
                fetchAddressDetails(userLatitude, userLongitude);
                checkIfLocationIsCanaryConnected();
            }
        });
    }

    private void fetchAddressDetails(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1, addresses -> {
                    processGeocoderAddresses(addresses);
                });
            } else {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null) {
                    processGeocoderAddresses(addresses);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            currentGeocoderAddress = "Unknown Area (Geocoder Error)";
        }
    }

    private void processGeocoderAddresses(@NonNull List<Address> addresses) {
        if (!addresses.isEmpty()) {
            Address address = addresses.get(0);
            currentGeocoderAddress = address.getFeatureName() != null ? address.getFeatureName() :
                    address.getLocality() != null ? address.getLocality() :
                            address.getSubLocality() != null ? address.getSubLocality() :
                                    address.getThoroughfare() != null ? address.getThoroughfare() :
                                            "Unknown Area";
            requireActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.locationNameText.setText(currentGeocoderAddress);
                }
            });
        } else {
            currentGeocoderAddress = "Unknown Area (No Geocoder Result)";
        }
    }

    private void updateMapWithUserLocation() {
        if (googleMap != null && userLatitude != 0.0 && userLongitude != 0.0) {
            LatLng userLatLng = new LatLng(userLatitude, userLongitude);
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(userLatLng).title("You are here"));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 17f));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        if (userLatitude != 0.0 && userLongitude != 0.0) {
            updateMapWithUserLocation();
        } else {
            LatLng defaultLatLng = new LatLng(28.6668404, 77.2147314);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 17f));
        }
    }

    private void checkIfLocationIsCanaryConnected() {
        Log.d(TAG, "Checking if user location is inside polygon...");
        isLocationCanaryConnected = false;
        matchedLocationInfo = null;

        if (userLatitude == 0.0 && userLongitude == 0.0) {
            // fallback to address
            updateUIForDisconnectedLocation();
            return;
        }

        LocationManager.LocationInfo matchedLoc = locationManager.checkUserLocation(userLatitude, userLongitude);
        if (matchedLoc != null) {
            int id = matchedLoc.getId();
            String name = matchedLoc.getName();
            boolean canaryConnected = matchedLoc.isCanaryConnected();

            Log.d(TAG, "Matched Location ID: " + id + ", Name: " + name + ", Canary: " + canaryConnected);

            isCanaryConnected = canaryConnected;
            isLocationCanaryConnected = canaryConnected;
            matchedLocationInfo = new LocationInfo(id, 0, name);
            updateUIForMatchedLocation(matchedLoc);

            if (canaryConnected) {
                Log.d(TAG, "Location is Canary connected, loading venues...");
                NavigineSdkManager.LocationManager.setLocationId(id);

                loadVenuesForLocation(id);
            } else {
                Log.d(TAG, "Location is not Canary connected, using default suggestions");
                setDefaultSuggestions();
            }
        } else {
            // polygon not matched
            Log.d(TAG, "No polygon matched for user location");
            updateUIForDisconnectedLocation();
            matchedLocationInfo = null;
            isCanaryConnected = false;
            isLocationCanaryConnected = false;
        }
    }

    private void updateUIForDisconnectedLocation() {
        requireActivity().runOnUiThread(() -> {
            if (binding != null) {
                binding.locationNameText.setText(currentGeocoderAddress);
                binding.locationStatusText.setText("You're at");
                binding.locationInfoText.setText("This location is not Canary Connected");

                // Fixed: Set proper color for disconnected state
                binding.locationInfoCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorSecondaryVariant));
                binding.canaryStatusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
                binding.canaryStatusText.setText("Status: Disconnected");
            }
        });
    }

    private void updateUIForMatchedLocation(@NonNull LocationManager.LocationInfo loc) {
        requireActivity().runOnUiThread(() -> {
            if (binding != null && loc != null) {
                binding.locationStatusText.setText("You're at");
                binding.locationNameText.setText(loc.getName());

                if (isCanaryConnected) {
                    Log.d(TAG, "Updating UI for connected location");
                    binding.locationInfoText.setText("This location is Canary Connected!");
                    binding.locationInfoCardView.setCardBackgroundColor(0xFFE6FFB3); // Light green
                    binding.canaryStatusIndicator.setBackgroundResource(R.drawable.circle_indicator_green);
                    binding.canaryStatusText.setText("Status: Connected");
                } else {
                    Log.d(TAG, "Updating UI for disconnected location");
                    binding.locationInfoText.setText("This location is not Canary Connected");
                    // Fixed: Use proper light red/orange color instead of solid red
                    binding.locationInfoCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground_80));
                    binding.canaryStatusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
                    binding.canaryStatusText.setText("Status: Disconnected");
                }
            } else {
                // fallback
                binding.locationStatusText.setText("You're at");
                binding.locationNameText.setText(currentGeocoderAddress);
                binding.locationInfoText.setText("This location is not Canary Connected");
                binding.locationInfoCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground_80));
                binding.canaryStatusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
                binding.canaryStatusText.setText("Status: Disconnected");
            }
        });
    }

    // SDK initialization
    private void initializeNavigineAndLoadLocations() {
        try {
            NavigineSdkManager.initializeSdk();
            loadNavigineLocationList();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SDK: " + e.getMessage());
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Failed to initialize location services.", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void loadNavigineLocationList() {
        try {
            LocationListListener listener = new LocationListListener() {
                @Override
                public void onLocationListLoaded(@NonNull HashMap<Integer, LocationInfo> hashMap) {
                    Log.d(TAG, "Location list loaded: " + hashMap.size() + " locations");
                    loadedLocations.clear();
                    loadedLocations.putAll(hashMap);

                    // Log all available locations
                    for (Map.Entry<Integer, LocationInfo> entry : hashMap.entrySet()) {
                        Log.d(TAG, "Available location: ID=" + entry.getKey() + ", Name=" + entry.getValue().getName());
                    }
                }
                @Override
                public void onLocationListFailed(@NonNull Error error) {
                    Log.e(TAG, "Failed to load location list: " + error.getMessage());
                }
            };
            NavigineSdkManager.LocationListManager.addLocationListListener(listener);
            NavigineSdkManager.LocationListManager.updateLocationList();
        } catch (Exception e) {
            Log.e(TAG, "Error loading location list: " + e.getMessage());
        }
    }

    private void startSpeechRecognition() {
        Log.d(TAG, "Starting speech recognition");
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognitionLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition: " + e.getMessage());
            Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up listeners to prevent memory leaks
        try {
            if (NavigineSdkManager.LocationListManager != null) {
                // Remove listeners if your SDK supports it
                // NavigineSdkManager.LocationListManager.removeAllLocationListListeners();
            }
            if (NavigineSdkManager.LocationManager != null) {
                // Remove listeners if your SDK supports it
                // NavigineSdkManager.LocationManager.removeAllLocationListeners();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up listeners: " + e.getMessage());
        }
    }
}