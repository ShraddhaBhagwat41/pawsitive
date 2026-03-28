package com.pawsitive.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private TextView tvVerifyMessage, tvVerifiedStatus;
    private Button btnResendEmail, btnGoToLogin;
    private FirebaseAuth mAuth;
    private Handler checkStatusHandler;
    private Runnable checkStatusRunnable;
    private boolean isVerified = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        mAuth = FirebaseAuth.getInstance();

        tvVerifyMessage = findViewById(R.id.tvVerifyMessage);
        tvVerifiedStatus = findViewById(R.id.tvVerifiedStatus);
        btnResendEmail = findViewById(R.id.btnResendEmail);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        String email = getIntent().getStringExtra("email");
        if (email != null) {
            tvVerifyMessage.setText("We have sent a verification link to " + email + ". Please check your inbox and verify your email to continue.");
        }

        btnResendEmail.setOnClickListener(v -> resendEmail());

        btnGoToLogin.setOnClickListener(v -> goToLogin());

        // Setup handler to check status periodically while screen is open
        checkStatusHandler = new Handler();
        checkStatusRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isVerified) {
                    checkEmailVerificationStatusAutomatically();
                    checkStatusHandler.postDelayed(this, 3000); // Check every 3 seconds
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isVerified) {
            checkEmailVerificationStatusAutomatically();
            checkStatusHandler.postDelayed(checkStatusRunnable, 3000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkStatusHandler.removeCallbacks(checkStatusRunnable);
    }

    private void checkEmailVerificationStatusAutomatically() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    showVerifiedStatus();
                }
            });
        }
    }

    private void showVerifiedStatus() {
        isVerified = true;
        checkStatusHandler.removeCallbacks(checkStatusRunnable);
        
        tvVerifiedStatus.setVisibility(View.VISIBLE);
        btnGoToLogin.setVisibility(View.VISIBLE);
        btnResendEmail.setVisibility(View.GONE);
        
        tvVerifyMessage.setText("Your email has been successfully verified! You can now log in to your account.");
        tvVerifyMessage.setTextColor(ContextCompat.getColor(this, R.color.green_success));
        
        Toast.makeText(this, "Verified Successfully!", Toast.LENGTH_SHORT).show();
    }

    private void goToLogin() {
        mAuth.signOut(); // Ensure fresh login state
        startActivity(new Intent(VerifyEmailActivity.this, LoginActivity.class));
        finishAffinity();
    }

    private void resendEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User session expired. Please sign up again.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email resent.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to resend email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
