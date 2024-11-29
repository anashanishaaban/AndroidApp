//AddEventActivity.java
package com.example.campuseventsscheduler;

import android.app.TimePickerDialog;
import android.content.Intent;

import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;


import java.util.HashMap;
import java.util.Map;

public class AddEventActivity extends AppCompatActivity {

    private EditText eventNameEditText, eventDateEditText, eventTimeEditText, eventLocationEditText;
    private EditText eventDescriptionEditText;

    private Button submitEventButton;
    private FirebaseFirestore db;
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private Date selectedDate; // To store the selected date
    private double selectedLatitude = Double.NaN;
    private double selectedLongitude = Double.NaN;


    private ActivityResultLauncher<Intent> autocompleteLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the Places SDK
        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);

        // Create a new PlacesClient instance
        PlacesClient placesClient = Places.createClient(this);

        // Check user email
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.getEmail().endsWith("@msu.edu")) {
            Toast.makeText(this, "You are not authorized to create events.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity
            return;
        }

        setContentView(R.layout.activity_add_event);

        db = FirebaseFirestore.getInstance();

        autocompleteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        eventLocationEditText.setText(place.getAddress());
                        // Update latitude and longitude
                        if (place.getLatLng() != null) {
                            selectedLatitude = place.getLatLng().latitude;
                            selectedLongitude = place.getLatLng().longitude;
                        }

                    } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                        Status status = Autocomplete.getStatusFromIntent(result.getData());
                        Toast.makeText(this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                    }
                    // Handle RESULT_CANCELED if needed
                }
        );



        eventNameEditText = findViewById(R.id.eventNameEditText);
        eventDateEditText = findViewById(R.id.eventDateEditText);
        eventTimeEditText = findViewById(R.id.eventTimeEditText);
        eventLocationEditText = findViewById(R.id.eventLocationEditText);

        submitEventButton = findViewById(R.id.submitEventButton);

        eventDescriptionEditText = findViewById(R.id.eventDescriptionEditText);

        Button cancelButton = findViewById(R.id.cancelButton); // Reference the Cancel button

        eventDateEditText.setOnClickListener(v -> showDatePicker());

        submitEventButton.setOnClickListener(v -> addEventToFirestore());

        // Set click listener for the Cancel button
        cancelButton.setOnClickListener(v -> {
            finish(); // Close the AddEventActivity and go back to the previous screen
        });
        eventTimeEditText.setOnClickListener(v -> showTimePicker());

        eventLocationEditText.setOnClickListener(v -> {
            // Specify the fields to return
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

            // Create the intent
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .build(this);

            // Launch the autocomplete activity
            autocompleteLauncher.launch(intent);
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

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            // Convert to AM/PM format
            String amPm = selectedHour >= 12 ? "PM" : "AM";
            int hourIn12Format = selectedHour % 12 == 0 ? 12 : selectedHour % 12;

            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hourIn12Format, selectedMinute, amPm);
            eventTimeEditText.setText(formattedTime);
        }, hour, minute, false).show(); // `false` for 12-hour format

    }


    private void addEventToFirestore() {
        String name = eventNameEditText.getText().toString();
        String time = eventTimeEditText.getText().toString();
        String location = eventLocationEditText.getText().toString();
        String description = eventDescriptionEditText.getText().toString();

        if (name.isEmpty() || selectedDate == null || time.isEmpty() || location.isEmpty()
                || Double.isNaN(selectedLatitude) || Double.isNaN(selectedLongitude)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {


            // Add event data to Firestore
            Map<String, Object> event = new HashMap<>();
            event.put("name", name);
            event.put("date", selectedDate);
            event.put("time", time);
            event.put("location", location);
            event.put("latitude", selectedLatitude);
            event.put("longitude", selectedLongitude);
            event.put("attendees", new ArrayList<String>());
            event.put("attendeesCount", 0L);
            event.put("description", description); // Add description field


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
