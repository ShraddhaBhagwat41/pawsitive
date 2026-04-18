package com.pawsitive.app.ngo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pawsitive.app.R;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NGOPostAnimalActivity extends AppCompatActivity {

    private ImageView ivAnimalPreview;
    private EditText etAnimalType;
    private EditText etCondition;
    private EditText etDescription;
    private EditText etLocation;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private final ActivityResultLauncher<String> galleryPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedImageUri = uri;
                ivAnimalPreview.setImageURI(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo_post_animal);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("animals");

        ivAnimalPreview = findViewById(R.id.ivPostAnimalPreview);
        etAnimalType = findViewById(R.id.etPostAnimalType);
        etCondition = findViewById(R.id.etPostAnimalCondition);
        etDescription = findViewById(R.id.etPostAnimalDescription);
        etLocation = findViewById(R.id.etPostAnimalLocation);
        progressBar = findViewById(R.id.progressPostAnimal);

        Button btnUpload = findViewById(R.id.btnUploadAnimalImage);
        Button btnPost = findViewById(R.id.btnSubmitPostAnimal);
        ImageView ivBack = findViewById(R.id.ivPostAnimalBack);

        ivBack.setOnClickListener(v -> finish());
        btnUpload.setOnClickListener(v -> galleryPicker.launch("image/*"));
        btnPost.setOnClickListener(v -> submitAnimal());
    }

    private void submitAnimal() {
        String ngoId = FirebaseAuth.getInstance().getUid();
        if (ngoId == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        String animalType = etAnimalType.getText().toString().trim();
        String condition = etCondition.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (TextUtils.isEmpty(animalType) || TextUtils.isEmpty(condition) || TextUtils.isEmpty(description) || TextUtils.isEmpty(location)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);

        if (selectedImageUri != null) {
            uploadImageThenSave(ngoId, animalType, condition, description, location);
        } else {
            saveAnimal(ngoId, animalType, condition, description, location, "");
        }
    }

    private void uploadImageThenSave(String ngoId, String animalType, String condition, String description, String location) {
        String fileName = UUID.randomUUID() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        imageRef.putFile(selectedImageUri)
                .continueWithTask(task -> imageRef.getDownloadUrl())
                .addOnSuccessListener(uri -> saveAnimal(ngoId, animalType, condition, description, location, uri.toString()))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveAnimal(@NonNull String ngoId,
                            @NonNull String animalType,
                            @NonNull String condition,
                            @NonNull String description,
                            @NonNull String location,
                            @NonNull String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("image", imageUrl);
        data.put("animalType", animalType);
        data.put("condition", condition);
        data.put("description", description);
        data.put("location", location);
        data.put("postedBy", ngoId);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("animals").add(data)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Animal posted successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK, new Intent().putExtra("refreshAnimals", true));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Post failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

