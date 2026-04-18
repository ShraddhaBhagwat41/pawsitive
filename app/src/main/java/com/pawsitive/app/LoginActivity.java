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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.pawsitive.app.admin.AdminHomeActivity;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;
import com.pawsitive.app.ngo.NGOHomeActivity;
import com.pawsitive.app.staff.StaffDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignup;
    private ProgressBar progressBar;

    private NetworkManager networkManager;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        networkManager = new NetworkManager(this);
        firebaseAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignup = findViewById(R.id.tvSignup);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginUser());

        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, "Forgot Password clicked", Toast.LENGTH_SHORT).show());

        tvSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
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

        networkManager.login(email, password, new NetworkManager.ApiCallback<ApiService.LoginResponse>() {
            @Override
            public void onSuccess(ApiService.LoginResponse response) {
                // Keep Firebase auth in sync for storage/firestore rules that rely on request.auth.
                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);

                            if (!task.isSuccessful()) {
                                String msg = task.getException() != null ? task.getException().getMessage() : "Unknown Firebase auth error";
                                Toast.makeText(LoginActivity.this, "Login ok, but Firebase auth failed: " + msg, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            }
                            goToHome(response != null ? response.role : null);
                        });
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToHome(String role) {
        Intent intent;
        if ("ADMIN".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, AdminHomeActivity.class);
        } else if ("NGO".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, NGOHomeActivity.class);
        } else if ("STAFF".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, StaffDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, HomeActivity.class);
        }
        startActivity(intent);
        finish();
    }
}

