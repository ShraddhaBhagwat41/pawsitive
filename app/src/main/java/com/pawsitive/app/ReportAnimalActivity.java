package com.pawsitive.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ReportAnimalActivity extends AppCompatActivity {

    private static final String TAG = "ReportAnimalActivity";
    private static final int CAMERA_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 102;
    private static final int MAX_IMAGES = 3;

    private ImageView ivBack, ivPhotoIcon;
    private TextView tvImageCount;
    private Spinner spinnerAnimalType, spinnerCondition;
    private EditText etLocation, etDescription;
    private Button btnFetchLocation, btnSubmitReport;

    private final ArrayList<Uri> capturedImages = new ArrayList<>();
    private String currentPhotoPath;

    private FusedLocationProviderClient fusedLocationClient;
    private Location incidentLocation;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_animal);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ivBack = findViewById(R.id.ivBack);
        ivPhotoIcon = findViewById(R.id.ivPhotoIcon);
        tvImageCount = findViewById(R.id.tvImageCount);
        spinnerAnimalType = findViewById(R.id.spinnerAnimalType);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        btnFetchLocation = findViewById(R.id.btnFetchLocation);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);

        setupSpinners();

        ivBack.setOnClickListener(v -> finish());
        ivPhotoIcon.setOnClickListener(v -> {
            if (capturedImages.size() >= MAX_IMAGES) {
                Toast.makeText(this, "Max 3 images allowed", Toast.LENGTH_SHORT).show();
                return;
            }
            checkCameraPermission();
        });
        btnFetchLocation.setOnClickListener(v -> checkLocationPermissionAndFetch());
        btnSubmitReport.setOnClickListener(v -> validateAndSubmit());
    }

    private void setupSpinners() {
        String[] types = {"Select Animal Type", "Dog", "Cat", "Rabbit", "Other"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerAnimalType.setAdapter(typeAdapter);

        String[] conds = {"Select Condition", "Injured", "Sick", "Lost", "Critical"};
        ArrayAdapter<String> condAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, conds);
        spinnerCondition.setAdapter(condAdapter);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("INCIDENT_" + timeStamp + "_", ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            fetchGPSLocation();
        }
    }

    private void fetchGPSLocation() {
        btnFetchLocation.setEnabled(false);
        btnFetchLocation.setText("Fetching...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(this, location -> {
                    btnFetchLocation.setEnabled(true);
                    btnFetchLocation.setText("FETCH CURRENT LOCATION");
                    if (location != null) {
                        incidentLocation = location;
                        updateAddressUI(location);
                    } else {
                        Toast.makeText(this, "Location not found. Enable GPS.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateAddressUI(Location loc) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                etLocation.setText(addresses.get(0).getAddressLine(0));
            } else {
                etLocation.setText(loc.getLatitude() + ", " + loc.getLongitude());
            }
        } catch (IOException e) {
            etLocation.setText(loc.getLatitude() + ", " + loc.getLongitude());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            capturedImages.add(Uri.fromFile(new File(currentPhotoPath)));
            tvImageCount.setText("Images captured: " + capturedImages.size() + "/3");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) openCamera();
        } else if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) fetchGPSLocation();
        }
    }

    private void validateAndSubmit() {
        if (capturedImages.isEmpty()) {
            Toast.makeText(this, "Capture at least 1 image", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spinnerAnimalType.getSelectedItemPosition() == 0 || spinnerCondition.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select animal type and condition", Toast.LENGTH_SHORT).show();
            return;
        }
        if (incidentLocation == null) {
            Toast.makeText(this, "Please fetch location", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(etDescription.getText())) {
            etDescription.setError("Required");
            return;
        }
        uploadAndSave();
    }

    private void uploadAndSave() {
        btnSubmitReport.setEnabled(false);
        btnSubmitReport.setText("SUBMITTING...");
        List<String> urls = new ArrayList<>();
        uploadRecursive(0, urls);
    }

    private void uploadRecursive(int index, List<String> urls) {
        if (index >= capturedImages.size()) {
            saveToFirestore(urls);
            return;
        }
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("incidents/" + UUID.randomUUID().toString() + ".jpg");
        ref.putFile(capturedImages.get(index))
                .continueWithTask(task -> ref.getDownloadUrl())
                .addOnSuccessListener(uri -> {
                    urls.add(uri.toString());
                    uploadRecursive(index + 1, urls);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Upload failed. Saving without image.", Toast.LENGTH_SHORT).show();
                    saveToFirestore(urls);
                });
    }

    private void saveToFirestore(List<String> urls) {
        Map<String, Object> data = new HashMap<>();
        data.put("images", urls);
        data.put("animalType", spinnerAnimalType.getSelectedItem().toString());
        data.put("condition", spinnerCondition.getSelectedItem().toString());
        data.put("description", etDescription.getText().toString());
        data.put("timestamp", new Date());
        Map<String, Object> loc = new HashMap<>();
        loc.put("lat", incidentLocation.getLatitude());
        loc.put("lng", incidentLocation.getLongitude());
        loc.put("address", etLocation.getText().toString());
        data.put("location", loc);
        data.put("status", "PENDING");

        FirebaseFirestore.getInstance().collection("incidents").add(data).addOnSuccessListener(doc -> {
            Toast.makeText(this, "Reported successfully", Toast.LENGTH_SHORT).show();

            double lat = incidentLocation != null ? incidentLocation.getLatitude() : 0;
            double lng = incidentLocation != null ? incidentLocation.getLongitude() : 0;
            Log.d(TAG, "Starting IncidentMapActivity with lat=" + lat + ", lng=" + lng);

            // Navigate to the incident map screen that shows nearby NGOs.
            Intent intent = new Intent(this, IncidentMapActivity.class);
            // Match the keys expected by IncidentMapActivity ("lat" and "lng").
            intent.putExtra("lat", lat);
            intent.putExtra("lng", lng);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            btnSubmitReport.setEnabled(true);
            btnSubmitReport.setText("SUBMIT REPORT");
            Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
        });
    }
}
