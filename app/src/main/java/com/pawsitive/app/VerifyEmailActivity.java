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

import com.pawsitive.app.user.UserHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class VerifyEmailActivity extends AppCompatActivity {

    private TextView tvVerifyMessage, tvVerifiedStatus;
    private Button btnResendEmail, btnGoToLogin;
    private FirebaseAuth mAuth;
    private Handler checkStatusHandler;
    private Runnable checkStatusRunnable;
    private boolean isVerified = false;
    private int verificationCheckCount = 0;
    private static final int MAX_CHECKS = 30;  // 30 seconds of checking (1 check per second)

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

        btnGoToLogin.setOnClickListener(v -> {
            if (isVerified) {
                android.util.Log.d("VerifyEmail", "Redirecting to dashboard");
                goToDashboard();
            } else {
                // Before going to login, do one final aggressive check
                android.util.Log.d("VerifyEmail", "Final verification check before login");
                verificationCheckCount = 0;  // Reset to give it 30 fresh checks
                checkEmailVerificationStatusImmediately();
            }
        });
        
        // Automatically send Firebase verification email
        sendFirebaseVerificationEmail();
        
        // Also check immediately on load
        checkEmailVerificationStatusImmediately();

        // Setup handler to check status periodically while screen is open
        checkStatusHandler = new Handler();
        checkStatusRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isVerified) {
                    checkEmailVerificationStatusImmediately();
                    checkStatusHandler.postDelayed(this, 1000); // Check every 1 second (faster detection)
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset check count each time screen comes back - user might have verified on phone
        verificationCheckCount = 0;
        android.util.Log.d("VerifyEmail", "Screen resumed - resetting verification check count");
        
        // Check immediately when screen comes back into view
        checkEmailVerificationStatusImmediately();
        if (!isVerified) {
            // Then check every 1 second
            checkStatusHandler.postDelayed(checkStatusRunnable, 1000);
        }
    }

    private void checkEmailVerificationStatusImmediately() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            verificationCheckCount++;
            android.util.Log.d("VerifyEmail", "Verification check #" + verificationCheckCount + " of " + MAX_CHECKS);
            
            // First, force refresh the ID token from backend to sync email verification status
            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                if (tokenTask.isSuccessful()) {
                    android.util.Log.d("VerifyEmail", "ID token refreshed successfully");
                    // Now reload user data from Firebase backend
                    user.reload().addOnCompleteListener(reloadTask -> {
                        if (reloadTask.isSuccessful()) {
                            android.util.Log.d("VerifyEmail", "User reloaded after token refresh. Email verified: " + user.isEmailVerified());
                            
                            if (user.isEmailVerified()) {
                                showVerifiedStatus();
                            } else {
                                // Firebase Auth shows not verified, check Firestore as backup
                                checkFirestoreVerification(user.getUid());
                            }
                        } else {
                            android.util.Log.e("VerifyEmail", "Reload failed: " + reloadTask.getException().getMessage());
                            // Check Firestore as backup
                            checkFirestoreVerification(user.getUid());
                        }
                    });
                } else {
                    android.util.Log.e("VerifyEmail", "Token refresh failed: " + tokenTask.getException().getMessage());
                    // Check Firestore as backup
                    checkFirestoreVerification(user.getUid());
                }
            });
        } else {
            android.util.Log.w("VerifyEmail", "No current user");
        }
    }
    
    private void checkFirestoreVerification(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
            .addOnSuccessListener(document -> {
                if (document.exists() && document.getBoolean("email_verified") != null && document.getBoolean("email_verified")) {
                    android.util.Log.d("VerifyEmail", "✓ Email verified in Firestore!");
                    showVerifiedStatus();
                } else {
                    // Not verified yet, check retry conditions
                    if (verificationCheckCount == 10) {
                        showManualRetryOption();
                    } else if (verificationCheckCount >= MAX_CHECKS) {
                        showReLoginSuggestion();
                    }
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.w("VerifyEmail", "Firestore check failed: " + e.getMessage());
                // Continue with normal retry flow
                if (verificationCheckCount == 10) {
                    showManualRetryOption();
                } else if (verificationCheckCount >= MAX_CHECKS) {
                    showReLoginSuggestion();
                }
            });
    }
    
    private void showManualRetryOption() {
        android.util.Log.d("VerifyEmail", "Showing manual retry option after 10 checks");
        tvVerifyMessage.setText("Still waiting for verification...\n\n✓ If you've already clicked the verification link, tap 'Go to Login' to retry\n✗ If you haven't verified yet, click 'Re-send verification email' and check your inbox");
        tvVerifyMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        btnGoToLogin.setText("I've Verified - Check Again");
    }
    
    private void showReLoginSuggestion() {
        android.util.Log.d("VerifyEmail", "Showing re-login suggestion after " + MAX_CHECKS + " checks");
        tvVerifyMessage.setText("⚠️ Verification not detected after 30 seconds.\n\nPlease:\n• Click 'Go to Login' to sign out and back in\n• This will refresh your session and detect verification");
        tvVerifyMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        btnGoToLogin.setText("Sign Out & Try Again");
    }
    
    private void checkServerSideVerification(String email) {
        if (email == null) return;
        
        // This will help us debug if Firebase Auth backend knows about verification
        android.util.Log.d("VerifyEmail", "Checking server-side verification status for: " + email);
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkStatusHandler.removeCallbacks(checkStatusRunnable);
    }

    private void sendFirebaseVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            android.util.Log.d("VerifyEmail", "Verification email sent by backend during registration");
            android.util.Log.d("VerifyEmail", "Email: " + user.getEmail());
            tvVerifyMessage.setText(
                "📧 Verification email sent!\n\n" +
                "✓ Check your inbox (and spam folder) for a link from Pawsitive\n\n" +
                "📱 If you're using your PHONE to click the link:\n" +
                "   • Click the ✓ button in the email\n" +
                "   • Your email will be verified instantly\n" +
                "   • Return to this emulator app\n" +
                "   • It will auto-detect the verification\n\n" +
                "Refreshing verification status..."
            );
            tvVerifyMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            Toast.makeText(VerifyEmailActivity.this, "Verification email sent to " + user.getEmail(), Toast.LENGTH_LONG).show();
        } else {
            android.util.Log.w("VerifyEmail", "No current user to send verification email");
            tvVerifyMessage.setText("⚠️ Session error - please sign up again");
            btnGoToLogin.setText("Go to Sign Up");
        }
    }

    private void showVerifiedStatus() {
        if (isVerified) {
            return; // Already verified, avoid double execution
        }
        
        isVerified = true;
        checkStatusHandler.removeCallbacks(checkStatusRunnable);
        
        tvVerifiedStatus.setVisibility(View.VISIBLE);
        btnGoToLogin.setVisibility(View.VISIBLE);
        btnResendEmail.setVisibility(View.GONE);
        
        tvVerifyMessage.setText("✓ Your email has been successfully verified! Redirecting to dashboard...");
        tvVerifyMessage.setTextColor(ContextCompat.getColor(this, R.color.green_success));
        
        Toast.makeText(this, "🎉 Email Verified Successfully!", Toast.LENGTH_SHORT).show();
        
        // Update button text
        btnGoToLogin.setText("Go to Dashboard");
        
        android.util.Log.d("VerifyEmail", "Email verified! Redirecting in 1 second...");
        
        // Auto-redirect after 1 second
        new Handler().postDelayed(() -> {
            android.util.Log.d("VerifyEmail", "Auto-redirecting to dashboard");
            if (!isFinishing()) {
                goToDashboard();
            }
        }, 1000);
    }

    private void goToDashboard() {
        android.util.Log.d("VerifyEmail", "Opening UserHomeActivity");
        try {
            Intent intent = new Intent(VerifyEmailActivity.this, UserHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        } catch (Exception e) {
            android.util.Log.e("VerifyEmail", "Error opening dashboard: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void goToLogin() {
        try {
            mAuth.signOut(); // Ensure fresh login state
            Intent intent = new Intent(VerifyEmailActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        } catch (Exception e) {
            android.util.Log.e("VerifyEmail", "Error going to login: " + e.getMessage());
        }
    }

    private void resendEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            android.util.Log.e("VerifyEmail", "No user session - cannot resend email");
            tvVerifyMessage.setText("⚠️ Session expired. Please click 'Go to Login' to sign in again.");
            tvVerifyMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            return;
        }

        android.util.Log.d("VerifyEmail", "Resending verification email...");
        Toast.makeText(this, "Sending verification email...", Toast.LENGTH_SHORT).show();
        
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("VerifyEmail", "✓ Verification email resent successfully");
                        Toast.makeText(this, "✓ Verification email resent! Check your inbox and spam folder.", Toast.LENGTH_LONG).show();
                        tvVerifyMessage.setText("Verification email resent! Please check your inbox (including spam folder) and click the verification link.\n\n When done, click 'Check Again' below.");
                        tvVerifyMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                        btnGoToLogin.setText("Check Again");
                    } else {
                        Exception exception = task.getException();
                        String errorMsg = exception != null ? exception.getMessage() : "Unknown error";
                        android.util.Log.e("VerifyEmail", "Failed to resend email: " + errorMsg);
                        android.util.Log.e("VerifyEmail", "Exception: " + exception);
                        
                        tvVerifyMessage.setText("⚠️ Failed to send verification email:\n\n" + errorMsg + 
                                "\n\nPlease:\n• Wait a moment and try again\n• Or click 'Go to Login' to sign in and try again");
                        tvVerifyMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                        
                        Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
