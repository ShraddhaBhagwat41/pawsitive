package com.pawsitive.app.user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pawsitive.app.R;
import com.pawsitive.app.VerifyEmailActivity;
import com.pawsitive.app.util.FirestoreHelper;

import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private EditText etFullName, etPhone, etAbout;
    private TextView tvLocation, tvStatusMessage;
    private ImageView ivProfilePhoto;
    private Button btnSave;
    private ProgressBar progressBar;
    private ScrollView scrollView;

    private Uri imageUri;
    private String email, password;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            imageUri = uri;
            ivProfilePhoto.setImageURI(uri);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        etFullName = findViewById(R.id.etUserFullName);
        etPhone = findViewById(R.id.etUserPhone);
        etAbout = findViewById(R.id.etUserAbout);
        tvLocation = findViewById(R.id.tvLocation);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnSave = findViewById(R.id.btnSaveUserProfile);
        progressBar = findViewById(R.id.progressUserProfile);

        // Find ScrollView safely
        View v = findViewById(R.id.ivProfilePhoto);
        while (v != null && !(v instanceof ScrollView)) {
            v = (v.getParent() instanceof View) ? (View) v.getParent() : null;
        }
        scrollView = (ScrollView) v;

        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");
        String fullName = getIntent().getStringExtra("full_name");
        if (fullName != null) etFullName.setText(fullName);

        findViewById(R.id.btnChangePhoto).setOnClickListener(view -> pickImage.launch("image/*"));
        btnSave.setOnClickListener(view -> performSignup());
    }

    private void performSignup() {
        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatusMessage.setVisibility(View.VISIBLE);
        updateStatus("Creating account...", R.color.brown_primary);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            Log.d(TAG, "Auth created successfully");
                            updateStatus("Account created! Sending verification email...", R.color.brown_primary);
                            // Priority 1: Send verification email
                            sendEmailAndSaveData(user, name, phone);
                        }
                    } else {
                        handleError("Signup failed: " + task.getException().getMessage());
                    }
                });
    }

    private void sendEmailAndSaveData(FirebaseUser user, String name, String phone) {
        // Send email immediately
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Verification email sent");
                updateStatus("Email is sent! Please check your inbox.", R.color.green_success);
                
                // Priority 2: Save user data in background
                saveUserData(user.getUid(), name, phone);
                
                // Priority 3: Redirect to verification screen
                tvStatusMessage.postDelayed(() -> {
                    if (!isFinishing()) {
                        Intent intent = new Intent(UserProfileActivity.this, VerifyEmailActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finishAffinity();
                    }
                }, 4000);
            } else {
                handleError("Failed to send verification email: " + task.getException().getMessage());
            }
        });
    }

    private void saveUserData(String uid, String name, String phone) {
        FirestoreHelper helper = new FirestoreHelper();
        helper.uploadProfileImage(uid, imageUri, false, (url, error) -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", uid);
            userData.put("full_name", name);
            userData.put("email", email);
            userData.put("phone", phone);
            userData.put("description", etAbout.getText().toString().trim());
            userData.put("profile_photo_url", url);
            userData.put("role", "USER");
            userData.put("created_at", Timestamp.now());

            FirebaseFirestore.getInstance().collection("users").document(uid).set(userData)
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore save failed: " + e.getMessage()));
        });
    }

    private void updateStatus(String message, int colorRes) {
        runOnUiThread(() -> {
            tvStatusMessage.setText(message);
            tvStatusMessage.setTextColor(ContextCompat.getColor(this, colorRes));
            if (scrollView != null) scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void handleError(String message) {
        runOnUiThread(() -> {
            btnSave.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            updateStatus(message, R.color.red_primary);
        });
    }
}
