package com.example.campuseventsscheduler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private Button loginButton;
    private Button signUpButton;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Check if the user is logged in
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            if (currentUser != null && currentUser.isEmailVerified()) {
                // User is logged in and verified, go to EventsPage
                Intent intent = new Intent(MainActivity.this, EventsPage.class);
                startActivity(intent);
                finish(); // Prevent returning to the MainActivity
                return;
            } else {
                // User is not verified or currentUser is null, reset login state
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(KEY_IS_LOGGED_IN, false);
                editor.apply();
                auth.signOut();
            }
        }

        setContentView(R.layout.activity_main);

        // Initialize buttons
        loginButton = findViewById(R.id.LoginButton);
        signUpButton = findViewById(R.id.signUpButton);

        // Set click listeners
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}
