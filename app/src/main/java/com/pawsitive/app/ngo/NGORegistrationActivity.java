package com.pawsitive.app.ngo;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pawsitive.app.LoginActivity;
import com.pawsitive.app.R;
import com.pawsitive.app.network.NetworkManager;
import com.pawsitive.app.network.ApiService;

import java.util.UUID;

public class NGORegistrationActivity extends AppCompatActivity {

    private ImageView ivNgoProfilePhoto;
    private Button btnChangeNgoPhoto, btnUploadCertificate, btnSendForVerification;
    private EditText etNgoName, etNgoPhone, etNgoAddress, etNgoLicense, etNgoDescription;
    private TextView tvCertificateStatus, tvNgoStatusMessage, tvLoginRedirect;
    private ProgressBar progressNgo;
    private ScrollView scrollView;

    private Uri profilePhotoUri = null;
    private Uri certificateUri = null;

    private NetworkManager networkManager;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;

    private ActivityResultLauncher<String> profilePhotoPicker;
    private ActivityResultLauncher<String> certificatePicker;

    private String email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo_registration);

        networkManager = new NetworkManager(this);
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();

        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        scrollView = findViewById(R.id.ngoRegistrationScrollView);
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
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect);
        progressNgo = findViewById(R.id.progressNgo);

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
                        tvCertificateStatus.setText("Certificate selected ✓");
                    }
                }
        );

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
        String name = etNgoName.getText().toString().trim();
        String phone = etNgoPhone.getText().toString().trim();
        String address = etNgoAddress.getText().toString().trim();
        String license = etNgoLicense.getText().toString().trim();
        String description = etNgoDescription.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || license.isEmpty()) {
            showError("Please fill all required fields");
            return;
        }

        setLoading(true);
        tvNgoStatusMessage.setText("Creating account...");

        // STEP 1: Create the user in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        tvNgoStatusMessage.setText("Account created. Uploading documents...");
                        // STEP 2: Upload files
                        startFileUploads(uid, name, phone, address, license, description);
                    } else {
                        showError("Auth Error: " + (task.getException() != null ? task.getException().getMessage() : "Failed"));
                    }
                });
    }

    private void startFileUploads(String uid, String name, String phone, String address, String license, String description) {
        if (profilePhotoUri != null) {
            uploadFile("ngo_docs/" + uid + "/profile.jpg", profilePhotoUri, profileUrl -> {
                uploadCertificate(uid, name, phone, address, license, description, profileUrl);
            });
        } else {
            uploadCertificate(uid, name, phone, address, license, description, null);
        }
    }

    private void uploadCertificate(String uid, String name, String phone, String address, String license, String description, String profileUrl) {
        if (certificateUri != null) {
            uploadFile("ngo_docs/" + uid + "/certificate", certificateUri, certUrl -> {
                tvNgoStatusMessage.setText("Files uploaded. Finalizing registration...");
                callBackendRegister(name, phone, address, license, description, profileUrl, certUrl);
            });
        } else {
            callBackendRegister(name, phone, address, license, description, profileUrl, null);
        }
    }

    private void callBackendRegister(String name, String phone, String address, String license, String description, String profilePhotoUrl, String certificateUrl) {
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

        networkManager.registerNGO(request, new NetworkManager.ApiCallback<ApiService.RegisterResponse>() {
            @Override
            public void onSuccess(ApiService.RegisterResponse response) {
                setLoading(false);
                tvNgoStatusMessage.setVisibility(View.VISIBLE);
                tvNgoStatusMessage.setTextColor(ContextCompat.getColor(NGORegistrationActivity.this, R.color.green_success));
                tvNgoStatusMessage.setText("details sent for verification");
                
                tvLoginRedirect.setVisibility(View.VISIBLE);
                btnSendForVerification.setText("SENT FOR VERIFICATION");
                btnSendForVerification.setEnabled(false);
                scrollToBottom();
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
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> successListener.onSuccess(downloadUri.toString()))
                .addOnFailureListener(e -> showError("Upload Error: " + e.getMessage()));
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
        tvNgoStatusMessage.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    interface OnUploadSuccess {
        void onSuccess(String url);
    }
}
