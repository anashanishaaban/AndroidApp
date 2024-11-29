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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.res.ColorStateList;

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

    // Member variables
    private Button RsvpButton;
    private TextView AttendeesCount;
    private String currentUserId;
    private boolean isUserAttending = false;
    private ListenerRegistration eventListenerRegistration;

    private TextView EventDescription;

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
        EditButton = findViewById(R.id.EditButton);
        BackButton = findViewById(R.id.BackButton);

        EventDescription = findViewById(R.id.EventDescription);

        AttendeesCount = findViewById(R.id.AttendeesCount);
        RsvpButton = findViewById(R.id.RsvpButton);

        // Get current user ID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            // Handle unauthenticated user appropriately
            finish();
            return;
        }

        // Set up RSVP button click listener
        RsvpButton.setOnClickListener(v -> handleRsvp());

        // Get event ID from Intent
        Intent intent = getIntent();
        eventId = intent.getStringExtra("eventId");

        if (eventId == null) {
            Toast.makeText(this, "Event ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Back button logic
        BackButton.setOnClickListener(v -> onBack());

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

        // Add a snapshot listener for real-time updates
        eventListenerRegistration = eventRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading event data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                com.google.firebase.Timestamp timestamp = documentSnapshot.getTimestamp("date");
                Date date = timestamp != null ? timestamp.toDate() : null;
                String time = documentSnapshot.getString("time");
                String location = documentSnapshot.getString("location");
                Double lat = documentSnapshot.getDouble("latitude");
                Double lng = documentSnapshot.getDouble("longitude");
                String eventEmail = documentSnapshot.getString("userEmail");
                List<String> attendees = (List<String>) documentSnapshot.get("attendees");
                Long attendeesCount = documentSnapshot.getLong("attendeesCount");
                String description = documentSnapshot.getString("description");
                EventDescription.setText("Description: " + (description != null && !description.isEmpty() ? description : "Not provided"));

                // Update UI
                EventName.setText(name);

                if (date != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    EventDate.setText("Date: " + dateFormat.format(date));
                } else {
                    EventDate.setText("Date: Unknown Date");
                }

                // Remove leading zero from time if it exists
                if (time != null && time.startsWith("0")) {
                    time = time.substring(1); // Remove the first character
                }
                EventTime.setText("Time: " + time);

                EventLocation.setText("Location: " + location);
                EventCreator.setText("Event created by: " + (eventEmail != null ? eventEmail : "Unknown"));

                // Update attendees count
                AttendeesCount.setText("Attendees: " + (attendeesCount != null ? attendeesCount : 0));

                // Retrieve eventOwnerId from Firestore document
                String ownerId = documentSnapshot.getString("userId"); // Ensure this field exists in Firestore

                if (ownerId == null) {
                    Toast.makeText(this, "Event owner not found", Toast.LENGTH_SHORT).show();
                    EditButton.setVisibility(View.GONE);
                } else {
                    // Check user permission for editing
                    if (!currentUserId.equals(ownerId)) {
                        EditButton.setVisibility(View.GONE); // Hide Edit button if the user is not the creator
                    } else {
                        EditButton.setVisibility(View.VISIBLE);
                        EditButton.setOnClickListener(v -> {
                            Intent editIntent = new Intent(DetailsActivity.this, EditEventActivity.class);
                            editIntent.putExtra("eventId", eventId); // Pass event ID to EditEventActivity
                            startActivity(editIntent);
                        });
                    }
                }

                // Check if the event is past due
                if (date != null && isPastEvent(date)) {
                    disableRsvpButton();
                } else {
                    // Check if the current user is in the attendees list
                    isUserAttending = attendees != null && attendees.contains(currentUserId);
                    updateRsvpButton();
                }

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
        });
    }

    private boolean isPastEvent(Date eventDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1); // Subtract 1 day to include "1 day ago"
        Date oneDayAgo = calendar.getTime();

        return eventDate.before(oneDayAgo);
    }

    private void disableRsvpButton() {
        RsvpButton.setEnabled(false);
        RsvpButton.setText("Past Due");
        RsvpButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY)); // Gray color
    }

    private void updateRsvpButton() {
        if (isUserAttending) {
            RsvpButton.setText("Cancel RSVP?");
            RsvpButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF6F61"))); // Peachy red color
        } else {
            RsvpButton.setText("RSVP?");
            RsvpButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#000000"))); // Original color
        }
    }

    private void handleRsvp() {
        DocumentReference eventRef = db.collection("events").document(eventId);

        if (isUserAttending) {
            // User wants to cancel RSVP
            eventRef.update(
                    "attendees", FieldValue.arrayRemove(currentUserId),
                    "attendeesCount", FieldValue.increment(-1)
            ).addOnSuccessListener(aVoid -> {
                isUserAttending = false;
                updateRsvpButton();
                Toast.makeText(this, "RSVP canceled", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to cancel RSVP: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            // User wants to RSVP
            eventRef.update(
                    "attendees", FieldValue.arrayUnion(currentUserId),
                    "attendeesCount", FieldValue.increment(1)
            ).addOnSuccessListener(aVoid -> {
                isUserAttending = true;
                updateRsvpButton();
                Toast.makeText(this, "RSVP successful", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to RSVP: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
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

                            // Convert to miles
                            float distanceInMiles = distanceInMeters / 1609.34f; // 1 mile = 1609.34 meters

                            // Format the distance and display
                            if (distanceInMiles >= 1) {
                                DistanceText.setText(String.format("Distance: %.1f miles away", distanceInMiles));
                            } else {
                                // Display smaller distances in feet
                                float distanceInFeet = distanceInMiles * 5280; // 1 mile = 5280 feet
                                DistanceText.setText(String.format("Distance: %.0f feet away", distanceInFeet));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventListenerRegistration != null) {
            eventListenerRegistration.remove();
        }
    }
}
