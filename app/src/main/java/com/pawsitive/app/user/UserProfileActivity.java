package com.pawsitive.app.user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pawsitive.app.MainActivity;
import com.pawsitive.app.R;
import com.pawsitive.app.util.FirestoreHelper;

import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private EditText etFullName, etPhone, etAbout;
    private TextView tvLocation;
    private ImageView ivProfilePhoto;
    private Button btnChangePhoto, btnUseLocation, btnSave;
    private ProgressBar progressBar;

    private Uri imageUri;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etFullName = findViewById(R.id.etUserFullName);
        etPhone = findViewById(R.id.etUserPhone);
        etAbout = findViewById(R.id.etUserAbout);
        tvLocation = findViewById(R.id.tvLocation);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnUseLocation = findViewById(R.id.btnUseLocation);
        btnSave = findViewById(R.id.btnSaveUserProfile);
        progressBar = findViewById(R.id.progressUserProfile);

        String fullName = getIntent().getStringExtra("full_name");
        if (fullName != null) {
            etFullName.setText(fullName);
        }

        btnChangePhoto.setOnClickListener(v -> pickImage.launch("image/*"));

        btnUseLocation.setOnClickListener(v -> getCurrentLocation());

        btnSave.setOnClickListener(v -> saveUserProfile());
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                tvLocation.setText("Location: " + latitude + ", " + longitude);
            } else {
                Toast.makeText(UserProfileActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveUserProfile() {
        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String about = etAbout.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (uid.isEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            return;
        }

        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.uploadProfileImage(uid, imageUri, false, new FirestoreHelper.UploadCallback() {
            @Override
            public void onSuccess(String url) {
                Map<String, Object> user = new HashMap<>();
                user.put("uid", uid);
                user.put("full_name", name);
                user.put("email", auth.getCurrentUser().getEmail());
                user.put("phone", phone);
                user.put("description", about);
                user.put("latitude", latitude);
                user.put("longitude", longitude);
                user.put("profile_photo_url", url);
                user.put("role", "USER");
                user.put("created_at", Timestamp.now());

                db.collection("users").document(uid).set(user)
                        .addOnSuccessListener(aVoid -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(UserProfileActivity.this, "Profile Saved", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(UserProfileActivity.this, MainActivity.class)); // Go to Home
                            finishAffinity();
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(UserProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(UserProfileActivity.this, "Upload failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
