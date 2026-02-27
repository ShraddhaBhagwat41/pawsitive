package com.example.pawsitive;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PetProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_profile);

        // Get views
        ImageView ivBack = findViewById(R.id.ivBackProfile);
        ImageView ivFavorite = findViewById(R.id.ivProfileFavorite);
        ImageView ivPetImage = findViewById(R.id.ivPetProfileImage);
        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvBreedAge = findViewById(R.id.tvProfileBreedAge);
        TextView tvDistance = findViewById(R.id.tvProfileDistance);
        TextView tvDescription = findViewById(R.id.tvProfileDescription);
        Button btnAdopt = findViewById(R.id.btnAdopt);

        // Get data passed from PetAdapter
        String name = getIntent().getStringExtra("name");
        String breed = getIntent().getStringExtra("breed");
        String age = getIntent().getStringExtra("age");
        String description = getIntent().getStringExtra("description");
        String distance = getIntent().getStringExtra("distance");
        boolean isFavorite = getIntent().getBooleanExtra("isFavorite", false);
        int imageResId = getIntent().getIntExtra("imageResId", R.drawable.ic_pet_placeholder);

        // Set data to views
        tvName.setText(name);
        tvBreedAge.setText(breed + " • " + age);
        tvDistance.setText(distance);
        tvDescription.setText(description);
        ivPetImage.setImageResource(imageResId);

        // Set favorite icon
        ivFavorite.setImageResource(isFavorite ?
                R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        // Favorite toggle
        ivFavorite.setOnClickListener(v -> {
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        });

        // Back button
        ivBack.setOnClickListener(v -> finish());

        // Adopt button
        btnAdopt.setOnClickListener(v -> {
            Toast.makeText(this, "Adoption request sent for " + name, Toast.LENGTH_SHORT).show();
        });
    }
}