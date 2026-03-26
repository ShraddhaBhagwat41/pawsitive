package com.pawsitive.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pawsitive.app.ngo.NGOHomeActivity;
import com.pawsitive.app.user.UserHomeActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignup;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignup = findViewById(R.id.tvSignup);
        progressBar = findViewById(R.id.progressBar);

        // Login button click listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Forgot Password click listener (placeholder)
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
            }
        });

        // Signup text click listener
        tvSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user == null) {
                                Toast.makeText(LoginActivity.this,
                                        "Login succeeded but user is null.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (!user.isEmailVerified()) {
                                Toast.makeText(LoginActivity.this,
                                        "Please verify your email before logging in.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            // Fetch role from Firestore and redirect
                            fetchRoleAndRedirect(user.getUid());
                        } else {
                            String message = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Login failed";
                            Toast.makeText(LoginActivity.this,
                                    "Error: " + message,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void fetchRoleAndRedirect(String uid) {
        // First check in users collection (USER role)
        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Assume role USER
                        String role = documentSnapshot.getString("role");
                        if (role == null || role.equalsIgnoreCase("USER")) {
                            goToUserHome();
                        } else if (role.equalsIgnoreCase("NGO")) {
                            goToNgoHome();
                        } else {
                            goToUserHome();
                        }
                    } else {
                        // Not in users, check ngo_profiles
                        checkNgoProfile(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this,
                            "Failed to fetch role: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void checkNgoProfile(String uid) {
        firestore.collection("ngo_profiles").document(uid)
                .get()
                .addOnSuccessListener((DocumentSnapshot documentSnapshot) -> {
                    if (documentSnapshot.exists()) {
                        goToNgoHome();
                    } else {
                        // Default to user home if nothing found
                        goToUserHome();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this,
                            "Failed to fetch NGO profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void goToUserHome() {
        Intent intent = new Intent(LoginActivity.this, UserHomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToNgoHome() {
        Intent intent = new Intent(LoginActivity.this, NGOHomeActivity.class);
        startActivity(intent);
        finish();
    }
}
