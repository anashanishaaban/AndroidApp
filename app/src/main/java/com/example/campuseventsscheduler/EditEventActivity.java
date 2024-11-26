//EditEventActivity.java

package com.example.campuseventsscheduler;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditEventActivity extends AppCompatActivity {

    private EditText eventNameEditText, eventDateEditText, eventTimeEditText, eventLocationEditText, latitudeEditText, longitudeEditText;
    private Button saveChangesButton;
    private FirebaseFirestore db;
    private String eventId;
    private Date selectedDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        // Initialize Firestore and UI components
        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId"); // Retrieve the event ID from Intent


        eventNameEditText = findViewById(R.id.eventNameEditText);
        eventDateEditText = findViewById(R.id.eventDateEditText);
        eventTimeEditText = findViewById(R.id.eventTimeEditText);
        eventLocationEditText = findViewById(R.id.eventLocationEditText);
        latitudeEditText = findViewById(R.id.latitudeEditText);
        longitudeEditText = findViewById(R.id.longitudeEditText);
        saveChangesButton = findViewById(R.id.submitEventButton);
        Button cancelButton = findViewById(R.id.cancelButton); // Reference the cancel button


        // Initialize the date picker
        eventDateEditText.setOnClickListener(v -> showDatePicker());

        // Update button text
        saveChangesButton.setText("Save Changes");

        // Load the existing event data into the form
        loadEventData();

        // Set click listener for saving changes
        saveChangesButton.setOnClickListener(v -> saveEventChanges());

        // Set click listener for the Cancel button
        cancelButton.setOnClickListener(v -> finish()); // Close the activity and go back


    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            selectedDate = calendar.getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            eventDateEditText.setText(dateFormat.format(selectedDate));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
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
                latitudeEditText.setText(String.valueOf(documentSnapshot.getDouble("latitude")));
                longitudeEditText.setText(String.valueOf(documentSnapshot.getDouble("longitude")));
            }
        }).addOnFailureListener(e -> Toast.makeText(EditEventActivity.this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveEventChanges() {
        String name = eventNameEditText.getText().toString();


        String time = eventTimeEditText.getText().toString();
        String location = eventLocationEditText.getText().toString();
        String latitudeStr = latitudeEditText.getText().toString().trim();
        String longitudeStr = longitudeEditText.getText().toString().trim();

        // Add validation for selectedDate:
        if (name.isEmpty() || selectedDate == null || time.isEmpty() || location.isEmpty() || latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);

            // Update Firestore document
            DocumentReference eventRef = db.collection("events").document(eventId);


            // Update the Firestore document with the correct data types:
            eventRef.update("name", name,
                            "date", selectedDate,
                            "time", time,
                            "location", location,
                            "latitude", latitude,
                            "longitude", longitude)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditEventActivity.this, "Event updated successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity and return to the previous screen
                    })
                    .addOnFailureListener(e -> Toast.makeText(EditEventActivity.this, "Failed to update event: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Latitude and Longitude must be valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
}
