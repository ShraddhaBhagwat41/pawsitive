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
import com.pawsitive.app.R;
import com.pawsitive.app.VerifyEmailActivity;
import com.pawsitive.app.util.FirestoreHelper;
import com.pawsitive.app.network.NetworkManager;
import com.pawsitive.app.network.ApiService;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.auth.FirebaseAuth;

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

    private NetworkManager networkManager;
    private FirebaseStorage storage;

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

        // Initialize NetworkManager and Firebase Storage
        networkManager = new NetworkManager(this);
        storage = FirebaseStorage.getInstance();

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

        // Validate email and password before proceeding
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Email is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password == null || password.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatusMessage.setVisibility(View.VISIBLE);
        updateStatus("Uploading profile photo...", R.color.brown_primary);

        // Upload profile photo if selected, then register via API
        if (imageUri != null) {
            uploadProfilePhoto();
        } else {
            registerUserViaAPI(name, phone, null);
        }
    }

    private void uploadProfilePhoto() {
        String path = "user_profiles/" + System.currentTimeMillis() + "/profile.jpg";
        StorageReference ref = storage.getReference().child(path);
        
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            updateStatus("Photo uploaded! Registering account...", R.color.brown_primary);
                            registerUserViaAPI(
                                    etFullName.getText().toString().trim(),
                                    etPhone.getText().toString().trim(),
                                    uri.toString()
                            );
                        })
                        .addOnFailureListener(e -> handleError("Photo upload failed: " + e.getMessage())))
                .addOnFailureListener(e -> handleError("Photo upload failed: " + e.getMessage()));
    }

    private void registerUserViaAPI(String name, String phone, String profilePhotoUrl) {
        // Create user registration request
        ApiService.UserRegistrationRequest request = new ApiService.UserRegistrationRequest();
        request.email = email;
        request.password = password;
        request.full_name = name;
        request.phone = phone;
        request.description = etAbout.getText().toString().trim();
        request.profile_photo_url = profilePhotoUrl;

        // DEBUG: Log the request data
        android.util.Log.d(TAG, "Registration Request: " + 
                "email=" + email + 
                ", full_name=" + name + 
                ", phone=" + phone + 
                ", description=" + request.description + 
                ", profile_photo_url=" + profilePhotoUrl);

        // Call REST API to register user
        networkManager.registerUser(request, new NetworkManager.ApiCallback<ApiService.RegisterResponse>() {
            @Override
            public void onSuccess(ApiService.RegisterResponse response) {
                updateStatus("✓ Registration successful! Sending verification email...", R.color.brown_primary);
                
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(signInTask -> {
                        if (signInTask.isSuccessful() && FirebaseAuth.getInstance().getCurrentUser() != null) {
                            FirebaseAuth.getInstance().getCurrentUser().sendEmailVerification();
                        }
                        updateStatus("✓ Verification email has been sent! Please check your inbox.", R.color.green_success);
                        tvStatusMessage.postDelayed(() -> {
                            if (!isFinishing()) {
                                Intent intent = new Intent(UserProfileActivity.this, VerifyEmailActivity.class);
                                intent.putExtra("email", email);
                                startActivity(intent);
                                finishAffinity();
                            }
                        }, 2000);
                    });
            }

            @Override
            public void onError(String error) {
                android.util.Log.e(TAG, "Registration error: " + error);
                handleError("Registration failed: " + error);
            }
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
