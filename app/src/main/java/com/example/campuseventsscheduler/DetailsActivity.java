package com.example.campuseventsscheduler;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;
import android.widget.Toast;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class DetailsActivity extends AppCompatActivity implements  OnMapReadyCallback{
    private TextView EventName;
    private TextView EventDate;
    private TextView EventTime;
    private TextView EventLocation;

    private GoogleMap mMap;

    private double longitude;

    private double latitude;

    private Button BackButton;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        EventName = findViewById(R.id.EventName);
        EventDate = findViewById(R.id.EventDate);
        EventTime = findViewById(R.id.EventTime);
        EventLocation = findViewById(R.id.EventLocation);

        BackButton = findViewById(R.id.BackButton);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");
        String location = intent.getStringExtra("location");
        longitude = intent.getDoubleExtra("longitude",0);
        latitude = intent.getDoubleExtra("latitude",0);




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
        // error handling.
        // sets up the map using SupportMapFragment which shows up within the app.
        // used AI help to connect the google maps to the users.
        try
        {
            // finds the fragment with the id map.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null)
            {
                // sends the map fragment when retrieved to the onMapReady
                mapFragment.getMapAsync(this);
            }
            else {
                // if the map fragment isi not successfully retrieved it pops a toast box.
                Toast.makeText(this, "Not available", Toast.LENGTH_SHORT).show();
            }
        }
        catch(Exception e)
        {
            Toast.makeText(this, "Error occurred : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        requestLocationPermission();

    }

    private void requestLocationPermission()
    {
        // requesting the permission to access user's location.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    public void onMapReady (@NonNull GoogleMap map)
    {
        mMap = map;

        // error handling
        try {
            // gets the latitudes and longitudes provided by the add event and sets the event location on the google map.
            LatLng eventLocation = new LatLng(latitude, longitude);
            // debug to see if the longitude and latitude are right values.
            Log.d("MapDebug", "Adding marker at: " + eventLocation.latitude + ", " + eventLocation.longitude);
            // Adding marker to the location provided.
            mMap.addMarker(new MarkerOptions().position(eventLocation).title("Event Location"));
            // Shows screen zoomed up to 17f.
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 17f));
        }
        catch (Exception e) {
            // if theres any exception a toast box pops up.
            Toast.makeText(this, "Error occurred : " + e.getMessage(), Toast.LENGTH_LONG).show();

        }


    }

    private void enableUserLocation() {
        // when permission is granted this function enables the user's location.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // result after requesting for location. 
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void onBack()
    {
        Intent intent = new Intent(this, EventsPage.class);
        startActivity(intent);
    }
}
