package com.pawsitive.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pawsitive.app.auth.RoleSelectionActivity;

public class SignupActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etFullName, etEmail, etPassword;
    private CheckBox cbTerms;
    private Button btnContinue;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        // Bind views
        ivBack = findViewById(R.id.ivBack);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnContinue = findViewById(R.id.btnContinue);
        tvLogin = findViewById(R.id.tvLogin);

        // Back button
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, WelcomeActivity.class));
                finish();
            }
        });

        // Login text
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();
            }
        });

        // Continue / Sign up button
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSignup();
            }
        });
    }

    private void attemptSignup() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

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

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(SignupActivity.this, "Please agree to terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading and create Firebase user
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Optionally send verification email
                                user.sendEmailVerification();

                                Toast.makeText(SignupActivity.this,
                                        "Account created. Please verify your email.",
                                        Toast.LENGTH_LONG).show();

                                // Navigate to RoleSelectionActivity, pass name & email
                                Intent intent = new Intent(SignupActivity.this, RoleSelectionActivity.class);
                                intent.putExtra("full_name", fullName);
                                intent.putExtra("email", email);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(SignupActivity.this,
                                        "Signup succeeded, but user is null.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String message = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Signup failed";
                            Toast.makeText(SignupActivity.this,
                                    "Error: " + message,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
