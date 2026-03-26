package com.pawsitive.app.ngo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pawsitive.app.R;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NGORegistrationActivity extends AppCompatActivity {

    private ImageView ivNgoProfilePhoto;
    private Button btnChangeNgoPhoto, btnUploadCertificate, btnSendForVerification;
    private EditText etNgoName, etNgoPhone, etNgoAddress, etNgoLicense, etNgoDescription;
    private TextView tvCertificateStatus;
    private ProgressBar progressNgo;

    private Uri profilePhotoUri = null;
    private Uri certificateUri = null;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private ProgressDialog progressDialog;

    private ActivityResultLauncher<String> profilePhotoPicker;
    private ActivityResultLauncher<String> certificatePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo_registration);

        // Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting application...");
        progressDialog.setCancelable(false);

        // Bind views
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
        btnChangeNgoPhoto.setOnClickListener(v ->
                profilePhotoPicker.launch("image/*"));

        btnUploadCertificate.setOnClickListener(v ->
                certificatePicker.launch("*/*")); // allow image/pdf

        btnSendForVerification.setOnClickListener(v -> submitApplication());
    }

    private void submitApplication() {
        String name = etNgoName.getText().toString().trim();
        String phone = etNgoPhone.getText().toString().trim();
        String address = etNgoAddress.getText().toString().trim();
        String license = etNgoLicense.getText().toString().trim();
        String description = etNgoDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etNgoName.setError("NGO name required");
            etNgoName.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            etNgoPhone.setError("Phone required");
            etNgoPhone.requestFocus();
            return;
        }
        if (address.isEmpty()) {
            etNgoAddress.setError("Address required");
            etNgoAddress.requestFocus();
            return;
        }
        if (license.isEmpty()) {
            etNgoLicense.setError("License number required");
            etNgoLicense.requestFocus();
            return;
        }
        if (certificateUri == null) {
            Toast.makeText(this, "Please upload your certificate", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in to submit.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        progressNgo.setVisibility(android.view.View.VISIBLE);
        btnSendForVerification.setEnabled(false);

        String uid = user.getUid();

        // First upload profile photo (if present), then certificate, then save Firestore
        if (profilePhotoUri != null) {
            uploadFileToStorage("ngo_profiles/" + uid + "/profile_" + UUID.randomUUID() + ".jpg",
                    profilePhotoUri,
                    new StorageCallback() {
                        @Override
                        public void onSuccess(String profileUrl) {
                            uploadCertificateAndSave(uid, name, phone, address, license, description, profileUrl);
                        }

                        @Override
                        public void onFailure(String error) {
                            onUploadFailed(error);
                        }
                    });
        } else {
            // No profile photo, just upload certificate
            uploadCertificateAndSave(uid, name, phone, address, license, description, null);
        }
    }

    private void uploadCertificateAndSave(String uid,
                                          String name,
                                          String phone,
                                          String address,
                                          String license,
                                          String description,
                                          String profilePhotoUrl) {
        uploadFileToStorage("ngo_profiles/" + uid + "/certificate_" + UUID.randomUUID(),
                certificateUri,
                new StorageCallback() {
                    @Override
                    public void onSuccess(String certificateUrl) {
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
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        ref.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        callback.onSuccess(uri.toString());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        callback.onFailure(e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    private void saveNgoProfile(String uid,
                                String name,
                                String phone,
                                String address,
                                String license,
                                String description,
                                String profilePhotoUrl,
                                String certificateUrl) {

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

        firestore.collection("ngo_profiles")
                .document(uid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    progressNgo.setVisibility(android.view.View.GONE);
                    btnSendForVerification.setEnabled(true);
                    Toast.makeText(NGORegistrationActivity.this,
                            "Your application has been sent for verification.",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    onUploadFailed(e.getMessage());
                });
    }

    private void onUploadFailed(String error) {
        progressDialog.dismiss();
        progressNgo.setVisibility(android.view.View.GONE);
        btnSendForVerification.setEnabled(true);
        Toast.makeText(this, "Failed to submit: " + error, Toast.LENGTH_LONG).show();
    }

    private interface StorageCallback {
        void onSuccess(String url);
        void onFailure(String error);
    }
}
