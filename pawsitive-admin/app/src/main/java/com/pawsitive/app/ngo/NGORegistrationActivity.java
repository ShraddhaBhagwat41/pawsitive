}
                tvNgoStatusMessage.setText("✓ Thank you for registering!\n\n Status: PENDING\n\nAdmin will review your documents and send you login credentials via email.\n\nYou can close this app and check your email for updates.");
                btnSendForVerification.setEnabled(true);
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.auth.FirebaseAuth;
import com.pawsitive.app.R;
import com.pawsitive.app.HomeActivity;
import com.pawsitive.app.VerifyEmailActivity;
import com.pawsitive.app.network.NetworkManager;
import com.pawsitive.app.network.ApiService;

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

    private NetworkManager networkManager;
    private FirebaseStorage storage;

    private ActivityResultLauncher<String> profilePhotoPicker;
    private ActivityResultLauncher<String> certificatePicker;

    private String email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo_registration);

        // Initialize NetworkManager and Firebase Storage
        networkManager = new NetworkManager(this);
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
        String description = etNgoDescription.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || license.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email == null || password == null) {
            Toast.makeText(this, "Missing signup credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        progressNgo.setVisibility(View.VISIBLE);
        tvNgoStatusMessage.setVisibility(View.VISIBLE);
        tvNgoStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.brown_primary));
        tvNgoStatusMessage.setText("Uploading files...");
        btnSendForVerification.setEnabled(false);
        scrollToBottom();

        // Upload files and then register via API
        startUploadThenRegister(name, phone, address, license, description);
    }

    private void startUploadThenRegister(String name, String phone, String address, String license, String description) {
        if (profilePhotoUri != null) {
            uploadFileToStorage("ngo_profiles/" + System.currentTimeMillis() + "/profile.jpg",
                    profilePhotoUri,
                    new StorageCallback() {
                        @Override
                        public void onSuccess(String profileUrl) {
                            runOnUiThread(() -> {
                                tvNgoStatusMessage.setText("Profile photo uploaded! Uploading certificate...");
                                scrollToBottom();
                            });
                            uploadCertificateThenRegister(name, phone, address, license, description, profileUrl);
                        }

                        @Override
                        public void onFailure(String error) {
                            onUploadFailed(error);
                        }
                    });
        } else {
            runOnUiThread(() -> tvNgoStatusMessage.setText("Uploading certificate..."));
            uploadCertificateThenRegister(name, phone, address, license, description, null);
        }
    }

    private void uploadCertificateThenRegister(String name, String phone, String address, String license, String description, String profilePhotoUrl) {
        if (certificateUri != null) {
            uploadFileToStorage("ngo_profiles/" + System.currentTimeMillis() + "/certificate",
                    certificateUri,
                    new StorageCallback() {
                        @Override
                        public void onSuccess(String certificateUrl) {
                            runOnUiThread(() -> {
                                tvNgoStatusMessage.setText("Files uploaded! Registering NGO...");
                                scrollToBottom();
                            });
                            registerNGOViaAPI(name, phone, address, license, description, profilePhotoUrl, certificateUrl);
                        }

                        @Override
                        public void onFailure(String error) {
                            onUploadFailed(error);
                        }
                    });
        } else {
            runOnUiThread(() -> {
                tvNgoStatusMessage.setText("Registering NGO...");
                scrollToBottom();
            });
            registerNGOViaAPI(name, phone, address, license, description, profilePhotoUrl, null);
        }
    }

    private void registerNGOViaAPI(String name, String phone, String address, String license, String description, String profilePhotoUrl, String certificateUrl) {
        // Create NGO registration request
        ApiService.NGORegistrationRequest request = new ApiService.NGORegistrationRequest();
        request.email = email;
        request.password = password;
        request.organization_name = name;
        request.phone = phone;
        request.address = address;
        request.license_number = license;
        request.description = description;
        request.profile_photo_url = profilePhotoUrl;
        request.certificate_url = certificateUrl;

        // DEBUG: Log the request
        android.util.Log.d("NGORegistration", "Registration Request: email=" + email + ", org=" + name + ", phone=" + phone);

        // Call REST API to register NGO
        networkManager.registerNGO(request, new NetworkManager.ApiCallback<ApiService.RegisterResponse>() {
            @Override
                tvNgoStatusMessage.setVisibility(View.VISIBLE);
            public void onSuccess(ApiService.RegisterResponse response) {
                tvNgoStatusMessage.setText("Sent for verification. You can check your status through login.");
                btnSendForVerification.setVisibility(View.GONE);
                
                tvNgoStatusMessage.setOnClickListener(v -> {
                    Intent intent = new Intent(NGORegistrationActivity.this, com.pawsitive.app.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finishAffinity();
                });
                
                tvNgoStatusMessage.setText("✓ Thank you for registering!\n\n Status: PENDING\n\nAdmin will review your documents and send you login credentials via email.\n\nYou can close this app and check your email for updates.");
                btnSendForVerification.setEnabled(true);
                scrollToBottom();
            }

            @Override
                try {
                    if (error != null && error.trim().startsWith("{")) {
                        org.json.JSONObject errObj = new org.json.JSONObject(error);
                        if (errObj.has("error")) {
                            error = errObj.getString("error");
                        }
                    }
                } catch (Exception e) {
                    // ignore parsing error
                }
            public void onError(String error) {
                android.util.Log.e("NGORegistration", "Registration error: " + error);
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

    private void callBackendRegister(String name, String phone, String address, String license, String description, String profileUrl, String certUrl) {
        ApiService.NGORegistrationRequest request = new ApiService.NGORegistrationRequest();
        request.email = email;
        request.password = password;
        request.organization_name = name;
        request.phone = phone;
        request.address = address;
        request.license_number = license;
        request.description = description;
        request.profile_photo_url = profileUrl;
        request.certificate_url = certUrl;

        networkManager.registerNGO(request, new NetworkManager.ApiCallback<ApiService.RegisterResponse>() {
            @Override
            public void onSuccess(ApiService.RegisterResponse response) {
                progressNgo.setVisibility(View.GONE);
                tvNgoStatusMessage.setVisibility(View.VISIBLE);
                tvNgoStatusMessage.setTextColor(ContextCompat.getColor(NGORegistrationActivity.this, R.color.green_success));
                tvNgoStatusMessage.setText("Sent for verification. You can check your status through login.");
                btnSendForVerification.setVisibility(View.GONE);
                
                tvNgoStatusMessage.setOnClickListener(v -> {
                    Intent intent = new Intent(NGORegistrationActivity.this, com.pawsitive.app.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finishAffinity();
                });
            }
    }
            @Override
            public void onError(String error) {
                try {
                    if (error != null && error.trim().startsWith("{")) {
                        org.json.JSONObject errObj = new org.json.JSONObject(error);
                        if (errObj.has("error")) {
                            error = errObj.getString("error");
                        }
                    }
                } catch (Exception e) {
                    // ignore parsing error
                }
                showError(error);
            }
        });
    }

    private void uploadFile(String path, Uri uri, OnUploadSuccess successListener) {
        StorageReference ref = storage.getReference().child(path);
        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri1 -> successListener.onSuccess(uri1.toString()))
                        .addOnFailureListener(e -> successListener.onFailure(e.getMessage())))
                .addOnFailureListener(e -> successListener.onFailure(e.getMessage()));
    }
}
}

