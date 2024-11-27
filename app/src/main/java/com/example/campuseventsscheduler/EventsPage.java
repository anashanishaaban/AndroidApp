//EventsPage.java

package com.example.campuseventsscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventsPage extends AppCompatActivity {
    private LinearLayout events;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        db = FirebaseFirestore.getInstance();

        events = findViewById(R.id.events);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button addEventButton = findViewById(R.id.addEventButton);
        ImageView addEventTooltip = findViewById(R.id.addEventTooltip);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Manage Add Event button based on email domain
        if (currentUser != null && currentUser.getEmail() != null) {
            String email = currentUser.getEmail();
            if (email.endsWith("@msu.edu")) {
                // Allow users with @msu.edu email to add events
                addEventButton.setEnabled(true);
                addEventButton.setAlpha(1.0f);
                addEventTooltip.setVisibility(View.GONE);
                addEventButton.setOnClickListener(v -> {
                    Intent intent = new Intent(EventsPage.this, AddEventActivity.class);
                    startActivity(intent);
                });
            } else {
                // Disable Add Event button for non-MSU users
                addEventButton.setEnabled(false);
                addEventButton.setAlpha(0.5f);
                addEventTooltip.setVisibility(View.VISIBLE);
                addEventTooltip.setOnClickListener(v -> showTooltip());
            }
        } else {
            // Disable Add Event button if the user is not logged in
            addEventButton.setEnabled(false);
            addEventButton.setAlpha(0.5f);
            addEventTooltip.setVisibility(View.VISIBLE);
            addEventTooltip.setOnClickListener(v -> showTooltip());
        }

        logoutButton.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(EventsPage.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        fetchEventsFromFirebase();
    }

    private void showTooltip() {
        // Show a simple toast or dialog
        Toast.makeText(this, "Only users with a verified @msu.edu email can add new events.", Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the event list every time the user returns to this activity
        fetchEventsFromFirebase();
    }

    // Method to fetch events from Firestore
    private void fetchEventsFromFirebase() {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    events.removeAllViews(); // Clear any existing views
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");

                        // Handle the date field as a timestamp
                        com.google.firebase.Timestamp timestamp = document.getTimestamp("date");
                        Date date = timestamp != null ? timestamp.toDate() : null;

                        String time = document.getString("time");
                        String location = document.getString("location");

                        // Handle latitude and longitude with null safety
                        Double latitude = document.getDouble("latitude");
                        Double longitude = document.getDouble("longitude");
                        if (latitude == null || longitude == null) {
                            Toast.makeText(this, "Invalid event data", Toast.LENGTH_SHORT).show();
                            continue; // Skip this document
                        }

                        String eventId = document.getId();
                        String userId = document.getString("userId");
                        String userEmail = document.getString("userEmail");

                        // Add each event to the layout dynamically
                        addEventToLayout(name, date, time, location, latitude, longitude, eventId, userId, userEmail);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Method to add event to the layout dynamically
    private void addEventToLayout(String name, Date date, String time, String location, double latitude, double longitude, String eventId, String userId, String userEmail) {
        View item = LayoutInflater.from(this).inflate(R.layout.event_item, events, false);
        TextView eventView = item.findViewById(R.id.eventView);
        Button detailsButton = item.findViewById(R.id.DetailsButton);


        // Set event name and date
        eventView.setText(name);

        // Set click listener for the "Details" button
        detailsButton.setOnClickListener(v -> onDetails(name, date, time, location, latitude, longitude, eventId, userId, userEmail));

        // Add the event view to the layout
        events.addView(item);
    }

    private void onDetails(String name, Date date, String Time, String Location, double Latitude, double Longitude, String eventId, String userId, String userEmail) {
        Toast.makeText(this, "Details for " + name, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("name", name);

        // Pass the date as a long to avoid serialization issues
        if (date != null) {
            intent.putExtra("date", date.getTime()); // Pass date as a timestamp
        }

        intent.putExtra("time", Time);
        intent.putExtra("location", Location);
        intent.putExtra("latitude", Latitude);
        intent.putExtra("longitude", Longitude);
        intent.putExtra("eventId", eventId);
        intent.putExtra("userId", userId);
        intent.putExtra("userEmail", userEmail); // Pass the email

        startActivity(intent);
    }
}