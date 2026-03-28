package com.pawsitive.app.ngo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pawsitive.app.R;
import com.pawsitive.app.VerifyEmailActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NGORegistrationActivity extends AppCompatActivity {

    private ImageView ivNgoProfilePhoto;
    private Button btnChangeNgoPhoto, btnUploadCertificate, btnSendForVerification;
    private EditText etNgoName, etNgoPhone, etNgoAddress, etNgoLicense, etNgoDescription;
    private TextView tvCertificateStatus, tvNgoStatusMessage;
    private ProgressBar progressNgo;
    private ScrollView scrollView;

    private Uri profilePhotoUri = null;
    private Uri certificateUri = null;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private ActivityResultLauncher<String> profilePhotoPicker;
    private ActivityResultLauncher<String> certificatePicker;

    private String email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo_registration);

        // Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Get credentials from previous activities
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        // Bind views
        scrollView = findViewById(R.id.ivNgoProfilePhoto).getRootView().findViewById(android.R.id.content).findViewWithTag("ngo_scroll"); // Attempt to find if tagged, otherwise get by class
        // Fallback for getting ScrollView
        View v = findViewById(R.id.ivNgoProfilePhoto);
        while (v != null && !(v instanceof ScrollView)) {
            v = (View) v.getParent();
        }
        scrollView = (ScrollView) v;

        ivNgoProfilePhoto = findViewById(R.id.ivNgoProfilePhoto);
        btnChangeNgoPhoto = findViewById(R.id.btnChangeNgoPhoto);
        etNgoName = findViewById(R.id.etNgoName);
        etNgoPhone = findViewById(R.id.etNgoPhone);
        etNgoAddress = findViewById(R.id.etNgoAddress);
        etNgoLicense = findViewById(R.id.etNgoLicense);
        btnUploadCertificate = findViewById(R.id.btnUploadCertificate);
        tvCertificateStatus = findViewById(R.id.tvCertificateStatus);
        etNgoDescription = findViewById(R.id.etNgoDescription);
        btnSendForVerification = findViewById(R.id.btnSendForVerification);
        tvNgoStatusMessage = findViewById(R.id.tvNgoStatusMessage);
        progressNgo = findViewById(R.id.progressNgo);

        // Register pickers
        profilePhotoPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        profilePhotoUri = uri;
                        ivNgoProfilePhoto.setImageURI(uri);
                    }
                }
        );

        certificatePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        certificateUri = uri;
                        tvCertificateStatus.setText("Certificate selected");
                    }
                }
        );

        // Click listeners
        btnChangeNgoPhoto.setOnClickListener(vClick ->
                profilePhotoPicker.launch("image/*"));

        btnUploadCertificate.setOnClickListener(vClick ->
                certificatePicker.launch("*/*")); // allow image/pdf

        btnSendForVerification.setOnClickListener(vClick -> performSignup());
    }

    private void performSignup() {
        String name = etNgoName.getText().toString().trim();
        String phone = etNgoPhone.getText().toString().trim();
        String address = etNgoAddress.getText().toString().trim();
        String license = etNgoLicense.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || license.isEmpty() || certificateUri == null) {
            Toast.makeText(this, "Please fill all required fields and upload certificate", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email == null || password == null) {
            Toast.makeText(this, "Missing signup credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        progressNgo.setVisibility(View.VISIBLE);
        tvNgoStatusMessage.setVisibility(View.VISIBLE);
        tvNgoStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.brown_primary));
        tvNgoStatusMessage.setText("Creating account...");
        btnSendForVerification.setEnabled(false);
        scrollToBottom();

        // Create Firebase account
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            runOnUiThread(() -> {
                                tvNgoStatusMessage.setText("Account created! Uploading files...");
                                scrollToBottom();
                            });
                            startUploadProcess(user.getUid());
                        }
                    } else {
                        progressNgo.setVisibility(View.GONE);
                        tvNgoStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.red_primary));
                        tvNgoStatusMessage.setText("Signup failed: " + task.getException().getMessage());
                        btnSendForVerification.setEnabled(true);
                    }
                });
    }

    private void startUploadProcess(String uid) {
        if (profilePhotoUri != null) {
            uploadFileToStorage("ngo_profiles/" + uid + "/profile.jpg",
                    profilePhotoUri,
                    new StorageCallback() {
                        @Override
                        public void onSuccess(String profileUrl) {
                            runOnUiThread(() -> {
                                tvNgoStatusMessage.setText("Profile photo uploaded! Uploading certificate...");
                                scrollToBottom();
                            });
                            uploadCertificateAndSave(uid, profileUrl);
                        }

                        @Override
                        public void onFailure(String error) {
                            onUploadFailed(error);
                        }
                    });
        } else {
            runOnUiThread(() -> tvNgoStatusMessage.setText("Uploading certificate..."));
            uploadCertificateAndSave(uid, null);
        }
    }

    private void uploadCertificateAndSave(String uid, String profilePhotoUrl) {
        String name = etNgoName.getText().toString().trim();
        String phone = etNgoPhone.getText().toString().trim();
        String address = etNgoAddress.getText().toString().trim();
        String license = etNgoLicense.getText().toString().trim();
        String description = etNgoDescription.getText().toString().trim();

        uploadFileToStorage("ngo_profiles/" + uid + "/certificate",
                certificateUri,
                new StorageCallback() {
                    @Override
                    public void onSuccess(String certificateUrl) {
                        runOnUiThread(() -> {
                            tvNgoStatusMessage.setText("Files uploaded! Saving NGO profile...");
                            scrollToBottom();
                        });
                        saveNgoProfile(uid, name, phone, address, license, description, profilePhotoUrl, certificateUrl);
                    }

                    @Override
                    public void onFailure(String error) {
                        onUploadFailed(error);
                    }
                });
    }

    private void uploadFileToStorage(String path, Uri fileUri, StorageCallback callback) {
        StorageReference ref = storage.getReference().child(path);
        ref.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void saveNgoProfile(String uid, String name, String phone, String address, String license, String description, String profilePhotoUrl, String certificateUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("organization_name", name);
        data.put("phone", phone);
        data.put("address", address);
        data.put("license_number", license);
        data.put("description", description);
        data.put("profile_photo_url", profilePhotoUrl);
        data.put("certificate_url", certificateUrl);
        data.put("verification_status", "PENDING");
        data.put("role", "NGO");

        firestore.collection("ngo_profiles").document(uid).set(data)
                .addOnSuccessListener(unused -> {
                    runOnUiThread(() -> {
                        tvNgoStatusMessage.setText("NGO profile saved! Sending verification email...");
                        scrollToBottom();
                    });
                    sendVerificationEmail();
                })
                .addOnFailureListener(e -> onUploadFailed(e.getMessage()));
    }

    private void sendVerificationEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        progressNgo.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            runOnUiThread(() -> {
                                tvNgoStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.green_success));
                                tvNgoStatusMessage.setText("Email is sent! Please verify your account to continue.");
                                scrollToBottom();
                            });
                            
                            tvNgoStatusMessage.postDelayed(() -> {
                                if (!isFinishing()) {
                                    Intent intent = new Intent(NGORegistrationActivity.this, VerifyEmailActivity.class);
                                    intent.putExtra("email", email);
                                    startActivity(intent);
                                    finishAffinity();
                                }
                            }, 4000);
                        } else {
                            tvNgoStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.red_primary));
                            tvNgoStatusMessage.setText("Failed to send verification email: " + task.getException().getMessage());
                            btnSendForVerification.setEnabled(true);
                        }
                    });
        }
    }

    private void onUploadFailed(String error) {
        runOnUiThread(() -> {
            progressNgo.setVisibility(View.GONE);
            tvNgoStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.red_primary));
            tvNgoStatusMessage.setText("Error: " + error);
            btnSendForVerification.setEnabled(true);
        });
    }

    private void scrollToBottom() {
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private interface StorageCallback {
        void onSuccess(String url);
        void onFailure(String error);
    }
}
