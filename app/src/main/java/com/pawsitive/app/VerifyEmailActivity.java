package com.pawsitive.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private TextView tvVerifyMessage;
    private Button btnResendEmail, btnGoToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        mAuth = FirebaseAuth.getInstance();

        tvVerifyMessage = findViewById(R.id.tvVerifyMessage);
        btnResendEmail = findViewById(R.id.btnResendEmail);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        String email = getIntent().getStringExtra("email");
        if (email != null) {
            tvVerifyMessage.setText("We have sent a verification link to " + email + ". Please check your inbox.");
        }

        btnResendEmail.setOnClickListener(v -> resendEmail());

        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(VerifyEmailActivity.this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void resendEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email resent.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to resend email.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
