package com.pawsitive.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pawsitive.app.auth.RoleSelectionActivity;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

public class SignupActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etEmail, etPassword, etConfirmPassword;
    private CheckBox cbTerms;
    private Button btnContinue;
    private TextView tvLogin;
    private NetworkManager networkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        networkManager = new NetworkManager(this);

        // Bind views
        ivBack = findViewById(R.id.ivBack);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnContinue = findViewById(R.id.btnContinue);
        tvLogin = findViewById(R.id.tvLogin);

        // Back button
        ivBack.setOnClickListener(v -> finish());

        // Login text
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });

        // Continue button
        btnContinue.setOnClickListener(v -> checkEmailAndProceed());
    }

    private void checkEmailAndProceed() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to terms", Toast.LENGTH_SHORT).show();
            return;
        }

        btnContinue.setEnabled(false);
        btnContinue.setText("Checking...");

        networkManager.checkEmail(email, new NetworkManager.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                btnContinue.setEnabled(true);
                btnContinue.setText("Continue");
                if (exists) {
                    etEmail.setError("Email already registered!");
                    Toast.makeText(SignupActivity.this, "Email already registered", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(SignupActivity.this, RoleSelectionActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    startActivity(intent);
                }
            }

            @Override
            public void onError(String errorMessage) {
                btnContinue.setEnabled(true);
                btnContinue.setText("Continue");
                Toast.makeText(SignupActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
