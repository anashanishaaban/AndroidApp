package com.example.campuseventsscheduler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isLoggedIn", false);
                editor.apply();

                Intent intent = new Intent(EventsPage.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Add Event button logic
        addEventButton.setOnClickListener(v -> {
            Intent intent = new Intent(EventsPage.this, AddEventActivity.class);
            startActivity(intent);
        });

        fetchEventsFromFirebase();

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
                        String date = document.getString("date");
                        String time = document.getString("time");
                        String location = document.getString("location");
                        double latitude = document.getDouble("latitude");
                        double longitude = document.getDouble("longitude");

                        // Add each event to the layout dynamically
                        addEventToLayout(name, date, time, location, latitude, longitude);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Method to add event to the layout dynamically
    private void addEventToLayout(String name, String date, String time, String location, double latitude, double longitude) {
        View item = LayoutInflater.from(this).inflate(R.layout.event_item, events, false);
        TextView eventView = item.findViewById(R.id.eventView);
        Button detailsButton = item.findViewById(R.id.DetailsButton);

        // Set event name and button color
        eventView.setText(name);
        detailsButton.setBackgroundColor(Color.rgb(24, 69, 59));

        // Set click listener for the "Details" button
        detailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDetails(name, date, time, location, latitude, longitude);
            }
        });

        // Add the event view to the layout
        events.addView(item);
    }

    private void onDetails(String name, String Date, String Time, String Location, double Longitude, double Latitude) {
        Toast.makeText(this, "Details for " + name, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("date", Date);
        intent.putExtra("time", Time);
        intent.putExtra("location", Location);
        intent.putExtra("latitude", Latitude);
        intent.putExtra("longitude", Longitude);

        startActivity(intent);
    }
}