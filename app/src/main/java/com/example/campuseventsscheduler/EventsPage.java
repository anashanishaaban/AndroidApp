// EventsPage.java

package com.example.campuseventsscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventsPage extends AppCompatActivity {
    private LinearLayout events;
    private FirebaseFirestore db;
    private boolean showPastEvents = false; // Flag to toggle filter
    private boolean isFilterButtonAdded = false; // To ensure only one filter button is added

    private String lastAddedMonth = ""; // Tracks the last added month header

    private static final String PREFS_NAME = "FilterPrefs";
    private static final String PREF_SHOW_PAST_EVENTS = "showPastEvents";

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

        // Restore filter state if available
        if (savedInstanceState != null) {
            showPastEvents = savedInstanceState.getBoolean(PREF_SHOW_PAST_EVENTS, false);
        } else {
            // Load filter state from SharedPreferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            showPastEvents = prefs.getBoolean(PREF_SHOW_PAST_EVENTS, false);
        }

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PREF_SHOW_PAST_EVENTS, showPastEvents);
    }

    // Method to fetch events from Firestore with filtering
    private void fetchEventsFromFirebase() {
        db.collection("events")
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    events.removeAllViews(); // Clear any existing views
                    isFilterButtonAdded = false; // Reset to add filter button again if needed
                    lastAddedMonth = ""; // Reset the last added month tracker

                    Date currentDate = new Date();

                    SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");

                        // Handle the date field as a timestamp
                        com.google.firebase.Timestamp timestamp = document.getTimestamp("date");
                        Date date = timestamp != null ? timestamp.toDate() : null;

                        // Skip if date is null
                        if (date == null) {
                            Toast.makeText(this, "Invalid event data: Missing date", Toast.LENGTH_SHORT).show();
                            continue;
                        }

                        // Apply filtering based on showPastEvents flag
                        if (showPastEvents && !date.before(currentDate)) {
                            // If showing past events, skip events that are not in the past
                            continue;
                        } else if (!showPastEvents && date.before(currentDate)) {
                            // If showing current/future events, skip past events
                            continue;
                        }

                        String time = document.getString("time");
                        String location = document.getString("location");

                        // Handle latitude and longitude with null safety
                        Double latitude = document.getDouble("latitude");
                        Double longitude = document.getDouble("longitude");
                        if (latitude == null || longitude == null) {
                            Toast.makeText(this, "Invalid event data: Missing location", Toast.LENGTH_SHORT).show();
                            continue; // Skip this document
                        }

                        String eventId = document.getId();
                        String userId = document.getString("userId");
                        String userEmail = document.getString("userEmail");

                        // Determine the month of the current event
                        String currentMonth = monthFormat.format(date);

                        // Add a month header if this event belongs to a new month
                        if (!currentMonth.equals(lastAddedMonth)) {
                            addMonthHeader(currentMonth);
                            lastAddedMonth = currentMonth; // Update the tracker
                        }

                        // Add each event to the layout dynamically
                        addEventToLayout(name, date, time, location, latitude, longitude, eventId, userId, userEmail);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addMonthHeader(String month) {
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setPadding(0, 16, 0, 8);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView monthHeader = new TextView(this);
        monthHeader.setText(month);
        monthHeader.setTextSize(20f);
        monthHeader.setTextColor(Color.BLACK);
        monthHeader.setPadding(16, 16, 16, 8);
        monthHeader.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f)); // Weight 1 to occupy remaining space

        // Add the month header to the row
        headerRow.addView(monthHeader);

        // Add the filter button only once, next to the first month header
        if (!isFilterButtonAdded) {
            Button filterButton = new Button(this);
            filterButton.setText(showPastEvents ? "show current" : "show past events");
            filterButton.setBackground(ContextCompat.getDrawable(this, showPastEvents ? R.drawable.button_filter_active : R.drawable.button_filter));
            filterButton.setTextColor(showPastEvents ? Color.WHITE : Color.BLACK);
            filterButton.setPadding(16, 8, 16, 8);
            filterButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            // **Disable All Caps Transformation**
            filterButton.setAllCaps(false);

            filterButton.setOnClickListener(v -> {
                // Toggle filter flag
                showPastEvents = !showPastEvents;

                // Update button text and background based on the new state
                if (showPastEvents) {
                    filterButton.setText("show current");
                    filterButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_filter_active));
                    filterButton.setTextColor(Color.WHITE);
                } else {
                    filterButton.setText("show past events");
                    filterButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_filter));
                    filterButton.setTextColor(Color.BLACK);
                }

                // Save the filter state to SharedPreferences
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(PREF_SHOW_PAST_EVENTS, showPastEvents);
                editor.apply();

                // Refresh the event list
                fetchEventsFromFirebase();
            });

            headerRow.addView(filterButton);
            isFilterButtonAdded = true;
        }

        events.addView(headerRow);
    }

    // Method to add event to the layout dynamically
    private void addEventToLayout(String name, Date date, String time, String location, double latitude, double longitude, String eventId, String userId, String userEmail) {
        View item = LayoutInflater.from(this).inflate(R.layout.event_item, events, false);
        TextView eventView = item.findViewById(R.id.eventView);
        Button detailsButton = item.findViewById(R.id.DetailsButton);

        eventView.setText(name);

        detailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(EventsPage.this, DetailsActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("date", date.getTime());
            intent.putExtra("time", time);
            intent.putExtra("location", location);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        events.addView(item);
    }

    private void onDetails(String name, Date date, String Time, String Location, double Latitude, double Longitude, String eventId, String userId, String userEmail) {

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
