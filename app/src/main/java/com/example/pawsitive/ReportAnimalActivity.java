package com.example.pawsitive;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class ReportAnimalActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int MAX_IMAGES = 2;

    private ImageView ivBack, ivPhotoIcon;
    private Spinner spinnerAnimalType, spinnerCondition;
    private EditText etLocation, etDescription;
    private Button btnSubmitReport;

    private ArrayList<Uri> selectedImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_animal);

        // Initialize views
        ivBack = findViewById(R.id.ivBack);
        ivPhotoIcon = findViewById(R.id.ivPhotoIcon);
        spinnerAnimalType = findViewById(R.id.spinnerAnimalType);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);

        selectedImages = new ArrayList<>();

        // Setup spinners
        setupSpinners();

        // Back button click listener
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Photo icon click listener - show image picker options
        ivPhotoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImages.size() >= MAX_IMAGES) {
                    Toast.makeText(ReportAnimalActivity.this, "Maximum " + MAX_IMAGES + " images allowed",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                showImagePickerDialog();
            }
        });

        // Submit button click listener
        btnSubmitReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport();
            }
        });
    }

    private void setupSpinners() {
        // Animal Type spinner
        String[] animalTypes = { "Animal Type", "Dog", "Cat", "Bird", "Rabbit", "Other" };
        ArrayAdapter<String> animalAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, animalTypes);
        animalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnimalType.setAdapter(animalAdapter);

        // Condition spinner
        String[] conditions = { "Condition", "Injured", "Sick", "Abandoned", "Lost", "Abused", "Other" };
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, conditions);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(conditionAdapter);
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");
        String[] options = { "Take Photo", "Choose from Gallery" };

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Take photo
                checkCameraPermission();
            } else {
                // Choose from gallery
                checkStoragePermission();
            }
        });

        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, STORAGE_PERMISSION_CODE);
        } else {
            openGallery();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    selectedImages.add(imageUri);
                    Toast.makeText(this, "Image added (" + selectedImages.size() + "/" + MAX_IMAGES + ")",
                            Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                // TODO: Save bitmap and add to selectedImages
                Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitReport() {
        String animalType = spinnerAnimalType.getSelectedItem().toString();
        String condition = spinnerCondition.getSelectedItem().toString();
        String location = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validation
        if (animalType.equals("Animal Type")) {
            Toast.makeText(this, "Please select animal type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (condition.equals("Condition")) {
            Toast.makeText(this, "Please select condition", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(location)) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        // TODO: Submit data to server/database
        Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_LONG).show();

        // Navigate to MainActivity to show map with responders
        Intent intent = new Intent(ReportAnimalActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
