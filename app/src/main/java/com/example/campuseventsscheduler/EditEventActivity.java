package com.example.campuseventsscheduler;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditEventActivity extends AppCompatActivity {

    private EditText eventNameEditText, eventDateEditText, eventTimeEditText, eventLocationEditText;
    private Button saveChangesButton;
    private FirebaseFirestore db;
    private String eventId;
    private Date selectedDate;
    private double selectedLatitude = Double.NaN;
    private double selectedLongitude = Double.NaN;
    private EditText eventDescriptionEditText;



    private ActivityResultLauncher<Intent> autocompleteLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        // Initialize the Places SDK
        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);

        // Create a new PlacesClient instance
        PlacesClient placesClient = Places.createClient(this);

        // Initialize Firestore and UI components
        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId"); // Retrieve the event ID from Intent

        eventNameEditText = findViewById(R.id.eventNameEditText);
        eventDateEditText = findViewById(R.id.eventDateEditText);
        eventTimeEditText = findViewById(R.id.eventTimeEditText);

        eventLocationEditText = findViewById(R.id.eventLocationEditText);

        eventDescriptionEditText = findViewById(R.id.eventDescriptionEditText);

        saveChangesButton = findViewById(R.id.submitEventButton);
        Button cancelButton = findViewById(R.id.cancelButton); // Reference the cancel button

        eventTimeEditText.setOnClickListener(v -> showTimePicker());

        // Initialize the date picker
        eventDateEditText.setOnClickListener(v -> showDatePicker());

        // Initialize the Places Autocomplete launcher
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

        // Set click listener for the event location field to launch Places Autocomplete
        eventLocationEditText.setOnClickListener(v -> {
            // Specify the fields to return
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

            // Create the intent
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .build(this);

            // Launch the autocomplete activity
            autocompleteLauncher.launch(intent);
        });

        // Load the existing event data into the form
        loadEventData();

        // Set click listener for saving changes
        saveChangesButton.setOnClickListener(v -> saveEventChanges());

        // Set click listener for the Cancel button
        cancelButton.setOnClickListener(v -> finish()); // Close the activity and go back
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            // Set calendar to the selected date
            calendar.setTime(selectedDate);
        }
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            selectedDate = calendar.getTime();
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


    private void loadEventData() {
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                eventNameEditText.setText(documentSnapshot.getString("name"));

                com.google.firebase.Timestamp timestamp = documentSnapshot.getTimestamp("date");
                Date date = timestamp != null ? timestamp.toDate() : null;
                if (date != null) {
                    selectedDate = date; // Store the date for later use
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    eventDateEditText.setText(dateFormat.format(date));
                }

                eventTimeEditText.setText(documentSnapshot.getString("time"));
                eventLocationEditText.setText(documentSnapshot.getString("location"));
                selectedLatitude = documentSnapshot.getDouble("latitude");
                selectedLongitude = documentSnapshot.getDouble("longitude");
                eventDescriptionEditText.setText(documentSnapshot.getString("description"));


            }
        }).addOnFailureListener(e -> Toast.makeText(EditEventActivity.this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveEventChanges() {
        String name = eventNameEditText.getText().toString();
        String time = eventTimeEditText.getText().toString();
        String location = eventLocationEditText.getText().toString();
        String description = eventDescriptionEditText.getText().toString();



        // Validate fields
        if (name.isEmpty() || selectedDate == null || time.isEmpty() || location.isEmpty()
                || Double.isNaN(selectedLatitude) || Double.isNaN(selectedLongitude)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }


        // Update Firestore document
        DocumentReference eventRef = db.collection("events").document(eventId);

        // Prepare data
        eventRef.update(
                "name", name,
                "date", selectedDate,
                "time", time,
                "location", location,
                "latitude", selectedLatitude,
                "longitude", selectedLongitude,
                "description", description
        ).addOnSuccessListener(aVoid -> {
            Toast.makeText(EditEventActivity.this, "Event updated successfully!", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity and return to the previous screen
        }).addOnFailureListener(e -> Toast.makeText(EditEventActivity.this, "Failed to update event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
