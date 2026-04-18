package com.pawsitive.app.ngo;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pawsitive.app.LoginActivity;
import com.pawsitive.app.R;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NGORegistrationActivity extends AppCompatActivity {

    private ImageView ivNgoProfilePhoto;
    private Button btnChangeNgoPhoto, btnUploadCertificate, btnSendForVerification;
    private EditText etNgoName, etNgoPhone, etNgoAddress, etNgoLicense, etNgoDescription;
    private TextView tvCertificateStatus, tvNgoStatusMessage, tvLoginRedirect;
    private ProgressBar progressNgo;
    private ScrollView scrollView;

    private Uri profilePhotoUri;
    private Uri certificateUri;

    private NetworkManager networkManager;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<String> profilePhotoPicker;
    private ActivityResultLauncher<String> certificatePicker;

    private String email;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo_registration);

        networkManager = new NetworkManager(this);
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        scrollView = findViewById(R.id.ngoRegistrationScrollView);
        ivNgoProfilePhoto = findViewById(R.id.ivNgoProfilePhoto);
        btnChangeNgoPhoto = findViewById(R.id.btnChangeNgoPhoto);
        etNgoName = findViewById(R.id.etNgoName);
        etNgoPhone = findViewById(R.id.etNgoPhone);
        etNgoAddress = findViewById(R.id.etNgoAddress);
        etNgoLicense = findViewById(R.id.etNgoLicense);
        etNgoDescription = findViewById(R.id.etNgoDescription);
        btnUploadCertificate = findViewById(R.id.btnUploadCertificate);
        tvCertificateStatus = findViewById(R.id.tvCertificateStatus);
        btnSendForVerification = findViewById(R.id.btnSendForVerification);
        tvNgoStatusMessage = findViewById(R.id.tvNgoStatusMessage);
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect);
        progressNgo = findViewById(R.id.progressNgo);

        profilePhotoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                profilePhotoUri = uri;
                ivNgoProfilePhoto.setImageURI(uri);
            }
        });

        certificatePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                certificateUri = uri;
                tvCertificateStatus.setText("Certificate selected");
            }
        });

        btnChangeNgoPhoto.setOnClickListener(v -> profilePhotoPicker.launch("image/*"));
        btnUploadCertificate.setOnClickListener(v -> certificatePicker.launch("*/*"));
        btnSendForVerification.setOnClickListener(v -> performRegistrationFlow());
        tvLoginRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(NGORegistrationActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void performRegistrationFlow() {
        final String name = etNgoName.getText().toString().trim();
        final String phone = etNgoPhone.getText().toString().trim();
        final String address = etNgoAddress.getText().toString().trim();
        final String license = etNgoLicense.getText().toString().trim();
        final String description = etNgoDescription.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || license.isEmpty()) {
            showError("Please fill all required fields");
            return;
        }
        if (email == null || password == null) {
            showError("Missing signup credentials. Please register again.");
            return;
        }

        setLoading(true);
        tvNgoStatusMessage.setText("Registering...");

        if (mAuth.getCurrentUser() != null && email.equalsIgnoreCase(mAuth.getCurrentUser().getEmail())) {
            proceedWithUploads(mAuth.getCurrentUser().getUid(), name, phone, address, license, description);
            return;
        }

        // Try sign-in first; if account does not exist, create it.
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        proceedWithUploads(authResult.getUser().getUid(), name, phone, address, license, description);
                    } else {
                        showError("Unable to continue registration");
                    }
                })
                .addOnFailureListener(e -> mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(createResult -> {
                            if (createResult.getUser() != null) {
                                proceedWithUploads(createResult.getUser().getUid(), name, phone, address, license, description);
                            } else {
                                showError("Unable to create account");
                            }
                        })
                        .addOnFailureListener(createError -> showError("Registration Error: " + createError.getMessage()))
                );
    }

    private void proceedWithUploads(String uid, String name, String phone, String address, String license, String description) {
        tvNgoStatusMessage.setText("Uploading documents...");
        if (profilePhotoUri != null) {
            uploadFile("ngo_docs/" + uid + "/profile.jpg", profilePhotoUri, profileUrl ->
                    uploadCertificate(uid, name, phone, address, license, description, profileUrl));
        } else {
            uploadCertificate(uid, name, phone, address, license, description, null);
        }
    }

    private void uploadCertificate(String uid, String name, String phone, String address, String license, String description, String profileUrl) {
        if (certificateUri != null) {
            uploadFile("ngo_docs/" + uid + "/certificate", certificateUri, certUrl ->
                    callBackendRegister(uid, name, phone, address, license, description, profileUrl, certUrl));
        } else {
            callBackendRegister(uid, name, phone, address, license, description, profileUrl, null);
        }
    }

    private void callBackendRegister(String uid, String name, String phone, String address, String license, String description, String profileUrl, String certUrl) {
        ApiService.NGORegistrationRequest request = new ApiService.NGORegistrationRequest();
        request.uid = uid;
        request.email = email;
        request.password = password;
        request.organization_name = name;
        request.phone = phone;
        request.address = address;
        request.license_number = license;
        request.description = description;
        request.profile_photo_url = profileUrl;
        request.certificate_url = certUrl;

        Double[] latLng = geocodeAddress(address);

        networkManager.registerNGO(request, new NetworkManager.ApiCallback<ApiService.RegisterResponse>() {
            @Override
            public void onSuccess(ApiService.RegisterResponse response) {
                saveNgoProfileToFirestore(uid, name, phone, address, profileUrl, certUrl, latLng[0], latLng[1], () -> {
                    setLoading(false);
                    tvNgoStatusMessage.setVisibility(View.VISIBLE);
                    tvNgoStatusMessage.setTextColor(ContextCompat.getColor(NGORegistrationActivity.this, R.color.green_success));
                    tvNgoStatusMessage.setText("Details sent for verification to admin. Check your status through login.");
                    tvLoginRedirect.setVisibility(View.VISIBLE);
                    btnSendForVerification.setText("SENT FOR VERIFICATION");
                    btnSendForVerification.setEnabled(false);
                    scrollToBottom();
                });
            }

            @Override
            public void onError(String error) {
                showError("Database Error: " + error);
            }
        });
    }

    private void uploadFile(String path, Uri uri, OnUploadSuccess successListener) {
        StorageReference ref = storage.getReference().child(path);
        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null ? task.getException() : new RuntimeException("Upload failed");
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> successListener.onSuccess(downloadUri.toString()))
                .addOnFailureListener(e -> showError("Upload Error: " + e.getMessage()));
    }

    private Double[] geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return new Double[]{null, null};
        }

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> results = geocoder.getFromLocationName(address, 1);
            if (results != null && !results.isEmpty()) {
                Address a = results.get(0);
                return new Double[]{a.getLatitude(), a.getLongitude()};
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // Geocoding is best-effort only.
        }
        return new Double[]{null, null};
    }

    private void saveNgoProfileToFirestore(String uid, String name, String phone, String address, String profileUrl, String certUrl,
                                           Double latitude, Double longitude, Runnable onComplete) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("organization_name", name);
        data.put("ngo_email", email);
        data.put("phone", phone);
        data.put("address", address);
        data.put("license_number", licenseSafe());
        data.put("verification_status", "PENDING");
        data.put("email_verified", false);
        if (profileUrl != null) data.put("profile_photo_url", profileUrl);
        if (certUrl != null) data.put("certificate_url", certUrl);
        if (latitude != null && longitude != null) {
            data.put("latitude", latitude);
            data.put("longitude", longitude);
        }

        db.collection("ngo_profiles")
                .document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> onComplete.run())
                .addOnFailureListener(e -> onComplete.run());
    }

    private String licenseSafe() {
        return etNgoLicense.getText() != null ? etNgoLicense.getText().toString().trim() : "";
    }

    private void showError(String message) {
        setLoading(false);
        tvNgoStatusMessage.setVisibility(View.VISIBLE);
        tvNgoStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.red_primary));
        tvNgoStatusMessage.setText(message);
        scrollToBottom();
    }

    private void setLoading(boolean isLoading) {
        progressNgo.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSendForVerification.setEnabled(!isLoading);
        tvNgoStatusMessage.setVisibility(View.VISIBLE);
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    interface OnUploadSuccess {
        void onSuccess(String url);
    }
}
