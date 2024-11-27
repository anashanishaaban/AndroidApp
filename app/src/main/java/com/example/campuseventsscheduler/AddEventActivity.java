//AddEventActivity.java
package com.example.campuseventsscheduler;

import android.content.Intent;

import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class AddEventActivity extends AppCompatActivity {

    private EditText eventNameEditText, eventDateEditText, eventTimeEditText, eventLocationEditText, latitudeEditText, longitudeEditText;
    private Button submitEventButton;
    private FirebaseFirestore db;
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private Date selectedDate; // To store the selected date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check user email
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.getEmail().endsWith("@msu.edu")) {
            Toast.makeText(this, "You are not authorized to create events.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity
            return;
        }

        setContentView(R.layout.activity_add_event);

        db = FirebaseFirestore.getInstance();


        eventNameEditText = findViewById(R.id.eventNameEditText);
        eventDateEditText = findViewById(R.id.eventDateEditText);
        eventTimeEditText = findViewById(R.id.eventTimeEditText);
        eventLocationEditText = findViewById(R.id.eventLocationEditText);
        latitudeEditText = findViewById(R.id.latitudeEditText);
        longitudeEditText = findViewById(R.id.longitudeEditText);
        submitEventButton = findViewById(R.id.submitEventButton);

        Button cancelButton = findViewById(R.id.cancelButton); // Reference the Cancel button

        eventDateEditText.setOnClickListener(v -> showDatePicker());

        submitEventButton.setOnClickListener(v -> addEventToFirestore());

        // Set click listener for the Cancel button
        cancelButton.setOnClickListener(v -> {
            finish(); // Close the AddEventActivity and go back to the previous screen
        });

    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            selectedDate = calendar.getTime(); // Save the selected date
            // Format and display the date in the EditText
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            eventDateEditText.setText(dateFormat.format(selectedDate));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addEventToFirestore() {
        String name = eventNameEditText.getText().toString();
//        String date = eventDateEditText.getText().toString();
        String time = eventTimeEditText.getText().toString();
        String location = eventLocationEditText.getText().toString();
        String latitudeStr = latitudeEditText.getText().toString().trim();
        String longitudeStr = longitudeEditText.getText().toString().trim();

        if (name.isEmpty() || selectedDate == null || time.isEmpty() || location.isEmpty() || latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);

            // Add event data to Firestore
            Map<String, Object> event = new HashMap<>();
            event.put("name", name);
            event.put("date", selectedDate);
            event.put("time", time);
            event.put("location", location);
            event.put("latitude", latitude);
            event.put("longitude", longitude);

            // Add user ID and email
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail(); // Get the user's email
            event.put("userId", userId);
            event.put("userEmail", userEmail); // Add email to Firestore

            db.collection("events")
                    .add(event)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Event added successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Return to EventsPage
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Latitude and Longitude must be valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

}
