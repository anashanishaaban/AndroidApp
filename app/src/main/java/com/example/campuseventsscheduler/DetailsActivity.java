package com.example.campuseventsscheduler;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;
import android.widget.Toast;
import android.content.pm.PackageManager;

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
import com.google.android.gms.tasks.OnSuccessListener;

public class DetailsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView EventName;
    private TextView EventDate;
    private TextView EventTime;
    private TextView EventLocation;
    private TextView DistanceText; // New TextView for distance
    private GoogleMap mMap;
    private double longitude;
    private double latitude;
    private Button BackButton;
    private FusedLocationProviderClient fusedLocationClient; // For getting user's location
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        EventName = findViewById(R.id.EventName);
        EventDate = findViewById(R.id.EventDate);
        EventTime = findViewById(R.id.EventTime);
        EventLocation = findViewById(R.id.EventLocation);
        DistanceText = findViewById(R.id.DistanceText); // Initialize the new TextView
        BackButton = findViewById(R.id.BackButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");
        String location = intent.getStringExtra("location");
        longitude = intent.getDoubleExtra("longitude", 0);
        latitude = intent.getDoubleExtra("latitude", 0);

        EventName.setText(name);
        EventDate.setText("Date: " + date);
        EventTime.setText("Time: " + time);
        EventLocation.setText("Location: " + location);

        BackButton.setBackgroundColor(Color.rgb(24, 69, 59));

        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBack();
            }
        });

        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                Toast.makeText(this, "Not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error occurred : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        requestLocationPermission();
    }

    private void calculateDistance() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location userLocation) {
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
    public void onMapReady(@NonNull GoogleMap map) {
        mMap = map;
        try {
            LatLng eventLocation = new LatLng(latitude, longitude);
            Log.d("MapDebug", "Adding marker at: " + eventLocation.latitude + ", " + eventLocation.longitude);
            mMap.addMarker(new MarkerOptions().position(eventLocation).title("Event Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 17f));
            enableUserLocation();
        } catch (Exception e) {
            Toast.makeText(this, "Error occurred : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            calculateDistance(); // Calculate distance when map is ready and location is enabled
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
                calculateDistance(); // Calculate distance when permission is granted
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onBack() {
        Intent intent = new Intent(this, EventsPage.class);
        startActivity(intent);
    }
}