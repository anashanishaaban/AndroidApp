package com.example.campuseventsscheduler;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView EventName;
    private TextView EventDate;
    private TextView EventTime;
    private TextView EventLocation;
    private TextView DistanceText;
    private TextView EventCreator;

    private GoogleMap mMap;
    private double longitude;
    private double latitude;
    private Button BackButton;
    private Button EditButton;
    private String eventId;
    private String eventOwnerId;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize UI components
        EventName = findViewById(R.id.EventName);
        EventDate = findViewById(R.id.EventDate);
        EventTime = findViewById(R.id.EventTime);
        EventLocation = findViewById(R.id.EventLocation);
        EventCreator = findViewById(R.id.EventCreator);
        DistanceText = findViewById(R.id.DistanceText);
        BackButton = findViewById(R.id.BackButton);
        EditButton = findViewById(R.id.EditButton);

        // Get event ID and owner ID from Intent
        Intent intent = getIntent();
        eventId = intent.getStringExtra("eventId");
        eventOwnerId = intent.getStringExtra("userId");

        BackButton.setBackgroundColor(Color.rgb(24, 69, 59));
        EditButton.setBackgroundColor(Color.rgb(24, 69, 59));

        // Back button logic
        BackButton.setOnClickListener(v -> onBack());

        // Check user permission for editing
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (!currentUserId.equals(eventOwnerId)) {
            EditButton.setVisibility(View.GONE); // Hide Edit button if the user is not the creator
        } else {
            EditButton.setVisibility(View.VISIBLE);
            EditButton.setOnClickListener(v -> {
                Intent editIntent = new Intent(DetailsActivity.this, EditEventActivity.class);
                editIntent.putExtra("eventId", eventId); // Pass event ID to EditEventActivity
                startActivity(editIntent);
            });
        }

        // Initialize map
        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        requestLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load event data from Firestore
        loadEventData();
    }

    private void loadEventData() {
        if (eventId == null) {
            Toast.makeText(this, "Event ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                com.google.firebase.Timestamp timestamp = documentSnapshot.getTimestamp("date");
                Date date = timestamp != null ? timestamp.toDate() : null;
                String time = documentSnapshot.getString("time");
                String location = documentSnapshot.getString("location");
                Double lat = documentSnapshot.getDouble("latitude");
                Double lng = documentSnapshot.getDouble("longitude");
                String eventEmail = documentSnapshot.getString("userEmail");

                // Update UI
                EventName.setText(name);

                if (date != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    EventDate.setText("Date: " + dateFormat.format(date));
                } else {
                    EventDate.setText("Date: Unknown Date");
                }

                EventTime.setText("Time: " + time);
                EventLocation.setText("Location: " + location);
                EventCreator.setText("Created by: " + (eventEmail != null ? eventEmail : "Unknown"));

                // Update location data
                if (lat != null && lng != null) {
                    latitude = lat;
                    longitude = lng;
                    // Update map marker if map is ready
                    if (mMap != null) {
                        mMap.clear();
                        LatLng eventLocation = new LatLng(latitude, longitude);
                        mMap.addMarker(new MarkerOptions().position(eventLocation).title("Event Location"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 17f));
                    }
                    calculateDistance();
                }
            } else {
                Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        mMap = map;
        // Initialize the map marker if location data is available
        if (latitude != 0 && longitude != 0) {
            LatLng eventLocation = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(eventLocation).title("Event Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 17f));
        }

        // Set a click listener on the map to open Google Maps
        mMap.setOnMapClickListener(latLng -> {
            // Create a URI for the geo intent
            String geoUri = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(Event Location)";
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(geoUri));
            mapIntent.setPackage("com.google.android.apps.maps"); // Ensure it opens in Google Maps

            // Check if the Maps app is installed
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Google Maps is not installed on this device.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void calculateDistance() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, userLocation -> {
                        if (userLocation != null) {
                            // Create Location object for the event
                            Location eventLocation = new Location("event");
                            eventLocation.setLatitude(latitude);
                            eventLocation.setLongitude(longitude);

                            // Calculate distance in meters
                            float distanceInMeters = userLocation.distanceTo(eventLocation);

                            // Convert to kilometers if distance is large
                            if (distanceInMeters >= 1000) {
                                float distanceInKm = distanceInMeters / 1000;
                                DistanceText.setText(String.format("Distance: %.1f km away", distanceInKm));
                            } else {
                                DistanceText.setText(String.format("Distance: %.0f meters away", distanceInMeters));
                            }
                        } else {
                            DistanceText.setText("Unable to determine distance");
                        }
                    });
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            calculateDistance(); // Calculate distance if permission is already granted
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                calculateDistance();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onBack() {
        finish(); // Close current activity and return to previous screen
    }
}
