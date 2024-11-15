package com.example.campuseventsscheduler;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button signUpButton;

    // Add SharedPreferences
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Check if user is already logged in
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            // User is logged in, go directly to EventsPage
            startActivity(new Intent(LoginActivity.this, EventsPage.class));
            finish(); // Prevent returning to login screen
            return;
        }

        setContentView(R.layout.activity_main);

        // Your existing login setup code here
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.LoginButton);
        signUpButton = findViewById(R.id.signUpButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                // Your existing login validation logic here
                if (isValidLogin(username, password)) {
                    // Save login state
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(KEY_IS_LOGGED_IN, true);
                    editor.apply();

                    // Navigate to EventsPage
                    Intent intent = new Intent(LoginActivity.this, EventsPage.class);
                    startActivity(intent);
                    finish(); // Prevent returning to login screen
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean isValidLogin(String username, String password) {
        // Your existing login validation logic here
        return !username.isEmpty() && !password.isEmpty();
    }
}