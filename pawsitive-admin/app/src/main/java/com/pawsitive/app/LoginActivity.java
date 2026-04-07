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

import com.pawsitive.app.ngo.NGOHomeActivity;
import com.pawsitive.app.user.UserHomeActivity;
import com.pawsitive.app.admin.AdminHomeActivity;
import com.pawsitive.app.network.NetworkManager;
import com.pawsitive.app.network.ApiService;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignup;
    private ProgressBar progressBar;

    private NetworkManager networkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Network Manager
        networkManager = new NetworkManager(this);

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignup = findViewById(R.id.tvSignup);
        progressBar = findViewById(R.id.progressBar);

        // Login button click listener
        btnLogin.setOnClickListener(v -> loginUser());

        // Forgot Password click listener (placeholder)
        tvForgotPassword.setOnClickListener(v -> 
            Toast.makeText(LoginActivity.this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
        );

        // Signup text click listener
        tvSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
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

        // Call REST API for login
        networkManager.login(email, password, new NetworkManager.ApiCallback<ApiService.LoginResponse>() {
            @Override
            public void onSuccess(ApiService.LoginResponse response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                
                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                goToHome(response.role);
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
        Intent intent = null;
        
        if ("ADMIN".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, AdminHomeActivity.class);
        } else if ("NGO".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, NGOHomeActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, UserHomeActivity.class);
        }
        
        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}

