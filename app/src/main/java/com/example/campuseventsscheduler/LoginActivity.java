//LoginActivity.java
package com.example.campuseventsscheduler;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpLink; // Changed to TextView for consistency
    private TextView resendEmailLink; // Changed to TextView for consistency

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

        // Check if user is already logged in
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            auth = FirebaseAuth.getInstance();
            currentUser = auth.getCurrentUser();
            if (currentUser != null && currentUser.isEmailVerified()) {
                // Navigate directly to EventsPage if logged in and verified
                startActivity(new Intent(LoginActivity.this, EventsPage.class));
                finish(); // Prevent returning to the login screen
                return;
            } else {
                // Sign out user if not verified
                auth.signOut();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(KEY_IS_LOGGED_IN, false);
                editor.apply();
            }
        }

        setContentView(R.layout.activity_login);

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance();

        // Initialize UI components
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.LoginButton);
        signUpLink = findViewById(R.id.signUpLink);
        resendEmailLink = findViewById(R.id.resendEmailLink);

        // Login button logic
        loginButton.setOnClickListener(v -> {
            String email = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        // Resend Verification Email link logic
        resendEmailLink.setOnClickListener(v -> {
            if (currentUser != null) {
                currentUser.sendEmailVerification()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(LoginActivity.this, "Please log in first.", Toast.LENGTH_SHORT).show();
            }
        });

        // Sign Up link logic
        signUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser = auth.getCurrentUser();
                        if (currentUser != null && currentUser.isEmailVerified()) {
                            // Save login state in SharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(KEY_IS_LOGGED_IN, true);
                            editor.apply();

                            // Navigate to EventsPage
                            Intent intent = new Intent(LoginActivity.this, EventsPage.class);
                            startActivity(intent);
                            finish(); // Prevent returning to login screen
                        } else {
                            Toast.makeText(LoginActivity.this, "Please verify your email address.", Toast.LENGTH_LONG).show();
                            auth.signOut(); // Sign out the user
                        }
                    } else {
                        // Login failed
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
