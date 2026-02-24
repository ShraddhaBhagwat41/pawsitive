package com.example.pawsitive;

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

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etUsername, etEmailPhone, etPassword;
    private CheckBox cbTerms;
    private Button btnContinue;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize views
        ivBack = findViewById(R.id.ivBack);
        etUsername = findViewById(R.id.etUsername);
        etEmailPhone = findViewById(R.id.etEmailPhone);
        etPassword = findViewById(R.id.etPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnContinue = findViewById(R.id.btnContinue);
        tvLogin = findViewById(R.id.tvLogin);

        // Back button click listener
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Continue button click listener
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String emailPhone = etEmailPhone.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // Validate inputs
                if (TextUtils.isEmpty(username)) {
                    etUsername.setError("Username is required");
                    etUsername.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(emailPhone)) {
                    etEmailPhone.setError("Email or Phone is required");
                    etEmailPhone.requestFocus();
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
                    Toast.makeText(SignupActivity.this, "Please agree to terms and conditions", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                // TODO: Proceed to next step (Profile screen)
                // For now, navigate to HomeActivity
                Toast.makeText(SignupActivity.this, "Registration Step 1 Complete!", Toast.LENGTH_SHORT).show();

                // Navigate to Profile setup (Step 2) - Create this activity next
                // Intent intent = new Intent(SignupActivity.this, ProfileSetupActivity.class);
                // startActivity(intent);

                // Temporary: Navigate to HomeActivity
                Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Login text click listener
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to Login Activity
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
