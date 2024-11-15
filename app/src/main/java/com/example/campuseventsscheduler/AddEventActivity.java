package com.example.campuseventsscheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddEventActivity extends AppCompatActivity {

    private EditText eventNameEditText, eventDateEditText, eventTimeEditText, eventLocationEditText, latitudeEditText, longitudeEditText;
    private Button submitEventButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        db = FirebaseFirestore.getInstance();

        eventNameEditText = findViewById(R.id.eventNameEditText);
        eventDateEditText = findViewById(R.id.eventDateEditText);
        eventTimeEditText = findViewById(R.id.eventTimeEditText);
        eventLocationEditText = findViewById(R.id.eventLocationEditText);
        latitudeEditText = findViewById(R.id.latitudeEditText);
        longitudeEditText = findViewById(R.id.longitudeEditText);
        submitEventButton = findViewById(R.id.submitEventButton);

        submitEventButton.setOnClickListener(v -> addEventToFirestore());

    }

    private void addEventToFirestore() {
        String name = eventNameEditText.getText().toString();
        String date = eventDateEditText.getText().toString();
        String time = eventTimeEditText.getText().toString();
        String location = eventLocationEditText.getText().toString();
        String latitudeStr = latitudeEditText.getText().toString().trim();
        String longitudeStr = longitudeEditText.getText().toString().trim();

        if (name.isEmpty() || date.isEmpty() || time.isEmpty() || location.isEmpty() || latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);

            // Add event data to Firestore
            Map<String, Object> event = new HashMap<>();
            event.put("name", name);
            event.put("date", date);
            event.put("time", time);
            event.put("location", location);
            event.put("latitude", latitude);
            event.put("longitude", longitude);

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
