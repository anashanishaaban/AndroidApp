package com.example.campuseventsscheduler;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.file.attribute.FileTime;

public class EventsPage extends AppCompatActivity
{
    private LinearLayout events;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        events = findViewById(R.id.events);


        AddEvent(
                "The Civility Project " ,
                "September 30, 2024",
                "6:00 PM - 7:00PM",
                "Pasant Theater, Wharton Center",
                -84.4707142946247,
                42.72390930325365);

        AddEvent(
                "UAB Member Connect  " ,
                "October 1, 2024",
                "5:00 PM - 6:00PM",
                "MSU Union, Lake Ontario Room, 3rd Floor",
                -84.48286991610922,
                42.73428445659781);


        AddEvent(
                "Global Learning Expo" ,
                "October 2, 2024",
                "1:00 PM - 5:00PM",
                "Breslin Center",
                -84.4922975258043,
                42.72818447405987
        );

    }


    private void AddEvent(String name, String Date, String Time, String Location, double Longitude, double Latitude)
    {

        View item = LayoutInflater.from(this).inflate(R.layout.event_item,events,false);
        TextView eventView = item.findViewById(R.id.eventView);
        Button DetailsButton = item.findViewById(R.id.DetailsButton);

        eventView.setText(name);

        DetailsButton.setBackgroundColor(Color.rgb(24, 69, 59));


        DetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDetails(name, Date, Time, Location, Longitude, Latitude);
            }
        });

        events.addView(item);
    }

    private void onDetails(String name, String Date, String Time, String Location, double Longitude, double Latitude)
    {
        Toast.makeText(this, "Details for "+ name, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DetailsActivity.class);

        intent.putExtra("name", name);
        intent.putExtra("date", Date);
        intent.putExtra("time", Time);
        intent.putExtra("location", Location);
        intent.putExtra("longitude", Longitude);
        intent.putExtra("latitude", Latitude);
        startActivity(intent);
    }



}