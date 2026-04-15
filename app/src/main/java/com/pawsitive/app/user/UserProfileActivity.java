package com.pawsitive.app.user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pawsitive.app.R;
import com.pawsitive.app.VerifyEmailActivity;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private EditText etFullName, etPhone, etAbout;
    private TextView tvLocation, tvStatusMessage;
    private ImageView ivProfilePhoto;
    private Button btnSave, btnUseLocation;
    private ProgressBar progressBar;
    private ScrollView scrollView;

    private Uri imageUri;
    private String email, password;
    private Double latitude = null;
    private Double longitude = null;
    private String locationAddress = null;

    private NetworkManager networkManager;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    private FusedLocationProviderClient fusedLocationClient;

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

        networkManager = new NetworkManager(this);
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etFullName = findViewById(R.id.etUserFullName);
        etPhone = findViewById(R.id.etUserPhone);
        etAbout = findViewById(R.id.etUserAbout);
        tvLocation = findViewById(R.id.tvLocation);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnSave = findViewById(R.id.btnSaveUserProfile);
        btnUseLocation = findViewById(R.id.btnUseLocation);
        progressBar = findViewById(R.id.progressUserProfile);
        scrollView = findViewById(R.id.userProfileScrollView);

        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        findViewById(R.id.btnChangePhoto).setOnClickListener(view -> pickImage.launch("image/*"));
        btnUseLocation.setOnClickListener(view -> checkLocationPermission());
        btnSave.setOnClickListener(view -> performSignup());
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        btnUseLocation.setEnabled(false);
        tvLocation.setText("Fetching fresh location...");

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(this, location -> {
                    btnUseLocation.setEnabled(true);
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        getAddressFromLocation(latitude, longitude);
                    } else {
                        tvLocation.setText("Location not found. Ensure GPS is on.");
                    }
                })
                .addOnFailureListener(e -> {
                    btnUseLocation.setEnabled(true);
                    tvLocation.setText("Error: " + e.getMessage());
                });
    }

    private void getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                locationAddress = address.getAddressLine(0);
                tvLocation.setText("Location: " + locationAddress);
            } else {
                tvLocation.setText("Address not found.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
            tvLocation.setText("Error fetching address.");
        }
    }

    private void performSignup() {
        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        updateStatus("Finalizing registration...", R.color.brown_primary);

        ensureFirebaseUserSignedIn(() -> {
            if (imageUri != null) {
                uploadProfilePhoto(name, phone);
            } else {
                registerUserViaAPI(name, phone, null);
            }
        });
    }

    /**
     * Storage rules typically require request.auth != null.
     * Make sure the app is authenticated with Firebase before attempting any Storage upload.
     */
    private void ensureFirebaseUserSignedIn(@NonNull Runnable onReady) {
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            onReady.run();
            return;
        }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            handleError("Missing credentials. Please go back and try again.");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        onReady.run();
                    } else {
                        // If the account already exists, createUser... fails; fall back to sign-in.
                        auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, signInTask -> {
                                    if (signInTask.isSuccessful() && auth.getCurrentUser() != null) {
                                        onReady.run();
                                    } else {
                                        String msg = (signInTask.getException() != null)
                                                ? signInTask.getException().getMessage()
                                                : "Authentication failed";
                                        handleError("Auth Error: " + msg);
                                    }
                                });
                    }
                });
    }

    private void uploadProfilePhoto(String name, String phone) {
        StorageReference profileRef = storage.getReference().child("profiles/" + UUID.randomUUID().toString() + ".jpg");
        profileRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return profileRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> registerUserViaAPI(name, phone, downloadUri.toString()))
                .addOnFailureListener(e -> handleError("Upload Error: " + e.getMessage()));
    }

    private void registerUserViaAPI(String name, String phone, String profilePhotoUrl) {
        ApiService.UserRegistrationRequest request = new ApiService.UserRegistrationRequest();
        request.email = email;
        request.password = password;
        request.full_name = name;
        request.phone = phone;
        request.description = etAbout.getText().toString().trim();
        request.profile_photo_url = profilePhotoUrl;
        request.latitude = latitude;
        request.longitude = longitude;
        request.location_address = locationAddress;

        networkManager.registerUser(request, new NetworkManager.ApiCallback<ApiService.RegisterResponse>() {
            @Override
            public void onSuccess(ApiService.RegisterResponse response) {
                updateStatus("✓ Registration successful! Check your email for verification.", R.color.green_success);
                
                tvStatusMessage.postDelayed(() -> {
                    if (!isFinishing()) {
                        Intent intent = new Intent(UserProfileActivity.this, VerifyEmailActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finishAffinity();
                    }
                }, 2500);
            }

            @Override
            public void onError(String error) {
                handleError("Registration failed: " + error);
            }
        });
    }

    private void updateStatus(String message, int colorRes) {
        runOnUiThread(() -> {
            tvStatusMessage.setText(message);
            tvStatusMessage.setTextColor(ContextCompat.getColor(this, colorRes));
            tvStatusMessage.setVisibility(View.VISIBLE);
            if (scrollView != null) scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void handleError(String message) {
        runOnUiThread(() -> {
            setLoading(false);
            updateStatus(message, R.color.red_primary);
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
    }
}
